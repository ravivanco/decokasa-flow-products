package com.decokasa.seguimiento.bodegas.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Tabla bodega de V9 (país desde V19). Solo existe en este adaptador (R3). */
@Entity
@Table(name = "bodega")
public class BodegaEntity {

    @Id
    private UUID id;

    // V19: país de la bodega; no cambia después del alta.
    @Column(name = "pais_codigo", updatable = false)
    private String paisCodigo;

    private String nombre;

    // Nula mientras la ciudad está pendiente (V19).
    private String ciudad;

    private boolean activa;

    @Column(name = "creada_en", updatable = false)
    private Instant creadaEn;

    @Column(name = "actualizada_en")
    private Instant actualizadaEn;

    // Nula en una bodega nueva: así Spring Data la inserta (persist) y Hibernate empieza en 0.
    @Version
    private Long version;

    protected BodegaEntity() {
    }

    BodegaEntity(UUID id, String paisCodigo, Instant creadaEn) {
        this.id = id;
        this.paisCodigo = paisCodigo;
        this.creadaEn = creadaEn;
    }

    void actualizar(String nombre, String ciudad, boolean activa, Instant ahora) {
        this.nombre = nombre;
        this.ciudad = ciudad;
        this.activa = activa;
        this.actualizadaEn = ahora;
    }

    UUID getId() {
        return id;
    }

    String getPaisCodigo() {
        return paisCodigo;
    }

    String getNombre() {
        return nombre;
    }

    String getCiudad() {
        return ciudad;
    }

    boolean isActiva() {
        return activa;
    }

    Long getVersion() {
        return version;
    }
}
