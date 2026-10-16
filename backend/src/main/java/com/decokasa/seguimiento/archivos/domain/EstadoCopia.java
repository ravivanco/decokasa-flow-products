package com.decokasa.seguimiento.archivos.domain;

/**
 * Estado de la copia a Drive (contrato: EstadoCopia). En la fase 2 todo adjunto queda PENDIENTE; la copia es de la
 * fase 3.
 */
public enum EstadoCopia {
    PENDIENTE,
    COPIADO,
    FALLIDO
}
