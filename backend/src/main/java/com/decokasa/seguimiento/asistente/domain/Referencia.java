package com.decokasa.seguimiento.asistente.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * Referencia ya verificada en el servidor (contrato: ReferenciaAsistente). Solo se construye a partir de una cita que
 * apunta a una fuente enviada en esa misma consulta y a una página o un tramo que existen ({@link VerificadorReferencias}).
 *
 * @param etapa etapa del dato o del documento citado; nula si el dato no pertenece a una etapa
 * @param archivoId nulo si la fuente son los datos del pedido
 * @param pagina página del PDF, desde 1; nula si la fuente no tiene páginas
 */
public record Referencia(String numeroPedido, Integer etapa, UUID archivoId, String nombreArchivo, Integer pagina,
        String textoCitado) {

    public Referencia {
        Objects.requireNonNull(numeroPedido, "numeroPedido");
        if (pagina != null && pagina < 1) {
            throw new IllegalArgumentException("Página inválida");
        }
        if ((archivoId == null) != (nombreArchivo == null)) {
            throw new IllegalArgumentException("Archivo incompleto");
        }
        if (archivoId != null && pagina == null) {
            throw new IllegalArgumentException("Un PDF se cita con su página");
        }
    }

    /** Sin texto citado ni nombre de archivo: nada del contenido va a los logs. */
    @Override
    public String toString() {
        return "Referencia[etapa=" + etapa + ", archivoId=" + archivoId + ", pagina=" + pagina + "]";
    }
}
