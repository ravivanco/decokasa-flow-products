package com.decokasa.seguimiento.archivos.domain;

/**
 * Tipo de un archivo deducido de sus primeros bytes, nunca del nombre ni del {@code Content-Type} que envía el
 * cliente. Solo se registra: no decide si el archivo se acepta, porque los formatos siguen pendientes del cliente (⏸).
 */
public final class TipoDetectado {

    private TipoDetectado() {
    }

    /** Nulo si la firma no se reconoce. */
    public static String desdeFirma(byte[] inicio, int longitud) {
        if (empieza(inicio, longitud, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return "application/pdf";
        }
        if (empieza(inicio, longitud, 0x50, 0x4B, 0x03, 0x04)) {
            // ZIP, y también los formatos de Office modernos (docx, xlsx), que son ZIP.
            return "application/zip";
        }
        if (empieza(inicio, longitud, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1)) {
            // Formato compuesto de Office anterior a 2007 (doc, xls).
            return "application/x-ole-storage";
        }
        if (empieza(inicio, longitud, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return "image/png";
        }
        if (empieza(inicio, longitud, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (empieza(inicio, longitud, 0x47, 0x49, 0x46, 0x38)) {
            return "image/gif";
        }
        return null;
    }

    private static boolean empieza(byte[] inicio, int longitud, int... firma) {
        if (longitud < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((inicio[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
