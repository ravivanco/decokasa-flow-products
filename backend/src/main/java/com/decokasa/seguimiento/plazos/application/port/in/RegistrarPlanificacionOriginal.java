package com.decokasa.seguimiento.plazos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.UUID;

/**
 * Guarda la planificación original de un pedido nuevo (ADR-0030, V25). Lo llama {@code pedidos} una sola vez, en la
 * transacción que crea el pedido. Solo usa la hora de creación y la definición del flujo: nunca hechos posteriores.
 */
public interface RegistrarPlanificacionOriginal {

    /**
     * @param flujoId versión del flujo que sigue el pedido
     * @param datos   datos del pedido recién creado; se descartan sus hechos y se calcula como al crearlo
     */
    void registrar(CodigoPais pais, UUID flujoId, CalcularCronograma.Datos datos);
}
