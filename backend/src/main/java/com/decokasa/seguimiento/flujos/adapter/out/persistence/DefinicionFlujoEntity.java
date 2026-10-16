package com.decokasa.seguimiento.flujos.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/** Tabla definicion_flujo de V2. Solo lectura: las definiciones se cargan con migraciones (ADR-0004). */
@Entity
@Immutable
@Table(name = "definicion_flujo")
public class DefinicionFlujoEntity {

    @Id
    private UUID id;

    @Column(name = "pais_codigo")
    private String paisCodigo;

    @Column(name = "tipo_flujo")
    private String tipoFlujo;

    @Column(name = "version")
    private int version;

    @Column(name = "vigente")
    private boolean vigente;

    protected DefinicionFlujoEntity() {
    }

    UUID getId() {
        return id;
    }

    String getPaisCodigo() {
        return paisCodigo;
    }

    String getTipoFlujo() {
        return tipoFlujo;
    }

    int getVersion() {
        return version;
    }

    boolean isVigente() {
        return vigente;
    }
}
