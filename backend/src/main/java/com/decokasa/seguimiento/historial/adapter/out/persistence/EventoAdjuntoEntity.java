package com.decokasa.seguimiento.historial.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Tabla evento_adjunto de V3: archivos de cada evento del historial (SUBIDO o PRESENTADO). Solo inserción, como
 * evento_pedido (R7); los triggers de V3 rechazan UPDATE, DELETE y TRUNCATE.
 */
@Entity
@Immutable
@Table(name = "evento_adjunto")
@IdClass(EventoAdjuntoEntity.Clave.class)
public class EventoAdjuntoEntity {

    @Id
    @Column(name = "evento_id", updatable = false)
    private UUID eventoId;

    @Id
    @Column(name = "adjunto_id", updatable = false)
    private UUID adjuntoId;

    @Column(name = "relacion", updatable = false, nullable = false)
    private String relacion;

    protected EventoAdjuntoEntity() {
    }

    EventoAdjuntoEntity(UUID eventoId, UUID adjuntoId, String relacion) {
        this.eventoId = eventoId;
        this.adjuntoId = adjuntoId;
        this.relacion = relacion;
    }

    UUID getEventoId() {
        return eventoId;
    }

    UUID getAdjuntoId() {
        return adjuntoId;
    }

    /** Clave compuesta (evento, adjunto). */
    public static class Clave implements Serializable {

        private static final long serialVersionUID = 1L;

        private UUID eventoId;
        private UUID adjuntoId;

        public Clave() {
        }

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave clave && Objects.equals(eventoId, clave.eventoId)
                    && Objects.equals(adjuntoId, clave.adjuntoId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(eventoId, adjuntoId);
        }
    }
}
