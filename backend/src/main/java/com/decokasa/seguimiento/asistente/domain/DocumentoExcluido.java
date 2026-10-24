package com.decokasa.seguimiento.asistente.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Documento que el usuario puede descargar pero que no se envió al modelo (contrato: DocumentoExcluidoAsistente). Los
 * documentos que no puede descargar nunca se nombran.
 */
public record DocumentoExcluido(UUID archivoId, String nombreArchivo, Motivo motivo) {

    public enum Motivo {
        /** No es PDF por su contenido (F7-D3: solo PDF). */
        FORMATO_NO_ADMITIDO,
        /** No cabe en el límite de la consulta. */
        CONTEXTO_EXCEDIDO,
        /** No está en el manifiesto revisado del piloto, o el entorno no es el piloto ficticio (F7-D3, F7-D6). */
        NO_AUTORIZADO_PILOTO
    }

    public DocumentoExcluido {
        Objects.requireNonNull(archivoId, "archivoId");
        Objects.requireNonNull(nombreArchivo, "nombreArchivo");
        Objects.requireNonNull(motivo, "motivo");
    }

    @Override
    public String toString() {
        return "DocumentoExcluido[archivoId=" + archivoId + ", motivo=" + motivo + "]";
    }
}
