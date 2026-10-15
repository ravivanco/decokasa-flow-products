package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.adminEn;
import static com.decokasa.seguimiento.usuarios.application.Dobles.desactivarEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.quitarAdminEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.solicitanteEn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.Dobles.RolRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarRol;
import com.decokasa.seguimiento.usuarios.application.usecase.ActualizarRolCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// PUT /roles/{id} del contrato: el admin reemplaza código, nombre y permisos; el código sigue siendo único.
class ActualizarRolTest {

    private final RolRepositorioEnMemoria roles = new RolRepositorioEnMemoria();
    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();

    private ActualizarRol actualizarRol;
    private Rol logistica;
    private Solicitante admin;

    @BeforeEach
    void preparar() {
        actualizarRol = new ActualizarRolCasoDeUso(roles, usuarios);
        logistica = roles.buscarPorCodigo(new CodigoRol("logistica")).orElseThrow();
        admin = adminEn(usuarios);
    }

    // --- Hallazgo A1: la autorización usa el estado actual de la base, no solo los roles del token ---

    @Test
    void unAdminDesactivadoNoCambiaRolesConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> actualizarRol.actualizar(admin, renombrarLogistica()))
                .isInstanceOf(SinPermiso.class);
        assertThat(roles.buscarPorCodigo(new CodigoRol("logistica")).orElseThrow().nombre()).isEqualTo("logistica");
    }

    @Test
    void unAdminSinElRolAdminNoCambiaRolesConSuTokenAnterior() {
        quitarAdminEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> actualizarRol.actualizar(admin, renombrarLogistica()))
                .isInstanceOf(SinPermiso.class);
        assertThat(roles.buscarPorCodigo(new CodigoRol("logistica")).orElseThrow().nombre()).isEqualTo("logistica");
    }

    private ActualizarRol.Comando renombrarLogistica() {
        return new ActualizarRol.Comando(logistica.id(), new CodigoRol("logistica"), "Otra", Set.of());
    }

    @Test
    void elAdminReemplazaNombreYPermisos() {
        Rol actualizado = actualizarRol.actualizar(admin, new ActualizarRol.Comando(logistica.id(),
                new CodigoRol("logistica"), "Logística Ecuador", Set.of("bodegas.gestionar")));

        assertThat(actualizado.id()).isEqualTo(logistica.id());
        assertThat(actualizado.nombre()).isEqualTo("Logística Ecuador");
        assertThat(roles.buscarPorCodigo(new CodigoRol("logistica")).orElseThrow().permisos())
                .containsExactly("bodegas.gestionar");
    }

    @Test
    void noPuedeTomarElCodigoDeOtroRol() {
        assertThatThrownBy(() -> actualizarRol.actualizar(admin, new ActualizarRol.Comando(logistica.id(),
                new CodigoRol("marketing"), "Logística", Set.of())))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("codigo");
    }

    @Test
    void validaElNombreYQueElRolExista() {
        assertThatThrownBy(() -> actualizarRol.actualizar(admin, new ActualizarRol.Comando(logistica.id(),
                new CodigoRol("logistica"), " ", Set.of())))
                .isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> actualizarRol.actualizar(admin, new ActualizarRol.Comando(UUID.randomUUID(),
                new CodigoRol("logistica"), "Logística", Set.of())))
                .isInstanceOf(NoEncontrado.class);
    }

    @Test
    void quienNoEsAdminNoCambiaRoles() {
        assertThatThrownBy(() -> actualizarRol.actualizar(solicitanteEn(usuarios, "logistica"),
                new ActualizarRol.Comando(logistica.id(), new CodigoRol("logistica"), "Otra", Set.of())))
                .isInstanceOf(SinPermiso.class);
    }
}
