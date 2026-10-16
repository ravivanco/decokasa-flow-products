package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** El archivo supera el límite configurado (código ARCHIVO_DEMASIADO_GRANDE, 413 en la API). */
public final class ArchivoDemasiadoGrande extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public ArchivoDemasiadoGrande(String mensaje) {
        super(CodigoError.ARCHIVO_DEMASIADO_GRANDE, mensaje);
    }
}
