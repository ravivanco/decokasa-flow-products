package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.adminEn;
import static com.decokasa.seguimiento.usuarios.application.Dobles.ana;
import static com.decokasa.seguimiento.usuarios.application.Dobles.desactivarEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.quitarAdminEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.roles;
import static com.decokasa.seguimiento.usuarios.application.Dobles.solicitanteEn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.Dobles.RolRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarRoles;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual;
import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual.UsuarioActual;
import com.decokasa.seguimiento.usuarios.application.usecase.ConsultarRolesCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ConsultarUsuariosCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ObtenerUsuarioActualCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Consultas: usuarios y roles solo para el admin (R6); /auth/yo con los permisos de todos los roles del usuario.
class ConsultasTest {

    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();
    private final RolRepositorioEnMemoria rolesGuardados = new RolRepositorioEnMemoria();

    private ConsultarUsuarios consultarUsuarios;
    private ConsultarRoles consultarRoles;
    private ObtenerUsuarioActual obtenerUsuarioActual;
    private Usuario ana;
    private Solicitante admin;

    @BeforeEach
    void preparar() {
        consultarUsuarios = new ConsultarUsuariosCasoDeUso(usuarios);
        consultarRoles = new ConsultarRolesCasoDeUso(rolesGuardados, usuarios);
        obtenerUsuarioActual = new ObtenerUsuarioActualCasoDeUso(usuarios, rolesGuardados);
        ana = ana(roles("compras_china", "logistica"), true);
        usuarios.guardar(ana);
        admin = adminEn(usuarios);
    }

    @Test
    void elAdminListaYConsultaUsuarios() {
        Usuario elAdmin = usuarios.buscarPorId(admin.usuarioId()).orElseThrow();

        assertThat(consultarUsuarios.listar(admin, null)).containsExactly(ana, elAdmin);
        assertThat(consultarUsuarios.listar(admin, false)).isEmpty();
        assertThat(consultarUsuarios.obtener(admin, ana.id())).isEqualTo(ana);
    }

    // --- Hallazgo A1: la autorización usa el estado actual de la base, no solo los roles del token ---

    @Test
    void unAdminDesactivadoNoConsultaUsuariosNiRolesConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> consultarUsuarios.listar(admin, null)).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarUsuarios.obtener(admin, ana.id())).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarRoles.listar(admin)).isInstanceOf(SinPermiso.class);
    }

    @Test
    void unAdminSinElRolAdminNoConsultaUsuariosNiRolesConSuTokenAnterior() {
        quitarAdminEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> consultarUsuarios.listar(admin, null)).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarUsuarios.obtener(admin, ana.id())).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarRoles.listar(admin)).isInstanceOf(SinPermiso.class);
    }

    @Test
    void unSolicitanteQueNoExisteEnLaBaseNoConsultaAunqueSuTokenDigaAdmin() {
        Solicitante desconocido = new Solicitante(UUID.randomUUID(), roles("admin"));

        assertThatThrownBy(() -> consultarUsuarios.listar(desconocido, null)).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarRoles.listar(desconocido)).isInstanceOf(SinPermiso.class);
    }

    @Test
    void consultarUnUsuarioInexistenteNoSeEncuentra() {
        assertThatThrownBy(() -> consultarUsuarios.obtener(admin, UUID.randomUUID()))
                .isInstanceOf(NoEncontrado.class);
    }

    @Test
    void quienNoEsAdminNoConsultaUsuariosNiRoles() {
        assertThatThrownBy(() -> consultarUsuarios.listar(solicitanteEn(usuarios, "marketing"), null))
                .isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarUsuarios.obtener(solicitanteEn(usuarios, "marketing"), ana.id()))
                .isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> consultarRoles.listar(solicitanteEn(usuarios, "marketing")))
                .isInstanceOf(SinPermiso.class);
    }

    @Test
    void elAdminListaLosRoles() {
        assertThat(consultarRoles.listar(admin)).hasSize(7);
    }

    @Test
    void elUsuarioActualTieneLaUnionDeLosPermisosDeSusRoles() {
        rolesGuardados.guardar(new Rol(UUID.randomUUID(), new CodigoRol("compras_china"), "Compras China",
                Set.of("pedidos.crear_b", "pedidos.ver")));
        rolesGuardados.guardar(new Rol(UUID.randomUUID(), new CodigoRol("logistica"), "Logística",
                Set.of("bodegas.gestionar", "pedidos.ver")));

        UsuarioActual actual = obtenerUsuarioActual.obtener(ana.id());

        assertThat(actual.usuario()).isEqualTo(ana);
        assertThat(actual.permisos()).containsExactlyInAnyOrder("pedidos.crear_b", "pedidos.ver", "bodegas.gestionar");
    }

    @Test
    void unUsuarioDesactivadoOSinRolesNoTieneSesion() {
        usuarios.guardar(new Usuario(ana.id(), ana.nombre(), ana.correo(), ana.usuario(), ana.telefono(),
                ana.contrasenaHash(), ana.roles(), ana.paises(), false, 0, null, 0));

        assertThatThrownBy(() -> obtenerUsuarioActual.obtener(ana.id())).isInstanceOf(SesionInvalida.class);
        assertThatThrownBy(() -> obtenerUsuarioActual.obtener(UUID.randomUUID())).isInstanceOf(SesionInvalida.class);
    }
}
