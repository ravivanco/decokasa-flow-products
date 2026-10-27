package com.decokasa.seguimiento.archivos.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Formatos admitidos (ADR-0026, aprobados por Richard el 2026-10-24): PDF, Excel, CSV e imágenes de evidencia. El
 * servidor decide por la extensión del nombre, el tipo declarado (si llega) y el contenido técnico; el filtro del
 * navegador solo ayuda. No se interpreta el contenido comercial. Java puro (R1).
 */
public enum FormatoArchivo {
    PDF("application/pdf", Set.of("pdf"), Set.of("application/pdf", "application/x-pdf")),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", Set.of("xlsx"),
            Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/zip",
                    "application/x-zip-compressed")),
    XLS("application/vnd.ms-excel", Set.of("xls"), Set.of("application/vnd.ms-excel", "application/msexcel",
            "application/x-msexcel", "application/x-excel", "application/x-ole-storage")),
    // Windows con Excel instalado declara los CSV como application/vnd.ms-excel; no es un error del usuario.
    CSV("text/csv", Set.of("csv"), Set.of("text/csv", "text/plain", "application/csv", "text/comma-separated-values",
            "application/vnd.ms-excel", "text/x-csv", "application/x-csv")),
    JPEG("image/jpeg", Set.of("jpg", "jpeg"), Set.of("image/jpeg", "image/pjpeg", "image/jpg")),
    PNG("image/png", Set.of("png"), Set.of("image/png", "image/x-png"));

    /** Para el atributo {@code accept} del navegador y los mensajes. */
    public static final String EXTENSIONES = ".pdf,.xlsx,.xls,.csv,.jpg,.jpeg,.png";

    private final String mime;
    private final Set<String> extensiones;
    private final Set<String> tiposDeclarados;

    FormatoArchivo(String mime, Set<String> extensiones, Set<String> tiposDeclarados) {
        this.mime = mime;
        this.extensiones = extensiones;
        this.tiposDeclarados = tiposDeclarados;
    }

    /** Tipo MIME canónico que se guarda (adjunto.tipo_detectado) y se usa al copiar a Drive. */
    public String mime() {
        return mime;
    }

    /**
     * Formato según la extensión del nombre original, sin distinguir mayúsculas.
     *
     * @throws TipoArchivoNoAceptado si no tiene extensión o no es una de las admitidas
     */
    public static FormatoArchivo desdeNombre(String nombreOriginal) {
        // Sin recortar a 255 caracteres (Adjunto.limpiarNombre): un nombre largo no debe perder su extensión.
        String nombre = nombreOriginal == null ? "" : nombreOriginal;
        nombre = nombre.substring(Math.max(nombre.lastIndexOf('/'), nombre.lastIndexOf('\\')) + 1).strip();
        int punto = nombre.lastIndexOf('.');
        if (punto < 1 || punto == nombre.length() - 1) {
            throw new TipoArchivoNoAceptado();
        }
        String extension = nombre.substring(punto + 1).strip().toLowerCase(Locale.ROOT);
        for (FormatoArchivo formato : values()) {
            if (formato.extensiones.contains(extension)) {
                return formato;
            }
        }
        throw new TipoArchivoNoAceptado();
    }

    /**
     * Comprueba el {@code Content-Type} que declaró el navegador, si llegó. Vacío o {@code application/octet-stream}
     * (navegadores sin asociación del tipo) no dicen nada y se aceptan; el contenido se comprueba aparte.
     *
     * @throws TipoArchivoNoAceptado si el tipo declarado contradice la extensión
     */
    public void exigirTipoDeclarado(String tipoDeclarado) {
        if (tipoDeclarado == null) {
            return;
        }
        String tipo = tipoDeclarado.split(";", 2)[0].strip().toLowerCase(Locale.ROOT);
        if (tipo.isEmpty() || tipo.equals("application/octet-stream") || tiposDeclarados.contains(tipo)) {
            return;
        }
        throw new TipoArchivoNoAceptado();
    }
}
