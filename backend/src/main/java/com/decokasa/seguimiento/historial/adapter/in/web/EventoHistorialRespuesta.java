package com.decokasa.seguimiento.historial.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.UsuarioResumenRespuesta;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Esquema EventoHistorial del contrato 0.7.0 para GET /pedidos/{id}/historial. */
public record EventoHistorialRespuesta(UUID id, UUID pedidoId, String numeroPedido, int etapa, String tipo,
        UsuarioResumenRespuesta usuario, String rolIntervencion, Detalle detalle, String fecha, String observacion,
        String nivelUrgencia, List<UUID> adjuntoIds) {

    /** Esquema DetalleEvento: cada campo es nulo cuando no aplica. */
    public record Detalle(Integer etapaDestino, String motivoDevolucion, Boolean urgenciaTerminada,
            UUID asignadoAnterior, UUID asignadoA, String fechaEstimadaAnterior, String fechaEstimadaSolicitada,
            String nuevaFechaEstimadaLlegada, UUID bodegaId) {
    }

    /** Solo para el historial de su propio módulo; otros módulos arman su respuesta desde el puerto (R4). */
    static EventoHistorialRespuesta de(ConsultarHistorialPedido.Entrada entrada) {
        ConsultarHistorialPedido.Evento evento = entrada.evento();
        ConsultarHistorialPedido.Persona persona = entrada.usuario();
        return new EventoHistorialRespuesta(evento.id(), evento.pedidoId(), entrada.numeroPedido(), evento.etapa(),
                evento.tipo(),
                persona == null ? null : UsuarioResumenRespuesta.de(persona.id(), persona.nombre(), persona.roles()),
                evento.rolIntervencion() == null ? null : evento.rolIntervencion().valor(),
                new Detalle(evento.etapaDestino(),
                        evento.motivoDevolucion() == null ? null : evento.motivoDevolucion().name(),
                        evento.urgenciaTerminada(), evento.asignadoAnterior(), evento.asignadoA(),
                        texto(evento.fechaEstimadaAnterior()), texto(evento.fechaEstimadaSolicitada()),
                        texto(evento.nuevaFechaEstimadaLlegada()), evento.bodegaId()),
                FechasWeb.texto(evento.ocurridoEn()), evento.observacion(),
                evento.nivelUrgencia() == null ? null : evento.nivelUrgencia().name(), evento.adjuntoIds());
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
