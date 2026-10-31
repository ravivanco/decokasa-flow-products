package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.time.Duration;
import java.util.OptionalLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Bucket PRIVADO de Supabase Storage como almacenamiento temporal duradero de los adjuntos pendientes de copiar a Drive
 * (ADR-0034). Solo lo usa el backend, con la clave secreta del proyecto (nunca la publicable ni el navegador). No hay
 * URL pública ni firmada. Escritura idempotente ({@code x-upsert}): la clave lleva el UUID del adjunto y su contenido
 * no cambia. Nunca registra la clave, cuerpos ni mensajes del proveedor.
 */
public final class RespaldoSupabaseStorage {
    private static final Pattern BUCKET = Pattern.compile("[a-z0-9][a-z0-9_-]{2,62}");
    private static final Pattern URL = Pattern.compile("https://[a-z0-9.-]{3,253}(:[0-9]{2,5})?");
    private final String base;
    private final String clave;
    private final String bucket;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NEVER).build();

    public RespaldoSupabaseStorage(String url, String clave, String bucket) {
        String limpia = url == null ? "" : url.strip().replaceAll("/+$", "");
        if (!URL.matcher(limpia).matches()) {
            throw new IllegalArgumentException("SUPABASE_URL debe ser https://<proyecto>.supabase.co, sin ruta.");
        }
        if (clave == null || clave.isBlank() || clave.strip().startsWith("sb_publishable_")) {
            throw new IllegalArgumentException("Falta la clave secreta de Supabase (no la publicable).");
        }
        if (bucket == null || !BUCKET.matcher(bucket).matches()) {
            throw new IllegalArgumentException("Nombre de bucket de Supabase inválido.");
        }
        this.base = limpia + "/storage/v1/object";
        this.clave = clave.strip();
        this.bucket = bucket;
    }

    /** Sube el contenido completo y comprueba después que el objeto guardado tiene exactamente ese tamaño. */
    public void subir(String objeto, Supplier<InputStream> contenido, long tamano) {
        HttpRequest peticion = autenticada(URI.create(base + "/" + bucket + "/" + objeto))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "application/octet-stream")
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.fromPublisher(HttpRequest.BodyPublishers.ofInputStream(contenido),
                        tamano))
                .build();
        int estado = enviarSinCuerpo(peticion);
        if (estado != 200) { throw fallo("subir", estado); }
        OptionalLong guardado = tamano(objeto);
        if (guardado.isEmpty() || guardado.getAsLong() != tamano) { throw fallo("verificar", 0); }
    }

    /** Tamaño del objeto, o vacío si no existe (Supabase responde 400 o 404 según la versión). */
    public OptionalLong tamano(String objeto) {
        HttpRequest peticion = autenticada(URI.create(base + "/authenticated/" + bucket + "/" + objeto))
                .timeout(Duration.ofSeconds(20))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            HttpResponse<Void> respuesta = http.send(peticion, HttpResponse.BodyHandlers.discarding());
            int estado = respuesta.statusCode();
            if (estado == 404 || estado == 400) { return OptionalLong.empty(); }
            if (estado != 200) { throw fallo("consultar", estado); }
            return respuesta.headers().firstValueAsLong("content-length");
        } catch (IOException error) {
            throw fallo("consultar", 0);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw fallo("consultar", 0);
        }
    }

    /** Flujo del objeto; el llamador verifica tamaño y SHA-256. Si no existe, {@link NoSuchFileException}. */
    public InputStream descargar(String objeto) {
        HttpRequest peticion = autenticada(URI.create(base + "/authenticated/" + bucket + "/" + objeto))
                .timeout(Duration.ofMinutes(5)).GET().build();
        try {
            HttpResponse<InputStream> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofInputStream());
            int estado = respuesta.statusCode();
            if (estado == 200) { return respuesta.body(); }
            respuesta.body().close();
            if (estado == 404 || estado == 400) {
                throw new UncheckedIOException(new NoSuchFileException("respaldo"));
            }
            throw fallo("descargar", estado);
        } catch (IOException error) {
            throw fallo("descargar", 0);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw fallo("descargar", 0);
        }
    }

    /** Borra los objetos indicados (rutas exactas); idempotente: borrar uno inexistente no es un error. */
    public void borrar(java.util.List<String> objetos) {
        if (objetos.isEmpty()) { return; }
        for (String objeto : objetos) {
            // Las rutas las genera el servidor; aun así, nada que pueda romper el JSON.
            if (!objeto.matches("[A-Za-z0-9._/-]{1,400}")) { throw new IllegalArgumentException("Ruta inválida"); }
        }
        String cuerpo = "{\"prefixes\":[\"" + String.join("\",\"", objetos) + "\"]}";
        HttpRequest peticion = autenticada(URI.create(base + "/" + bucket))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8))
                .build();
        int estado = enviarSinCuerpo(peticion);
        if (estado != 200 && estado != 404) { throw fallo("borrar", estado); }
    }

    private HttpRequest.Builder autenticada(URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri).header("apikey", clave);
        // Las claves heredadas (service_role) son JWT; las nuevas sb_secret_ solo van en apikey.
        if (clave.startsWith("eyJ")) { builder.header("Authorization", "Bearer " + clave); }
        return builder;
    }

    private int enviarSinCuerpo(HttpRequest peticion) {
        try {
            return http.send(peticion, HttpResponse.BodyHandlers.discarding()).statusCode();
        } catch (IOException error) {
            throw fallo("conectar", 0);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw fallo("conectar", 0);
        }
    }

    private static UncheckedIOException fallo(String operacion, int estado) {
        // Sin causa ni cuerpo: pueden incluir cabeceras o detalles del proveedor.
        return new UncheckedIOException(new IOException("Respaldo Supabase no disponible al " + operacion
                + (estado > 0 ? " (HTTP " + estado + ")" : "")));
    }

    @Override
    public String toString() { return "RespaldoSupabaseStorage[PROTEGIDO]"; }
}
