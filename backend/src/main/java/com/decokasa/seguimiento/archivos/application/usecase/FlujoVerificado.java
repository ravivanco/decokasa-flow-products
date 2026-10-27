package com.decokasa.seguimiento.archivos.application.usecase;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Flujo de un documento leído de Drive (ADR-0028) que cuenta bytes y calcula el SHA-256 mientras se transmite, sin
 * guardarlo en memoria. Si se pasa del tamaño registrado o, al terminar, el tamaño o la huella no coinciden, la lectura
 * falla con {@link IOException}: la respuesta HTTP se corta y el navegador no recibe un archivo completo pero alterado.
 */
final class FlujoVerificado extends FilterInputStream {
    private final long esperado;
    private final String sha256;
    private final MessageDigest resumen;
    private long leidos;
    private boolean verificado;

    FlujoVerificado(InputStream origen, long esperado, String sha256) {
        super(origen);
        this.esperado = esperado;
        this.sha256 = sha256;
        try {
            this.resumen = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible.", error);
        }
    }

    @Override public int read() throws IOException {
        int dato = super.read();
        if (dato == -1) { verificarFinal(); } else { contar(new byte[] {(byte) dato}, 0, 1); }
        return dato;
    }

    @Override public int read(byte[] destino, int desde, int cantidad) throws IOException {
        int n = super.read(destino, desde, cantidad);
        if (n == -1) { verificarFinal(); } else { contar(destino, desde, n); }
        return n;
    }

    @Override public long skip(long n) throws IOException {
        // Saltar rompería la huella: se lee y se descarta.
        byte[] bloque = new byte[8192];
        long saltados = 0;
        while (saltados < n) {
            int r = read(bloque, 0, (int) Math.min(bloque.length, n - saltados));
            if (r == -1) { break; }
            saltados += r;
        }
        return saltados;
    }

    @Override public boolean markSupported() { return false; }

    private void contar(byte[] datos, int desde, int n) throws IOException {
        leidos += n;
        if (leidos > esperado) { throw new IOException("El documento de Drive no coincide con el registrado."); }
        resumen.update(datos, desde, n);
        // No entregar el último bloque antes de verificar: con Content-Length el cliente da la respuesta por
        // completa al recibir el último byte, sin esperar a que el servidor lea EOF. La lectura adicional también
        // detecta un contenido más largo antes de devolver el bloque que completaría la respuesta.
        if (leidos == esperado) {
            if (in.read() != -1) { throw new IOException("El documento de Drive no coincide con el registrado."); }
            verificarFinal();
        }
    }

    private void verificarFinal() throws IOException {
        if (verificado) { return; }
        if (leidos != esperado || !HexFormat.of().formatHex(resumen.digest()).equalsIgnoreCase(sha256)) {
            throw new IOException("El documento de Drive no coincide con el registrado.");
        }
        verificado = true;
    }
}
