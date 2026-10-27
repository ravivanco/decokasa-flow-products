package com.decokasa.seguimiento.plazos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Tabla configuracion_plazos de V14 (una sola fila, id = 1). Solo existe en este adaptador (R3). */
@Entity
@Table(name = "configuracion_plazos")
public class ConfiguracionPlazosEntity {

    static final short FILA_UNICA = 1;

    @Id
    private Short id;

    @Column(name = "meta_dias")
    private int metaDias;

    @Column(name = "meta_origen")
    private String metaOrigen;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Column(name = "actualizado_por")
    private UUID actualizadoPor;

    @Version
    private Long version;

    protected ConfiguracionPlazosEntity() {
    }

    void actualizar(int metaDias, String metaOrigen, Instant actualizadoEn, UUID actualizadoPor) {
        this.metaDias = metaDias;
        this.metaOrigen = metaOrigen;
        this.actualizadoEn = actualizadoEn;
        this.actualizadoPor = actualizadoPor;
    }

    int getMetaDias() {
        return metaDias;
    }

    String getMetaOrigen() {
        return metaOrigen;
    }

    Instant getActualizadoEn() {
        return actualizadoEn;
    }

    UUID getActualizadoPor() {
        return actualizadoPor;
    }

    Long getVersion() {
        return version;
    }
}
