package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.nio.charset.StandardCharsets;

/**
 * Contraseña que se va a guardar (al crear un usuario o al cambiarla). Al menos 8 caracteres y como máximo
 * 72 bytes en UTF-8, que es lo que procesa BCrypt; nunca se recorta en silencio (modelo-datos.md 3.1).
 */
public record ContrasenaNueva(String valor) {

    /** Límite de BCrypt, en bytes UTF-8. */
    public static final int MAXIMO_BYTES = 72;

    /** Mínimo en caracteres (contrato: {@code contrasenaInicial.minLength}). */
    public static final int MINIMO_CARACTERES = 8;

    /** @throws DatoInvalido si no cumple los límites */
    public ContrasenaNueva {
        if (valor == null || valor.codePointCount(0, valor.length()) < MINIMO_CARACTERES) {
            throw new DatoInvalido("La contraseña debe tener al menos 8 caracteres.");
        }
        if (superaElLimite(valor)) {
            throw new DatoInvalido("La contraseña admite como máximo 72 bytes. "
                    + "Las letras con tilde y los emoji ocupan más de uno.");
        }
    }

    /** Si la contraseña pasa de 72 bytes en UTF-8: BCrypt la recortaría, así que nunca se acepta. */
    public static boolean superaElLimite(String contrasena) {
        return contrasena.getBytes(StandardCharsets.UTF_8).length > MAXIMO_BYTES;
    }
}
