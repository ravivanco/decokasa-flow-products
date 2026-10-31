package com.decokasa.seguimiento.archivos.adapter.out.google;

import com.decokasa.seguimiento.archivos.adapter.out.google.AlmacenTokensGoogle.Tokens;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bóveda en PostgreSQL (ADR-0034, Render + Supabase): una fila de {@code credencial_google} con el contenedor
 * AES-256-GCM. La llave llega por variable de entorno y nunca se guarda en la base. Sin transacción larga ni bloqueo de
 * fila durante la red: la renovación escribe con {@code version} condicional; si otro proceso renovó antes, se usa lo
 * que él guardó. Mismas reglas que la bóveda local ({@link ReglasTokensGoogle}).
 */
final class AlmacenTokensGoogleSql implements BovedaTokensGoogle {
    private static final byte[] VERSION = new byte[] {'D', 'K', 'G', 2};
    private static final byte[] CONTEXTO = "decokasa:google-oauth:postgresql:1".getBytes(StandardCharsets.UTF_8);
    private final JdbcTemplate sql;
    private final SecretosGoogle.FuenteLlave llave;
    private final JsonMapper json;
    private final SecureRandom azar = new SecureRandom();

    AlmacenTokensGoogleSql(DataSource fuente, SecretosGoogle.FuenteLlave llave, JsonMapper json) {
        this.sql = new JdbcTemplate(fuente);
        this.llave = llave;
        this.json = json;
    }

    @Override
    public void preparar() throws IOException {
        byte[] clave = llave.leer();
        Arrays.fill(clave, (byte) 0);
    }

    @Override
    public synchronized void guardar(Tokens nuevos, Instant ahora) throws IOException, GeneralSecurityException {
        preparar();
        Fila fila = fila();
        Tokens anterior = fila == null ? null : descifrar(fila.contenedor());
        if (!escribir(ReglasTokensGoogle.combinar(anterior, nuevos, ahora), fila)) {
            throw new IOException("Conexión en curso en otro proceso");
        }
    }

    @Override
    public synchronized Tokens vigente(Instant ahora, UnaryOperator<Tokens> renovar)
            throws IOException, GeneralSecurityException {
        Fila fila = fila();
        if (fila == null) { throw new IOException("Sin autorización guardada"); }
        Tokens anterior = descifrar(fila.contenedor());
        ReglasTokensGoogle.exigirAutorizacion(anterior, ahora);
        if (ReglasTokensGoogle.accesoVigente(anterior, ahora)) { return anterior; }
        Tokens nuevo = renovar.apply(anterior);
        ReglasTokensGoogle.validarRenovacion(anterior, nuevo, ahora);
        if (escribir(nuevo, fila)) { return nuevo; }
        // Otro proceso guardó antes (renovación o nuevo consentimiento): usar su resultado si sigue vigente.
        Fila otra = fila();
        if (otra == null) { throw new IOException("Sin autorización guardada"); }
        Tokens actual = descifrar(otra.contenedor());
        ReglasTokensGoogle.exigirAutorizacion(actual, ahora);
        if (!ReglasTokensGoogle.accesoVigente(actual, ahora)) { throw new IOException("Renovación concurrente"); }
        return actual;
    }

    private Fila fila() {
        List<Fila> filas = sql.query("select contenedor, version from credencial_google where id = 1",
                (r, n) -> new Fila(r.getBytes(1), r.getLong(2)));
        return filas.isEmpty() ? null : filas.getFirst();
    }

    /** Inserta la primera vez o actualiza solo si nadie escribió desde la lectura. */
    private boolean escribir(Tokens tokens, Fila anterior) throws IOException, GeneralSecurityException {
        byte[] claro = ReglasTokensGoogle.serializar(tokens, json);
        try {
            byte[] contenedor = cifrar(claro);
            int filas = anterior == null
                    ? sql.update("insert into credencial_google (id, contenedor, version, actualizado_en) "
                            + "values (1, ?, 1, now()) on conflict (id) do nothing", (Object) contenedor)
                    : sql.update("update credencial_google set contenedor = ?, version = version + 1, "
                            + "actualizado_en = now() where id = 1 and version = ?", contenedor, anterior.version());
            return filas == 1;
        } finally { Arrays.fill(claro, (byte) 0); }
    }

    private Tokens descifrar(byte[] datos) throws IOException, GeneralSecurityException {
        if (datos == null || datos.length < 32 || !Arrays.equals(VERSION, Arrays.copyOf(datos, VERSION.length))) {
            throw new IOException("Contenedor inválido");
        }
        byte[] claro = transformar(Cipher.DECRYPT_MODE, Arrays.copyOfRange(datos, 4, 16),
                Arrays.copyOfRange(datos, 16, datos.length));
        try { return ReglasTokensGoogle.leer(claro, json); }
        finally { Arrays.fill(claro, (byte) 0); }
    }

    private byte[] cifrar(byte[] claro) throws IOException, GeneralSecurityException {
        byte[] nonce = new byte[12];
        azar.nextBytes(nonce);
        byte[] cifrado = transformar(Cipher.ENCRYPT_MODE, nonce, claro);
        return ByteBuffer.allocate(16 + cifrado.length).put(VERSION).put(nonce).put(cifrado).array();
    }

    private byte[] transformar(int modo, byte[] nonce, byte[] datos) throws IOException, GeneralSecurityException {
        byte[] clave = llave.leer();
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(modo, new SecretKeySpec(clave, "AES"), new GCMParameterSpec(128, nonce));
            // Versión y contexto en el AAD: un contenedor de la bóveda local no se acepta aquí, ni al revés.
            cipher.updateAAD(VERSION);
            cipher.updateAAD(CONTEXTO);
            return cipher.doFinal(datos);
        } finally { Arrays.fill(clave, (byte) 0); }
    }

    private record Fila(byte[] contenedor, long version) {
        @Override public String toString() { return "FilaCredencialGoogle[PROTEGIDO]"; }
    }
}
