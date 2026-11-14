package com.decokasa.seguimiento.notificaciones.adapter.out.whatsapp;

import com.decokasa.seguimiento.notificaciones.application.port.out.ColaWhatsApp;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * WhatsApp Cloud API de Meta, conexión directa desde el backend (sin n8n). Envía una plantilla aprobada con un solo
 * parámetro de texto, porque fuera de la ventana de 24 horas Meta solo admite plantillas. Sin verificar contra Meta:
 * falta la cuenta, el número y las credenciales del cliente. Nunca registra el teléfono ni el texto.
 */
public final class WhatsAppCloud implements ColaWhatsApp.Mensajero {

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final JsonMapper json;
    private final String token;
    private final String numeroId;
    private final String plantilla;
    private final String idioma;
    private final String version;

    public WhatsAppCloud(JsonMapper json, String token, String numeroId, String plantilla, String idioma,
            String version) {
        if (blanco(token) || blanco(numeroId) || blanco(plantilla)) {
            throw new IllegalStateException("WHATSAPP_MODO=cloud exige token, número y plantilla de WhatsApp.");
        }
        this.json = json;
        this.token = token.strip();
        this.numeroId = numeroId.strip();
        this.plantilla = plantilla.strip();
        this.idioma = blanco(idioma) ? "es" : idioma.strip();
        this.version = blanco(version) ? "v21.0" : version.strip();
    }

    @Override
    public String enviar(String telefono, String texto) {
        // Meta no admite saltos de línea en los parámetros de una plantilla.
        String parametro = texto.replaceAll("\\s+", " ").strip();
        if (parametro.length() > 1000) {
            parametro = parametro.substring(0, 1000);
        }
        Map<String, Object> cuerpo = Map.of(
                "messaging_product", "whatsapp",
                "to", telefono.substring(1),
                "type", "template",
                "template", Map.of("name", plantilla, "language", Map.of("code", idioma),
                        "components", List.of(Map.of("type", "body",
                                "parameters", List.of(Map.of("type", "text", "text", parametro))))));
        try {
            HttpResponse<String> respuesta = http.send(HttpRequest.newBuilder()
                    .uri(URI.create("https://graph.facebook.com/" + version + "/" + numeroId + "/messages"))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)))
                    .build(), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() / 100 != 2) {
                throw new IllegalStateException("WhatsApp respondió " + respuesta.statusCode());
            }
            JsonNode mensajes = json.readTree(respuesta.body()).path("messages");
            return mensajes.isArray() && !mensajes.isEmpty() ? mensajes.get(0).path("id").asString() : "sin-id";
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Envío interrumpido");
        } catch (java.io.IOException error) {
            throw new IllegalStateException("WhatsApp no respondió");
        }
    }

    private static boolean blanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
