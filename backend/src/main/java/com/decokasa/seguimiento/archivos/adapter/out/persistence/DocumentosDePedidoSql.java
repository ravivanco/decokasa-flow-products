package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de solo lectura de ADR-0028; comparte DataSource/transacción de la aplicación. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class DocumentosDePedidoSql implements DocumentosDePedido {
    private final NamedParameterJdbcTemplate sql;

    public DocumentosDePedidoSql(DataSource fuente) { sql = new NamedParameterJdbcTemplate(fuente); }

    @Override public List<Documento> delPedido(UUID pedidoId) {
        var parametros = new MapSqlParameterSource("pedido", pedidoId);
        Map<UUID, List<Presentacion>> presentaciones = new HashMap<>();
        sql.query("""
                select ea.adjunto_id, e.id as evento_id, e.tipo, e.etapa, e.ocurrido_en
                from evento_adjunto ea
                join evento_pedido e on e.id = ea.evento_id
                join adjunto a on a.id = ea.adjunto_id
                where a.pedido_id = :pedido and ea.relacion = 'PRESENTADO'
                order by e.ocurrido_en, e.id
                """, parametros, fila -> {
            presentaciones.computeIfAbsent(fila.getObject("adjunto_id", UUID.class), k -> new ArrayList<>())
                    .add(new Presentacion(fila.getObject("evento_id", UUID.class), fila.getString("tipo"),
                            fila.getInt("etapa"), fila.getTimestamp("ocurrido_en").toInstant()));
        });
        return sql.query("""
                select a.id, a.pedido_id, a.etapa, a.entrada_id, en.entrada_en, a.nombre, a.tipo_documento,
                  a.tipo_detectado, a.tamano_bytes, a.clave_local, a.estado_copia,
                  case when a.estado_copia = 'COPIADO' then a.drive_id end as drive_id, a.subido_por, a.subido_en
                from adjunto a
                join entrada_etapa en on en.id = a.entrada_id
                where a.pedido_id = :pedido
                order by a.subido_en, a.id
                """, parametros, (r, n) -> {
            UUID id = r.getObject("id", UUID.class);
            return new Documento(id, r.getObject("pedido_id", UUID.class), r.getInt("etapa"),
                    r.getObject("entrada_id", UUID.class), instante(r.getTimestamp("entrada_en")), r.getString("nombre"),
                    r.getString("tipo_documento"), r.getString("tipo_detectado"), r.getLong("tamano_bytes"),
                    r.getString("clave_local"), r.getString("estado_copia"), r.getString("drive_id"),
                    r.getObject("subido_por", UUID.class), instante(r.getTimestamp("subido_en")),
                    presentaciones.getOrDefault(id, List.of()));
        });
    }

    @Override public List<Transicion> transicionesVigentes(UUID pedidoId) {
        return sql.query("""
                select e.id, e.tipo from evento_pedido e join pedido p on p.id = e.pedido_id
                where e.pedido_id = :pedido and not p.finalizado
                  and e.tipo in ('ETAPA_ENVIADA', 'ETAPA_APROBADA', 'ETAPA_RECHAZADA', 'PEDIDO_DEVUELTO')
                order by e.secuencia desc nulls last, e.ocurrido_en desc, e.id desc
                """, new MapSqlParameterSource("pedido", pedidoId),
                (r, n) -> new Transicion(r.getObject("id", UUID.class), r.getString("tipo")));
    }

    @Override public Optional<String> driveIdCopiado(UUID adjuntoId) {
        return sql.queryForList(
                "select drive_id from adjunto where id = :id and estado_copia = 'COPIADO' and drive_id is not null",
                new MapSqlParameterSource("id", adjuntoId), String.class).stream().findFirst();
    }

    private static Instant instante(java.sql.Timestamp valor) { return valor == null ? null : valor.toInstant(); }
}
