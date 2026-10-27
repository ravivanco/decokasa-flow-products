package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Solicitudes de recuperación en SQL (ADR-0033, V28). Usa la transacción del llamador; nunca registra el hash. */
@Repository
public class RecuperacionSql implements RecuperacionRepositorio {

    private final NamedParameterJdbcTemplate sql;

    public RecuperacionSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    private static OffsetDateTime t(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    @Override
    public int contarDesde(UUID usuarioId, Instant desde) {
        Integer total = sql.queryForObject(
                "select count(*) from recuperacion_contrasena where usuario_id = :u and creada_en >= :desde",
                new MapSqlParameterSource("u", usuarioId).addValue("desde", t(desde)), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public void registrar(UUID id, UUID usuarioId, Instant creadaEn, Instant expiraEn) {
        sql.update("insert into recuperacion_contrasena (id, usuario_id, creada_en, expira_en)"
                        + " values (:id, :u, :creada, :expira)",
                new MapSqlParameterSource("id", id).addValue("u", usuarioId).addValue("creada", t(creadaEn))
                        .addValue("expira", t(expiraEn)));
    }

    @Override
    public Optional<Vigente> bloquearVigente(String tokenHash, Instant ahora) {
        List<Vigente> filas = sql.query("select id, usuario_id from recuperacion_contrasena"
                        + " where token_hash = :h and estado in ('ENVIANDO', 'ENVIADA') and expira_en > :ahora"
                        + " for update",
                new MapSqlParameterSource("h", tokenHash).addValue("ahora", t(ahora)),
                (f, n) -> new Vigente(f.getObject("id", UUID.class), f.getObject("usuario_id", UUID.class)));
        return filas.stream().findFirst();
    }

    @Override
    public void marcarUsada(UUID id, Instant ahora) {
        sql.update("update recuperacion_contrasena set estado = 'USADA', usada_en = :ahora where id = :id",
                new MapSqlParameterSource("id", id).addValue("ahora", t(ahora)));
    }

    @Override
    public int anularDeUsuario(UUID usuarioId, Instant ahora) {
        return sql.update("update recuperacion_contrasena set estado = 'ANULADA', anulada_en = :ahora"
                        + " where usuario_id = :u and estado in ('PENDIENTE', 'ENVIANDO', 'ENVIADA')",
                new MapSqlParameterSource("u", usuarioId).addValue("ahora", t(ahora)));
    }

    @Override
    public int anularCaducadas(Instant ahora) {
        return sql.update("update recuperacion_contrasena set estado = 'ANULADA', anulada_en = :ahora"
                        + " where estado = 'PENDIENTE' and expira_en <= :ahora",
                new MapSqlParameterSource("ahora", t(ahora)));
    }

    @Override
    public Optional<Reclamada> reclamar(String tokenHash, Instant ahora) {
        List<Reclamada> filas = sql.query("""
                with elegida as (
                    select id from recuperacion_contrasena
                    where estado = 'PENDIENTE' and expira_en > :ahora
                    order by creada_en limit 1 for update skip locked)
                update recuperacion_contrasena r set estado = 'ENVIANDO', token_hash = :h
                from elegida where r.id = elegida.id
                returning r.id, r.usuario_id, r.expira_en,
                    (select u.correo from usuario u where u.id = r.usuario_id) as correo,
                    (select u.nombre from usuario u where u.id = r.usuario_id) as nombre,
                    (select u.activo and exists (select 1 from usuario_rol ur where ur.usuario_id = u.id)
                       from usuario u where u.id = r.usuario_id) as activa""",
                new MapSqlParameterSource("h", tokenHash).addValue("ahora", t(ahora)),
                (f, n) -> new Reclamada(f.getObject("id", UUID.class), f.getObject("usuario_id", UUID.class),
                        f.getObject("expira_en", OffsetDateTime.class).toInstant(), f.getString("correo"),
                        f.getString("nombre"), f.getBoolean("activa")));
        return filas.stream().findFirst();
    }

    @Override
    public void marcarEnviada(UUID id, Instant ahora) {
        sql.update("update recuperacion_contrasena set estado = 'ENVIADA', enviada_en = :ahora"
                        + " where id = :id and estado = 'ENVIANDO'",
                new MapSqlParameterSource("id", id).addValue("ahora", t(ahora)));
    }

    @Override
    public void marcarFallida(UUID id, String codigo, Instant ahora) {
        sql.update("update recuperacion_contrasena set estado = 'FALLIDA', ultimo_error = :c"
                        + " where id = :id and estado = 'ENVIANDO'",
                new MapSqlParameterSource("id", id).addValue("c", codigo));
    }

    @Override
    public void anular(UUID id, Instant ahora) {
        sql.update("update recuperacion_contrasena set estado = 'ANULADA', anulada_en = :ahora"
                        + " where id = :id and estado in ('PENDIENTE', 'ENVIANDO')",
                new MapSqlParameterSource("id", id).addValue("ahora", t(ahora)));
    }
}
