package com.decokasa.seguimiento.bodegas.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

/** Consultas de Spring Data sobre la tabla bodega. */
public interface BodegaDatos extends JpaRepository<BodegaEntity, UUID> {

    Optional<BodegaEntity> findByNombreIgnoreCase(String nombre);

    List<BodegaEntity> findByActiva(boolean activa, Sort orden);
}
