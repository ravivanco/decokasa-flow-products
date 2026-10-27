package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * No se puede iniciar sesión. Es el mismo error para una contraseña incorrecta, un usuario inexistente, desactivado
 * o sin roles, para no revelar qué cuentas existen (contrato: 401 NO_AUTENTICADO).
 */
public final class CredencialesInvalidas extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public CredencialesInvalidas() {
        super(CodigoError.NO_AUTENTICADO, "Credenciales incorrectas.");
    }
}
