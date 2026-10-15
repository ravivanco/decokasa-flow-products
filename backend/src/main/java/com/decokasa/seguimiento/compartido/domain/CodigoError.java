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
    VERSION_DESACTUALIZADA
}
