package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** El archivo no es de un formato admitido (ADR-0026; código TIPO_ARCHIVO_NO_ACEPTADO, 415 en la API). */
public final class TipoArchivoNoAceptado extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public static final String MENSAJE =
            "Formato no admitido. Sube un PDF, un Excel (.xlsx o .xls), un CSV o una imagen JPG o PNG.";

    public TipoArchivoNoAceptado() {
        super(CodigoError.TIPO_ARCHIVO_NO_ACEPTADO, MENSAJE);
    }
}
