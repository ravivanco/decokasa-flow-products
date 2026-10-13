package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * No se puede renovar la sesión: token desconocido, revocado o vencido, o cuenta desactivada o sin roles
 * (ADR-0002; contrato: 401 NO_AUTENTICADO).
 */
public final class SesionInvalida extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public SesionInvalida() {
        super(CodigoError.NO_AUTENTICADO, "La sesión terminó. Inicia sesión de nuevo.");
    }
}
