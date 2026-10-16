package com.decokasa.seguimiento.compartido.domain.eventos;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contrato de los eventos de un pedido (P-15, decisión técnica del equipo del 2026-10-15). Viven en
 * {@code compartido.domain.eventos} para que {@code pedidos} los produzca y {@code historial} (fase 2) y
 * {@code notificaciones} (fase 3) los escuchen sin depender entre sí (R4, R9). Son registros inmutables en Java puro.
 *
 * <p>Todos llevan el país y el tipo de flujo del pedido, que no cambian, el actor con su rol de intervención y la hora
 * del servidor ({@code Reloj}, R5). No llevan tokens ni datos personales: solo identificadores.
 */
public sealed interface EventoDePedido
        permits PedidoCreado, EtapaEnviada, EtapaAprobada, EtapaRechazada, PedidoDevuelto, EtapaReasignada,
        ObservacionAgregada, ArchivoSubido {

    UUID pedidoId();

    String numeroPedido();

    CodigoPais pais();

    TipoFlujo tipoFlujo();

    /** Etapa en la que ocurrió (la de origen, en un movimiento). */
    int etapa();

    Interviniente actor();

    Instant ocurridoEn();

    /** Identificador que el evento trae para el historial; nulo si lo genera el historial al registrarlo. */
    default UUID eventoId() {
        return null;
    }

    /**
     * Archivos del evento: los presentados con una acción o el subido en ARCHIVO_SUBIDO. Vacío si no hay ninguno.
     * Solo identificadores; el contenido nunca viaja en un evento.
     */
    default List<UUID> adjuntoIds() {
        return List.of();
    }
}
