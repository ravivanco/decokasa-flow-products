package com.decokasa.seguimiento.compartido.domain;

/**
 * Acciones sobre la etapa actual de un pedido (contrato: AccionEtapa; columna transicion_permitida.accion). Qué acción
 * existe en cada etapa, su destino y sus requisitos los definen los datos del flujo (ADR-0004), no este enum.
 */
public enum AccionEtapa {
    ENVIAR,
    APROBAR,
    RECHAZAR,
    DEVOLVER_POR_ERROR,
    DEVOLVER_POR_PRODUCTO_NUEVO,
    REPORTAR_DEMORA,
    ELEGIR_BODEGA
}
