package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Un plazo venció (tipo PLAZO_VENCIDO; fase 5, paso 5.2). Evento del sistema: no tiene actor. La detección lo registra
 * una sola vez por plazo: la entrada de la etapa y su vencimiento ({@code ETAPA}) o la activación de la urgencia y su
 * vencimiento ({@code URGENCIA}). Un plazo pendiente del cliente no tiene vencimiento y nunca produce este evento.
 *
 * @param etapa                etapa actual del pedido al detectar el vencimiento
 * @param entradaId            entrada de la etapa (P-18); solo en {@code ETAPA}
 * @param urgenciaActivacionId entrada creada por la devolución que activó la urgencia; solo en {@code URGENCIA}
 * @param eventoId             identificador del evento en el historial; lo fija quien lo detecta
 */
public record PlazoVencido(DatosDelPedido pedido, int etapa, ClaseVencimiento clase, Instant venceEn, UUID entradaId,
        UUID urgenciaActivacionId, Instant ocurridoEn, UUID eventoId) implements EventoDePedido {

    public PlazoVencido {
        Objects.requireNonNull(pedido, "pedido");
        Objects.requireNonNull(clase, "clase");
        Objects.requireNonNull(venceEn, "venceEn");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(eventoId, "eventoId");
        boolean deEtapa = clase == ClaseVencimiento.ETAPA;
        boolean coherente = deEtapa ? entradaId != null && urgenciaActivacionId == null
                : urgenciaActivacionId != null && entradaId == null;
        if (!coherente) {
            throw new IllegalArgumentException("Un plazo vencido se identifica por su entrada o por su urgencia.");
        }
    }

    @Override
    public UUID pedidoId() {
        return pedido.pedidoId();
    }

    @Override
    public String numeroPedido() {
        return pedido.numeroPedido();
    }

    @Override
    public CodigoPais pais() {
        return pedido.pais();
    }

    @Override
    public TipoFlujo tipoFlujo() {
        return pedido.tipoFlujo();
    }

    @Override
    public Interviniente actor() {
        return null;
    }
}
