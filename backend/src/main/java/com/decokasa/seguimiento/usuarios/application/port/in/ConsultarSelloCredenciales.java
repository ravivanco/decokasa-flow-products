package com.decokasa.seguimiento.usuarios.application.port.in;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Momento del último cambio de credenciales de una cuenta (ADR-0033). Un token de acceso emitido antes ya no vale.
 * Vacío si la cuenta no existe o nunca cambió su contraseña desde V28.
 */
public interface ConsultarSelloCredenciales {

    Optional<Instant> cambiadasEn(UUID usuarioId);
}
