package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a la tabla usuario con Spring Data. Lo usa solo {@link UsuarioJpaRepositorio}. */
public interface UsuarioDatos extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByCorreoIgnoreCase(String correo);

    Optional<UsuarioEntity> findByUsuarioIgnoreCase(String usuario);

    Optional<UsuarioEntity> findByTelefono(String telefono);

    // Contadores del login (D10). Sin "versioned": esta actualización no incrementa la versión administrativa.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update UsuarioEntity u set u.intentosFallidos = :intentos, u.bloqueadoHasta = :bloqueadoHasta "
            + "where u.id = :id")
    int guardarIntentos(@Param("id") UUID id, @Param("intentos") int intentos,
            @Param("bloqueadoHasta") Instant bloqueadoHasta);
}
