package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla rol con Spring Data. Lo usan los adaptadores de este paquete. */
public interface RolDatos extends JpaRepository<RolEntity, UUID> {

    Optional<RolEntity> findByCodigo(String codigo);
}
