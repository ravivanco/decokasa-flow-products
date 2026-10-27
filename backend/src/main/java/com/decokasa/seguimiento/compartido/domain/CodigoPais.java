package com.decokasa.seguimiento.compartido.domain;

import java.time.ZoneId;

/** Países del sistema (contrato: CodigoPais). EC activo; CO inactivo en v1 (ADR-0009). */
public enum CodigoPais {
    EC(ZoneId.of("America/Guayaquil")),
    CO(ZoneId.of("America/Bogota"));

    private final ZoneId zona;

    CodigoPais(ZoneId zona) {
        this.zona = zona;
    }

    /** Zona horaria del país: fecha local de los pedidos (ADR-0026: carpeta del pedido por su fecha de creación). */
    public ZoneId zona() {
        return zona;
    }
}
