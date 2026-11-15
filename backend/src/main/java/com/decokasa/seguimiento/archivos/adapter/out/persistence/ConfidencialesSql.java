package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial;
import com.decokasa.seguimiento.archivos.application.port.out.ConfidencialesRepositorio;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * SQL de la confidencialidad (V37). Confidencial: adjunto de una etapa con checklist cuyo tipo está entre los documentos
 * exigidos para aprobarla, según la versión del flujo del pedido (sin números de etapa fijos).
 */
@Repository
public class ConfidencialesSql implements ConfidencialesRepositorio {

    private final NamedParameterJdbcTemplate sql;

    public ConfidencialesSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public Set<UUID> confidenciales(UUID pedidoId) {
        return new HashSet<>(sql.queryForList("""
                select a.id from adjunto a
                join pedido p on p.id = a.pedido_id
                join definicion_etapa e on e.flujo_id = p.flujo_id and e.numero = a.etapa and e.checklist_documentos
                join transicion_permitida t on t.flujo_id = p.flujo_id and t.etapa_origen = a.etapa and t.accion = 'APROBAR'
                join documento_requerido d on d.transicion_id = t.id and d.tipo_documento = a.tipo_documento
                where a.pedido_id = :pedido
                """, new MapSqlParameterSource("pedido", pedidoId), UUID.class));
    }

    @Override
    public List<AccesoConfidencial.PorEtapa> porEtapa(UUID pedidoId) {
        return sql.query("""
                select a.etapa, count(*) as cantidad from adjunto a
                join pedido p on p.id = a.pedido_id
                join definicion_etapa e on e.flujo_id = p.flujo_id and e.numero = a.etapa and e.checklist_documentos
                join transicion_permitida t on t.flujo_id = p.flujo_id and t.etapa_origen = a.etapa and t.accion = 'APROBAR'
                join documento_requerido d on d.transicion_id = t.id and d.tipo_documento = a.tipo_documento
                where a.pedido_id = :pedido group by a.etapa order by a.etapa
                """, new MapSqlParameterSource("pedido", pedidoId),
                (r, n) -> new AccesoConfidencial.PorEtapa(r.getInt("etapa"), r.getInt("cantidad")));
    }

    @Override
    public boolean accesoPorRol(UUID usuarioId) {
        return existe("""
                select 1 from usuario u join usuario_rol ur on ur.usuario_id = u.id join rol r on r.id = ur.rol_id
                where u.id = :usuario and u.activo and r.codigo in (:roles)
                """, new MapSqlParameterSource("usuario", usuarioId).addValue("roles", AccesoConfidencial.ROLES));
    }

    @Override
    public boolean accesoOtorgado(UUID pedidoId, UUID usuarioId) {
        return existe("""
                select 1 from acceso_confidencial c join usuario u on u.id = c.usuario_id and u.activo
                where c.pedido_id = :pedido and c.usuario_id = :usuario and c.revocado_en is null
                """, new MapSqlParameterSource("pedido", pedidoId).addValue("usuario", usuarioId));
    }

    @Override
    public boolean esAdminVigente(UUID usuarioId) {
        return existe("""
                select 1 from usuario u join usuario_rol ur on ur.usuario_id = u.id join rol r on r.id = ur.rol_id
                where u.id = :usuario and u.activo and r.codigo = 'admin'
                """, new MapSqlParameterSource("usuario", usuarioId));
    }

    @Override
    public boolean usuarioActivo(UUID usuarioId) {
        return existe("select 1 from usuario where id = :usuario and activo",
                new MapSqlParameterSource("usuario", usuarioId));
    }

    @Override
    public List<AccesoConfidencial.ConAcceso> conAcceso(UUID pedidoId) {
        return sql.query("""
                select u.id, u.nombre, true as por_rol, null::text as por, null::timestamptz as en
                from usuario u
                join usuario_pais up on up.usuario_id = u.id
                join pedido p on p.id = :pedido and p.pais_codigo = up.pais_codigo
                where u.activo and exists (select 1 from usuario_rol ur join rol r on r.id = ur.rol_id
                  where ur.usuario_id = u.id and r.codigo in (:roles))
                union all
                select u.id, u.nombre, false, a.nombre, c.otorgado_en
                from acceso_confidencial c
                join usuario u on u.id = c.usuario_id and u.activo
                join usuario a on a.id = c.otorgado_por
                where c.pedido_id = :pedido and c.revocado_en is null
                  and not exists (select 1 from usuario_rol ur join rol r on r.id = ur.rol_id
                    where ur.usuario_id = u.id and r.codigo in (:roles))
                order by 3 desc, 2
                """, new MapSqlParameterSource("pedido", pedidoId).addValue("roles", AccesoConfidencial.ROLES),
                (r, n) -> {
                    Timestamp en = r.getTimestamp("en");
                    return new AccesoConfidencial.ConAcceso(r.getObject("id", UUID.class), r.getString("nombre"),
                            r.getBoolean("por_rol"), r.getString("por"), en == null ? null : en.toInstant());
                });
    }

    @Override
    public void otorgar(UUID id, UUID pedidoId, UUID usuarioId, UUID porAdmin, Instant ahora) {
        sql.update("""
                insert into acceso_confidencial (id, pedido_id, usuario_id, otorgado_por, otorgado_en)
                values (:id, :pedido, :usuario, :por, :ahora)
                on conflict (pedido_id, usuario_id) where revocado_en is null do nothing
                """, new MapSqlParameterSource("id", id).addValue("pedido", pedidoId).addValue("usuario", usuarioId)
                .addValue("por", porAdmin).addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public void revocar(UUID pedidoId, UUID usuarioId, UUID porAdmin, Instant ahora) {
        sql.update("""
                update acceso_confidencial set revocado_por = :por, revocado_en = :ahora
                where pedido_id = :pedido and usuario_id = :usuario and revocado_en is null
                """, new MapSqlParameterSource("pedido", pedidoId).addValue("usuario", usuarioId)
                .addValue("por", porAdmin).addValue("ahora", Timestamp.from(ahora)));
    }

    private boolean existe(String consulta, MapSqlParameterSource parametros) {
        return Boolean.TRUE.equals(sql.queryForObject("select exists (" + consulta + ")", parametros, Boolean.class));
    }
}
