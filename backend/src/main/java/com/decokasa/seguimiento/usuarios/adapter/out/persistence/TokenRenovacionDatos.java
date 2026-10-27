package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a la tabla token_renovacion con Spring Data. Lo usa solo {@link TokenRenovacionJpaRepositorio}. */
public interface TokenRenovacionDatos extends JpaRepository<TokenRenovacionEntity, UUID> {

    Optional<TokenRenovacionEntity> findByTokenHash(String tokenHash);

    // Una sola sentencia: PostgreSQL bloquea la fila, y la segunda solicitud, al obtenerla, vuelve a evaluar
    // "revocado_en is null" con el valor ya confirmado y no actualiza nada (hallazgo A2).
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRenovacionEntity t set t.revocadoEn = :ahora where t.id = :id and t.revocadoEn is null")
    int consumir(@Param("id") UUID id, @Param("ahora") Instant ahora);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRenovacionEntity t set t.revocadoEn = :ahora "
            + "where t.familiaId = :familiaId and t.revocadoEn is null")
    int revocarFamilia(@Param("familiaId") UUID familiaId, @Param("ahora") Instant ahora);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update TokenRenovacionEntity t set t.revocadoEn = :ahora "
            + "where t.usuarioId = :usuarioId and t.revocadoEn is null")
    int revocarDeUsuario(@Param("usuarioId") UUID usuarioId, @Param("ahora") Instant ahora);
}
