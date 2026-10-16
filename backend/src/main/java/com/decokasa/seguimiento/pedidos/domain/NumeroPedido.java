package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.Locale;

/**
 * Número de un pedido: único y, una vez confirmado, nunca se reutiliza (requerimientos 4.2 #5; lectura A, decisión
 * del equipo del 2026-10-15). El formato {@code DK-{pais}-{anio}-{NNNN}} y la separación por país y año siguen
 * provisionales (⏸): este es el único lugar que los conoce. NNNN tiene como mínimo cuatro dígitos y los números
 * mayores se escriben completos, sin truncar ni reiniciar. La reserva del número es del generador (paso 2.4).
 */
public record NumeroPedido(String valor) {

    public NumeroPedido {
        if (valor == null || valor.isBlank() || valor.length() > 40) {
            throw new DatoInvalido("El número de pedido debe tener entre 1 y 40 caracteres.");
        }
    }

    /** Compone el número provisional: {@code componer(EC, 2026, 7)} da {@code DK-EC-2026-0007}. */
    public static NumeroPedido componer(CodigoPais pais, int anio, int consecutivo) {
        if (anio <= 0 || consecutivo <= 0) {
            throw new DatoInvalido("El año y el consecutivo del número de pedido deben ser positivos.");
        }
        return new NumeroPedido(String.format(Locale.ROOT, "DK-%s-%d-%04d", pais.name(), anio, consecutivo));
    }
}
