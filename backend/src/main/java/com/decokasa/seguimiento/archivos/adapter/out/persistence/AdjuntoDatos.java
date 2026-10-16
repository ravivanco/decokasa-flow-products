package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Consultas de adjunto. Sin borrado (un adjunto queda para el historial). Lo usa solo
 * {@link AdjuntoJpaRepositorio}.
 */
public interface AdjuntoDatos extends Repository<AdjuntoEntity, UUID> {

    @Query("select a from AdjuntoEntity a where a.id in :ids")
    List<AdjuntoEntity> porIds(@Param("ids") Collection<UUID> ids);

    @Query("select a from AdjuntoEntity a where a.pedidoId = :pedidoId and a.etapa = :etapa order by a.subidoEn, a.id")
    List<AdjuntoEntity> deEtapa(@Param("pedidoId") UUID pedidoId, @Param("etapa") short etapa);

    @Query("select distinct a.tipoDocumento from AdjuntoEntity a where a.entradaId = :entradaId")
    List<String> tiposDeEntrada(@Param("entradaId") UUID entradaId);

    // evento_adjunto es del historial; aquí solo se lee la relación SUBIDO para mostrar Adjunto.eventoId.
    @Query(value = "select cast(adjunto_id as text), cast(evento_id as text) from evento_adjunto "
            + "where relacion = 'SUBIDO' and adjunto_id in (:ids)", nativeQuery = true)
    List<Object[]> eventosDeSubida(@Param("ids") Collection<UUID> ids);
}
