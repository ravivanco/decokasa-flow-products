package com.decokasa.seguimiento.archivos.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;

/**
 * Tamaño máximo de un archivo, en bytes. Valor provisional del equipo (2026-10-15), configurable y pendiente del
 * cliente: 100 MiB por archivo (104857600 bytes). requerimientos 4.2 #15 y ADR-0003 hablan de 100 MB por archivo, pero
 * la misma fuente deja abierto en §5 si es por archivo o por etapa (contradicción 7 del contrato).
 */
public record LimiteDeTamano(long bytes) {

    public LimiteDeTamano {
        if (bytes <= 0) {
            throw new DatoInvalido("El límite de tamaño de los archivos debe ser positivo.");
        }
    }
}
