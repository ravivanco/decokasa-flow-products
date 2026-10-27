package com.decokasa.seguimiento.historial.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.Immutable;

/**
 * Tabla evento_pedido de V2: historial de solo inserción (R7). {@code @Immutable} y {@code updatable = false} impiden
 * que Hibernate emita un UPDATE; no hay setters. Si algo lo intentara, los triggers de V2 lo rechazan. Solo vive en el
 * adaptador (R3).
 */
@Entity
@Immutable
@Table(name = "evento_pedido")
public class EventoPedidoEntity {

    @Id
    private UUID id;

    @Column(name = "pedido_id", updatable = false, nullable = false)
    private UUID pedidoId;

    @Column(name = "etapa", updatable = false, nullable = false)
    private short etapa;

    @Column(name = "tipo", updatable = false, nullable = false)
    private String tipo;

    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "rol_codigo", updatable = false)
    private String rolCodigo;

    @Column(name = "ocurrido_en", updatable = false, nullable = false)
    private Instant ocurridoEn;

    @Column(name = "observacion", updatable = false)
    private String observacion;

    @Column(name = "nivel_urgencia", updatable = false)
    private String nivelUrgencia;

    // jsonb: el texto JSON se convierte en la base al insertar.
    @Column(name = "datos", updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String datos;

    // V13 (F4-H01): número de inserción que asigna la base (valor por defecto de la columna). La aplicación nunca lo
    // escribe; nulo en los eventos anteriores a V13, cuyo orden dentro de una misma hora no se conoce.
    @Column(name = "secuencia", insertable = false, updatable = false)
    private Long secuencia;

    protected EventoPedidoEntity() {
    }

    EventoPedidoEntity(UUID id, UUID pedidoId, short etapa, String tipo, UUID usuarioId, String rolCodigo,
            Instant ocurridoEn, String observacion, String nivelUrgencia, String datos) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.etapa = etapa;
        this.tipo = tipo;
        this.usuarioId = usuarioId;
        this.rolCodigo = rolCodigo;
        this.ocurridoEn = ocurridoEn;
        this.observacion = observacion;
        this.nivelUrgencia = nivelUrgencia;
        this.datos = datos;
    }

    UUID getId() {
        return id;
    }

    UUID getPedidoId() {
        return pedidoId;
    }

    short getEtapa() {
        return etapa;
    }

    String getTipo() {
        return tipo;
    }

    UUID getUsuarioId() {
        return usuarioId;
    }

    String getRolCodigo() {
        return rolCodigo;
    }

    Instant getOcurridoEn() {
        return ocurridoEn;
    }

    String getObservacion() {
        return observacion;
    }

    String getNivelUrgencia() {
        return nivelUrgencia;
    }

    String getDatos() {
        return datos;
    }
}
