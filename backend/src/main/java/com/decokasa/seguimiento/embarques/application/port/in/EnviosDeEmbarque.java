package com.decokasa.seguimiento.embarques.application.port.in;

import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Lo que necesita el motor de pedidos cuando una transición gestiona embarques. Corre en la transacción de la acción:
 * el movimiento, la foto del envío, la decisión y el historial se confirman o se revierten juntos.
 */
public interface EnviosDeEmbarque {

    /** Sin módulo de embarques: los flujos que no lo usan nunca llegan a llamarlo. */
    EnviosDeEmbarque SIN_EMBARQUES = new EnviosDeEmbarque() {
        @Override public List<String> faltantes(UUID pedidoId) { return List.of(); }
        @Override public List<String> faltantesParaAprobar(UUID pedidoId, UUID actorId) { return List.of(); }
        @Override public void exigirAprobable(UUID pedidoId, UUID actorId) { }
        @Override public void registrarEnvio(DatosDelPedido pedido, int etapa, UUID entradaId, Interviniente actor,
                Instant ahora) { }
        @Override public void aprobar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora) { }
        @Override public void rechazar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora,
                String motivo, List<UUID> blSenalados) { }
    };

    /**
     * Requisitos del checklist marcados ✓ en la entrada (V35): cuentan como documentos presentes al ejecutar la acción.
     * Sin checklist, ninguno.
     */
    default Set<String> tiposMarcados(UUID entradaId) {
        return Set.of();
    }

    /** Lo que falta para enviar a aprobación: BL vinculados, fecha de salida y Packing List confirmado. */
    List<String> faltantes(UUID pedidoId);

    /** Lo que impide aprobar hoy, en lenguaje de pantalla. Rechazar siempre es posible. */
    List<String> faltantesParaAprobar(UUID pedidoId, UUID actorId);

    /**
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si quien aprueba es quien envió
     * @throws com.decokasa.seguimiento.compartido.domain.RequisitoFaltante si hay una versión nueva sin revisar
     */
    void exigirAprobable(UUID pedidoId, UUID actorId);

    void registrarEnvio(DatosDelPedido pedido, int etapa, UUID entradaId, Interviniente actor, Instant ahora);

    void aprobar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora);

    void rechazar(DatosDelPedido pedido, int etapa, Interviniente actor, Instant ahora, String motivo,
            List<UUID> blSenalados);
}
