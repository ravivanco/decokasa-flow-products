package com.decokasa.seguimiento.historial.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.eventos.ArchivoSubido;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaReasignada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.ObservacionAgregada;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoCreado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Una entrada del historial de un pedido (modelo-datos.md, evento_pedido). Inmutable: el historial es de solo
 * inserción y una corrección es un evento nuevo (R7). Guarda el actor, el rol con el que intervino y la hora del
 * servidor tal como llegaron en el evento, aunque después cambien los roles de la persona. Java puro (R1).
 *
 * @param usuarioId nulo solo en los eventos del sistema (fases 4 y 5)
 * @param rolCodigo nulo solo en los eventos del sistema; es el rol de intervención del contrato
 * @param adjuntoIds archivos del evento: el subido (ARCHIVO_SUBIDO) o los presentados con la acción
 */
public record EventoPedido(UUID id, UUID pedidoId, int etapa, TipoEventoPedido tipo, UUID usuarioId,
        CodigoRol rolCodigo, Instant ocurridoEn, String observacion, NivelUrgencia nivelUrgencia,
        DetalleEvento detalle, List<UUID> adjuntoIds) {

    public EventoPedido {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(detalle, "detalle");
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        if ((usuarioId == null) != (rolCodigo == null)) {
            throw new IllegalArgumentException("El actor y su rol van juntos.");
        }
    }

    /** Sin archivos. */
    public EventoPedido(UUID id, UUID pedidoId, int etapa, TipoEventoPedido tipo, UUID usuarioId, CodigoRol rolCodigo,
            Instant ocurridoEn, String observacion, NivelUrgencia nivelUrgencia, DetalleEvento detalle) {
        this(id, pedidoId, etapa, tipo, usuarioId, rolCodigo, ocurridoEn, observacion, nivelUrgencia, detalle,
                List.of());
    }

    /** Traduce un evento del pedido a su entrada de historial. */
    public static EventoPedido desde(UUID id, EventoDePedido evento) {
        TipoEventoPedido tipo;
        String observacion = null;
        NivelUrgencia nivel = null;
        DetalleEvento detalle;
        switch (evento) {
            case PedidoCreado creado -> {
                tipo = TipoEventoPedido.PEDIDO_CREADO;
                detalle = DetalleEvento.VACIO;
            }
            case EtapaEnviada enviada -> {
                tipo = TipoEventoPedido.ETAPA_ENVIADA;
                observacion = enviada.observacion();
                detalle = DetalleEvento.movimiento(enviada.etapaDestino(), enviada.urgenciaTerminada());
            }
            case EtapaAprobada aprobada -> {
                tipo = TipoEventoPedido.ETAPA_APROBADA;
                observacion = aprobada.observacion();
                detalle = DetalleEvento.movimiento(aprobada.etapaDestino(), aprobada.urgenciaTerminada());
            }
            case EtapaRechazada rechazada -> {
                tipo = TipoEventoPedido.ETAPA_RECHAZADA;
                observacion = rechazada.observacion();
                detalle = DetalleEvento.movimiento(rechazada.etapaDestino(), rechazada.urgenciaTerminada());
            }
            case PedidoDevuelto devuelto -> {
                tipo = TipoEventoPedido.PEDIDO_DEVUELTO;
                observacion = devuelto.observacion();
                nivel = devuelto.nivelUrgencia();
                detalle = new DetalleEvento(devuelto.etapaDestino(), devuelto.motivo(), null, null, null);
            }
            case EtapaReasignada reasignada -> {
                tipo = TipoEventoPedido.ETAPA_REASIGNADA;
                observacion = reasignada.motivo();
                detalle = new DetalleEvento(null, null, null, reasignada.asignadoAnterior(), reasignada.asignadoA());
            }
            case ObservacionAgregada agregada -> {
                tipo = TipoEventoPedido.OBSERVACION_AGREGADA;
                observacion = agregada.observacion();
                detalle = DetalleEvento.VACIO;
            }
            case ArchivoSubido subido -> {
                tipo = TipoEventoPedido.ARCHIVO_SUBIDO;
                detalle = DetalleEvento.VACIO;
            }
        }
        return new EventoPedido(id, evento.pedidoId(), evento.etapa(), tipo, evento.actor().usuarioId(),
                evento.actor().rol(), evento.ocurridoEn(), observacion, nivel, detalle, evento.adjuntoIds());
    }
}
