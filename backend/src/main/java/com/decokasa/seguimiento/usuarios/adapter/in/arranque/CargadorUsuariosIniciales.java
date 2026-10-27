package com.decokasa.seguimiento.usuarios.adapter.in.arranque;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Carga los usuarios iniciales desde un archivo fuera de Git (ADR-0015; plan, tarea 10). Solo actúa con la tabla de
 * usuarios vacía y usa el caso de uso {@link CrearUsuario}, así se aplican las mismas reglas que en un alta del admin.
 * La contraseña temporal llega por variable de entorno y nunca se escribe en el log.
 */
public final class CargadorUsuariosIniciales {

    private static final Logger LOG = LoggerFactory.getLogger(CargadorUsuariosIniciales.class);

    /** La carga la hace el propio sistema con permiso de admin; no es una persona. */
    private static final Solicitante SISTEMA = new Solicitante(new UUID(0, 0), Set.of(new CodigoRol("admin")));

    record UsuarioInicial(String nombre, String correo, String usuario, String telefono, List<String> roles) {
    }

    private final CrearUsuario crearUsuario;
    private final BooleanSupplier hayUsuarios;
    private final JsonMapper json;
    private final Path archivo;
    private final String contrasenaTemporal;

    public CargadorUsuariosIniciales(CrearUsuario crearUsuario, BooleanSupplier hayUsuarios, JsonMapper json,
            Path archivo, String contrasenaTemporal) {
        this.crearUsuario = crearUsuario;
        this.hayUsuarios = hayUsuarios;
        this.json = json;
        this.archivo = archivo;
        this.contrasenaTemporal = contrasenaTemporal;
    }

    public void cargar() {
        if (contrasenaTemporal == null || contrasenaTemporal.isBlank()) {
            throw new IllegalStateException("Define SEMILLA_CONTRASENA_TEMPORAL para cargar los usuarios iniciales.");
        }
        if (hayUsuarios.getAsBoolean()) {
            LOG.info("Ya hay usuarios: no se cargan los usuarios iniciales.");
            return;
        }
        List<UsuarioInicial> usuarios = leer();
        for (UsuarioInicial usuario : usuarios) {
            Set<CodigoRol> roles = usuario.roles() == null ? Set.of()
                    : usuario.roles().stream().map(CodigoRol::new).collect(Collectors.toSet());
            crearUsuario.crear(SISTEMA, new CrearUsuario.Comando(usuario.nombre(), usuario.correo(),
                    usuario.usuario(), usuario.telefono(), roles, Set.of(CodigoPais.EC), contrasenaTemporal));
        }
        LOG.info("Se cargaron {} usuarios iniciales.", usuarios.size());
    }

    private List<UsuarioInicial> leer() {
        if (archivo == null || !Files.isReadable(archivo)) {
            throw new IllegalStateException("No se puede leer el archivo de SEMILLA_ARCHIVO: " + archivo);
        }
        try {
            return json.readValue(Files.readString(archivo), new TypeReference<List<UsuarioInicial>>() { });
        } catch (IOException error) {
            throw new IllegalStateException("No se pudo leer el archivo de SEMILLA_ARCHIVO: " + archivo, error);
        }
    }
}
