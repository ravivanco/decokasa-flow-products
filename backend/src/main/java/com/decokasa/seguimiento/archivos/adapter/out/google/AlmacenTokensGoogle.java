package com.decokasa.seguimiento.archivos.adapter.out.google;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** AES-256-GCM, llave externa separada, bloqueo entre procesos y reemplazo atómico del contenedor. */
final class AlmacenTokensGoogle {
    private static final byte[] VERSION = new byte[] {'D', 'K', 'G', 1};
    private final Path tokens;
    private final Path llave;
    private final JsonMapper json;
    private final SecureRandom azar = new SecureRandom();

    AlmacenTokensGoogle(Path tokens, Path llave, JsonMapper json) throws IOException {
        if (tokens.equals(llave)) { throw new IOException("La llave y los tokens deben estar separados"); }
        this.tokens = tokens;
        this.llave = llave;
        this.json = json;
    }

    void preparar() throws IOException {
        ArchivosPrivadosGoogle.prepararDirectorio(tokens.getParent());
        byte[] clave = ArchivosPrivadosGoogle.leer(llave, 32);
        try { if (clave.length != 32) { throw new IOException("Llave de 32 bytes requerida"); } }
        finally { Arrays.fill(clave, (byte) 0); }
    }

    synchronized void guardar(Tokens nuevos, Instant ahora) throws IOException, GeneralSecurityException {
        preparar();
        Path bloqueo = tokens.resolveSibling(tokens.getFileName() + ".lock");
        // El directorio privado impide que otro usuario acceda a este lock técnico (no contiene secretos).
        if (Files.isSymbolicLink(bloqueo)) { throw new IOException("Lock inválido"); }
        try (FileChannel canal = FileChannel.open(bloqueo, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = canal.tryLock()) {
            if (lock == null) { throw new IOException("Conexión en curso en otro proceso"); }
            Tokens anterior = Files.exists(tokens, LinkOption.NOFOLLOW_LINKS) ? leer() : null;
            if (anterior != null && (!anterior.cuentaId().equals(nuevos.cuentaId())
                    || !anterior.clienteId().equals(nuevos.clienteId()))) {
                throw new IOException("No se sustituye otra cuenta o cliente sin intervención explícita");
            }
            Tokens definitivo = nuevos;
            if (nuevos.refreshToken().isBlank()) {
                if (anterior == null || !ahora.isBefore(anterior.autorizacionHasta())) {
                    throw new IOException("Se requiere un refresh token vigente");
                }
                definitivo = new Tokens(nuevos.clienteId(), nuevos.cuentaId(), nuevos.correo(),
                        nuevos.accessToken(), anterior.refreshToken(), nuevos.accessHasta(),
                        anterior.autorizacionHasta(), nuevos.ambitos());
            }
            byte[] claro = json.writeValueAsBytes(Map.of("clienteId", definitivo.clienteId(),
                    "cuentaId", definitivo.cuentaId(), "correo", definitivo.correo(),
                    "accessToken", definitivo.accessToken(), "refreshToken", definitivo.refreshToken(),
                    "accessHasta", definitivo.accessHasta().toString(),
                    "autorizacionHasta", definitivo.autorizacionHasta().toString(), "ambitos", definitivo.ambitos()));
            try { ArchivosPrivadosGoogle.escribirAtomico(tokens, cifrar(claro)); }
            finally { Arrays.fill(claro, (byte) 0); }
        }
    }

    /** Renovación bajo el mismo bloqueo que consentimiento; nunca prolonga el grant de En pruebas. */
    synchronized Tokens vigente(Instant ahora, java.util.function.UnaryOperator<Tokens> renovar)
            throws IOException, GeneralSecurityException {
        preparar();
        Path bloqueo = tokens.resolveSibling(tokens.getFileName() + ".lock");
        if (Files.isSymbolicLink(bloqueo)) { throw new IOException("Lock inválido"); }
        try (FileChannel canal = FileChannel.open(bloqueo, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock lock = canal.tryLock()) {
            if (lock == null) { throw new IOException("Conexión en curso"); }
            Tokens anterior = leer();
            if (!ahora.isBefore(anterior.autorizacionHasta())) { throw new IOException("Autorización caducada"); }
            if (ahora.plusSeconds(30).isBefore(anterior.accessHasta())) { return anterior; }
            Tokens nuevo = renovar.apply(anterior);
            if (!anterior.clienteId().equals(nuevo.clienteId()) || !anterior.cuentaId().equals(nuevo.cuentaId())
                    || !anterior.correo().equals(nuevo.correo()) || nuevo.accessToken().isBlank()
                    || !ahora.isBefore(nuevo.accessHasta())
                    || nuevo.autorizacionHasta().isAfter(anterior.autorizacionHasta())) {
                throw new IOException("Renovación inválida");
            }
            byte[] claro = json.writeValueAsBytes(Map.of("clienteId",nuevo.clienteId(),"cuentaId",nuevo.cuentaId(),
                    "correo",nuevo.correo(),"accessToken",nuevo.accessToken(),"refreshToken",nuevo.refreshToken(),
                    "accessHasta",nuevo.accessHasta().toString(),"autorizacionHasta",nuevo.autorizacionHasta().toString(),
                    "ambitos",nuevo.ambitos()));
            try { ArchivosPrivadosGoogle.escribirAtomico(tokens,cifrar(claro)); }
            finally { Arrays.fill(claro,(byte)0); }
            return nuevo;
        }
    }

    private Tokens leer() throws IOException, GeneralSecurityException {
        byte[] datos = ArchivosPrivadosGoogle.leer(tokens, 131072);
        if (datos.length < 32 || !Arrays.equals(VERSION, Arrays.copyOf(datos, VERSION.length))) {
            throw new IOException("Contenedor inválido");
        }
        byte[] claro = transformar(Cipher.DECRYPT_MODE, Arrays.copyOfRange(datos, 4, 16),
                Arrays.copyOfRange(datos, 16, datos.length));
        try {
            JsonNode doc = json.readTree(claro);
            return new Tokens(texto(doc, "clienteId"), texto(doc, "cuentaId"), texto(doc, "correo"),
                    texto(doc, "accessToken"), texto(doc, "refreshToken"),
                    Instant.parse(texto(doc, "accessHasta")), Instant.parse(texto(doc, "autorizacionHasta")),
                    texto(doc, "ambitos"));
        } finally { Arrays.fill(claro, (byte) 0); }
    }

    private byte[] cifrar(byte[] claro) throws IOException, GeneralSecurityException {
        byte[] nonce = new byte[12];
        azar.nextBytes(nonce);
        byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, nonce, claro);
        return ByteBuffer.allocate(16 + cifrado.length).put(VERSION).put(nonce).put(cifrado).array();
    }

    private byte[] transformar(int modo, byte[] nonce, byte[] datos) throws IOException, GeneralSecurityException {
        byte[] clave = ArchivosPrivadosGoogle.leer(llave, 32);
        try {
            if (clave.length != 32) { throw new IOException("Llave inválida"); }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(modo, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(VERSION);
            return cipher.doFinal(datos);
        } finally { Arrays.fill(clave, (byte) 0); }
    }

    private static String texto(JsonNode nodo, String campo) throws IOException {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isString()) { throw new IOException("Contenedor inválido"); }
        return valor.asString();
    }

    record Tokens(String clienteId, String cuentaId, String correo, String accessToken, String refreshToken,
            Instant accessHasta, Instant autorizacionHasta, String ambitos) {
        @Override
        public String toString() { return "TokensGoogle[PROTEGIDO]"; }
    }
}
