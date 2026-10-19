package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** Mensaje cerrado: nunca incorpora cuerpos HTTP, códigos de autorización, tokens ni excepciones del proveedor. */
public final class GoogleNoDisponible extends ErrorDeDominio {
    private static final long serialVersionUID = 1L;

    public GoogleNoDisponible() {
        super(CodigoError.GOOGLE_NO_DISPONIBLE,
                "No se pudo conectar Google. Revisa la configuración privada e inicia la autorización de nuevo.");
    }
}
