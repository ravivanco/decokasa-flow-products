package com.decokasa.seguimiento.compartido.domain;

/**
 * Códigos de error estables que ve el frontend (contrato openapi.yaml, esquema Problema).
 * El dominio solo conoce el código; la traducción a estado HTTP vive en el adaptador web.
 */
public enum CodigoError {
    VALIDACION,
    NO_AUTENTICADO,
    SIN_PERMISO,
    NO_ENCONTRADO,
    ETAPA_CAMBIO,
    TRANSICION_NO_PERMITIDA,
    DUPLICADO,
    REQUISITO_FALTANTE,
    ARCHIVO_DEMASIADO_GRANDE,
    TIPO_ARCHIVO_NO_ACEPTADO,
    COPIA_PENDIENTE,
    CUENTA_BLOQUEADA,
    VERSION_DESACTUALIZADA,
    ARCHIVO_NO_DISPONIBLE,
    GOOGLE_NO_DISPONIBLE,
    CARGA_CONFLICTO,
    CARGA_INCOMPLETA,
    CARGA_OCUPADA,
    CARGA_ALMACENAMIENTO,
    CUOTA_ALMACENAMIENTO,
    /** Asistente desactivado, sin presupuesto o proveedor no disponible (fase 7, paso 7.4; contrato 0.16.0). */
    IA_NO_DISPONIBLE,
    /** La conversación del asistente llegó a 10 preguntas (F7-D2). */
    LIMITE_PREGUNTAS,
    /** Límite del piloto: consulta en curso, consultas del día o presupuesto del mes (ADR-0022; paso 7.5). */
    LIMITE_CONSUMO,
    /** La pregunta y el historial no caben en el límite de entrada (F7-D2; paso 7.5). */
    CONTEXTO_EXCEDIDO,
    /** Enlace de recuperación desconocido, caducado, usado o anulado (ADR-0033); igual en todos los casos. */
    ENLACE_INVALIDO,
    /** Límite de solicitudes por origen superado (ADR-0033). */
    DEMASIADAS_SOLICITUDES
}
