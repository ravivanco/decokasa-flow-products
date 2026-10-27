package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Metadatos de un archivo real subido a una entrada de una etapa (modelo-datos §3.5; P-05 opción A, P-18). El tamaño,
 * la huella y el tipo los calcula el servidor sobre lo recibido. La fila no prueba que el archivo exista: quien lo
 * usa lo comprueba en el almacenamiento. Java puro (R1).
 *
 * @param claveLocal clave relativa en el almacenamiento local, generada por el servidor; nunca el nombre original
 * @param tipoDetectado tipo deducido del contenido; nulo si no se reconoce
 */
public record Adjunto(UUID id, UUID pedidoId, int etapa, UUID entradaId, String tipoDocumento, String nombre,
        long tamanoBytes, String sha256, String tipoDetectado, String claveLocal, EstadoCopia estadoCopia,
        UUID subidoPor, Instant subidoEn) {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    public Adjunto {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(entradaId, "entradaId");
        Objects.requireNonNull(estadoCopia, "estadoCopia");
        Objects.requireNonNull(subidoPor, "subidoPor");
        Objects.requireNonNull(subidoEn, "subidoEn");
        Objects.requireNonNull(claveLocal, "claveLocal");
        if (tamanoBytes <= 0) {
            throw new DatoInvalido("archivo", "El archivo está vacío.");
        }
        if (sha256 == null || !SHA256.matcher(sha256).matches()) {
            throw new IllegalArgumentException("La huella SHA-256 no es válida.");
        }
        tipoDocumento = validarTipoDocumento(tipoDocumento);
        nombre = limpiarNombre(nombre);
    }

    /** Tipo de documento según la definición de la etapa: 1 a 100 caracteres. */
    public static String validarTipoDocumento(String tipoDocumento) {
        String limpio = tipoDocumento == null ? "" : tipoDocumento.strip();
        if (limpio.isEmpty() || limpio.length() > 100) {
            throw new DatoInvalido("tipoDocumento", "El tipo de documento debe tener entre 1 y 100 caracteres.");
        }
        return limpio;
    }

    /**
     * Nombre original solo para mostrar: sin carpetas ni caracteres de control, hasta 255 caracteres. Nunca se usa
     * para guardar el archivo ni para decidir su tipo.
     */
    public static String limpiarNombre(String original) {
        String nombre = original == null ? "" : original;
        int barra = Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\'));
        if (barra >= 0) {
            nombre = nombre.substring(barra + 1);
        }
        nombre = nombre.codePoints()
                .filter(punto -> !Character.isISOControl(punto))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .strip();
        if (nombre.isEmpty()) {
            return "archivo";
        }
        if (nombre.codePointCount(0, nombre.length()) > 255) {
            nombre = nombre.substring(0, nombre.offsetByCodePoints(0, 255));
        }
        return nombre;
    }
}
