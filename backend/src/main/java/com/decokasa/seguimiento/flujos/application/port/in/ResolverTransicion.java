package com.decokasa.seguimiento.flujos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que pasa si se ejecuta una acción en una etapa, según la versión del flujo del pedido (arquitectura.md 4.2). Lo
 * usa {@code pedidos} (R4: solo por este puerto). No decide permisos ni comprueba documentos subidos: entrega el rol
 * responsable de la etapa de origen para que {@code pedidos} decida quién actúa (paso 2.3).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el flujo no existe
 * @throws com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida si la acción no existe en esa etapa
 */
public interface ResolverTransicion {

    Resolucion resolver(UUID flujoId, int etapaOrigen, AccionEtapa accion);

    /**
     * @param etapaDestino nulo si el pedido se queda en la etapa
     * @param documentosPendientes los documentos obligatorios no están definidos: la acción no se puede ejecutar y
     *        {@code documentosObligatorios} no es la lista completa
     * @param plazoDestino plazo de la etapa de destino; nulo si no hay destino
     * @param destinoEsRetornoDeUrgencia al entrar en el destino termina la urgencia (A4 / B5)
     * @param rolResponsableOrigen rol responsable de la etapa de origen; nulo si la etapa es automática (S-1)
     */
    record Resolucion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
            Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
            CodigoRol rolResponsableOrigen) {
    }
}
