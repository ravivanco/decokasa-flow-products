package com.decokasa.seguimiento.embarques.application.port.out;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import java.util.Optional;
import java.util.UUID;

/**
 * Reglas de pedidos que necesita {@code embarques}, igual que {@code PedidosParaArchivos}: embarques no depende de
 * pedidos (R9). Se usa dentro de la transacción del caso de uso.
 */
public interface PedidosParaEmbarques {

    /**
     * @param flujoGestiona la versión del flujo del pedido tiene una etapa de BL; falso en pedidos anteriores (v1)
     * @param etapaGestiona la etapa actual es la de BL
     * @param etapaAprueba la etapa actual es la de aprobación de embarque
     * @param transitoDias tránsito de referencia del flujo; nulo si no tiene
     */
    record Referencia(DatosDelPedido pedido, UUID flujoId, int etapaActual, UUID entradaActualId, boolean finalizado,
            boolean flujoGestiona, boolean etapaGestiona, boolean etapaAprueba, boolean etapaChecklist,
            Integer transitoDias) {
    }

    record Gestion(Referencia pedido, Interviniente actor) {
    }

    /** Bloquea el pedido y exige poder actuar en su etapa de BL (P-06). */
    Gestion autorizarGestion(Solicitante solicitante, UUID pedidoId);

    /** Bloquea el pedido y exige poder actuar en su etapa de aprobación (P-06). */
    Gestion autorizarRevision(Solicitante solicitante, UUID pedidoId);

    /** Exige poder ver el pedido (P-13 y país asignado). */
    Referencia autorizarConsulta(Solicitante solicitante, UUID pedidoId);

    /** La persona puede actuar hoy en la etapa actual; sin excepciones, para reflejarlo en pantalla. */
    boolean puedeActuar(Solicitante solicitante, UUID pedidoId);

    /** Abrir un Packing List desde un pedido: admin, quien actúa en su etapa actual o P-17 sobre el archivo. */
    void autorizarPackingList(Solicitante solicitante, UUID pedidoId, UUID pedidoDelArchivo, int etapaDelArchivo);

    /** Pedido en curso del mismo país, con ese número, que admite vincular un BL. Vacío si no existe o no se ve. */
    Optional<Referencia> vinculable(Solicitante solicitante, String numero);

    Referencia referencia(UUID pedidoId);
}
