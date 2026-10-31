package com.decokasa.seguimiento.archivos.adapter.out.google;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import javax.sql.DataSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Origen del cliente OAuth, de la llave AES-256 y de la bóveda de tokens (ADR-0034). Nada se lee al construir: los
 * errores aparecen al usar Google, nunca al arrancar.
 *
 * <ul>
 *   <li>{@link #archivos}: desarrollo y Docker con disco persistente. JSON del cliente, llave y tokens en archivos
 *       privados fuera de Git (paso 3.1).</li>
 *   <li>{@link #entorno}: Render Free. Cliente y llave en variables de entorno secretas; tokens cifrados en
 *       PostgreSQL ({@code credencial_google}). No necesita disco.</li>
 * </ul>
 */
public abstract sealed class SecretosGoogle {

    /** Devuelve una copia nueva de la llave de 32 bytes; quien la recibe la borra después de usarla. */
    @FunctionalInterface
    public interface FuenteLlave {
        byte[] leer() throws IOException;
    }

    record Cliente(String id, String secreto) {
        @Override public String toString() { return "ClienteGoogle[PROTEGIDO]"; }
    }

    private SecretosGoogle() { }

    public static SecretosGoogle archivos(String clienteRuta, String tokensRuta, String llaveRuta) {
        return new Archivos(clienteRuta, tokensRuta, llaveRuta);
    }

    public static SecretosGoogle entorno(String clienteId, String clienteSecreto, String llaveBase64,
            DataSource fuente) {
        return new Entorno(clienteId, clienteSecreto, llaveBase64, fuente);
    }

    /** Cliente web OAuth; si el JSON trae redirect_uris, debe incluir exactamente {@code callback}. */
    abstract Cliente cliente(JsonMapper json, String callback) throws IOException;

    abstract BovedaTokensGoogle boveda(JsonMapper json) throws IOException;

    public abstract FuenteLlave llave();

    private static final class Archivos extends SecretosGoogle {
        private final String clienteRuta;
        private final String tokensRuta;
        private final String llaveRuta;

        private Archivos(String clienteRuta, String tokensRuta, String llaveRuta) {
            this.clienteRuta = clienteRuta;
            this.tokensRuta = tokensRuta;
            this.llaveRuta = llaveRuta;
        }

        @Override
        Cliente cliente(JsonMapper json, String callback) throws IOException {
            byte[] bytes = ArchivosPrivadosGoogle.leer(ArchivosPrivadosGoogle.externo(clienteRuta), 65536);
            try {
                JsonNode web = json.readTree(bytes).path("web");
                // El JSON descargado puede omitir esta metadata. El retorno usado siempre es el configurado, que debe
                // estar registrado en Cloud; nunca se obtiene del navegador ni de esta lista.
                if (web.has("redirect_uris")) {
                    JsonNode retornos = web.get("redirect_uris");
                    if (!retornos.isArray()) { throw new IOException("Cliente inválido"); }
                    boolean retornoExacto = false;
                    for (JsonNode uri : retornos) {
                        if (!uri.isString()) { throw new IOException("Cliente inválido"); }
                        retornoExacto |= callback.equals(uri.asString());
                    }
                    if (!retornoExacto) { throw new IOException("Retorno no registrado"); }
                }
                return new Cliente(texto(web, "client_id"), texto(web, "client_secret"));
            } finally { Arrays.fill(bytes, (byte) 0); }
        }

        @Override
        BovedaTokensGoogle boveda(JsonMapper json) throws IOException {
            Path cliente = ArchivosPrivadosGoogle.externo(clienteRuta);
            Path tokens = ArchivosPrivadosGoogle.externo(tokensRuta);
            Path llave = ArchivosPrivadosGoogle.externo(llaveRuta);
            if (cliente.equals(tokens) || cliente.equals(llave)) { throw new IOException("Rutas distintas requeridas"); }
            return new AlmacenTokensGoogle(tokens, llave, json);
        }

        @Override
        public FuenteLlave llave() {
            return () -> ArchivosPrivadosGoogle.leer(ArchivosPrivadosGoogle.externo(llaveRuta), 32);
        }
    }

    private static final class Entorno extends SecretosGoogle {
        private final String clienteId;
        private final String clienteSecreto;
        private final String llaveBase64;
        private final DataSource fuente;
        private volatile BovedaTokensGoogle boveda;

        private Entorno(String clienteId, String clienteSecreto, String llaveBase64, DataSource fuente) {
            this.clienteId = clienteId == null ? "" : clienteId.strip();
            this.clienteSecreto = clienteSecreto == null ? "" : clienteSecreto.strip();
            this.llaveBase64 = llaveBase64 == null ? "" : llaveBase64.strip();
            this.fuente = fuente;
        }

        @Override
        Cliente cliente(JsonMapper json, String callback) throws IOException {
            if (clienteId.isEmpty() || clienteSecreto.isEmpty() || clienteId.length() > 512
                    || clienteSecreto.length() > 512) {
                throw new IOException("Faltan GOOGLE_OAUTH_CLIENTE_ID o GOOGLE_OAUTH_CLIENTE_SECRETO");
            }
            return new Cliente(clienteId, clienteSecreto);
        }

        @Override
        synchronized BovedaTokensGoogle boveda(JsonMapper json) {
            // Una sola instancia: su synchronized serializa renovaciones dentro del proceso.
            if (boveda == null) { boveda = new AlmacenTokensGoogleSql(fuente, llave(), json); }
            return boveda;
        }

        @Override
        public FuenteLlave llave() {
            return () -> {
                byte[] clave;
                try {
                    String texto = llaveBase64.replace('-', '+').replace('_', '/');
                    clave = Base64.getDecoder().decode(texto.getBytes(StandardCharsets.US_ASCII));
                } catch (IllegalArgumentException error) {
                    throw new IOException("GOOGLE_OAUTH_LLAVE no es Base64");
                }
                if (clave.length != 32) {
                    Arrays.fill(clave, (byte) 0);
                    throw new IOException("GOOGLE_OAUTH_LLAVE debe tener 32 bytes");
                }
                return clave;
            };
        }
    }

    private static String texto(JsonNode nodo, String campo) throws IOException {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isString() || valor.asString().isBlank() || valor.asString().length() > 16384) {
            throw new IOException("Cliente inválido");
        }
        return valor.asString();
    }

    @Override
    public String toString() { return "SecretosGoogle[PROTEGIDO]"; }
}
