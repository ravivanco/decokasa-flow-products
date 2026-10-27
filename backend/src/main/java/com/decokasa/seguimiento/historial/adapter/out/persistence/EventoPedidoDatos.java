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

    /**
     * Orden de inserción (F4-H01, V13): hora del servidor y, en empate, la secuencia que asigna la base al insertar, que
     * sigue el orden de emisión del dominio. El id solo desempata los eventos anteriores a V13 (secuencia nula), cuyo
     * orden real no se conoce.
     */
    List<EventoPedidoEntity> findByPedidoIdOrderByOcurridoEnAscSecuenciaAscIdAsc(UUID pedidoId);

    @Query("select a from EventoAdjuntoEntity a where a.eventoId in :eventos order by a.adjuntoId")
    List<EventoAdjuntoEntity> adjuntosDe(@Param("eventos") Collection<UUID> eventos);
}
