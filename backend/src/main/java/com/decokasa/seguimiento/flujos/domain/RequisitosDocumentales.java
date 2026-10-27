package com.decokasa.seguimiento.flujos.domain;

/**
 * Estado de los documentos obligatorios de una transición (columna transicion_permitida.requisitos_documentales).
 * PENDIENTES no equivale a "sin documentos": la transición no se puede ejecutar hasta que el cliente los defina
 * (decisión S-2 del equipo, 2026-10-15).
 */
public enum RequisitosDocumentales {
    DEFINIDOS,
    PENDIENTES
}
