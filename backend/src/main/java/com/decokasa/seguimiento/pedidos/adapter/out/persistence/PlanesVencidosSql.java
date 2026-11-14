package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import com.decokasa.seguimiento.pedidos.application.port.out.PlanesVencidos;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** SQL de la alerta de fecha planificada vencida: el historial es la única marca de «ya avisado». */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class PlanesVencidosSql implements PlanesVencidos {

    private final NamedParameterJdbcTemplate sql;

    public PlanesVencidosSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public List<UUID> enCurso(int limite) {
        return sql.queryForList("select id from pedido where not finalizado order by creado_en, id limit :limite",
                new MapSqlParameterSource("limite", limite), UUID.class);
    }

    @Override
    public boolean avisado(UUID pedidoId, int etapa) {
        return Boolean.TRUE.equals(sql.queryForObject("""
                select exists (select 1 from evento_pedido
                  where pedido_id = :pedido and etapa = :etapa and tipo = 'PLAN_VENCIDO')
                """, new MapSqlParameterSource("pedido", pedidoId).addValue("etapa", etapa), Boolean.class));
    }
}
