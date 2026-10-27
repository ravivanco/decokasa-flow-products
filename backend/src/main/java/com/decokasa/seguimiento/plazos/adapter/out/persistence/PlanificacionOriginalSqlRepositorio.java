package com.decokasa.seguimiento.plazos.adapter.out.persistence;

import com.decokasa.seguimiento.plazos.application.port.out.PlanificacionesOriginales;
import com.decokasa.seguimiento.plazos.domain.PlanificacionOriginal;
import jakarta.persistence.EntityManager;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
 * Adaptador SQL de {@link PlanificacionesOriginales} sobre planificacion_original y planificacion_original_etapa (V25,
 * solo inserción: triggers en la base). Corre en la transacción de quien lo llama: la creación del pedido o la consulta
 * del cronograma.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class PlanificacionOriginalSqlRepositorio implements PlanificacionesOriginales {

    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate sql;

    public PlanificacionOriginalSqlRepositorio(EntityManager entityManager, DataSource fuente) {
        this.entityManager = entityManager;
        this.sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public Optional<PlanificacionOriginal> de(UUID pedidoId) {
        MapSqlParameterSource parametros = new MapSqlParameterSource("pedido", pedidoId);
        List<PlanificacionOriginal.EtapaPrevista> etapas = sql.query(
                "select numero, fin_previsto from planificacion_original_etapa where pedido_id = :pedido"
                        + " order by numero",
                parametros,
                (fila, n) -> new PlanificacionOriginal.EtapaPrevista(fila.getInt("numero"),
                        fila.getObject("fin_previsto", LocalDate.class)));
        return sql.query("select reglas, calculada_en, llegada_prevista, finalizacion_prevista"
                        + " from planificacion_original where pedido_id = :pedido",
                parametros,
                (fila, n) -> new PlanificacionOriginal(pedidoId, PlanificacionOriginal.Origen.GUARDADA,
                        fila.getString("reglas"), fila.getObject("calculada_en", OffsetDateTime.class).toInstant(),
                        fila.getObject("llegada_prevista", LocalDate.class),
                        fila.getObject("finalizacion_prevista", LocalDate.class), etapas))
                .stream().findFirst();
    }

    @Override
    public void guardar(UUID flujoId, PlanificacionOriginal planificacion) {
        // El pedido se insertó con JPA en esta transacción: se escribe antes (FK planificación → pedido).
        entityManager.flush();
        sql.update("insert into planificacion_original (pedido_id, flujo_id, reglas, calculada_en, llegada_prevista,"
                        + " finalizacion_prevista) values (:pedido, :flujo, :reglas, :calculada, :llegada, :finalizacion)",
                new MapSqlParameterSource("pedido", planificacion.pedidoId())
                        .addValue("flujo", flujoId)
                        .addValue("reglas", planificacion.reglas())
                        .addValue("calculada", OffsetDateTime.ofInstant(planificacion.calculadaEn(), ZoneOffset.UTC))
                        .addValue("llegada", planificacion.llegada(), Types.DATE)
                        .addValue("finalizacion", planificacion.finalizacion(), Types.DATE));
        sql.batchUpdate("insert into planificacion_original_etapa (pedido_id, numero, fin_previsto)"
                        + " values (:pedido, :numero, :fin)",
                planificacion.etapas().stream()
                        .map(e -> new MapSqlParameterSource("pedido", planificacion.pedidoId())
                                .addValue("numero", (short) e.numero())
                                .addValue("fin", e.fin(), Types.DATE))
                        .toArray(MapSqlParameterSource[]::new));
    }
}
