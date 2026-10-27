package com.decokasa.seguimiento.usuarios.application.port.out;

import java.util.UUID;

/**
 * Avisa, dentro de la transacción del alta, que el admin creó un usuario (ADR-0026: archivos programa su carpeta de
 * Drive). No llama a Google: si el aviso falla, el alta se revierte; si Google falla después, el usuario sigue creado.
 */
@FunctionalInterface
public interface AvisoAltaUsuario {

    /** Sin aviso: la carga inicial (ADR-0015) y las pruebas anteriores al 2026-10-24. */
    AvisoAltaUsuario NINGUNO = usuarioId -> { };

    void usuarioCreado(UUID usuarioId);
}
