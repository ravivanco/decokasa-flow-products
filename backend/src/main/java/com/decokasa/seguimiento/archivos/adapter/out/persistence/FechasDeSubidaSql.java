package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.FechasDeSubida;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Lee {@code adjunto.subido_en} (ADR-0027); comparte DataSource/transacción de la aplicación. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class FechasDeSubidaSql implements FechasDeSubida {
    private final JdbcTemplate sql;

    public FechasDeSubidaSql(DataSource fuente) { sql = new JdbcTemplate(fuente); }

    @Override public Optional<Instant> subidoEn(UUID adjuntoId) {
        return sql.query("select subido_en from adjunto where id = ?", (r, n) -> r.getTimestamp(1), adjuntoId)
                .stream().findFirst().map(Timestamp::toInstant);
    }
}
