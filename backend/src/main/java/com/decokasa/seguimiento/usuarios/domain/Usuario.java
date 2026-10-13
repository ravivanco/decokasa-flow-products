package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Persona que usa el sistema (modelo-datos.md 3.1). Tiene uno o varios roles, que solo asigna el admin; sin roles o
 * desactivada no inicia ni renueva sesión. No se borra: se desactiva (requerimientos 4.7). Java puro (R1).
 * Los límites de los campos son los mismos de la base y del contrato (modelo-datos.md 3.1, "Límites de campos").
 */
public final class Usuario {

    private final UUID id;
    private String nombre;
    private String correo;
    private final String usuario;
    private Telefono telefono;
    private String contrasenaHash;
    private Set<CodigoRol> roles;
    private Set<CodigoPais> paises;
    private boolean activo;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private final long version;

    /** Reconstruye un usuario guardado, sin validar reglas de creación. */
    public Usuario(UUID id, String nombre, String correo, String usuario, Telefono telefono, String contrasenaHash,
            Set<CodigoRol> roles, Set<CodigoPais> paises, boolean activo, int intentosFallidos,
            Instant bloqueadoHasta, long version) {
        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.usuario = usuario;
        this.telefono = telefono;
        this.contrasenaHash = contrasenaHash;
        this.roles = Set.copyOf(roles);
        this.paises = Set.copyOf(paises);
        this.activo = activo;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.version = version;
    }

    /**
     * Usuario nuevo: activo, sin intentos fallidos y con al menos un rol.
     *
     * @throws DatoInvalido si no tiene roles o un campo está fuera de sus límites
     */
    public static Usuario nuevo(String nombre, String correo, String usuario, Telefono telefono,
            String contrasenaHash, Set<CodigoRol> roles, Set<CodigoPais> paises) {
        validarNombre(nombre);
        validarCorreo(correo);
        validarNombreDeUsuario(usuario);
        validarRoles(roles);
        return new Usuario(UUID.randomUUID(), nombre.strip(), correo.strip(), usuario.strip(), telefono,
                contrasenaHash, roles, paises, true, 0, null, 0);
    }

    /** Activo y con al menos un rol. */
    public boolean puedeIniciarSesion() {
        return activo && !roles.isEmpty();
    }

    public boolean tieneRol(CodigoRol rol) {
        return roles.contains(rol);
    }

    public boolean estaBloqueado(Instant ahora) {
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Suma un intento fallido y bloquea la cuenta al llegar al máximo de la política. */
    public void registrarFallo(Instant ahora, PoliticaBloqueo politica) {
        if (bloqueadoHasta != null && !estaBloqueado(ahora)) {
            // Un bloqueo ya vencido no cuenta: el contador empieza de nuevo.
            intentosFallidos = 0;
            bloqueadoHasta = null;
        }
        intentosFallidos++;
        if (intentosFallidos >= politica.intentosMaximos()) {
            bloqueadoHasta = ahora.plus(politica.duracion());
        }
    }

    /** Un inicio de sesión correcto reinicia el contador de intentos. */
    public void registrarAcierto() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    /**
     * Reemplaza los roles. Exige al menos uno: para retirar el acceso se desactiva la cuenta.
     *
     * @throws DatoInvalido si la lista está vacía
     */
    public void asignarRoles(Set<CodigoRol> nuevosRoles) {
        validarRoles(nuevosRoles);
        roles = Set.copyOf(nuevosRoles);
    }

    public void asignarPaises(Set<CodigoPais> nuevosPaises) {
        paises = Set.copyOf(nuevosPaises);
    }

    /** Cambia los datos que no sean nulos, con los mismos límites que al crear. */
    public void cambiarDatos(String nuevoNombre, String nuevoCorreo, Telefono nuevoTelefono) {
        if (nuevoNombre != null) {
            validarNombre(nuevoNombre);
        }
        if (nuevoCorreo != null) {
            validarCorreo(nuevoCorreo);
        }
        if (nuevoNombre != null) {
            nombre = nuevoNombre.strip();
        }
        if (nuevoCorreo != null) {
            correo = nuevoCorreo.strip();
        }
        if (nuevoTelefono != null) {
            telefono = nuevoTelefono;
        }
    }

    public void cambiarContrasenaHash(String nuevoHash) {
        contrasenaHash = nuevoHash;
    }

    public void desactivar() {
        activo = false;
    }

    public void activar() {
        activo = true;
    }

    private static void validarRoles(Set<CodigoRol> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new DatoInvalido("roles", "Elige al menos un rol.");
        }
    }

    private static void validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.strip().length() > 200) {
            throw new DatoInvalido("nombre", "El nombre debe tener de 1 a 200 caracteres.");
        }
    }

    private static void validarCorreo(String correo) {
        String limpio = correo == null ? "" : correo.strip();
        if (limpio.indexOf('@') < 1 || limpio.length() > 254) {
            throw new DatoInvalido("correo", "Escribe un correo válido.");
        }
    }

    private static void validarNombreDeUsuario(String usuario) {
        String limpio = usuario == null ? "" : usuario.strip();
        if (limpio.length() < 3 || limpio.length() > 50) {
            throw new DatoInvalido("usuario", "El usuario debe tener entre 3 y 50 caracteres.");
        }
    }

    public UUID id() {
        return id;
    }

    public String nombre() {
        return nombre;
    }

    public String correo() {
        return correo;
    }

    public String usuario() {
        return usuario;
    }

    public Telefono telefono() {
        return telefono;
    }

    public String contrasenaHash() {
        return contrasenaHash;
    }

    public Set<CodigoRol> roles() {
        return roles;
    }

    public Set<CodigoPais> paises() {
        return paises;
    }

    public boolean activo() {
        return activo;
    }

    public int intentosFallidos() {
        return intentosFallidos;
    }

    public Instant bloqueadoHasta() {
        return bloqueadoHasta;
    }

    public long version() {
        return version;
    }
}
