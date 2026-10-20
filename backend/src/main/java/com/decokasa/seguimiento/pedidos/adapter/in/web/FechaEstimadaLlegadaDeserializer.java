package com.decokasa.seguimiento.pedidos.adapter.in.web;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.util.AccessPattern;

/** Valida exclusivamente el campo web de la demora; no altera las coerciones globales de Jackson. */
public final class FechaEstimadaLlegadaDeserializer extends ValueDeserializer<LocalDate> {

    private static final Pattern FORMATO = Pattern.compile("[0-9]{4}-[0-9]{2}-[0-9]{2}");
    private static final String MENSAJE =
            "nuevaFechaEstimadaLlegada debe ser una cadena AAAA-MM-DD con una fecha válida.";

    @Override
    public LocalDate deserialize(JsonParser parser, DeserializationContext contexto) {
        if (!parser.hasToken(JsonToken.VALUE_STRING)) {
            return contexto.reportInputMismatch(LocalDate.class, MENSAJE);
        }
        String texto = parser.getString();
        if (!FORMATO.matcher(texto).matches()) {
            return contexto.reportInputMismatch(LocalDate.class, MENSAJE);
        }
        try {
            // ISO_LOCAL_DATE resuelve estrictamente: no normaliza fechas imposibles a otro día.
            return LocalDate.parse(texto, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException error) {
            return contexto.reportInputMismatch(LocalDate.class, MENSAJE);
        }
    }

    @Override
    public LocalDate getNullValue(DeserializationContext contexto) {
        return contexto.reportInputMismatch(LocalDate.class, MENSAJE);
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        // El rechazo corresponde al null explícito de la petición, no a la creación del deserializador.
        return AccessPattern.DYNAMIC;
    }

    @Override
    public LocalDate getAbsentValue(DeserializationContext contexto) {
        // El campo es opcional para otras acciones. REPORTAR_DEMORA conserva su requisito de dominio (422).
        return null;
    }
}
