package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * El enlace de recuperación no sirve (ADR-0033). Es el mismo error para un token desconocido, caducado, usado o
 * anulado, o de una cuenta que ya no puede entrar: no revela cuál de esos casos ocurrió.
 */
public final class EnlaceInvalido extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public EnlaceInvalido() {
        super(CodigoError.ENLACE_INVALIDO, "El enlace no es válido o ya caducó. Pide uno nuevo.");
    }
}
