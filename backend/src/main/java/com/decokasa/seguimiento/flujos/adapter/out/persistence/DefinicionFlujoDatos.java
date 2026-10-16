package com.decokasa.seguimiento.flujos.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Lecturas de las tablas de definición de flujo. Extiende {@link Repository} para no ofrecer escrituras: las
 * definiciones solo cambian con migraciones. Lo usa solo {@link DefinicionFlujoJpaRepositorio}.
 */
public interface DefinicionFlujoDatos extends Repository<DefinicionFlujoEntity, UUID> {

    Optional<DefinicionFlujoEntity> findById(UUID id);

    Optional<DefinicionFlujoEntity> findByPaisCodigoAndTipoFlujoAndVigenteTrue(String paisCodigo, String tipoFlujo);

    Optional<DefinicionFlujoEntity> findByPaisCodigoAndTipoFlujoAndVersion(String paisCodigo, String tipoFlujo,
            int version);

    @Query("select e from DefinicionEtapaEntity e where e.flujoId = :flujoId order by e.numero")
    List<DefinicionEtapaEntity> etapasDe(@Param("flujoId") UUID flujoId);

    // Una consulta con sus documentos, sin N+1. Con join fetch y no con @EntityGraph: el grafo se resuelve contra el
    // tipo raíz del repositorio (DefinicionFlujoEntity), que no tiene "documentos" (2.B2-D01). Left join: una
    // transición sin documentos también se devuelve; distinct evita repetirla por cada documento.
    @Query("select distinct t from TransicionPermitidaEntity t left join fetch t.documentos where t.flujoId = :flujoId")
    List<TransicionPermitidaEntity> transicionesDe(@Param("flujoId") UUID flujoId);
}
