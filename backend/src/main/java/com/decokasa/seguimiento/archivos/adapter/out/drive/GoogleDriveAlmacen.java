package com.decokasa.seguimiento.archivos.adapter.out.drive;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Drive v3, Mi unidad. Transporte pasivo; sin workers, carpetas automáticas ni políticas de reintento. */
public final class GoogleDriveAlmacen implements AlmacenArchivos {
    private static final String API = "https://www.googleapis.com/drive/v3/files";
    private static final String CAMPOS = "id,name,size,webViewLink,appProperties,parents,mimeType,sha256Checksum,md5Checksum,trashed,ownedByMe,driveId";
    private final boolean habilitada;
    private final AccesoCuentaGoogle acceso;
    private final JsonMapper json;
    private final com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    public GoogleDriveAlmacen(boolean habilitada, AccesoCuentaGoogle acceso, JsonMapper json,
            com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj) {
        this.habilitada = habilitada; this.acceso = acceso; this.json = json; this.reloj = reloj;
    }
    @Override public String reservarId() {
        JsonNode nodo = contenido(enviar(HttpRequest.newBuilder(URI.create(API + "/generateIds?count=1&space=drive&type=files")).GET()));
        String id = nodo.path("ids").path(0).asString(); validarId(id); return id;
    }
    @Override public Sesion iniciar(Copia copia) { return iniciar(copia,false); }
    @Override public Sesion reiniciar(Copia copia) { return iniciar(copia,true); }
    private Sesion iniciar(Copia copia,boolean actualizar) {
        byte[] datos = actualizar ? "{}".getBytes(java.nio.charset.StandardCharsets.UTF_8) : json.writeValueAsBytes(Map.of("id",copia.idReservado(),"name",copia.nombre(),"mimeType",copia.tipoDetectado(),
                "parents",List.of(copia.carpetaId()),"appProperties",Map.of("adjuntoId",copia.adjuntoId().toString(),
                        "pedidoId",copia.pedidoId().toString(),"entradaId",copia.entradaId().toString())));
        HttpResponse<byte[]> respuesta = enviar(HttpRequest.newBuilder(URI.create(
                "https://www.googleapis.com/upload/drive/v3/files"+(actualizar ? "/"+copia.idReservado() : "")
                        +"?uploadType=resumable&fields=" + CAMPOS))
                .header("Content-Type","application/json; charset=UTF-8")
                .header("X-Upload-Content-Type",copia.tipoDetectado())
                .header("X-Upload-Content-Length",Long.toString(copia.tamano()))
                .method(actualizar ? "PATCH" : "POST",HttpRequest.BodyPublishers.ofByteArray(datos)));
        exigirExito(respuesta);
        String uri = respuesta.headers().firstValue("Location").orElseThrow(() -> fallo(Causa.RESPUESTA_INVALIDA));
        sesion(uri); return new Sesion(uri);
    }
    @Override public Progreso consultar(Sesion sesion, long total) {
        validarTotal(total);
        return progreso(enviar(HttpRequest.newBuilder(sesion(sesion.uri())).header("Content-Range","bytes */"+total)
                .PUT(HttpRequest.BodyPublishers.noBody())),total);
    }
    @Override public Progreso enviarFragmento(Sesion sesion, long inicio, long total, byte[] fragmento) {
        validarTotal(total);
        if (fragmento == null || fragmento.length < 1 || fragmento.length > 8 * 1024 * 1024 || inicio < 0
                || inicio > total - fragmento.length
                || (inicio + fragmento.length < total && fragmento.length % 262144 != 0)) {
            throw new IllegalArgumentException("Fragmento Drive inválido");
        }
        return progreso(enviar(HttpRequest.newBuilder(sesion(sesion.uri())).header("Content-Type","application/octet-stream")
                .header("Content-Range","bytes "+inicio+"-"+(inicio+fragmento.length-1)+"/"+total)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(fragmento))),total);
    }
    @Override public Optional<Remoto> metadatos(String id) {
        validarId(id);
        HttpResponse<byte[]> respuesta = enviar(HttpRequest.newBuilder(URI.create(API+"/"+id+"?fields="+CAMPOS)).GET());
        if (respuesta.statusCode() == 404) { return Optional.empty(); }
        return Optional.of(remoto(contenido(respuesta)));
    }
    private Progreso progreso(HttpResponse<byte[]> respuesta, long total) {
        if (respuesta.statusCode()==404 || respuesta.statusCode()==410) { throw fallo(Causa.SESION_CADUCADA); }
        if (respuesta.statusCode() == 308) {
            String rango = respuesta.headers().firstValue("Range").orElse("");
            if (rango.isEmpty()) { return new Progreso(0,Optional.empty()); }
            if (!rango.matches("bytes=0-[0-9]{1,12}")) { throw fallo(Causa.RESPUESTA_INVALIDA); }
            long siguiente = Long.parseLong(rango.substring(8)) + 1;
            if (siguiente > total) { throw fallo(Causa.RESPUESTA_INVALIDA); }
            return new Progreso(siguiente,Optional.empty());
        }
        Remoto remoto = remoto(contenido(respuesta));
        if (remoto.tamano() != total) { throw fallo(Causa.RESPUESTA_INVALIDA); }
        return new Progreso(total,Optional.of(remoto));
    }
    @Override public Remoto crearCarpeta(String id,String nombre,String padre,String clave) {
        validarId(id);validarId(padre);
        if (nombre==null || nombre.isBlank() || nombre.codePointCount(0,nombre.length())>255
                || clave==null || clave.length()>100) { throw fallo(Causa.CONFIGURACION); }
        byte[] cuerpo=json.writeValueAsBytes(Map.of("id",id,"name",nombre,
                "mimeType","application/vnd.google-apps.folder","parents",List.of(padre),
                "appProperties",Map.of("carpetaClave",clave)));
        return remoto(contenido(enviar(HttpRequest.newBuilder(URI.create(API+"?fields="+CAMPOS))
                .header("Content-Type","application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo)))));
    }
    private Remoto remoto(JsonNode nodo) {
        String id=texto(nodo,"id");validarId(id);
        var padres=new java.util.ArrayList<String>();
        for (JsonNode padre:nodo.path("parents")) { String valor=padre.asString();validarId(valor);padres.add(valor); }
        JsonNode props=nodo.path("appProperties");
        return new Remoto(id,nodo.path("size").asLong(-1),texto(nodo,"webViewLink"),
                texto(props,"adjuntoId"),texto(props,"pedidoId"),texto(props,"entradaId"),padres,
                texto(nodo,"mimeType"),texto(nodo,"sha256Checksum"),texto(nodo,"md5Checksum"),
                nodo.path("trashed").asBoolean(false),nodo.path("ownedByMe").asBoolean(false),
                texto(nodo,"driveId"),texto(props,"carpetaClave"),texto(nodo,"name"));
    }
    private static String texto(JsonNode nodo,String campo) {
        String valor=nodo.path(campo).asString();return valor==null ? "" : valor;
    }
    private JsonNode contenido(HttpResponse<byte[]> respuesta) {
        exigirExito(respuesta);
        try { return json.readTree(respuesta.body()); }
        catch (Exception error) { throw fallo(Causa.RESPUESTA_INVALIDA); }
    }
    private static URI sesion(String valor) {
        try {
            URI uri = URI.create(valor);
            if (!"https".equals(uri.getScheme()) || !"www.googleapis.com".equals(uri.getHost())
                    || uri.getPort() != -1 || uri.getUserInfo() != null || uri.getFragment() != null
                    || !uri.getPath().matches("/upload/drive/v3/files(?:/[A-Za-z0-9_-]{1,200})?") || uri.getRawQuery() == null
                    || valor.length() > 4096 || !uri.getRawQuery().matches("[A-Za-z0-9_%=&.+-]+")) {
                throw fallo(Causa.RESPUESTA_INVALIDA);
            }
            return uri;
        } catch (IllegalArgumentException error) { throw fallo(Causa.RESPUESTA_INVALIDA); }
    }
    private static void validarId(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{1,200}")) { throw fallo(Causa.RESPUESTA_INVALIDA); }
    }
    private static void validarTotal(long total) {
        if (total < 1 || total > 104857600L) { throw new IllegalArgumentException("Tamaño fuera del límite provisional"); }
    }
    private void exigirExito(HttpResponse<byte[]> respuesta) {
        int estado=respuesta.statusCode();
        if (estado>=200 && estado<300) { return; }
        boolean cuota=estado==429;
        if (estado==403) {
            try {
                String razon=texto(json.readTree(respuesta.body()).path("error").path("errors").path(0),"reason");
                cuota=java.util.Set.of("rateLimitExceeded","userRateLimitExceeded","dailyLimitExceeded").contains(razon);
            } catch (Exception error) {
                // Sin un cuerpo válido no se deduce una cuota; el fallo HTTP sigue siendo explícito y cerrado.
                throw fallo(Causa.PERMANENTE);
            }
        }
        Causa causa=estado==401 ? Causa.AUTORIZACION_REQUERIDA : cuota ? Causa.CUOTA
                : estado==409 ? Causa.CONFLICTO_REMOTO : estado>=500 || estado==408 ? Causa.TRANSITORIA : Causa.PERMANENTE;
        java.time.Duration espera=java.time.Duration.ZERO;
        String retry=respuesta.headers().firstValue("Retry-After").orElse("");
        try {
            if (retry.matches("[0-9]{1,8}")) { espera=java.time.Duration.ofSeconds(Long.parseLong(retry)); }
            else if (!retry.isBlank()) {
                espera=java.time.Duration.between(reloj.ahora(),java.time.ZonedDateTime.parse(retry,
                        java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant());
            }
        } catch (java.time.DateTimeException error) {
            // El valor inválido no controla la espera; se conserva el backoff de aplicación.
            espera=java.time.Duration.ZERO;
        }
        throw new FalloIntegracionGoogle(causa,espera);
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
    private HttpResponse<byte[]> enviar(HttpRequest.Builder peticion) {
        if (!habilitada) { throw fallo(Causa.DESACTIVADA); }
        if (TransactionSynchronizationManager.isActualTransactionActive()) { throw fallo(Causa.TRANSACCION_ACTIVA); }
        return transportar(peticion.timeout(Duration.ofSeconds(20))
                .header("Authorization","Bearer "+acceso.credencial().tokenAcceso()).build());
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
