package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CarpetaPedidoRepositorio;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code carpeta_pedido_drive} (V24, ADR-0027), con el patrón de {@code CarpetaUsuarioSqlRepositorio}: reclamo con
 * {@code FOR UPDATE SKIP LOCKED}, lease y actualizaciones condicionales al propietario vigente. El estado lee también la
 * reserva confirmada de la carpeta del pedido en {@code carpeta_drive} (clave {@code d/o/{UUID en base64url}}), que pudo
 * crear la copia de un documento del pedido.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class CarpetaPedidoSqlRepositorio implements CarpetaPedidoRepositorio {
    /** Misma clave que {@code CarpetasDrive.clavePedido}: base64url sin relleno de los 16 bytes del UUID. */
    private static final String CLAVE_CARPETA =
            "'d/o/' || rtrim(translate(encode(uuid_send(p.id), 'base64'), '+/', '-_'), '=')";
    private static final String ESTADO = """
            select p.id as pedido_id, p.pais_codigo, cp.estado, coalesce(cp.intentos, 0) as intentos, cp.ultimo_error,
              cp.programada_en, cp.creada_en, coalesce(cd.confirmada, false) as confirmada
            from pedido p
            left join carpeta_pedido_drive cp on cp.pedido_id = p.id
            left join carpeta_drive cd on cd.clave = """ + CLAVE_CARPETA + "\nwhere p.id = :id";
    private final NamedParameterJdbcTemplate sql;

    public CarpetaPedidoSqlRepositorio(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }

    @Override public void programar(UUID pedidoId, Origen origen, boolean reabrirFallida, Instant ahora) {
        sql.update("""
                insert into carpeta_pedido_drive (pedido_id, estado, origen, intentos, programada_en, actualizada_en)
                values (:id, 'PENDIENTE', :origen, 0, :ahora, :ahora)
                on conflict (pedido_id) do update set estado = 'PENDIENTE', intentos = 0, ultimo_error = null,
                  proximo_intento_en = null, origen = excluded.origen, programada_en = excluded.programada_en,
                  actualizada_en = excluded.actualizada_en
                where :reabrir and carpeta_pedido_drive.estado = 'FALLIDA'
                """, new MapSqlParameterSource("id", pedidoId).addValue("origen", origen.name())
                .addValue("ahora", Timestamp.from(ahora)).addValue("reabrir", reabrirFallida));
    }

    @Override public List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario) {
        if (limite < 1 || limite > 100 || !hasta.isAfter(ahora) || propietario == null) {
            throw new IllegalArgumentException("Reclamación inválida");
        }
        return sql.query("""
                with lote as (
                  select pedido_id from carpeta_pedido_drive where estado = 'PENDIENTE'
                  and (proximo_intento_en is null or proximo_intento_en <= :ahora)
                  and (reclamado_hasta is null or reclamado_hasta <= :ahora)
                  and (en_intento or intentos < 4)
                  order by programada_en, pedido_id limit :limite for update skip locked
                ) update carpeta_pedido_drive c set reclamado_por = :propietario, reclamado_hasta = :hasta,
                  intentos = intentos + case when en_intento then 0 else 1 end, en_intento = true, actualizada_en = :ahora
                from lote where c.pedido_id = lote.pedido_id returning c.pedido_id, c.intentos
                """, parametros(null, propietario, ahora).addValue("limite", limite).addValue("hasta", Timestamp.from(hasta)),
                (r, n) -> new Pendiente(r.getObject("pedido_id", UUID.class), r.getInt("intentos")));
    }

    @Override public boolean renovar(UUID pedidoId, UUID propietario, Instant ahora, Instant hasta) {
        if (!hasta.isAfter(ahora)) { throw new IllegalArgumentException("Lease inválido"); }
        return sql.update("""
                update carpeta_pedido_drive set reclamado_hasta = :hasta where pedido_id = :id
                and reclamado_por = :propietario and reclamado_hasta > :ahora and estado = 'PENDIENTE' and en_intento
                """, parametros(pedidoId, propietario, ahora).addValue("hasta", Timestamp.from(hasta))) == 1;
    }

    @Override public boolean confirmar(UUID pedidoId, UUID propietario, String driveId, Instant ahora) {
        if (driveId == null || !driveId.matches("[A-Za-z0-9_-]{1,200}")) {
            throw new IllegalArgumentException("Id Drive inválido");
        }
        return sql.update("""
                update carpeta_pedido_drive set estado = 'CREADA', drive_id = :drive, creada_en = :ahora,
                  ultimo_error = null, proximo_intento_en = null, reclamado_por = null, reclamado_hasta = null,
                  en_intento = false, actualizada_en = :ahora
                where pedido_id = :id and reclamado_por = :propietario and reclamado_hasta > :ahora
                  and estado = 'PENDIENTE'
                """, parametros(pedidoId, propietario, ahora).addValue("drive", driveId)) == 1;
    }

    @Override public boolean registrarFallo(UUID pedidoId, UUID propietario, Causa causa, Instant siguiente,
            boolean terminal, Instant ahora) {
        if (!terminal && siguiente == null) { throw new IllegalArgumentException("Falta próxima fecha"); }
        return sql.update("""
                update carpeta_pedido_drive set estado = :estado, ultimo_error = :causa, proximo_intento_en = :siguiente,
                  reclamado_por = null, reclamado_hasta = null, en_intento = false, actualizada_en = :ahora
                where pedido_id = :id and reclamado_por = :propietario and reclamado_hasta > :ahora
                  and estado = 'PENDIENTE'
                """, parametros(pedidoId, propietario, ahora).addValue("estado", terminal ? "FALLIDA" : "PENDIENTE")
                .addValue("causa", causa.name())
                .addValue("siguiente", siguiente == null ? null : Timestamp.from(siguiente))) == 1;
    }

    @Override public Optional<Estado> estado(UUID pedidoId) {
        return sql.query(ESTADO, new MapSqlParameterSource("id", pedidoId), (r, n) -> estado(r)).stream().findFirst();
    }

    private static Estado estado(ResultSet r) throws SQLException {
        String estado = r.getBoolean("confirmada") ? "CREADA"
                : r.getString("estado") == null ? "SIN_PROGRAMAR" : r.getString("estado");
        return new Estado(r.getObject("pedido_id", UUID.class), CodigoPais.valueOf(r.getString("pais_codigo")), estado,
                r.getInt("intentos"), r.getString("ultimo_error"), instante(r.getTimestamp("programada_en")),
                instante(r.getTimestamp("creada_en")));
    }

    private static Instant instante(Timestamp valor) { return valor == null ? null : valor.toInstant(); }

    private static MapSqlParameterSource parametros(UUID id, UUID propietario, Instant ahora) {
        return new MapSqlParameterSource("id", id).addValue("propietario", propietario)
                .addValue("ahora", Timestamp.from(ahora));
    }
}
