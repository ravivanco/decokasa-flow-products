package com.decokasa.seguimiento.notificaciones.adapter.out.gmail;

import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/** Un solo POST, sin reintento oculto. Message-ID estable NO garantiza exactamente una entrega. */
public final class GmailNotificador implements Notificador {
    private final boolean habilitada;
    private final AccesoCuentaGoogle acceso;
    private final JsonMapper json;
    private final com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public GmailNotificador(boolean habilitada, AccesoCuentaGoogle acceso, JsonMapper json,
            com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj) {
        this.habilitada = habilitada; this.acceso = acceso; this.json = json; this.reloj = reloj;
    }
    @Override public Entrega enviar(Correo correo) {
        comprobar();
        AccesoCuentaGoogle.Credencial credencial;
        try { credencial=acceso.credencial(); }
        catch (FalloIntegracionGoogle error) {
            // No messages.send request has been constructed or started yet.
            if (error.causa()==Causa.CUOTA || error.causa()==Causa.TRANSITORIA) {
                throw FalloIntegracionGoogle.rechazoTransitorioConfirmado(error.causa(),error.esperaMinima());
            }
            throw error;
        }
        if (!credencial.correo().matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@(gmail\\.com|googlemail\\.com)")) {
            throw fallo(Causa.AUTORIZACION_REQUERIDA);
        }
        String fecha = java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME.format(
                reloj.ahora().atZone(java.time.ZoneOffset.UTC));
        String mime = "Date: " + fecha + "\r\nFrom: " + credencial.correo() + "\r\nTo: " + correo.destinatario()
                + "\r\nSubject: " + asunto(correo.asunto()) + "\r\nMessage-ID: " + correo.messageId()
                + "\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8"
                + "\r\nContent-Transfer-Encoding: base64\r\n\r\n"
                + Base64.getMimeEncoder(76,new byte[]{13,10}).encodeToString(correo.texto().getBytes(StandardCharsets.UTF_8));
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(mime.getBytes(StandardCharsets.US_ASCII));
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("https://gmail.googleapis.com/gmail/v1/users/me/messages/send"))
                .timeout(Duration.ofSeconds(20)).header("Authorization","Bearer "+credencial.tokenAcceso())
                .header("Content-Type","application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(Map.of("raw",raw)))).build();
        HttpResponse<byte[]> respuesta = transportar(peticion);
        int estado = respuesta.statusCode();
        if (estado < 200 || estado >= 300) {
            // Only a complete structured rejection proves non-acceptance. Status alone is insufficient.
            if (rechazoDeCuotaConfirmado(respuesta)) {
                throw FalloIntegracionGoogle.rechazoTransitorioConfirmado(Causa.CUOTA,retryAfter(respuesta));
            }
            throw fallo(estado == 401 ? Causa.AUTORIZACION_REQUERIDA : estado == 429 ? Causa.RESULTADO_INCIERTO
                    : estado >= 500 || estado == 408 ? Causa.RESULTADO_INCIERTO : Causa.PERMANENTE);
        }
        try {
            String id = json.readTree(respuesta.body()).path("id").asString();
            if (!id.matches("[A-Za-z0-9_-]{1,200}")) { throw fallo(Causa.RESULTADO_INCIERTO); }
            return new Entrega(id);
        } catch (Exception error) { throw fallo(Causa.RESULTADO_INCIERTO); }
    }
    private boolean rechazoDeCuotaConfirmado(HttpResponse<byte[]> respuesta) {
        int estado=respuesta.statusCode();
        if (estado!=403 && estado!=429) { return false; }
        try {
            var raiz=json.readTree(respuesta.body());
            if (raiz.has("id") || raiz.has("threadId")) { return false; }
            var error=raiz.path("error");
            if (error.path("code").asInt()!=estado) { return false; }
            var errores=error.path("errors");
            if (!errores.isArray() || errores.isEmpty()) { return false; }
            for (var detalle:errores) {
                String motivo=detalle.path("reason").asString();
                if (!"usageLimits".equals(detalle.path("domain").asString())
                        || !("rateLimitExceeded".equals(motivo) || "userRateLimitExceeded".equals(motivo))) {
                    return false;
                }
            }
            return true;
        } catch (Exception noConfirmado) { return false; }
    }
    private Duration retryAfter(HttpResponse<byte[]> respuesta) {
        String valor=respuesta.headers().firstValue("Retry-After").orElse("").trim();
        if (valor.isEmpty()) { return Duration.ZERO; }
        try {
            if (valor.matches("[0-9]+")) { return Duration.ofSeconds(Long.parseLong(valor)); }
            var fecha=java.time.ZonedDateTime.parse(valor,java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
            Duration espera=Duration.between(reloj.ahora(),fecha);
            return espera.isNegative() ? Duration.ZERO : espera;
        } catch (java.time.DateTimeException | ArithmeticException | NumberFormatException invalido) {
            // Invalid/overflow Retry-After: stop, do not silently shorten the provider's wait.
            throw fallo(Causa.CONFIGURACION);
        }
    }
    private static String asunto(String valor) {
        StringBuilder resultado = new StringBuilder();
        StringBuilder parte = new StringBuilder();
        for (int punto : valor.codePoints().toArray()) {
            String caracter = new String(Character.toChars(punto));
            if ((parte.toString()+caracter).getBytes(StandardCharsets.UTF_8).length > 30) {
                agregar(resultado,parte.toString()); parte.setLength(0);
            }
            parte.append(caracter);
        }
        agregar(resultado,parte.toString()); return resultado.toString();
    }
    private static void agregar(StringBuilder destino, String parte) {
        if (!destino.isEmpty()) { destino.append("\r\n "); }
        destino.append("=?UTF-8?B?").append(Base64.getEncoder().encodeToString(parte.getBytes(StandardCharsets.UTF_8))).append("?=");
    }
    private void comprobar() {
        if (!habilitada) { throw fallo(Causa.DESACTIVADA); }
        if (TransactionSynchronizationManager.isActualTransactionActive()) { throw fallo(Causa.TRANSACCION_ACTIVA); }
    }
    private static FalloIntegracionGoogle fallo(Causa causa) { return new FalloIntegracionGoogle(causa); }
    private HttpResponse<byte[]> transportar(HttpRequest peticion) {
        CompletableFuture<HttpResponse<byte[]>> futuro = http.sendAsync(peticion, info -> new RespuestaLimitada());
        try { return futuro.get(20,TimeUnit.SECONDS); }
        catch (InterruptedException error) {
            futuro.cancel(true); Thread.currentThread().interrupt(); throw fallo(Causa.RESULTADO_INCIERTO);
        } catch (Exception error) {
            futuro.cancel(true); throw fallo(Causa.RESULTADO_INCIERTO);
        }
    }

    private static final class RespuestaLimitada implements HttpResponse.BodySubscriber<byte[]> {
        private final CompletableFuture<byte[]> resultado = new CompletableFuture<>();
        private final ByteArrayOutputStream contenido = new ByteArrayOutputStream();
        private Flow.Subscription suscripcion;

        @Override public CompletionStage<byte[]> getBody() { return resultado; }
        @Override public void onSubscribe(Flow.Subscription recibida) {
            if (suscripcion != null) { recibida.cancel(); return; }
            suscripcion = recibida;
            recibida.request(Long.MAX_VALUE);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > 65536 - contenido.size()) {
                    suscripcion.cancel();
                    resultado.completeExceptionally(new IOException("Respuesta Google demasiado grande"));
                    return;
                }
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                contenido.writeBytes(bytes);
                Arrays.fill(bytes, (byte) 0);
            }
        }
        @Override public void onError(Throwable error) {
            resultado.completeExceptionally(new IOException("No se pudo recibir la respuesta Google"));
        }
        @Override public void onComplete() { resultado.complete(contenido.toByteArray()); }
    }
}
