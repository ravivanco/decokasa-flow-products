package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;
import java.util.function.Consumer;

/** Alta de usuarios: solo el admin (R6), uno o varios roles existentes y datos únicos (requerimientos 4.7). */
public final class CrearUsuarioCasoDeUso implements CrearUsuario {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final CodificadorContrasena codificador;
    private final Consumer<Solicitante> autorizar;

    /** Alta por la API: exige que el solicitante sea hoy un admin activo en la base (R6, hallazgo A1). */
    public CrearUsuarioCasoDeUso(UsuarioRepositorio usuarios, RolRepositorio roles, CodificadorContrasena codificador) {
        this(usuarios, roles, codificador, new Autorizacion(usuarios)::exigirAdmin);
    }

    private CrearUsuarioCasoDeUso(UsuarioRepositorio usuarios, RolRepositorio roles, CodificadorContrasena codificador,
            Consumer<Solicitante> autorizar) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.codificador = codificador;
        this.autorizar = autorizar;
    }

    /**
     * Alta de los usuarios iniciales (ADR-0015): la base todavía no tiene un admin que autorice, así que el permiso lo
     * da el despliegue (SEMILLA_HABILITADA con la tabla vacía). Solo la usa el cargador de arranque, que no la expone
     * como bean; la API usa el constructor, que exige un admin activo en la base.
     */
    public static CrearUsuario paraCargaInicial(UsuarioRepositorio usuarios, RolRepositorio roles,
            CodificadorContrasena codificador) {
        return new CrearUsuarioCasoDeUso(usuarios, roles, codificador, solicitante -> {
            // Sin revisión de admin: la autoriza el despliegue, no una persona.
        });
    }

    @Override
    public Usuario crear(Solicitante solicitante, Comando comando) {
        autorizar.accept(solicitante);
        ValidacionRoles.exigirExistentes(roles, comando.roles());
        Telefono telefono = Telefono.normalizar(comando.telefono());
        ContrasenaNueva contrasena = contrasenaInicial(comando.contrasenaInicial());
        Set<CodigoPais> paises = comando.paises() == null || comando.paises().isEmpty()
                ? Set.of(CodigoPais.EC)
                : comando.paises();
        // Valida los datos; la contraseña se codifica solo después de descartar duplicados.
        Usuario usuario = Usuario.nuevo(comando.nombre(), comando.correo(), comando.usuario(), telefono, null,
                comando.roles(), paises);

        if (usuarios.buscarPorCorreo(usuario.correo()).isPresent()) {
            throw new DatoDuplicado("correo", "Ya existe un usuario con ese correo.");
        }
        if (usuarios.buscarPorNombreDeUsuario(usuario.usuario()).isPresent()) {
            throw new DatoDuplicado("usuario", "Ya existe un usuario con ese nombre de usuario.");
        }
        if (usuarios.buscarPorTelefono(telefono).isPresent()) {
            throw new DatoDuplicado("telefono", "Ya existe un usuario con ese teléfono.");
        }

        usuario.cambiarContrasenaHash(codificador.codificar(contrasena.valor()));
        usuarios.guardar(usuario);
        return usuario;
    }

    private static ContrasenaNueva contrasenaInicial(String valor) {
        try {
            return new ContrasenaNueva(valor);
        } catch (DatoInvalido error) {
            throw new DatoInvalido("contrasenaInicial", error.getMessage());
        }
    }
}
