package com.decokasa.seguimiento.asistente.domain;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Fuente enviada al modelo en una consulta, en el mismo orden que los bloques {@code document}: su posición es el
 * {@code document_index} de las citas. Es lo único contra lo que se verifican las referencias.
 *
 * @param paginas páginas del PDF según el manifiesto revisado del piloto; 0 en los datos del pedido
 * @param texto texto exacto enviado como datos del pedido; nulo en un PDF
 * @param tramos qué etapa ocupa cada tramo del texto de datos
 */
public record FuenteEnviada(Tipo tipo, UUID archivoId, String nombreArchivo, Integer etapa, int paginas, String texto,
        List<TramoEtapa> tramos) {

    public enum Tipo {
        DATOS_PEDIDO,
        PDF
    }

    /** Caracteres [inicio, fin) del texto de datos que describen una etapa. */
    public record TramoEtapa(int inicio, int fin, int etapa) {
    }

    public FuenteEnviada {
        Objects.requireNonNull(tipo, "tipo");
        tramos = tramos == null ? List.of() : List.copyOf(tramos);
        if (tipo == Tipo.PDF && (archivoId == null || nombreArchivo == null || etapa == null || paginas < 1)) {
            throw new IllegalArgumentException("PDF incompleto");
        }
        if (tipo == Tipo.DATOS_PEDIDO && texto == null) {
            throw new IllegalArgumentException("Datos del pedido sin texto");
        }
    }

    public static FuenteEnviada datosPedido(String texto, List<TramoEtapa> tramos) {
        return new FuenteEnviada(Tipo.DATOS_PEDIDO, null, null, null, 0, texto, tramos);
    }

    public static FuenteEnviada pdf(UUID archivoId, String nombreArchivo, int etapa, int paginas) {
        return new FuenteEnviada(Tipo.PDF, archivoId, nombreArchivo, etapa, paginas, null, List.of());
    }

    Integer etapaEn(int caracter) {
        return tramos.stream().filter(tramo -> tramo.inicio() <= caracter && caracter < tramo.fin())
                .map(TramoEtapa::etapa).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "FuenteEnviada[tipo=" + tipo + ", archivoId=" + archivoId + "]";
    }
}
