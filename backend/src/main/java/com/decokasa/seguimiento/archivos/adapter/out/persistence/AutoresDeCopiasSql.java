package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.AutoresDeCopias;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Solo lectura del evento de subida (historial, append-only) enlazado al adjunto, como {@code AdjuntoDatos} lee
 * {@code evento_adjunto}. Exige un evento ARCHIVO_SUBIDO, enlazado como SUBIDO a ese adjunto, del mismo pedido y del
 * mismo autor que el adjunto, con rol registrado. Cualquier otro evento (aunque esté enlazado como SUBIDO) no cuenta:
 * no hay autor y la copia falla sin crear carpetas ni llamar a Drive por ellas (F3-ADR25-01).
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class AutoresDeCopiasSql implements AutoresDeCopias {
    private final NamedParameterJdbcTemplate sql;
    public AutoresDeCopiasSql(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }
    @Override public Optional<Autor> autor(UUID adjuntoId) {
        return sql.query("""
                select e.usuario_id, e.rol_codigo from adjunto a
                join evento_adjunto ea on ea.adjunto_id = a.id and ea.relacion = 'SUBIDO'
                join evento_pedido e on e.id = ea.evento_id and e.tipo = 'ARCHIVO_SUBIDO'
                where a.id = :id and e.pedido_id = a.pedido_id and e.usuario_id = a.subido_por
                  and e.rol_codigo is not null
                """, Map.of("id", adjuntoId), (r, n) -> new Autor(r.getObject("usuario_id", UUID.class),
                new CodigoRol(r.getString("rol_codigo")))).stream().findFirst();
    }
}
