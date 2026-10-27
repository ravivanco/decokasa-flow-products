package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.UsuarioResumenRespuesta;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Respuesta de POST /pedidos/{id}/observaciones: el esquema EventoHistorial del contrato 0.7.0. Pedidos arma su propia
 * respuesta HTTP desde el puerto de entrada de historial y no usa el adaptador web de ese módulo (R4; P20-R4-01).
 */
public record ObservacionRespuesta(UUID id, UUID pedidoId, String numeroPedido, int etapa, String tipo,
        UsuarioResumenRespuesta usuario, String rolIntervencion, Detalle detalle, String fecha, String observacion,
        String nivelUrgencia, List<UUID> adjuntoIds) {

    /** Esquema DetalleEvento: cada campo es nulo cuando no aplica. */
    public record Detalle(Integer etapaDestino, String motivoDevolucion, Boolean urgenciaTerminada,
            UUID asignadoAnterior, UUID asignadoA, String fechaEstimadaAnterior, String fechaEstimadaSolicitada,
            String nuevaFechaEstimadaLlegada, UUID bodegaId, String claseVencimiento, String venceEn) {
    }

    static ObservacionRespuesta de(ConsultarHistorialPedido.Entrada entrada) {
        ConsultarHistorialPedido.Evento evento = entrada.evento();
        ConsultarHistorialPedido.Persona persona = entrada.usuario();
        return new ObservacionRespuesta(evento.id(), evento.pedidoId(), entrada.numeroPedido(), evento.etapa(),
                evento.tipo(),
                persona == null ? null : UsuarioResumenRespuesta.de(persona.id(), persona.nombre(), persona.roles()),
                evento.rolIntervencion() == null ? null : evento.rolIntervencion().valor(),
                new Detalle(evento.etapaDestino(),
                        evento.motivoDevolucion() == null ? null : evento.motivoDevolucion().name(),
                        evento.urgenciaTerminada(), evento.asignadoAnterior(), evento.asignadoA(),
                        texto(evento.fechaEstimadaAnterior()), texto(evento.fechaEstimadaSolicitada()),
                        texto(evento.nuevaFechaEstimadaLlegada()), evento.bodegaId(),
                        evento.claseVencimiento(), FechasWeb.texto(evento.venceEn())),
                FechasWeb.texto(evento.ocurridoEn()), evento.observacion(),
                evento.nivelUrgencia() == null ? null : evento.nivelUrgencia().name(), evento.adjuntoIds());
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
