package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.domain.ArchivoDemasiadoGrande;
import com.decokasa.seguimiento.archivos.domain.TipoDetectado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Almacenamiento local persistente (P-05 opción A): un directorio en un volumen de Docker o en disco, nunca en memoria
 * ni en el /tmp del contenedor. Escribe en un temporal del mismo volumen, lo sincroniza a disco y lo mueve de forma
 * atómica a su clave definitiva. No registra nombres de archivo ni contenido en los logs.
 */
public final class AlmacenLocalEnDisco implements AlmacenLocal {

    private static final Logger LOG = LoggerFactory.getLogger(AlmacenLocalEnDisco.class);
    private static final String TEMPORALES = ".temporales";
    private static final int BLOQUE = 64 * 1024;
    private static final int FIRMA = 16;

    private final Path base;

    public AlmacenLocalEnDisco(Path directorio) {
        try {
            this.base = directorio.toAbsolutePath().normalize();
            Files.createDirectories(base.resolve(TEMPORALES));
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo preparar el almacenamiento de archivos.", error);
        }
    }

    @Override
    public ArchivoRecibido recibir(InputStream contenido, long limiteBytes) {
        String claveTemporal = TEMPORALES + "/" + UUID.randomUUID() + ".parte";
        Path temporal = ruta(claveTemporal);
        boolean completo = false;
        try (contenido; FileChannel canal = FileChannel.open(temporal, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bloque = new byte[BLOQUE];
            byte[] inicio = new byte[FIRMA];
            int enInicio = 0;
            long total = 0;
            int leidos;
            while ((leidos = contenido.read(bloque)) != -1) {
                total += leidos;
                if (total > limiteBytes) {
                    throw new ArchivoDemasiadoGrande("El archivo supera el tamaño máximo permitido.");
                }
                if (enInicio < FIRMA) {
                    int copiar = Math.min(FIRMA - enInicio, leidos);
                    System.arraycopy(bloque, 0, inicio, enInicio, copiar);
                    enInicio += copiar;
                }
                resumen.update(bloque, 0, leidos);
                ByteBuffer buffer = ByteBuffer.wrap(bloque, 0, leidos);
                while (buffer.hasRemaining()) {
                    canal.write(buffer);
                }
            }
            if (total == 0) {
                throw new DatoInvalido("archivo", "El archivo está vacío.");
            }
            canal.force(true);
            completo = true;
            return new ArchivoRecibido(claveTemporal, total, HexFormat.of().formatHex(resumen.digest()),
                    TipoDetectado.desdeFirma(inicio, enInicio));
        } catch (IOException error) {
            // Lectura cortada o fallo de escritura: la carga incompleta no se registra.
            throw new UncheckedIOException("No se pudo recibir el archivo completo.", error);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible.", error);
        } finally {
            if (!completo) {
                descartar(claveTemporal);
            }
        }
    }

    @Override
    public void confirmar(ArchivoRecibido recibido, String claveFinal) {
        Path destino = ruta(claveFinal);
        try {
            Files.createDirectories(destino.getParent());
            Files.move(ruta(recibido.claveTemporal()), destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo guardar el archivo.", error);
        }
    }

    @Override
    public void descartar(String clave) {
        try {
            Files.deleteIfExists(ruta(clave));
        } catch (IOException | RuntimeException error) {
            LOG.warn("No se pudo borrar un archivo descartado del almacenamiento local", error);
        }
    }

    @Override
    public OptionalLong tamano(String clave) {
        Path archivo = ruta(clave);
        try {
            return Files.isRegularFile(archivo) ? OptionalLong.of(Files.size(archivo)) : OptionalLong.empty();
        } catch (IOException error) {
            return OptionalLong.empty();
        }
    }

    @Override
    public InputStream abrir(String clave) {
        try {
            return Files.newInputStream(ruta(clave));
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo abrir el archivo.", error);
        }
    }

    /** Las claves las genera el servidor; aun así, nunca se sale del directorio base. */
    private Path ruta(String clave) {
        Path ruta = base.resolve(clave).normalize();
        if (!ruta.startsWith(base) || ruta.equals(base)) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida.");
        }
        return ruta;
    }
}
