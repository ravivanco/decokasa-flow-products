package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.Arrays;
import java.util.Set;

/**
 * Destinatarios de correo. Sin política no se inventa un receptor: vacío se conserva como pendiente de resolución.
 * ADR-0032: en producción, los correos registrados de cuentas activas con el país del pedido.
 */
public interface DirectorioDestinatarios {
    /** Correos de las administradoras del país (avisos de vencimiento). */
    Set<String> administradores(CodigoPais pais);

    /** El correo se puede usar hoy para un pedido de ese país; se revalida antes de cada envío. */
    default boolean autorizado(CodigoPais pais, String correo) {
        return correo != null && administradores(pais).contains(correo);
    }

    /** Hay algún destinatario posible; si no, el trabajador no reclama filas ni consume intentos. */
    default boolean algunoAutorizado() {
        return Arrays.stream(CodigoPais.values()).anyMatch(pais -> !administradores(pais).isEmpty());
    }
}
