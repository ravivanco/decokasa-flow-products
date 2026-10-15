package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tabla rol con sus permisos (rol_permiso). Solo vive en el adaptador (R3). */
@Entity
@Table(name = "rol")
public class RolEntity {

    @Id
    private UUID id;

    private String codigo;
    private String nombre;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @ElementCollection
    @CollectionTable(name = "rol_permiso", joinColumns = @JoinColumn(name = "rol_id"))
    @Column(name = "permiso")
    private Set<String> permisos = new HashSet<>();

    protected RolEntity() {
    }

    RolEntity(UUID id, Instant creadoEn) {
        this.id = id;
        this.creadoEn = creadoEn;
    }

    void actualizar(String codigo, String nombre, Set<String> nuevosPermisos) {
        this.codigo = codigo;
        this.nombre = nombre;
        permisos.retainAll(nuevosPermisos);
        permisos.addAll(nuevosPermisos);
    }

    UUID getId() {
        return id;
    }

    String getCodigo() {
        return codigo;
    }

    String getNombre() {
        return nombre;
    }

    Set<String> getPermisos() {
        return permisos;
    }
}
