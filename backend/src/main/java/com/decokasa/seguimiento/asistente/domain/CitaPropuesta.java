package com.decokasa.seguimiento.asistente.domain;

/**
 * Cita tal como la devuelve el modelo, antes de verificarla. No es evidencia hasta pasar por
 * {@link VerificadorReferencias}.
 *
 * @param indiceDocumento posición del documento en la consulta, desde 0 (document_index)
 * @param paginaInicio page_location de un PDF (start_page_number, desde 1); nula en otras citas
 * @param paginaFin page_location (end_page_number); nula en otras citas
 * @param caracterInicio char_location de un documento de texto (start_char_index); nula en otras citas
 * @param caracterFin char_location (end_char_index, exclusivo); nula en otras citas
 */
public record CitaPropuesta(long indiceDocumento, Long paginaInicio, Long paginaFin, Long caracterInicio,
        Long caracterFin, String textoCitado) {

    @Override
    public String toString() {
        return "CitaPropuesta[documento=" + indiceDocumento + "]";
    }
}
