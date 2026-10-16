package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Tabla etapa_pedido de V2. Las columnas de demora (motivo_retraso, fecha_planificada, movida_al_lunes) son de la
 * fase 4 y no se mapean: toman sus valores por defecto de la base.
 */
@Entity
@Table(name = "etapa_pedido")
public class EtapaPedidoEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", updatable = false)
    private PedidoEntity pedido;

    @Column(name = "numero", updatable = false)
    private short numero;

    @Column(name = "estado")
    private String estado;

    @Column(name = "asignado_a")
    private UUID asignadoA;

    @Column(name = "entrada_en")
    private Instant entradaEn;

    @Column(name = "salida_en")
    private Instant salidaEn;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "reentradas")
    private int reentradas;

    @Column(name = "entrada_id")
    private UUID entradaId;

    protected EtapaPedidoEntity() {
    }

    EtapaPedidoEntity(UUID id, PedidoEntity pedido, short numero) {
        this.id = id;
        this.pedido = pedido;
        this.numero = numero;
    }

    void cambiar(String estado, UUID asignadoA, Instant entradaEn, Instant salidaEn, Instant venceEn, int reentradas,
            UUID entradaId) {
        this.estado = estado;
        this.asignadoA = asignadoA;
        this.entradaEn = entradaEn;
        this.salidaEn = salidaEn;
        this.venceEn = venceEn;
        this.reentradas = reentradas;
        this.entradaId = entradaId;
    }

    UUID getId() {
        return id;
    }

    short getNumero() {
        return numero;
    }

    String getEstado() {
        return estado;
    }

    UUID getAsignadoA() {
        return asignadoA;
    }

    Instant getEntradaEn() {
        return entradaEn;
    }

    Instant getSalidaEn() {
        return salidaEn;
    }

    Instant getVenceEn() {
        return venceEn;
    }

    int getReentradas() {
        return reentradas;
    }

    UUID getEntradaId() {
        return entradaId;
    }
}
