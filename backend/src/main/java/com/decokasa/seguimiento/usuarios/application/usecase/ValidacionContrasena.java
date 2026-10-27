package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva;

/** Contraseña nueva y su confirmación, con la política existente (8 caracteres, 72 bytes; ADR-0033). */
final class ValidacionContrasena {

    private ValidacionContrasena() {
    }

    static void validarNueva(String nueva, String confirmacion) {
        try {
            new ContrasenaNueva(nueva);
        } catch (DatoInvalido error) {
            throw new DatoInvalido("contrasenaNueva", error.getMessage());
        }
        if (!nueva.equals(confirmacion)) {
            throw new DatoInvalido("confirmacion", "La confirmación no coincide con la contraseña nueva.");
        }
    }
}
