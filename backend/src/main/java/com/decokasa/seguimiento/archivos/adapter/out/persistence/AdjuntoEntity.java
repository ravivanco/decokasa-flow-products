package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Tabla adjunto de V3. Los datos del archivo no cambian ({@code updatable = false}); el estado de la copia a Drive lo
 * actualizará la fase 3. Solo vive en el adaptador (R3).
 */
@Entity
@Table(name = "adjunto")
public class AdjuntoEntity {

    @Id
    private UUID id;

    @Column(name = "pedido_id", updatable = false)
    private UUID pedidoId;

    @Column(name = "etapa", updatable = false)
    private short etapa;

    @Column(name = "entrada_id", updatable = false)
    private UUID entradaId;

    @Column(name = "tipo_documento", updatable = false)
    private String tipoDocumento;

    @Column(name = "nombre", updatable = false)
    private String nombre;

    @Column(name = "tamano_bytes", updatable = false)
    private long tamanoBytes;

    @Column(name = "sha256", updatable = false)
    private String sha256;

    @Column(name = "tipo_detectado", updatable = false)
    private String tipoDetectado;

    @Column(name = "clave_local")
    private String claveLocal;

    @Column(name = "drive_id")
    private String driveId;

    @Column(name = "estado_copia")
    private String estadoCopia;

    @Column(name = "intentos")
    private int intentos;

    @Column(name = "subido_por", updatable = false)
    private UUID subidoPor;

    @Column(name = "subido_en", updatable = false)
    private Instant subidoEn;

    protected AdjuntoEntity() {
    }

    AdjuntoEntity(UUID id, UUID pedidoId, short etapa, UUID entradaId, String tipoDocumento, String nombre,
            long tamanoBytes, String sha256, String tipoDetectado, String claveLocal, String estadoCopia,
            UUID subidoPor, Instant subidoEn) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.etapa = etapa;
        this.entradaId = entradaId;
        this.tipoDocumento = tipoDocumento;
        this.nombre = nombre;
        this.tamanoBytes = tamanoBytes;
        this.sha256 = sha256;
        this.tipoDetectado = tipoDetectado;
        this.claveLocal = claveLocal;
        this.estadoCopia = estadoCopia;
        this.subidoPor = subidoPor;
        this.subidoEn = subidoEn;
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

    UUID getEntradaId() {
        return entradaId;
    }

    String getTipoDocumento() {
        return tipoDocumento;
    }

    String getNombre() {
        return nombre;
    }

    long getTamanoBytes() {
        return tamanoBytes;
    }

    String getSha256() {
        return sha256;
    }

    String getTipoDetectado() {
        return tipoDetectado;
    }

    String getClaveLocal() {
        return claveLocal;
    }

    String getEstadoCopia() {
        return estadoCopia;
    }

    UUID getSubidoPor() {
        return subidoPor;
    }

    Instant getSubidoEn() {
        return subidoEn;
    }
}
