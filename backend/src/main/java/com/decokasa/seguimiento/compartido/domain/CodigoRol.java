package com.decokasa.seguimiento.compartido.domain;

import java.util.regex.Pattern;

/**
 * Código de un rol (contrato: CodigoRol). Es texto libre porque el admin puede crear roles (requisito 4.6),
 * con un formato estable: minúsculas y guion bajo, de 3 a 40 caracteres. Los 7 iniciales se cargan por migración.
 */
public record CodigoRol(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^[a-z_]{3,40}$");

    public CodigoRol {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new DatoInvalido("El código de rol debe tener de 3 a 40 letras minúsculas o guiones bajos.");
        }
    }
}
