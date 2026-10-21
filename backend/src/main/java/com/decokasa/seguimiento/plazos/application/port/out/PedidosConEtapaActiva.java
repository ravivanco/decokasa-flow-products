package com.decokasa.seguimiento.plazos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Pedidos con plazos que pueden vencer (arquitectura.md 3.1 y 4.7). Lo implementa un adaptador del módulo pedidos, para
 * que la dependencia vaya de pedidos hacia plazos y no haya ciclo (R9).
 */
public interface PedidosConEtapaActiva {

    /**
     * Pedidos en curso con la etapa actual o la urgencia vencidas antes de {@code ahora} y sin aviso registrado para
     * ese plazo exacto; como máximo {@code limite}. Lectura sin bloqueo: el caso de uso vuelve a comprobarlo.
     */
    List<UUID> candidatos(Instant ahora, int limite);

    /**
     * Bloquea la fila del pedido, igual que las acciones (P-11), y devuelve su estado vigente. Debe llamarse dentro de
     * la transacción en la que se registran el aviso, el historial y el outbox.
     */
    Optional<EstadoVigente> bloquear(UUID pedidoId);

    /**
     * Estado vigente de un pedido bloqueado.
     *
     * @param entradaId            entrada actual de la etapa actual (P-18)
     * @param venceEn              vencimiento de esa entrada; nulo si el plazo está pendiente o la etapa no tiene plazo
     * @param urgenciaActivacionId entrada que activó la urgencia vigente; nula sin urgencia
     * @param urgenciaVenceEn      vencimiento de la urgencia vigente; nulo sin urgencia
     */
    record EstadoVigente(DatosDelPedido pedido, boolean finalizado, int etapaActual, UUID entradaId, Instant venceEn,
            UUID urgenciaActivacionId, Instant urgenciaVenceEn) {
    }
}
