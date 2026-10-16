package com.decokasa.seguimiento.flujos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tabla definicion_etapa de V2. Solo lectura. */
@Entity
@Immutable
@Table(name = "definicion_etapa")
public class DefinicionEtapaEntity {

    @Id
    private UUID id;

    @Column(name = "flujo_id")
    private UUID flujoId;

    @Column(name = "numero")
    private short numero;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "rol_codigo")
    private String rolCodigo;

    @Column(name = "plazo_estado")
    private String plazoEstado;

    @Column(name = "plazo_cantidad")
    private Integer plazoCantidad;

    @Column(name = "plazo_tipo")
    private String plazoTipo;

    @Column(name = "automatica")
    private boolean automatica;

    @Column(name = "revisa_etapa")
    private Short revisaEtapa;

    protected DefinicionEtapaEntity() {
    }

    UUID getFlujoId() {
        return flujoId;
    }

    short getNumero() {
        return numero;
    }

    String getNombre() {
        return nombre;
    }

    String getRolCodigo() {
        return rolCodigo;
    }

    String getPlazoEstado() {
        return plazoEstado;
    }

    Integer getPlazoCantidad() {
        return plazoCantidad;
    }

    String getPlazoTipo() {
        return plazoTipo;
    }

    boolean isAutomatica() {
        return automatica;
    }

    Short getRevisaEtapa() {
        return revisaEtapa;
    }
}
