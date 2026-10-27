package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;

/**
 * Comprueba el contenido técnico de un archivo ya recibido en el almacenamiento local (ADR-0026): firma, contenedor o
 * texto según el formato, sin interpretar su contenido comercial.
 */
@FunctionalInterface
public interface VerificadorFormato {

    /**
     * @param clave clave local (temporal o definitiva) generada por el servidor
     * @throws com.decokasa.seguimiento.archivos.domain.TipoArchivoNoAceptado si el contenido no corresponde al formato
     */
    void verificar(String clave, FormatoArchivo formato);
}
