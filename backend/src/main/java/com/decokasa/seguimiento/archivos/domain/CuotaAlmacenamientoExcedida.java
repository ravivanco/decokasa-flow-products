package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

public final class CuotaAlmacenamientoExcedida extends ErrorDeDominio {
    private static final long serialVersionUID = 1L;
    public CuotaAlmacenamientoExcedida() {
        super(CodigoError.CUOTA_ALMACENAMIENTO,
            "No hay cuota o cupos de carga disponibles. Reanuda tus cargas pendientes o contacta al administrador.");
    }
}
