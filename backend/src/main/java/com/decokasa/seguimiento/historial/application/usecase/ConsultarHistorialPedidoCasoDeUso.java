package com.decokasa.seguimiento.historial.application.usecase;

import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.application.port.out.PedidosParaHistorial;
import com.decokasa.seguimiento.historial.domain.DetalleEvento;
import com.decokasa.seguimiento.historial.domain.EventoPedido;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Muestra el historial tal como se guardó: actor y rol de intervención del momento, aunque después cambien sus roles
 * (R7). El permiso de consulta es el de los pedidos (P-13, provisional del equipo). Sin Spring (R2).
 */
public final class ConsultarHistorialPedidoCasoDeUso implements ConsultarHistorialPedido {

    private final PedidosParaHistorial pedidos;
    private final HistorialRepositorio historial;
    private final ResumirUsuarios usuarios;

    public ConsultarHistorialPedidoCasoDeUso(PedidosParaHistorial pedidos, HistorialRepositorio historial,
            ResumirUsuarios usuarios) {
        this.pedidos = pedidos;
        this.historial = historial;
        this.usuarios = usuarios;
    }

    @Override
    public List<Entrada> historial(Solicitante solicitante, UUID pedidoId) {
        String numero = pedidos.autorizarConsulta(solicitante, pedidoId);
        // El repositorio devuelve el orden de inserción (hora y, en empate, secuencia de la base; F4-H01). El contrato
        // pide del más reciente al más antiguo: con la misma hora, primero el último evento emitido.
        List<EventoPedido> eventos = new ArrayList<>(historial.listarPorPedido(pedidoId));
        Collections.reverse(eventos);
        Map<UUID, ResumirUsuarios.Resumen> personas = usuarios.resumenes(eventos.stream()
                .map(EventoPedido::usuarioId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        return eventos.stream()
                .map(evento -> new Entrada(vista(evento), numero, persona(evento.usuarioId(), personas)))
                .toList();
    }

    @Override
    public Entrada evento(Solicitante solicitante, UUID pedidoId, UUID eventoId) {
        return historial(solicitante, pedidoId).stream()
                .filter(entrada -> entrada.evento().id().equals(eventoId))
                .findFirst()
                .orElseThrow(() -> new NoEncontrado("El evento no existe en el historial del pedido."));
    }

    /** El evento del dominio se entrega como vista del puerto: nada de historial.domain sale del módulo (R4). */
    private static Evento vista(EventoPedido evento) {
        DetalleEvento detalle = evento.detalle();
        return new Evento(evento.id(), evento.pedidoId(), evento.etapa(), evento.tipo().name(), evento.rolCodigo(),
                evento.ocurridoEn(), evento.observacion(), evento.nivelUrgencia(), detalle.etapaDestino(),
                detalle.motivoDevolucion(), detalle.urgenciaTerminada(), detalle.asignadoAnterior(), detalle.asignadoA(),
                detalle.fechaEstimadaAnterior(), detalle.fechaEstimadaSolicitada(), detalle.nuevaFechaEstimadaLlegada(),
                detalle.bodegaId(), detalle.claseVencimiento(), detalle.venceEn(), evento.adjuntoIds());
    }

    private static Persona persona(UUID id, Map<UUID, ResumirUsuarios.Resumen> personas) {
        if (id == null) {
            return null;
        }
        ResumirUsuarios.Resumen resumen = personas.get(id);
        return resumen == null ? new Persona(id, "Usuario no disponible", Set.of())
                : new Persona(resumen.id(), resumen.nombre(), resumen.roles());
    }
}
