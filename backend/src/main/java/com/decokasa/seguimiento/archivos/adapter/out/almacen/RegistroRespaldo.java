package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import java.util.List;
import java.util.OptionalInt;

/** Cuota y estado del almacenamiento temporal (tabla respaldo_temporal, ADR-0034). */
public interface RegistroRespaldo {

    /**
     * Reserva los bytes de una clave antes de subir. Idempotente para la misma clave.
     *
     * @throws com.decokasa.seguimiento.archivos.domain.CuotaAlmacenamientoExcedida si la suma superaría la cuota
     */
    void reservar(String clave, long bytes, int partes, long cuota);

    void completar(String clave);

    /** Marca para limpieza (nunca borra la fila de un adjunto que no esté COPIADO o sin registrar). */
    void marcarLiberando(String clave);

    void borrar(String clave);

    OptionalInt partes(String clave);

    /** Claves cuyo contenido ya puede borrarse del bucket: LIBERANDO, adjunto COPIADO o subida nunca registrada. */
    List<Pendiente> paraLimpiar(int limite);

    /** Bytes reservados en total (para diagnóstico). */
    long usados();

    record Pendiente(String clave, int partes) { }
}
