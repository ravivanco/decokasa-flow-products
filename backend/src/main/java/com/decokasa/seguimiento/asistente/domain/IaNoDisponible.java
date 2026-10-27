package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * 503 IA_NO_DISPONIBLE: desactivada, sin credenciales o sin modelo, sin presupuesto (paso 7.5), proveedor caído o
 * límite externo. El mensaje nunca lleva detalles del proveedor.
 */
public final class IaNoDisponible extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public IaNoDisponible() {
        super(CodigoError.IA_NO_DISPONIBLE, "El asistente no está disponible en este momento.");
    }
}
