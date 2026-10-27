package com.decokasa.seguimiento.bodegas.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.Objects;
import java.util.UUID;

/**
 * Bodega del catálogo (requisito 4.2 #14; modelo-datos §3.7). Se desactiva en lugar de borrarse: una bodega inactiva no
 * recibe asignaciones nuevas y los pedidos que ya la tienen no cambian (F4-D1, provisional del equipo). Java puro (R1).
 *
 * <p>Fase 7 (ADR-0024): cada bodega es de un país y solo la usan pedidos de ese país. La ciudad puede estar pendiente
 * (nula) en una bodega cargada así por migración, como "Bodega 1" de Colombia (V19, decisión de Richard del
 * 2026-10-22); una bodega nueva desde la aplicación sigue exigiendo ciudad.
 *
 * @param ciudad  nula solo si está pendiente
 * @param version versión administrativa leída de la base; una edición con otra versión se rechaza (409)
 * @param pais    país de la bodega
 */
public record Bodega(UUID id, String nombre, String ciudad, boolean activa, long version, CodigoPais pais) {

    public static final int LARGO_MAXIMO = 100;

    public Bodega {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pais, "pais");
        nombre = texto("nombre", nombre, "Escribe el nombre de la bodega.");
        ciudad = ciudad == null ? null : texto("ciudad", ciudad, "Escribe la ciudad de la bodega.");
        if (version < 0) {
            throw new IllegalArgumentException("La versión no puede ser negativa.");
        }
    }

    /**
     * Solo compatibilidad documentada con código y pruebas anteriores a la fase 7: una bodega de Ecuador. No decide
     * permisos; quien la crea o la edita se autoriza aparte con su país (ADR-0024).
     */
    public Bodega(UUID id, String nombre, String ciudad, boolean activa, long version) {
        this(id, nombre, ciudad, activa, version, CodigoPais.EC);
    }

    /** Una bodega nueva de Ecuador (firma anterior a la fase 7). */
    public static Bodega nueva(String nombre, String ciudad, Boolean activa) {
        return nueva(CodigoPais.EC, nombre, ciudad, activa);
    }

    /** Una bodega nueva: con ciudad, activa salvo que se indique lo contrario, con versión 0. */
    public static Bodega nueva(CodigoPais pais, String nombre, String ciudad, Boolean activa) {
        String conCiudad = texto("ciudad", ciudad, "Escribe la ciudad de la bodega.");
        return new Bodega(UUID.randomUUID(), nombre, conCiudad, activa == null || activa, 0, pais);
    }

    /**
     * Aplica solo los campos que llegan (los nulos no cambian). El país no cambia: los pedidos que la tienen son de ese
     * país. La versión la incrementa la persistencia.
     */
    public Bodega cambiar(String nuevoNombre, String nuevaCiudad, Boolean nuevaActiva) {
        return new Bodega(id, nuevoNombre == null ? nombre : nuevoNombre, nuevaCiudad == null ? ciudad : nuevaCiudad,
                nuevaActiva == null ? activa : nuevaActiva, version, pais);
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
