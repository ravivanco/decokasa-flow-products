package com.decokasa.seguimiento.archivos.application.port.out;

import java.io.InputStream;
import java.util.OptionalLong;

/**
 * Almacenamiento local persistente de la fase 2 (P-05 opción A; ADR-0003: la base solo guarda metadatos). En la
 * fase 3 se agrega Drive como otro puerto, sin reemplazar este.
 */
public interface AlmacenLocal {

    /**
     * Guarda lo recibido en un archivo temporal, midiendo el tamaño, calculando el SHA-256 y detectando el tipo por
     * el contenido. Si la lectura se corta, el archivo supera el límite o está vacío, borra el temporal y falla: una
     * carga incompleta nunca queda registrada.
     *
     * @throws com.decokasa.seguimiento.archivos.domain.ArchivoDemasiadoGrande si supera {@code limiteBytes}
     * @throws com.decokasa.seguimiento.compartido.domain.DatoInvalido si está vacío
     */
    ArchivoRecibido recibir(InputStream contenido, long limiteBytes);

    /** Mueve el temporal a su clave definitiva de forma atómica, ya escrito en disco. */
    void confirmar(ArchivoRecibido recibido, String claveFinal);

    /** Borra un archivo si existe; no falla si ya no está. */
    void descartar(String clave);

    /** Tamaño del archivo guardado; vacío si no existe. */
    OptionalLong tamano(String clave);

    /** Abre el archivo guardado para leerlo. */
    InputStream abrir(String clave);

    /**
     * Fuera de transacción y antes de registrar el adjunto: deja una copia duradera del temporal ya verificado bajo su
     * clave definitiva (ADR-0034, despliegue sin disco persistente). Idempotente. Con disco persistente no hace nada.
     *
     * @throws java.io.UncheckedIOException si no se pudo guardar; entonces el adjunto no se registra
     */
    default void respaldar(ArchivoRecibido recibido, String claveFinal) {
    }

    /**
     * La copia de Drive quedó confirmada y verificada: el almacenamiento temporal duradero puede liberar el archivo
     * (ADR-0034). Nunca falla por un error remoto; con disco persistente no hace nada.
     */
    default void liberarRespaldo(String clave) {
    }

    /** Archivo recibido en el área temporal, con los datos que calculó el servidor. */
    record ArchivoRecibido(String claveTemporal, long tamanoBytes, String sha256, String tipoDetectado) {
    }
}
