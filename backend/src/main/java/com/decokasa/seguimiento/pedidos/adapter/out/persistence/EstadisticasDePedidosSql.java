package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarEstadisticas;
import com.decokasa.seguimiento.pedidos.application.port.out.EstadisticasDePedidos;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agregados de ADR-0029 en SQL (nunca se descargan los pedidos para contarlos). Los filtros son fragmentos fijos con
 * parámetros: ningún dato de entrada se concatena. Zona de los períodos: America/Guayaquil (igual a Bogotá, UTC-5).
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class EstadisticasDePedidosSql implements EstadisticasDePedidos {
    private static final String ALCANCE = " p.pais_codigo in (:paises)"
            + " and (cast(:flujo as text) is null or p.tipo_flujo = cast(:flujo as text))";
    private static final String RETRASO = "(exists (select 1 from etapa_pedido ea where ea.pedido_id = p.id"
            + " and ea.estado = 'ACTUAL' and ea.vence_en < :ahora)"
            + " or (p.urgencia_nivel is not null and p.urgencia_vence_en < :ahora))";
    private static final String ZONA = "'America/Guayaquil'";

    private final NamedParameterJdbcTemplate sql;

    public EstadisticasDePedidosSql(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }

    private static MapSqlParameterSource alcance(Set<CodigoPais> paises, String flujo) {
        if (paises.isEmpty()) { throw new IllegalArgumentException("Sin países en el alcance"); }
        return new MapSqlParameterSource("paises", paises.stream().map(CodigoPais::name).toList())
                .addValue("flujo", flujo);
    }

    private static MapSqlParameterSource periodo(MapSqlParameterSource p, Instant inicio, Instant fin) {
        return p.addValue("inicio", Timestamp.from(inicio)).addValue("fin", Timestamp.from(fin));
    }

    @Override public ConsultarEstadisticas.Tarjetas tarjetas(Set<CodigoPais> paises, String flujo, Instant inicio,
            Instant fin, Instant ahora) {
        var p = periodo(alcance(paises, flujo), inicio, fin).addValue("ahora", Timestamp.from(ahora));
        return sql.queryForObject("select"
                + " count(*) filter (where p.creado_en >= :inicio and p.creado_en < :fin) as creados,"
                + " count(*) filter (where not p.finalizado) as en_curso,"
                + " count(*) filter (where p.finalizado and p.finalizado_en >= :inicio and p.finalizado_en < :fin) as finalizados,"
                + " count(*) filter (where not p.finalizado and " + RETRASO + ") as con_retraso,"
                + " percentile_cont(0.5) within group (order by extract(epoch from (p.finalizado_en - p.creado_en)))"
                + "   filter (where p.finalizado and p.finalizado_en >= :inicio and p.finalizado_en < :fin) as mediana"
                + " from pedido p where" + ALCANCE, p, (r, n) -> {
                    double mediana = r.getDouble("mediana");
                    Double valor = r.wasNull() ? null : mediana;
                    return new ConsultarEstadisticas.Tarjetas(r.getLong("creados"), r.getLong("en_curso"),
                            r.getLong("finalizados"), r.getLong("con_retraso"), valor, r.getLong("finalizados"));
                });
    }

    @Override public Map<Integer, Long> creadosPorAnio(Set<CodigoPais> paises, String flujo) {
        Map<Integer, Long> resultado = new LinkedHashMap<>();
        sql.query("select cast(extract(year from p.creado_en at time zone " + ZONA + ") as int) as anio, count(*) as n"
                + " from pedido p where" + ALCANCE + " group by 1 order by 1", alcance(paises, flujo),
                fila -> { resultado.put(fila.getInt("anio"), fila.getLong("n")); });
        return resultado;
    }

    @Override public Map<Integer, Long> creadosPorMes(Set<CodigoPais> paises, String flujo, int anio) {
        Map<Integer, Long> resultado = new LinkedHashMap<>();
        sql.query("select cast(extract(month from p.creado_en at time zone " + ZONA + ") as int) as mes, count(*) as n"
                + " from pedido p where" + ALCANCE
                + " and cast(extract(year from p.creado_en at time zone " + ZONA + ") as int) = :anio"
                + " group by 1 order by 1", alcance(paises, flujo).addValue("anio", anio),
                fila -> { resultado.put(fila.getInt("mes"), fila.getLong("n")); });
        return resultado;
    }

    @Override public TiemposPedidos tiempoPorPedido(Set<CodigoPais> paises, String flujo, Instant inicio, Instant fin,
            Instant ahora, int limite) {
        var p = periodo(alcance(paises, flujo), inicio, fin).addValue("ahora", Timestamp.from(ahora))
                .addValue("limite", limite);
        long[] total = {0};
        var lista = sql.query("select p.id, p.numero, p.tipo_flujo, p.pais_codigo, p.creado_en, p.finalizado_en,"
                + " p.finalizado, cast(extract(epoch from ((case when p.finalizado then p.finalizado_en else :ahora end)"
                + "  - p.creado_en)) as bigint) as segundos, count(*) over () as total"
                + " from pedido p where" + ALCANCE + " and p.creado_en >= :inicio and p.creado_en < :fin"
                + " order by segundos desc, p.numero limit :limite", p, (r, n) -> {
                    total[0] = r.getLong("total");
                    return new ConsultarEstadisticas.TiempoPedido(r.getObject("id", UUID.class), r.getString("numero"),
                            r.getString("tipo_flujo"), CodigoPais.valueOf(r.getString("pais_codigo")),
                            instante(r.getTimestamp("creado_en")), instante(r.getTimestamp("finalizado_en")),
                            r.getBoolean("finalizado"), Math.max(0, r.getLong("segundos")));
                });
        return new TiemposPedidos(lista, total[0]);
    }

    @Override public List<ConsultarEstadisticas.TiempoEtapa> tiempoPorEtapa(Set<CodigoPais> paises, String flujo,
            Instant inicio, Instant fin) {
        // Cada visita termina en la entrada siguiente del pedido o, si el pedido terminó, en su finalización. Las etapas
        // automáticas (Fin) no se miden.
        return sql.query("with visitas as ("
                + " select p.tipo_flujo, p.pais_codigo, p.flujo_id, en.etapa, en.entrada_en,"
                + "  coalesce(lead(en.entrada_en) over (partition by en.pedido_id order by en.entrada_en,"
                + "   (en.id = actual.entrada_id), en.id),"
                + "   case when p.finalizado then p.finalizado_en end) as salida"
                + " from entrada_etapa en join pedido p on p.id = en.pedido_id"
                + " left join etapa_pedido actual on actual.pedido_id = p.id and actual.numero = p.etapa_actual"
                + " where" + ALCANCE + ")"
                + " select v.tipo_flujo, v.pais_codigo, v.etapa, min(d.nombre) as nombre,"
                + "  percentile_cont(0.5) within group (order by extract(epoch from (v.salida - v.entrada_en)))"
                + "   filter (where v.salida is not null) as mediana,"
                + "  avg(extract(epoch from (v.salida - v.entrada_en))) filter (where v.salida is not null) as promedio,"
                + "  count(*) filter (where v.salida is not null) as terminadas,"
                + "  count(*) filter (where v.salida is null) as abiertas"
                + " from visitas v join definicion_etapa d on d.flujo_id = v.flujo_id and d.numero = v.etapa"
                + " where not d.automatica and v.entrada_en >= :inicio and v.entrada_en < :fin"
                + " group by v.tipo_flujo, v.pais_codigo, v.etapa order by v.tipo_flujo, v.pais_codigo, v.etapa",
                periodo(alcance(paises, flujo), inicio, fin), (r, n) -> {
                    double mediana = r.getDouble("mediana");
                    Double valor = r.wasNull() ? null : mediana;
                    double promedio = r.getDouble("promedio");
                    Double media = r.wasNull() ? null : promedio;
                    return new ConsultarEstadisticas.TiempoEtapa(r.getString("tipo_flujo"),
                            CodigoPais.valueOf(r.getString("pais_codigo")), r.getInt("etapa"), r.getString("nombre"),
                            valor, media, r.getLong("terminadas"), r.getLong("abiertas"));
                });
    }

    @Override public ConsultarEstadisticas.EstadoPedidos estado(Set<CodigoPais> paises, String flujo, Instant inicio,
            Instant fin, Instant ahora) {
        var p = periodo(alcance(paises, flujo), inicio, fin).addValue("ahora", Timestamp.from(ahora));
        return sql.queryForObject("select count(*) filter (where not p.finalizado) as en_curso,"
                + " count(*) filter (where p.finalizado) as finalizados,"
                + " count(*) filter (where not p.finalizado and " + RETRASO + ") as con_retraso,"
                + " count(*) filter (where not p.finalizado and p.urgencia_nivel is not null) as con_urgencia,"
                + " count(*) filter (where not p.finalizado and p.demora_reportada_en is not null"
                + "  and p.llegada_aduana_en is null) as con_demora"
                + " from pedido p where" + ALCANCE + " and p.creado_en >= :inicio and p.creado_en < :fin", p,
                (r, n) -> new ConsultarEstadisticas.EstadoPedidos(r.getLong("en_curso"), r.getLong("finalizados"),
                        r.getLong("con_retraso"), r.getLong("con_urgencia"), r.getLong("con_demora")));
    }

    @Override public List<ConsultarEstadisticas.RetrasoEtapa> retrasosPorEtapa(Set<CodigoPais> paises, String flujo,
            Instant ahora) {
        return sql.query("select p.tipo_flujo, e.numero, min(d.nombre) as nombre, count(*) as n,"
                + " count(*) filter (where e.vence_en < :ahora) as etapa_vencida,"
                + " count(*) filter (where p.urgencia_nivel is not null and p.urgencia_vence_en < :ahora) as urgencia_vencida"
                + " from pedido p join etapa_pedido e on e.pedido_id = p.id and e.estado = 'ACTUAL'"
                + " join definicion_etapa d on d.flujo_id = p.flujo_id and d.numero = e.numero"
                + " where" + ALCANCE + " and not p.finalizado and (e.vence_en < :ahora"
                + "  or (p.urgencia_nivel is not null and p.urgencia_vence_en < :ahora))"
                + " group by p.tipo_flujo, e.numero order by n desc, p.tipo_flujo, e.numero",
                alcance(paises, flujo).addValue("ahora", Timestamp.from(ahora)),
                (r, n) -> new ConsultarEstadisticas.RetrasoEtapa(r.getString("tipo_flujo"), r.getInt("numero"),
                        r.getString("nombre"), r.getLong("n"), r.getLong("etapa_vencida"), r.getLong("urgencia_vencida")));
    }

    @Override public List<FilaAlerta> alertas(Set<CodigoPais> paises, UUID usuarioId, Set<CodigoRol> roles,
            boolean todas, Instant ahora) {
        if (!todas && roles.isEmpty()) { return List.of(); }
        var p = alcance(paises, null).addValue("ahora", Timestamp.from(ahora)).addValue("todas", todas)
                .addValue("usuario", usuarioId)
                .addValue("roles", roles.isEmpty() ? List.of("-") : roles.stream().map(CodigoRol::valor).toList());
        // Mismo criterio que la bandeja (P-06): etapa actual asignada a la persona, o sin asignar y con su rol.
        return sql.query("select p.id, p.numero, p.nombre, p.pais_codigo, p.tipo_flujo, e.numero as etapa,"
                + " d.nombre as nombre_etapa, e.vence_en, e.motivo_retraso, p.urgencia_nivel, p.urgencia_vence_en,"
                + " p.demora_reportada_en, p.demora_nueva_fecha_llegada, p.demora_motivo, p.llegada_aduana_en,"
                + " u.nombre as asignado"
                + " from pedido p join etapa_pedido e on e.pedido_id = p.id and e.estado = 'ACTUAL'"
                + " join definicion_etapa d on d.flujo_id = p.flujo_id and d.numero = e.numero"
                + " left join usuario u on u.id = e.asignado_a"
                + " where not p.finalizado and" + ALCANCE
                + " and (cast(:todas as boolean) or (d.rol_codigo in (:roles)"
                + "  and (e.asignado_a is null or e.asignado_a = :usuario)))"
                + " and (e.vence_en < :ahora or (p.urgencia_nivel is not null and p.urgencia_vence_en < :ahora)"
                + "  or (p.demora_reportada_en is not null and p.llegada_aduana_en is null))", p,
                (r, n) -> new FilaAlerta(r.getObject("id", UUID.class), r.getString("numero"), r.getString("nombre"),
                        CodigoPais.valueOf(r.getString("pais_codigo")), r.getString("tipo_flujo"), r.getInt("etapa"),
                        r.getString("nombre_etapa"), instante(r.getTimestamp("vence_en")), r.getString("motivo_retraso"),
                        r.getString("urgencia_nivel") != null, instante(r.getTimestamp("urgencia_vence_en")),
                        instante(r.getTimestamp("demora_reportada_en")),
                        r.getObject("demora_nueva_fecha_llegada", java.time.LocalDate.class), r.getString("demora_motivo"),
                        instante(r.getTimestamp("llegada_aduana_en")), r.getString("asignado")));
    }

    private static Instant instante(Timestamp valor) { return valor == null ? null : valor.toInstant(); }
}
