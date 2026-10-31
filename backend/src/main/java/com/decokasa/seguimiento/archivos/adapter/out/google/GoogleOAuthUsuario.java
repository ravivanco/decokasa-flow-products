package com.decokasa.seguimiento.archivos.adapter.out.google;

import com.decokasa.seguimiento.archivos.adapter.out.google.AlmacenTokensGoogle.Tokens;
import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle.Inicio;
import com.decokasa.seguimiento.archivos.application.port.out.ConexionOAuthGoogle;
import com.decokasa.seguimiento.archivos.domain.GoogleNoDisponible;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OAuth de usuario: endpoints fijos, sin red al arrancar o iniciar; solo el callback intercambia el código.
 *
 * <p>Retorno local ({@link #CALLBACK}, desarrollo): la cookie de vínculo la fija {@code POST /inicio} en el mismo
 * origen. Retorno público HTTPS (ADR-0034, Render): el frontend vive en otro dominio, así que {@code /inicio} devuelve
 * una URL de salida de un solo uso en el dominio del callback; al abrirla, {@link #salida} vuelve a vincular la solicitud
 * a una cookie nueva de ese dominio y redirige a Google.
 */
public final class GoogleOAuthUsuario implements ConexionOAuthGoogle,
        com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle {
    public static final String CALLBACK = "http://localhost:8080/api/v1/integraciones/google/oauth2/callback";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EMAIL = "https://www.googleapis.com/auth/userinfo.email";
    private static final Set<String> AMBITOS = Set.of("openid", EMAIL,
            "https://www.googleapis.com/auth/gmail.send", "https://www.googleapis.com/auth/drive.file");
    private static final Duration VIGENCIA = Duration.ofMinutes(5);
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("decokasa.google.oauth");
    /** Retorno público: solo HTTPS, nombre de host en minúsculas, ruta fija, sin puerto, query ni fragmento. */
    private static final Pattern PUBLICO = Pattern.compile(
            "https://[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?)+"
                    + "/api/v1/integraciones/google/oauth2/callback");
    private static final long SIETE_DIAS = 7 * 86400L;
    /** App publicada sin refresh_token_expires_in: no caduca por tiempo; fecha nominal para el contenedor. */
    private static final Duration SIN_CADUCIDAD = Duration.ofDays(3650);
    private final boolean habilitada;
    private final SecretosGoogle secretos;
    private final String callback;
    /** Null con el retorno local; con el público, la URL de salida en el dominio del callback. */
    private final String salida;
    /** true solo con la app de Google publicada ("En producción"): sin el tope de siete días de "En pruebas". */
    private final boolean produccion;
    private final String correo;
    private final JsonMapper json;
    private final Reloj reloj;
    private final Consumer<UUID> revalidarAdmin;
    private final SecureRandom azar = new SecureRandom();
    private final Map<String, Solicitud> pendientes = new HashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private final NimbusJwtDecoder idTokens;

    /** Paso 3.1: archivos privados locales, retorno local y modo "En pruebas". */
    public GoogleOAuthUsuario(boolean habilitada, String clienteRuta, String tokensRuta, String llaveRuta,
            String correo, String callback, JsonMapper json, Reloj reloj, Consumer<UUID> revalidarAdmin) {
        this(habilitada, SecretosGoogle.archivos(clienteRuta, tokensRuta, llaveRuta), correo, callback, false,
                json, reloj, revalidarAdmin);
    }

    /** ADR-0034: origen de secretos, retorno (local o público HTTPS) y modo de publicación configurables. */
    public GoogleOAuthUsuario(boolean habilitada, SecretosGoogle secretos, String correo, String callback,
            boolean produccion, JsonMapper json, Reloj reloj, Consumer<UUID> revalidarAdmin) {
        this.habilitada = habilitada;
        this.secretos = secretos;
        this.produccion = produccion;
        this.correo = correo.strip().toLowerCase(java.util.Locale.ROOT);
        this.json = json;
        this.reloj = reloj;
        this.revalidarAdmin = revalidarAdmin;
        if (CALLBACK.equals(callback)) {
            this.salida = null;
        } else if (callback != null && PUBLICO.matcher(callback).matches()) {
            this.salida = callback.substring(0, callback.length() - "/callback".length()) + "/salida";
        } else {
            throw new IllegalArgumentException("El retorno OAuth no es el local ni una URL HTTPS válida");
        }
        this.callback = callback;
        // El constructor por JWK URI no hace discovery ni descarga claves al iniciar la aplicación.
        SimpleClientHttpRequestFactory transporteJwk = new SimpleClientHttpRequestFactory();
        transporteJwk.setConnectTimeout(Duration.ofSeconds(5));
        transporteJwk.setReadTimeout(Duration.ofSeconds(10));
        idTokens = NimbusJwtDecoder.withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .restOperations(new RestTemplate(transporteJwk))
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        idTokens.setJwtValidator(JwtValidators.createDefault());
    }

    @Override
    public synchronized Inicio iniciar(UUID adminId, String vinculoNavegador) {
        exigirVinculo(vinculoNavegador);
        Cliente cliente = leerCliente();
        try { almacen().preparar(); }
        catch (IOException error) { throw new GoogleNoDisponible(); }
        Instant ahora = reloj.ahora();
        pendientes.values().removeIf(s -> !ahora.isBefore(s.pendiente().caducaEn())
                || s.pendiente().adminId().equals(adminId));
        if (pendientes.size() >= 8) {
            throw new DatoInvalido("Hay conexiones Google en curso. Espera cinco minutos antes de iniciar otra.");
        }
        String estado = aleatorio();
        String verificador = aleatorio();
        String nonce = aleatorio();
        Instant caduca = ahora.plus(VIGENCIA);
        Map<String, String> parametros = new LinkedHashMap<>();
        parametros.put("client_id", cliente.id());
        parametros.put("redirect_uri", callback);
        parametros.put("response_type", "code");
        parametros.put("scope", AMBITOS.stream().sorted().collect(Collectors.joining(" ")));
        parametros.put("access_type", "offline");
        parametros.put("prompt", "consent select_account");
        parametros.put("state", estado);
        parametros.put("nonce", nonce);
        parametros.put("login_hint", correo);
        parametros.put("code_challenge", huella(verificador));
        parametros.put("code_challenge_method", "S256");
        String google = "https://accounts.google.com/o/oauth2/v2/auth?" + formulario(parametros);
        Pendiente pendiente = new Pendiente(adminId, verificador, nonce, caduca);
        if (salida == null) {
            pendientes.put(huella(estado), new Solicitud(pendiente, huella(vinculoNavegador), cliente.id(), null, null));
            return new Inicio(google, caduca);
        }
        // Retorno público: el valor recibido es un ticket de un solo uso; el vínculo real lo fija la salida.
        pendientes.put(huella(estado), new Solicitud(pendiente, null, cliente.id(), huella(vinculoNavegador), google));
        return new Inicio(salida + "?t=" + vinculoNavegador, caduca);
    }

    /**
     * Retorno público (ADR-0034): canjea el ticket de un solo uso, vincula la solicitud a la cookie nueva del dominio del
     * callback y devuelve la URL de Google. El ticket deja de valer al usarlo.
     */
    @Override
    public synchronized String salida(String ticket, String vinculoNavegador) {
        if (salida == null) { throw estadoInvalido(); }
        exigirVinculo(ticket);
        exigirVinculo(vinculoNavegador);
        String clave = huella(ticket);
        Instant ahora = reloj.ahora();
        for (Map.Entry<String, Solicitud> entrada : pendientes.entrySet()) {
            Solicitud s = entrada.getValue();
            if (s.ticket() != null && iguales(s.ticket(), clave)) {
                if (!ahora.isBefore(s.pendiente().caducaEn())) {
                    pendientes.remove(entrada.getKey());
                    throw estadoInvalido();
                }
                entrada.setValue(new Solicitud(s.pendiente(), huella(vinculoNavegador), s.clienteId(), null, null));
                return s.urlGoogle();
            }
        }
        throw estadoInvalido();
    }

    @Override
    public synchronized Pendiente consumir(String estado, String vinculoNavegador) {
        exigirVinculo(vinculoNavegador);
        if (estado == null || !estado.matches("[A-Za-z0-9_-]{43}")) { throw estadoInvalido(); }
        String clave = huella(estado);
        Solicitud solicitud = pendientes.get(clave);
        if (solicitud == null || !iguales(solicitud.vinculo(), huella(vinculoNavegador))) { throw estadoInvalido(); }
        pendientes.remove(clave);
        if (!reloj.ahora().isBefore(solicitud.pendiente().caducaEn())) { throw estadoInvalido(); }
        // La credencial no puede cambiar entre inicio y canje: el PKCE queda vinculado a ese cliente.
        if (!iguales(solicitud.clienteId(), leerCliente().id())) { throw estadoInvalido(); }
        return solicitud.pendiente();
    }

    @Override
    public synchronized void conectar(Pendiente pendiente, String codigo) {
        Cliente cliente = leerCliente();
        if (!reloj.ahora().isBefore(pendiente.caducaEn())) { throw estadoInvalido(); }
        try {
            Map<String, String> parametros = Map.of("client_id", cliente.id(), "client_secret", cliente.secreto(),
                    "code", codigo, "grant_type", "authorization_code", "redirect_uri", callback,
                    "code_verifier", pendiente.verificador());
            HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_URL)).timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario(parametros))).build();
            // El subscriber termina dentro del timeout de la petición y corta antes de acumular más de 64 KiB.
            CompletableFuture<HttpResponse<byte[]>> envio = http.sendAsync(request, info -> new RespuestaLimitada());
            HttpResponse<byte[]> respuesta;
            try { respuesta = envio.get(20, TimeUnit.SECONDS); }
            catch (TimeoutException | InterruptedException error) { envio.cancel(true); throw error; }
            JsonNode resultado;
            byte[] bytes = respuesta.body();
            try {
                if (respuesta.statusCode() != 200) { throw new GoogleNoDisponible(); }
                resultado = json.readTree(bytes);
            } finally { Arrays.fill(bytes, (byte) 0); }
            Jwt identidad = idTokens.decode(obligatorio(resultado, "id_token"));
            String emisor = identidad.getIssuer() == null ? "" : identidad.getIssuer().toString();
            String azp = identidad.getClaimAsString("azp");
            if (!("https://accounts.google.com".equals(emisor) || "accounts.google.com".equals(emisor))
                    || !identidad.getAudience().contains(cliente.id())
                    || identidad.getExpiresAt() == null || !reloj.ahora().isBefore(identidad.getExpiresAt())
                    || identidad.getIssuedAt() == null || identidad.getIssuedAt().isAfter(reloj.ahora().plusSeconds(60))
                    || (azp != null && !cliente.id().equals(azp))
                    || (identidad.getAudience().size() > 1 && azp == null)
                    || !iguales(pendiente.nonce(), identidad.getClaimAsString("nonce"))
                    || !Boolean.TRUE.equals(identidad.getClaims().get("email_verified"))
                    || !correo.equalsIgnoreCase(identidad.getClaimAsString("email"))
                    || identidad.getSubject() == null || identidad.getSubject().isBlank()) {
                throw new GoogleNoDisponible();
            }
            // OAuth permite omitir scope si coincide con el solicitado; si está presente, exige todos los permisos.
            String scopes = texto(resultado, "scope");
            Set<String> concedidos = scopes.isBlank() ? AMBITOS : Arrays.stream(scopes.split(" +"))
                    .map(s -> "email".equals(s) ? EMAIL : s).collect(Collectors.toSet());
            if (!concedidos.containsAll(AMBITOS) || !"Bearer".equalsIgnoreCase(obligatorio(resultado, "token_type"))) {
                throw new GoogleNoDisponible();
            }
            long segundos = resultado.path("expires_in").asLong(0);
            if (segundos <= 0 || segundos > 86400) { throw new GoogleNoDisponible(); }
            Instant ahora = reloj.ahora();
            Instant offlineHasta;
            if (resultado.has("refresh_token_expires_in")) {
                long offline = resultado.path("refresh_token_expires_in").asLong(0);
                if (offline <= 0) { throw new GoogleNoDisponible(); }
                offlineHasta = ahora.plusSeconds(produccion ? offline : Math.min(offline, SIETE_DIAS));
            } else {
                // "En pruebas" Google caduca el refresh token a los siete días aunque no lo informe.
                offlineHasta = produccion ? ahora.plus(SIN_CADUCIDAD) : ahora.plusSeconds(SIETE_DIAS);
            }
            revalidarAdmin.accept(pendiente.adminId());
            almacen().guardar(new Tokens(cliente.id(), identidad.getSubject(), correo,
                    obligatorio(resultado, "access_token"), texto(resultado, "refresh_token"),
                    ahora.plusSeconds(segundos), offlineHasta, String.join(" ", concedidos)), ahora);
            LOG.info("google.oauth autorización guardada produccion={} autorizacion_hasta={}", produccion, offlineHasta);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new GoogleNoDisponible();
        } catch (com.decokasa.seguimiento.compartido.domain.SinPermiso error) {
            throw error;
        } catch (Exception error) {
            // No adjuntar la causa: Jackson, JWT y HTTP pueden incluir secretos en mensajes o cuerpos.
            LOG.warn("google.oauth no se completó la autorización tipo={}", error.getClass().getSimpleName());
            throw new GoogleNoDisponible();
        }
    }

    /** Credencial interna de backend. Nunca se serializa en respuestas; renovación offline solo al solicitarla. */
    @Override public synchronized Credencial credencial() {
        var causa = com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa.AUTORIZACION_REQUERIDA;
        if (!habilitada) {
            throw new com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle(
                    com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa.DESACTIVADA);
        }
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle(
                    com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa.TRANSACCION_ACTIVA);
        }
        try {
            Cliente cliente = leerCliente();
            Tokens tokens = almacen().vigente(reloj.ahora(), anterior -> renovar(cliente, anterior));
            if (!cliente.id().equals(tokens.clienteId()) || !correo.equals(tokens.correo())
                    || !reloj.ahora().plusSeconds(30).isBefore(tokens.accessHasta())
                    || !reloj.ahora().isBefore(tokens.autorizacionHasta())
                    || !Set.of(tokens.ambitos().split(" +")).containsAll(AMBITOS)) {
                throw new GoogleNoDisponible();
            }
            return new Credencial(correo, tokens.accessToken());
        } catch (FalloIntegracionGoogle error) {
            LOG.warn("google.oauth credencial no disponible causa={}", error.causa());
            throw error;
        } catch (Exception error) {
            // Solo el tipo: los mensajes de JWT, JSON o HTTP pueden contener datos sensibles.
            LOG.warn("google.oauth credencial no disponible causa={} tipo={}", causa, error.getClass().getSimpleName());
            throw new com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle(causa);
        }
    }

    private Tokens renovar(Cliente cliente, Tokens anterior) {
        if (!cliente.id().equals(anterior.clienteId()) || !correo.equals(anterior.correo())
                || anterior.refreshToken().isBlank()
                || !Set.of(anterior.ambitos().split(" +")).containsAll(AMBITOS)) { throw new GoogleNoDisponible(); }
        try {
            HttpRequest peticion = HttpRequest.newBuilder(URI.create(TOKEN_URL)).timeout(Duration.ofSeconds(20))
                    .header("Content-Type","application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formulario(Map.of("client_id",cliente.id(),
                            "client_secret",cliente.secreto(),"refresh_token",anterior.refreshToken(),
                            "grant_type","refresh_token")))).build();
            CompletableFuture<HttpResponse<byte[]>> futuro = http.sendAsync(peticion, info -> new RespuestaLimitada());
            HttpResponse<byte[]> respuesta;
            try { respuesta = futuro.get(20,TimeUnit.SECONDS); }
            catch (InterruptedException error) {
                futuro.cancel(true);Thread.currentThread().interrupt();throw new FalloIntegracionGoogle(Causa.TRANSITORIA);
            } catch (Exception error) {
                futuro.cancel(true);throw new FalloIntegracionGoogle(Causa.TRANSITORIA);
            }
            byte[] bytes = respuesta.body();
            JsonNode resultado;
            try {
                int estado=respuesta.statusCode();
                if (estado!=200) {
                    throw new FalloIntegracionGoogle(estado==429 ? Causa.CUOTA
                            : estado>=500 || estado==408 ? Causa.TRANSITORIA : Causa.AUTORIZACION_REQUERIDA);
                }
                resultado = json.readTree(bytes);
            } finally { Arrays.fill(bytes,(byte)0); }
            long segundos = resultado.path("expires_in").asLong(0);
            String scopes = texto(resultado,"scope");
            Set<String> concedidos = scopes.isBlank() ? Set.of(anterior.ambitos().split(" +"))
                    : Arrays.stream(scopes.split(" +")).map(s -> "email".equals(s) ? EMAIL : s).collect(Collectors.toSet());
            if (segundos < 1 || segundos > 86400 || !concedidos.containsAll(AMBITOS)
                    || !"Bearer".equalsIgnoreCase(obligatorio(resultado,"token_type"))) { throw new GoogleNoDisponible(); }
            String refresh = texto(resultado,"refresh_token");
            Instant grantHasta = anterior.autorizacionHasta();
            if (resultado.has("refresh_token_expires_in")) {
                long duracion = resultado.path("refresh_token_expires_in").asLong(0);
                if (duracion < 1) { throw new GoogleNoDisponible(); }
                Instant limite = reloj.ahora().plusSeconds(produccion ? duracion : Math.min(duracion,SIETE_DIAS));
                if (limite.isBefore(grantHasta)) { grantHasta = limite; }
            }
            return new Tokens(cliente.id(),anterior.cuentaId(),correo,obligatorio(resultado,"access_token"),
                    refresh.isBlank() ? anterior.refreshToken() : refresh,reloj.ahora().plusSeconds(segundos),
                    grantHasta,String.join(" ",concedidos));
        } catch (FalloIntegracionGoogle error) { throw error; }
        catch (Exception error) { throw new GoogleNoDisponible(); }
    }

    private Cliente leerCliente() {
        if (!habilitada || !correo.matches("[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@(gmail\\.com|googlemail\\.com)")) {
            throw new GoogleNoDisponible();
        }
        try {
            SecretosGoogle.Cliente leido = secretos.cliente(json, callback);
            if (!leido.id().endsWith(".apps.googleusercontent.com")) { throw new GoogleNoDisponible(); }
            return new Cliente(leido.id(), leido.secreto());
        } catch (Exception error) { throw new GoogleNoDisponible(); }
    }

    private BovedaTokensGoogle almacen() throws IOException {
        return secretos.boveda(json);
    }

    private String aleatorio() {
        byte[] bytes = new byte[32]; azar.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String huella(String valor) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 no disponible"); }
    }

    private static boolean iguales(String a, String b) {
        return a != null && b != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    private static void exigirVinculo(String vinculo) {
        if (vinculo == null || !vinculo.matches("[A-Za-z0-9_-]{43}")) { throw estadoInvalido(); }
    }

    private static DatoInvalido estadoInvalido() {
        return new DatoInvalido("La conexión Google no es válida o ha caducado. Iníciala en este navegador de nuevo.");
    }

    private static String texto(JsonNode nodo, String campo) {
        JsonNode valor = nodo.get(campo);
        return valor != null && valor.isString() ? valor.asString() : "";
    }

    private static String obligatorio(JsonNode nodo, String campo) {
        String valor = texto(nodo, campo);
        if (valor.isBlank() || valor.length() > 16384) { throw new GoogleNoDisponible(); }
        return valor;
    }

    private static String formulario(Map<String, String> valores) {
        return valores.entrySet().stream().map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
    }

    private record Cliente(String id, String secreto) {
        @Override public String toString() { return "ClienteGoogle[PROTEGIDO]"; }
    }
    /** vinculo: huella de la cookie; ticket y urlGoogle: solo con retorno público hasta canjear la salida. */
    private record Solicitud(Pendiente pendiente, String vinculo, String clienteId, String ticket, String urlGoogle) {
        @Override public String toString() { return "SolicitudGoogle[PROTEGIDO]"; }
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
                    resultado.completeExceptionally(new IOException("Respuesta OAuth demasiado grande"));
                    return;
                }
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                contenido.writeBytes(bytes);
                Arrays.fill(bytes, (byte) 0);
            }
        }
        @Override public void onError(Throwable error) {
            resultado.completeExceptionally(new IOException("No se pudo recibir la respuesta OAuth"));
        }
        @Override public void onComplete() { resultado.complete(contenido.toByteArray()); }
    }
}
