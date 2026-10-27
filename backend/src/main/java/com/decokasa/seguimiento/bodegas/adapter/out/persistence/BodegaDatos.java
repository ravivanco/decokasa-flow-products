package com.decokasa.seguimiento.bodegas.adapter.out.persistence;

import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de Spring Data sobre la tabla bodega. */
public interface BodegaDatos extends JpaRepository<BodegaEntity, UUID> {


    List<BodegaEntity> findByActiva(boolean activa, Sort orden);

    // Fase 7 (V19): por país.
    Optional<BodegaEntity> findByPaisCodigoAndNombreIgnoreCase(String paisCodigo, String nombre);

    List<BodegaEntity> findByPaisCodigoIn(Collection<String> paises, Sort orden);

    List<BodegaEntity> findByPaisCodigoInAndActiva(Collection<String> paises, boolean activa, Sort orden);
}
