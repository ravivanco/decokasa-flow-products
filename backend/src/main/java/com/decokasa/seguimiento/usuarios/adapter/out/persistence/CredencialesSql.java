package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.usuarios.application.port.out.CredencialesRepositorio;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Contraseña y sello de credenciales en SQL (ADR-0033). El sello se guarda en milisegundos, la misma precisión que el
 * claim {@code emitido_ms} del JWT: un token emitido en el mismo instante o después sigue valiendo.
 */
@Repository
public class CredencialesSql implements CredencialesRepositorio {

    private final NamedParameterJdbcTemplate sql;

    public CredencialesSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public void cambiarContrasena(UUID usuarioId, String contrasenaHash, Instant ahora) {
        OffsetDateTime sello = OffsetDateTime.ofInstant(ahora.truncatedTo(ChronoUnit.MILLIS), ZoneOffset.UTC);
        int filas = sql.update("update usuario set contrasena_hash = :h, credenciales_cambiadas_en = :sello,"
                        + " intentos_fallidos = 0, bloqueado_hasta = null, actualizado_en = :sello,"
                        + " version = version + 1 where id = :id",
                new MapSqlParameterSource("id", usuarioId).addValue("h", Objects.requireNonNull(contrasenaHash))
                        .addValue("sello", sello));
        if (filas != 1) {
            throw new IllegalStateException("No se pudo guardar la contraseña.");
        }
    }

    @Override
    public Optional<Instant> cambiadasEn(UUID usuarioId) {
        List<OffsetDateTime> filas = sql.query("select credenciales_cambiadas_en from usuario where id = :id",
                new MapSqlParameterSource("id", usuarioId),
                (f, n) -> f.getObject("credenciales_cambiadas_en", OffsetDateTime.class));
        return filas.stream().filter(Objects::nonNull).findFirst().map(OffsetDateTime::toInstant);
    }
}
