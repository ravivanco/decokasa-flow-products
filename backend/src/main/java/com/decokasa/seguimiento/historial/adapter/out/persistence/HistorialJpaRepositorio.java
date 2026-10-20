package com.decokasa.seguimiento.historial.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.domain.DetalleEvento;
import com.decokasa.seguimiento.historial.domain.EventoPedido;
import com.decokasa.seguimiento.historial.domain.TipoEventoPedido;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Adaptador JPA del puerto {@link HistorialRepositorio}. Inserta con {@code persist} (nunca {@code merge}) en la
 * transacción abierta por la acción: Hibernate escribe la fila al confirmar o al vaciar la sesión, después de las
 * inserciones del pedido que la acción haya hecho antes, así que la FK a pedido se cumple.
 */
@Component
public class HistorialJpaRepositorio implements HistorialRepositorio {

    private final EntityManager entityManager;
    private final EventoPedidoDatos datos;
    private final JsonMapper json;

    public HistorialJpaRepositorio(EntityManager entityManager, EventoPedidoDatos datos, JsonMapper json) {
        this.entityManager = entityManager;
        this.datos = datos;
        this.json = json;
    }

    @Override
    public void insertar(EventoPedido evento) {
        entityManager.persist(new EventoPedidoEntity(evento.id(), evento.pedidoId(), (short) evento.etapa(),
                evento.tipo().name(), evento.usuarioId(),
                evento.rolCodigo() == null ? null : evento.rolCodigo().valor(), evento.ocurridoEn(),
                evento.observacion(), evento.nivelUrgencia() == null ? null : evento.nivelUrgencia().name(),
                aJson(evento.detalle())));
        // El adjunto ya existe en esta transacción (lo insertó antes el módulo de archivos): la FK se cumple.
        String relacion = evento.tipo() == TipoEventoPedido.ARCHIVO_SUBIDO ? "SUBIDO" : "PRESENTADO";
        for (UUID adjuntoId : evento.adjuntoIds()) {
            entityManager.persist(new EventoAdjuntoEntity(evento.id(), adjuntoId, relacion));
        }
    }

    @Override
    public List<EventoPedido> listarPorPedido(UUID pedidoId) {
        List<EventoPedidoEntity> filas = datos.findByPedidoIdOrderByOcurridoEnAscSecuenciaAscIdAsc(pedidoId);
        Map<UUID, List<UUID>> adjuntos = filas.isEmpty() ? Map.of()
                : adjuntosPorEvento(filas.stream().map(EventoPedidoEntity::getId).toList());
        return filas.stream()
                .map(fila -> aDominio(fila, adjuntos.getOrDefault(fila.getId(), List.of())))
                .toList();
    }

    private Map<UUID, List<UUID>> adjuntosPorEvento(Collection<UUID> eventos) {
        Map<UUID, List<UUID>> porEvento = new LinkedHashMap<>();
        for (EventoAdjuntoEntity fila : datos.adjuntosDe(eventos)) {
            porEvento.computeIfAbsent(fila.getEventoId(), clave -> new ArrayList<>()).add(fila.getAdjuntoId());
        }
        return porEvento;
    }

    private String aJson(DetalleEvento detalle) {
        Map<String, Object> campos = new LinkedHashMap<>();
        if (detalle.etapaDestino() != null) {
            campos.put("etapaDestino", detalle.etapaDestino());
        }
        if (detalle.motivoDevolucion() != null) {
            campos.put("motivoDevolucion", detalle.motivoDevolucion().name());
        }
        if (detalle.urgenciaTerminada() != null) {
            campos.put("urgenciaTerminada", detalle.urgenciaTerminada());
        }
        if (detalle.asignadoAnterior() != null) {
            campos.put("asignadoAnterior", detalle.asignadoAnterior().toString());
        }
        if (detalle.asignadoA() != null) {
            campos.put("asignadoA", detalle.asignadoA().toString());
        }
        // Fechas de una demora, en ISO (aaaa-mm-dd).
        if (detalle.fechaEstimadaAnterior() != null) {
            campos.put("fechaEstimadaAnterior", detalle.fechaEstimadaAnterior().toString());
        }
        if (detalle.fechaEstimadaSolicitada() != null) {
            campos.put("fechaEstimadaSolicitada", detalle.fechaEstimadaSolicitada().toString());
        }
        if (detalle.nuevaFechaEstimadaLlegada() != null) {
            campos.put("nuevaFechaEstimadaLlegada", detalle.nuevaFechaEstimadaLlegada().toString());
        }
        if (detalle.bodegaId() != null) {
            campos.put("bodegaId", detalle.bodegaId().toString());
        }
        return campos.isEmpty() ? null : json.writeValueAsString(campos);
    }

    private EventoPedido aDominio(EventoPedidoEntity entidad, List<UUID> adjuntoIds) {
        return new EventoPedido(entidad.getId(), entidad.getPedidoId(), entidad.getEtapa(),
                TipoEventoPedido.valueOf(entidad.getTipo()), entidad.getUsuarioId(),
                entidad.getRolCodigo() == null ? null : new CodigoRol(entidad.getRolCodigo()),
                entidad.getOcurridoEn(), entidad.getObservacion(),
                entidad.getNivelUrgencia() == null ? null : NivelUrgencia.valueOf(entidad.getNivelUrgencia()),
                deJson(entidad.getDatos()), adjuntoIds);
    }

    private DetalleEvento deJson(String texto) {
        if (texto == null || texto.isBlank()) {
            return new DetalleEvento(null, null, null, null, null);
        }
        JsonNode nodo = json.readTree(texto);
        return new DetalleEvento(
                nodo.hasNonNull("etapaDestino") ? nodo.get("etapaDestino").asInt() : null,
                nodo.hasNonNull("motivoDevolucion")
                        ? MotivoDevolucion.valueOf(nodo.get("motivoDevolucion").asString()) : null,
                nodo.hasNonNull("urgenciaTerminada") ? nodo.get("urgenciaTerminada").asBoolean() : null,
                nodo.hasNonNull("asignadoAnterior") ? UUID.fromString(nodo.get("asignadoAnterior").asString()) : null,
                nodo.hasNonNull("asignadoA") ? UUID.fromString(nodo.get("asignadoA").asString()) : null,
                fecha(nodo, "fechaEstimadaAnterior"), fecha(nodo, "fechaEstimadaSolicitada"),
                fecha(nodo, "nuevaFechaEstimadaLlegada"),
                nodo.hasNonNull("bodegaId") ? UUID.fromString(nodo.get("bodegaId").asString()) : null);
    }

    private static LocalDate fecha(JsonNode nodo, String campo) {
        return nodo.hasNonNull(campo) ? LocalDate.parse(nodo.get(campo).asString()) : null;
    }
}
