package com.decokasa.seguimiento.flujos.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Todas las acciones definidas para una etapa en la versión del flujo de un pedido, con sus requisitos. Lo usa
 * {@code pedidos} para armar {@code accionesDisponibles} (contrato 0.7.0). Solo tipos de {@code compartido} (R4).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el flujo no existe
 */
public interface ConsultarAccionesDeEtapa {

    /** En el orden del enum AccionEtapa; vacía si la etapa no tiene acciones (por ejemplo, Fin). */
    List<ResolverTransicion.Resolucion> accionesDe(UUID flujoId, int etapa);
}
