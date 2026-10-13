package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Tabla usuario con sus roles (usuario_rol) y países (usuario_pais). Solo vive en el adaptador (R3). */
@Entity
@Table(name = "usuario")
public class UsuarioEntity {

    @Id
    private UUID id;

    private String nombre;
    private String correo;
    private String usuario;
    private String telefono;

    @Column(name = "contrasena_hash")
    private String contrasenaHash;

    private boolean activo;

    // Contadores del login, fuera de la versión administrativa (D10): se insertan con el usuario, pero un guardado
    // de la entidad nunca los escribe; solo los cambia UsuarioDatos.guardarIntentos, que no toca la versión.
    @Column(name = "intentos_fallidos", updatable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta", updatable = false)
    private Instant bloqueadoHasta;

    @Column(name = "creado_en", updatable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    @Version
    private Long version;

    @ElementCollection
    @CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
    private Set<AsignacionRolEntity> roles = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "usuario_pais", joinColumns = @JoinColumn(name = "usuario_id"))
    @Column(name = "pais_codigo")
    private Set<String> paises = new HashSet<>();

    protected UsuarioEntity() {
    }

    UsuarioEntity(UUID id, Instant creadoEn) {
        this.id = id;
        this.creadoEn = creadoEn;
    }

    /** Datos administrativos: los que cubre la versión (D10). Los contadores del login no se tocan aquí. */
    void actualizar(String nombre, String correo, String usuario, String telefono, String contrasenaHash,
            boolean activo, Instant actualizadoEn) {
        this.nombre = nombre;
        this.correo = correo;
        this.usuario = usuario;
        this.telefono = telefono;
        this.contrasenaHash = contrasenaHash;
        this.activo = activo;
        this.actualizadoEn = actualizadoEn;
    }

    UUID getId() {
        return id;
    }

    String getNombre() {
        return nombre;
    }

    String getCorreo() {
        return correo;
    }

    String getUsuario() {
        return usuario;
    }

    String getTelefono() {
        return telefono;
    }

    String getContrasenaHash() {
        return contrasenaHash;
    }

    boolean isActivo() {
        return activo;
    }

    int getIntentosFallidos() {
        return intentosFallidos;
    }

    Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    Long getVersion() {
        return version;
    }

    Set<AsignacionRolEntity> getRoles() {
        return roles;
    }

    Set<String> getPaises() {
        return paises;
    }
}
