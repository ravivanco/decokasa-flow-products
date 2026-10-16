package com.decokasa.seguimiento.flujos.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tabla transicion_permitida de V2, con sus documentos obligatorios (documento_requerido). Solo lectura. */
@Entity
@Immutable
@Table(name = "transicion_permitida")
public class TransicionPermitidaEntity {

    @Id
    private UUID id;

    @Column(name = "flujo_id")
    private UUID flujoId;

    @Column(name = "etapa_origen")
    private short etapaOrigen;

    @Column(name = "accion")
    private String accion;

    @Column(name = "etapa_destino")
    private Short etapaDestino;

    @Column(name = "requiere_observacion")
    private boolean requiereObservacion;

    @Column(name = "requiere_urgencia")
    private boolean requiereUrgencia;

    @Column(name = "requiere_bodega")
    private boolean requiereBodega;

    @Column(name = "requisitos_documentales")
    private String requisitosDocumentales;

    @ElementCollection
    @Immutable
    @CollectionTable(name = "documento_requerido", joinColumns = @JoinColumn(name = "transicion_id"))
    @Column(name = "tipo_documento")
    private Set<String> documentos = new HashSet<>();

    protected TransicionPermitidaEntity() {
    }

    UUID getFlujoId() {
        return flujoId;
    }

    short getEtapaOrigen() {
        return etapaOrigen;
    }

    String getAccion() {
        return accion;
    }

    Short getEtapaDestino() {
        return etapaDestino;
    }

    boolean isRequiereObservacion() {
        return requiereObservacion;
    }

    boolean isRequiereUrgencia() {
        return requiereUrgencia;
    }

    boolean isRequiereBodega() {
        return requiereBodega;
    }

    String getRequisitosDocumentales() {
        return requisitosDocumentales;
    }

    Set<String> getDocumentos() {
        return documentos;
    }
}
