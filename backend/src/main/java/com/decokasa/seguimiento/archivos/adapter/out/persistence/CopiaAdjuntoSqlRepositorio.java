package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** SQL acotado sobre adjunto; comparte DataSource/transacción JPA de la aplicación. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class CopiaAdjuntoSqlRepositorio implements CopiaAdjuntoRepositorio {
    private final NamedParameterJdbcTemplate sql;
    public CopiaAdjuntoSqlRepositorio(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }
    @Override public List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario) {
        if (limite < 1 || limite > 100 || !hasta.isAfter(ahora) || propietario == null) {
            throw new IllegalArgumentException("Reclamación inválida");
        }
        return sql.query("""
                with lote as (
                  select id from adjunto where estado_copia = 'PENDIENTE' and clave_local is not null
                  and (proximo_intento_en is null or proximo_intento_en <= :ahora)
                  and (reclamado_hasta is null or reclamado_hasta <= :ahora)
                  and (copia_en_intento or intentos < 4)
                  order by subido_en, id limit :limite for update skip locked
                ) update adjunto a set reclamado_por = :propietario, reclamado_hasta = :hasta,
                  intentos = intentos + case when copia_en_intento then 0 else 1 end,
                  copia_en_intento = true from lote where a.id = lote.id returning a.*
                """, Map.of("limite", limite, "ahora", Timestamp.from(ahora), "hasta", Timestamp.from(hasta),
                "propietario", propietario), (r, n) -> new Pendiente(r.getObject("id", UUID.class),
                r.getObject("pedido_id", UUID.class), r.getObject("entrada_id", UUID.class), r.getString("nombre"),
                r.getLong("tamano_bytes"), r.getString("sha256"), r.getString("tipo_detectado"),
                r.getString("clave_local"), r.getString("drive_id_reservado"), r.getInt("intentos"),
                r.getBytes("drive_sesion_cifrada"),r.getLong("drive_offset"),r.getString("drive_carpeta_id")));
    }
    private MapSqlParameterSource parametros(UUID id, UUID propietario, Instant ahora) {
        return new MapSqlParameterSource("id", id).addValue("propietario", propietario)
                .addValue("ahora", Timestamp.from(ahora));
    }
    @Override public boolean reservarId(UUID id, UUID propietario, String remoto, Instant ahora) {
        validarId(remoto);
        return sql.update("""
                update adjunto set drive_id_reservado = :remoto where id = :id and reclamado_por = :propietario
                and reclamado_hasta > :ahora and estado_copia = 'PENDIENTE'
                and (drive_id_reservado is null or drive_id_reservado = :remoto)
                """, parametros(id, propietario, ahora).addValue("remoto", remoto)) == 1;
    }
    @Override public boolean confirmar(UUID id, UUID propietario, String remoto, String enlace, Instant ahora) {
        validarId(remoto);
        if (enlace == null || !enlace.matches("https://drive\\.google\\.com/[^\\s]{1,2000}")) {
            throw new IllegalArgumentException("Enlace Drive inválido");
        }
        return sql.update("""
                update adjunto set drive_id = :remoto, drive_enlace = :enlace, estado_copia = 'COPIADO',
                  copiado_en = :ahora, ultimo_error = null, proximo_intento_en = null,
                  reclamado_por = null, reclamado_hasta = null, copia_en_intento = false,
                  drive_sesion_cifrada = null, drive_offset = tamano_bytes where id = :id and reclamado_por = :propietario
                  and reclamado_hasta > :ahora and estado_copia = 'PENDIENTE' and drive_id_reservado = :remoto
                """, parametros(id, propietario, ahora).addValue("remoto", remoto).addValue("enlace", enlace)) == 1;
    }
    @Override public boolean registrarFallo(UUID id, UUID propietario, Causa causa, Instant siguiente,
            boolean terminal, Instant ahora) {
        if (!terminal && siguiente == null) { throw new IllegalArgumentException("Falta próxima fecha"); }
        return sql.update("""
                update adjunto set estado_copia = :estado, ultimo_error = :causa, proximo_intento_en = :siguiente,
                  reclamado_por = null, reclamado_hasta = null, copia_en_intento = false
                where id = :id and reclamado_por = :propietario
                  and reclamado_hasta > :ahora and estado_copia = 'PENDIENTE'
                """, parametros(id, propietario, ahora).addValue("estado", terminal ? "FALLIDO" : "PENDIENTE")
                .addValue("causa", causa.name()).addValue("siguiente", siguiente == null ? null : Timestamp.from(siguiente))) == 1;
    }
    @Override public boolean renovar(UUID id, UUID propietario, Instant ahora, Instant hasta) {
        if (!hasta.isAfter(ahora)) { throw new IllegalArgumentException("Lease inválido"); }
        return sql.update("""
                update adjunto set reclamado_hasta=:hasta where id=:id and reclamado_por=:propietario
                and reclamado_hasta>:ahora and estado_copia='PENDIENTE' and copia_en_intento
                """,parametros(id,propietario,ahora).addValue("hasta",Timestamp.from(hasta)))==1;
    }
    @Override public boolean sesion(UUID id, UUID propietario, String carpeta, byte[] cifrada, long offset, Instant ahora) {
        validarId(carpeta);
        return sql.update("""
                update adjunto set drive_carpeta_id=:carpeta,drive_sesion_cifrada=:sesion,drive_offset=:offset
                where id=:id and reclamado_por=:propietario and reclamado_hasta>:ahora and estado_copia='PENDIENTE'
                and drive_id_reservado is not null and :offset between 0 and tamano_bytes
                and (drive_carpeta_id is null or drive_carpeta_id=:carpeta)
                """,parametros(id,propietario,ahora).addValue("carpeta",carpeta)
                .addValue("sesion",cifrada,java.sql.Types.BINARY).addValue("offset",offset))==1;
    }
    @Override public boolean avanzar(UUID id, UUID propietario, long offset, Instant ahora) {
        return sql.update("""
                update adjunto set drive_offset=:offset where id=:id and reclamado_por=:propietario
                and reclamado_hasta>:ahora and estado_copia='PENDIENTE' and drive_sesion_cifrada is not null
                and :offset between 0 and tamano_bytes
                """,parametros(id,propietario,ahora).addValue("offset",offset))==1;
    }
    @Override public boolean ceder(UUID id, UUID propietario, Instant ahora) {
        // Contención de filesystem: no reinicia el intento ni consume un reintento.
        return sql.update("""
                update adjunto set reclamado_por=null,reclamado_hasta=null,
                proximo_intento_en=:siguiente where id=:id
                and reclamado_por=:propietario and reclamado_hasta>:ahora and estado_copia='PENDIENTE'
                """,parametros(id,propietario,ahora)
                    .addValue("siguiente",Timestamp.from(ahora.plusSeconds(30))))==1;
    }
    private static void validarId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,200}")) { throw new IllegalArgumentException("Id Drive inválido"); }
    }
}
