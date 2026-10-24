package com.decokasa.seguimiento.asistente.adapter.out.persistence;

import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;
import com.decokasa.seguimiento.asistente.domain.LimiteConsumo;
import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva atómica de ADR-0022 sobre PostgreSQL (V21). La comprobación y la suma se hacen sobre filas bloqueadas con
 * {@code FOR UPDATE} dentro de una sola transacción, siempre en el mismo orden (presupuesto del mes → uso del día → uso
 * del usuario; al cerrar, la reserva primero): dos consultas simultáneas se serializan y la segunda ve la reserva de la
 * primera. Sin cerrojos en memoria: sirve con varias instancias y sobrevive a reinicios.
 */
@Repository
public class PresupuestoIASql implements PresupuestoIA {

    private static final String CONFIGURACION = "select habilitada, motivo_deshabilitada, tokens_entrada_max, "
            + "tokens_salida_max, preguntas_por_conversacion, consultas_usuario_dia, en_curso_usuario, consultas_dia, "
            + "presupuesto_mes_micro_usd, espera_reserva_segundos, version from configuracion_ia where id = 1";

    private final NamedParameterJdbcTemplate sql;

    public PresupuestoIASql(DataSource fuente) {
        this.sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    @Transactional(readOnly = true)
    public LimitesIA limites() {
        return leerLimites("");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Tarifa> tarifaVigente(String modelo, LocalDate dia) {
        return sql.query("select " + TarifasIASql.COLUMNAS + " from precio_modelo_ia"
                + " where modelo = :modelo and vigente_desde <= :dia order by vigente_desde desc limit 1",
                new MapSqlParameterSource().addValue("modelo", modelo).addValue("dia", dia),
                (fila, n) -> TarifasIASql.tarifa(fila)).stream().findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public void comprobarDisponibilidad(UUID usuarioId, LocalDate dia) {
        LimitesIA limites = leerLimites("");
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("periodo", dia.withDayOfMonth(1))
                .addValue("dia", dia)
                .addValue("usuario", usuarioId);
        Map<String, Object> usuario = sql.queryForList("select consultas, en_curso from uso_ia_usuario_dia "
                + "where usuario_id = :usuario and dia = :dia", parametros).stream().findFirst().orElse(Map.of());
        if (usuario.containsKey("en_curso") && numero(usuario, "en_curso") >= limites.enCursoUsuario()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.EN_CURSO);
        }
        if (usuario.containsKey("consultas") && numero(usuario, "consultas") >= limites.consultasUsuarioDia()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.USUARIO_DIA);
        }
        List<Long> delDia = sql.queryForList("select consultas from uso_ia_dia where dia = :dia", parametros,
                Long.class);
        if (!delDia.isEmpty() && delDia.get(0) >= limites.consultasDia()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.DIA);
        }
        Map<String, Object> mes = sql.queryForList("select limite_micro_usd, consumido_micro_usd, reservado_micro_usd "
                + "from presupuesto_ia where periodo = :periodo", parametros).stream().findFirst().orElse(Map.of());
        if (!mes.isEmpty() && numero(mes, "consumido_micro_usd") + numero(mes, "reservado_micro_usd")
                >= numero(mes, "limite_micro_usd")) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.PRESUPUESTO);
        }
    }

    @Override
    @Transactional
    public Reserva reservar(UUID usuarioId, long maximoMicroUsd, LocalDate dia, Instant ahora) {
        // FOR SHARE: un apagado simultáneo espera a que termine esta reserva, y la siguiente ya lo ve.
        LimitesIA limites = leerLimites(" for share");
        if (!limites.habilitada()) {
            throw new IaNoDisponible();
        }
        LocalDate periodo = dia.withDayOfMonth(1);
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("periodo", periodo)
                .addValue("dia", dia)
                .addValue("usuario", usuarioId)
                .addValue("limite", limites.presupuestoMesMicroUsd());
        sql.update("insert into presupuesto_ia (periodo, limite_micro_usd) values (:periodo, :limite) "
                + "on conflict (periodo) do nothing", parametros);
        sql.update("insert into uso_ia_dia (dia) values (:dia) on conflict (dia) do nothing", parametros);
        sql.update("insert into uso_ia_usuario_dia (usuario_id, dia) values (:usuario, :dia) "
                + "on conflict (usuario_id, dia) do nothing", parametros);

        Map<String, Object> mes = sql.queryForMap("select limite_micro_usd, consumido_micro_usd, reservado_micro_usd "
                + "from presupuesto_ia where periodo = :periodo for update", parametros);
        long consultasDia = numero(sql.queryForMap("select consultas from uso_ia_dia where dia = :dia for update",
                parametros), "consultas");
        Map<String, Object> usuario = sql.queryForMap("select consultas, en_curso from uso_ia_usuario_dia "
                + "where usuario_id = :usuario and dia = :dia for update", parametros);

        if (numero(usuario, "en_curso") >= limites.enCursoUsuario()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.EN_CURSO);
        }
        if (numero(usuario, "consultas") >= limites.consultasUsuarioDia()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.USUARIO_DIA);
        }
        if (consultasDia >= limites.consultasDia()) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.DIA);
        }
        long ocupado = Math.addExact(Math.addExact(numero(mes, "consumido_micro_usd"),
                numero(mes, "reservado_micro_usd")), maximoMicroUsd);
        if (ocupado > numero(mes, "limite_micro_usd")) {
            throw new LimiteConsumo(LimiteConsumo.Motivo.PRESUPUESTO);
        }

        parametros.addValue("maximo", maximoMicroUsd);
        sql.update("update presupuesto_ia set reservado_micro_usd = reservado_micro_usd + :maximo "
                + "where periodo = :periodo", parametros);
        sql.update("update uso_ia_dia set consultas = consultas + 1 where dia = :dia", parametros);
        sql.update("update uso_ia_usuario_dia set consultas = consultas + 1, en_curso = en_curso + 1 "
                + "where usuario_id = :usuario and dia = :dia", parametros);
        Reserva reserva = new Reserva(UUID.randomUUID(), usuarioId, dia, periodo, maximoMicroUsd);
        sql.update("""
                insert into reserva_ia (id, usuario_id, dia, periodo, maximo_micro_usd, estado, creada_en)
                values (:id, :usuario, :dia, :periodo, :maximo, 'ABIERTA', :ahora)
                """, parametros.addValue("id", reserva.id()).addValue("ahora", Timestamp.from(ahora)));
        return reserva;
    }

    @Override
    @Transactional
    public void liquidar(Reserva reserva, long costoMicroUsd, Instant ahora) {
        cerrar(reserva, "LIQUIDADA", costoMicroUsd, false, ahora);
    }

    @Override
    @Transactional
    public void liberar(Reserva reserva, Instant ahora) {
        cerrar(reserva, "LIBERADA", 0, true, ahora);
    }

    @Override
    @Transactional
    public void darPorConsumida(Reserva reserva, Instant ahora) {
        cerrar(reserva, "CONSUMIDA", reserva.maximoMicroUsd(), false, ahora);
    }

    @Override
    @Transactional
    public int recuperarAbandonadas(Instant ahora) {
        Duration espera = leerLimites("").esperaReserva();
        List<Reserva> abandonadas = sql.query("""
                select id, usuario_id, dia, periodo, maximo_micro_usd
                  from reserva_ia
                 where estado = 'ABIERTA' and creada_en < :limite
                 order by creada_en
                 limit 100
                 for update skip locked
                """, Map.of("limite", Timestamp.from(ahora.minus(espera))),
                (fila, n) -> new Reserva(fila.getObject("id", UUID.class), fila.getObject("usuario_id", UUID.class),
                        fila.getObject("dia", LocalDate.class), fila.getObject("periodo", LocalDate.class),
                        fila.getLong("maximo_micro_usd")));
        abandonadas.forEach(reserva -> cerrar(reserva, "CONSUMIDA", reserva.maximoMicroUsd(), false, ahora));
        return abandonadas.size();
    }

    @Override
    @Transactional
    public void desactivar(String motivo, Instant ahora) {
        sql.update("""
                update configuracion_ia
                   set habilitada = false, motivo_deshabilitada = :motivo, actualizado_por = null,
                       actualizado_en = :ahora, version = version + 1
                 where id = 1 and habilitada
                """, new MapSqlParameterSource().addValue("motivo", motivo).addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    @Transactional
    public boolean cambiarHabilitada(boolean habilitada, String motivo, long version, UUID actualizadoPor,
            Instant ahora) {
        return sql.update("""
                update configuracion_ia
                   set habilitada = :habilitada, motivo_deshabilitada = :motivo, actualizado_por = :por,
                       actualizado_en = :ahora, version = version + 1
                 where id = 1 and version = :version
                """, new MapSqlParameterSource()
                .addValue("habilitada", habilitada)
                .addValue("motivo", motivo)
                .addValue("por", actualizadoPor)
                .addValue("ahora", Timestamp.from(ahora))
                .addValue("version", version)) == 1;
    }

    /**
     * Cierra una reserva abierta una sola vez: si la recuperación ya la dio por consumida, una liquidación tardía no
     * cambia nada. Mismo orden de bloqueo que la reserva, precedido por la fila de la reserva.
     */
    private void cerrar(Reserva reserva, String estado, long costoMicroUsd, boolean devolverConsulta, Instant ahora) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("id", reserva.id())
                .addValue("estado", estado)
                .addValue("costo", costoMicroUsd)
                .addValue("ahora", Timestamp.from(ahora))
                .addValue("maximo", reserva.maximoMicroUsd())
                .addValue("periodo", reserva.periodo())
                .addValue("dia", reserva.dia())
                .addValue("usuario", reserva.usuarioId())
                .addValue("devuelta", devolverConsulta ? 1 : 0);
        int filas = sql.update("""
                update reserva_ia set estado = :estado, costo_micro_usd = :costo, cerrada_en = :ahora
                 where id = :id and estado = 'ABIERTA'
                """, parametros);
        if (filas == 0) {
            return;
        }
        sql.update("""
                update presupuesto_ia
                   set reservado_micro_usd = greatest(reservado_micro_usd - :maximo, 0),
                       consumido_micro_usd = consumido_micro_usd + :costo
                 where periodo = :periodo
                """, parametros);
        if (devolverConsulta) {
            sql.update("update uso_ia_dia set consultas = greatest(consultas - 1, 0) where dia = :dia", parametros);
        }
        sql.update("""
                update uso_ia_usuario_dia
                   set en_curso = greatest(en_curso - 1, 0), consultas = greatest(consultas - :devuelta, 0)
                 where usuario_id = :usuario and dia = :dia
                """, parametros);
    }

    private LimitesIA leerLimites(String bloqueo) {
        return sql.queryForObject(CONFIGURACION + bloqueo, Map.of(), (fila, n) -> limites(fila));
    }

    private static LimitesIA limites(ResultSet fila) throws SQLException {
        return new LimitesIA(fila.getBoolean("habilitada"), fila.getString("motivo_deshabilitada"),
                fila.getInt("tokens_entrada_max"), fila.getInt("tokens_salida_max"),
                fila.getInt("preguntas_por_conversacion"), fila.getInt("consultas_usuario_dia"),
                fila.getInt("en_curso_usuario"), fila.getInt("consultas_dia"),
                fila.getLong("presupuesto_mes_micro_usd"), Duration.ofSeconds(fila.getInt("espera_reserva_segundos")),
                fila.getLong("version"));
    }

    private static long numero(Map<String, Object> fila, String columna) {
        return ((Number) fila.get(columna)).longValue();
    }
}
