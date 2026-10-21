package com.decokasa.seguimiento.historial.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Historial de un pedido (contrato: GET /pedidos/{id}/historial), del más reciente al más antiguo. P-13. */
public interface ConsultarHistorialPedido {

    List<Entrada> historial(Solicitante solicitante, UUID pedidoId);

    /**
     * Un evento del historial del pedido, con el mismo permiso (P-13).
     *
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el evento no es de ese pedido
     */
    Entrada evento(Solicitante solicitante, UUID pedidoId, UUID eventoId);

    /** @param usuario nulo en los eventos del sistema */
    record Entrada(Evento evento, String numeroPedido, Persona usuario) {
    }

    record Persona(UUID id, String nombre, Set<CodigoRol> roles) {
    }

    /**
     * Vista del evento para otros módulos y para la web, sin tipos del dominio de historial (R4): quien la usa no depende
     * de {@code historial.domain}.
     *
     * @param tipo valor del enum del contrato (EventoHistorial.tipo)
     * @param rolIntervencion rol con el que intervino en ese momento; nulo en los eventos del sistema
     */
    record Evento(UUID id, UUID pedidoId, int etapa, String tipo, CodigoRol rolIntervencion, Instant ocurridoEn,
            String observacion, NivelUrgencia nivelUrgencia, Integer etapaDestino, MotivoDevolucion motivoDevolucion,
            Boolean urgenciaTerminada, UUID asignadoAnterior, UUID asignadoA, LocalDate fechaEstimadaAnterior,
            LocalDate fechaEstimadaSolicitada, LocalDate nuevaFechaEstimadaLlegada, UUID bodegaId,
            String claseVencimiento, Instant venceEn, List<UUID> adjuntoIds) {

        public Evento {
            adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        }
    }
}
