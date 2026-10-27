package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CarpetaUsuarioRepositorio;
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
 * {@code carpeta_usuario_drive} (V23, ADR-0026), con el patrón de {@code CopiaAdjuntoSqlRepositorio}: reclamo con
 * {@code FOR UPDATE SKIP LOCKED}, lease y actualizaciones condicionales al propietario vigente. El estado lee también la
 * reserva confirmada de la carpeta del usuario en {@code carpeta_drive} (clave {@code d/u/{UUID en base64url}}), que
 * pudo crear la copia de un documento suyo. Lee la tabla usuario solo para listar a quién falta programar.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class CarpetaUsuarioSqlRepositorio implements CarpetaUsuarioRepositorio {
    /** Misma clave que {@code CarpetasDrive.claveUsuario}: base64url sin relleno de los 16 bytes del UUID. */
    private static final String CLAVE_CARPETA =
            "'d/u/' || rtrim(translate(encode(uuid_send(u.id), 'base64'), '+/', '-_'), '=')";
    private static final String ESTADOS = """
            select u.id as usuario_id, cu.estado, coalesce(cu.intentos, 0) as intentos, cu.ultimo_error,
              cu.programada_en, cu.creada_en, coalesce(cd.confirmada, false) as confirmada
            from usuario u
            left join carpeta_usuario_drive cu on cu.usuario_id = u.id
            left join carpeta_drive cd on cd.clave = """ + CLAVE_CARPETA + "\n";
    private final NamedParameterJdbcTemplate sql;

    public CarpetaUsuarioSqlRepositorio(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }

    @Override public void programar(UUID usuarioId, Origen origen, boolean reabrirFallida, Instant ahora) {
        sql.update("""
                insert into carpeta_usuario_drive (usuario_id, estado, origen, intentos, programada_en, actualizada_en)
                values (:id, 'PENDIENTE', :origen, 0, :ahora, :ahora)
                on conflict (usuario_id) do update set estado = 'PENDIENTE', intentos = 0, ultimo_error = null,
                  proximo_intento_en = null, origen = excluded.origen, programada_en = excluded.programada_en,
                  actualizada_en = excluded.actualizada_en
                where :reabrir and carpeta_usuario_drive.estado = 'FALLIDA'
                """, new MapSqlParameterSource("id", usuarioId).addValue("origen", origen.name())
                .addValue("ahora", Timestamp.from(ahora)).addValue("reabrir", reabrirFallida));
    }

    @Override public List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario) {
        if (limite < 1 || limite > 100 || !hasta.isAfter(ahora) || propietario == null) {
            throw new IllegalArgumentException("Reclamación inválida");
        }
        return sql.query("""
                with lote as (
                  select usuario_id from carpeta_usuario_drive where estado = 'PENDIENTE'
                  and (proximo_intento_en is null or proximo_intento_en <= :ahora)
                  and (reclamado_hasta is null or reclamado_hasta <= :ahora)
                  and (en_intento or intentos < 4)
                  order by programada_en, usuario_id limit :limite for update skip locked
                ) update carpeta_usuario_drive c set reclamado_por = :propietario, reclamado_hasta = :hasta,
                  intentos = intentos + case when en_intento then 0 else 1 end, en_intento = true, actualizada_en = :ahora
                from lote where c.usuario_id = lote.usuario_id returning c.usuario_id, c.intentos
                """, parametros(null, propietario, ahora).addValue("limite", limite).addValue("hasta", Timestamp.from(hasta)),
                (r, n) -> new Pendiente(r.getObject("usuario_id", UUID.class), r.getInt("intentos")));
    }

    @Override public boolean renovar(UUID usuarioId, UUID propietario, Instant ahora, Instant hasta) {
        if (!hasta.isAfter(ahora)) { throw new IllegalArgumentException("Lease inválido"); }
        return sql.update("""
                update carpeta_usuario_drive set reclamado_hasta = :hasta where usuario_id = :id
                and reclamado_por = :propietario and reclamado_hasta > :ahora and estado = 'PENDIENTE' and en_intento
                """, parametros(usuarioId, propietario, ahora).addValue("hasta", Timestamp.from(hasta))) == 1;
    }

    @Override public boolean confirmar(UUID usuarioId, UUID propietario, String driveId, Instant ahora) {
        if (driveId == null || !driveId.matches("[A-Za-z0-9_-]{1,200}")) {
            throw new IllegalArgumentException("Id Drive inválido");
        }
        return sql.update("""
                update carpeta_usuario_drive set estado = 'CREADA', drive_id = :drive, creada_en = :ahora,
                  ultimo_error = null, proximo_intento_en = null, reclamado_por = null, reclamado_hasta = null,
                  en_intento = false, actualizada_en = :ahora
                where usuario_id = :id and reclamado_por = :propietario and reclamado_hasta > :ahora
                  and estado = 'PENDIENTE'
                """, parametros(usuarioId, propietario, ahora).addValue("drive", driveId)) == 1;
    }

    @Override public boolean registrarFallo(UUID usuarioId, UUID propietario, Causa causa, Instant siguiente,
            boolean terminal, Instant ahora) {
        if (!terminal && siguiente == null) { throw new IllegalArgumentException("Falta próxima fecha"); }
        return sql.update("""
                update carpeta_usuario_drive set estado = :estado, ultimo_error = :causa, proximo_intento_en = :siguiente,
                  reclamado_por = null, reclamado_hasta = null, en_intento = false, actualizada_en = :ahora
                where usuario_id = :id and reclamado_por = :propietario and reclamado_hasta > :ahora
                  and estado = 'PENDIENTE'
                """, parametros(usuarioId, propietario, ahora).addValue("estado", terminal ? "FALLIDA" : "PENDIENTE")
                .addValue("causa", causa.name())
                .addValue("siguiente", siguiente == null ? null : Timestamp.from(siguiente))) == 1;
    }

    @Override public Optional<Estado> estado(UUID usuarioId) {
        return sql.query(ESTADOS + "where u.id = :id", new MapSqlParameterSource("id", usuarioId),
                (r, n) -> estado(r)).stream().findFirst();
    }

    @Override public List<Estado> estados() {
        return sql.query(ESTADOS + "order by u.nombre, u.id", new MapSqlParameterSource(), (r, n) -> estado(r));
    }

    private static Estado estado(ResultSet r) throws SQLException {
        String estado = r.getBoolean("confirmada") ? "CREADA"
                : r.getString("estado") == null ? "SIN_PROGRAMAR" : r.getString("estado");
        return new Estado(r.getObject("usuario_id", UUID.class), estado, r.getInt("intentos"),
                r.getString("ultimo_error"), instante(r.getTimestamp("programada_en")),
                instante(r.getTimestamp("creada_en")));
    }

    private static Instant instante(Timestamp valor) { return valor == null ? null : valor.toInstant(); }

    private static MapSqlParameterSource parametros(UUID id, UUID propietario, Instant ahora) {
        return new MapSqlParameterSource("id", id).addValue("propietario", propietario)
                .addValue("ahora", Timestamp.from(ahora));
    }
}
