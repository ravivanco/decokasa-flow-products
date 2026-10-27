package com.decokasa.seguimiento.compartido.domain;

import java.util.List;

/**
 * Falta un requisito de la acción (código REQUISITO_FALTANTE, 422 en la API): la observación, la urgencia, la bodega o
 * un documento obligatorio. {@link #faltantes()} los nombra, como pide el contrato (Problema.faltantes).
 */
public final class RequisitoFaltante extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    private final List<String> faltantes;

    public RequisitoFaltante(List<String> faltantes) {
        super(CodigoError.REQUISITO_FALTANTE,
                "Faltan requisitos para esta acción: " + String.join(", ", faltantes) + ".");
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
