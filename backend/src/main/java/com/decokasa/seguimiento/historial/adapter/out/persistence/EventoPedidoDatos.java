package com.decokasa.seguimiento.historial.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de evento_pedido con Spring Data. Extiende {@link Repository}, no {@code JpaRepository}, para no ofrecer
 * {@code save}, {@code delete} ni {@code deleteAll}: la inserción la hace {@link HistorialJpaRepositorio} con
 * {@code persist} (R7). Lo usa solo ese adaptador.
 */
public interface EventoPedidoDatos extends Repository<EventoPedidoEntity, UUID> {

    List<EventoPedidoEntity> findByPedidoIdOrderByOcurridoEnAscIdAsc(UUID pedidoId);

    @Query("select a from EventoAdjuntoEntity a where a.eventoId in :eventos order by a.adjuntoId")
    List<EventoAdjuntoEntity> adjuntosDe(@Param("eventos") Collection<UUID> eventos);
}
