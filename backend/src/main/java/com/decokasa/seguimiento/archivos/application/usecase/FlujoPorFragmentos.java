package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * ADR-0034: lee el archivo guardado en Drive y comprueba, fragmento a fragmento, que cada bloque de 4 MiB tiene el SHA-256
 * que el servidor verificó al recibirlo. Si Drive guardó algo distinto de lo que envió el usuario, la lectura falla con
 * {@link Distinto} y el documento no se registra. Java puro.
 */
final class FlujoPorFragmentos extends FilterInputStream {
    private final List<CargaArchivo.Fragmento> fragmentos;
    private final long total;
    private int indice;
    private long leidosEnFragmento;
    private long leidos;
    private MessageDigest resumen;

    FlujoPorFragmentos(InputStream entrada, List<CargaArchivo.Fragmento> fragmentos, long total) {
        super(entrada);
        this.fragmentos = List.copyOf(fragmentos);
        this.total = total;
        this.resumen = nuevo();
    }

    @Override
    public int read() throws IOException {
        byte[] uno = new byte[1];
        int n = read(uno, 0, 1);
        return n < 0 ? -1 : uno[0] & 0xff;
    }

    @Override
    public int read(byte[] b, int desde, int largo) throws IOException {
        if (largo == 0) { return 0; }
        if (indice < fragmentos.size()) {
            largo = (int) Math.min(largo, fragmentos.get(indice).tamano() - leidosEnFragmento);
        }
        int n = super.read(b, desde, largo);
        if (n < 0) {
            if (indice != fragmentos.size() || leidos != total) { throw new Distinto(); }
            return -1;
        }
        if (indice >= fragmentos.size()) { throw new Distinto(); }
        resumen.update(b, desde, n);
        leidosEnFragmento += n;
        leidos += n;
        CargaArchivo.Fragmento actual = fragmentos.get(indice);
        if (leidosEnFragmento == actual.tamano()) {
            if (!HexFormat.of().formatHex(resumen.digest()).equals(actual.sha256())) { throw new Distinto(); }
            indice++;
            leidosEnFragmento = 0;
            resumen = nuevo();
        }
        return n;
    }

    private static MessageDigest nuevo() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 no disponible", error); }
    }

    /** El contenido de Drive no coincide con los fragmentos confirmados. */
    static final class Distinto extends IOException {
        private static final long serialVersionUID = 1L;
        Distinto() { super("El archivo en Drive no coincide con los fragmentos confirmados"); }
    }
}
