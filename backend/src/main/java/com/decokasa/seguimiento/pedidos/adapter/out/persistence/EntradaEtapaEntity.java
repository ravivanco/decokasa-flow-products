package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tabla entrada_etapa de V3: una fila por cada entrada a una etapa (P-18). Solo inserción (triggers de V3). */
@Entity
@Immutable
@Table(name = "entrada_etapa")
public class EntradaEtapaEntity {

    @Id
    private UUID id;

    @Column(name = "pedido_id", updatable = false)
    private UUID pedidoId;

    @Column(name = "etapa", updatable = false)
    private short etapa;

    @Column(name = "entrada_en", updatable = false)
    private Instant entradaEn;

    protected EntradaEtapaEntity() {
    }

    EntradaEtapaEntity(UUID id, UUID pedidoId, short etapa, Instant entradaEn) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.etapa = etapa;
        this.entradaEn = entradaEn;
    }
}
