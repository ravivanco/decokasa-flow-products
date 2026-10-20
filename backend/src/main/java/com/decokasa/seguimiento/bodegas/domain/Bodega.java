package com.decokasa.seguimiento.bodegas.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.Objects;
import java.util.UUID;

/**
 * Bodega del catálogo (requisito 4.2 #14; modelo-datos §3.7). Se desactiva en lugar de borrarse: una bodega inactiva no
 * recibe asignaciones nuevas y los pedidos que ya la tienen no cambian (F4-D1, provisional del equipo). Java puro (R1).
 *
 * @param version versión administrativa leída de la base; una edición con otra versión se rechaza (409)
 */
public record Bodega(UUID id, String nombre, String ciudad, boolean activa, long version) {

    public static final int LARGO_MAXIMO = 100;

    public Bodega {
        Objects.requireNonNull(id, "id");
        nombre = texto("nombre", nombre, "Escribe el nombre de la bodega.");
        ciudad = texto("ciudad", ciudad, "Escribe la ciudad de la bodega.");
        if (version < 0) {
            throw new IllegalArgumentException("La versión no puede ser negativa.");
        }
    }

    /** Una bodega nueva: activa salvo que se indique lo contrario, con versión 0. */
    public static Bodega nueva(String nombre, String ciudad, Boolean activa) {
        return new Bodega(UUID.randomUUID(), nombre, ciudad, activa == null || activa, 0);
    }

    /** Aplica solo los campos que llegan (los nulos no cambian). La versión la incrementa la persistencia. */
    public Bodega cambiar(String nuevoNombre, String nuevaCiudad, Boolean nuevaActiva) {
        return new Bodega(id, nuevoNombre == null ? nombre : nuevoNombre, nuevaCiudad == null ? ciudad : nuevaCiudad,
                nuevaActiva == null ? activa : nuevaActiva, version);
    }

    /** Recorta los espacios sobrantes y exige de 1 a 100 caracteres, igual que la base (V9) y el contrato. */
    private static String texto(String campo, String valor, String mensajeVacio) {
        if (valor == null || valor.isBlank()) {
            throw new DatoInvalido(campo, mensajeVacio);
        }
        String limpio = valor.strip();
        if (limpio.length() > LARGO_MAXIMO) {
            throw new DatoInvalido(campo, "Hasta " + LARGO_MAXIMO + " caracteres.");
        }
        return limpio;
    }
}
