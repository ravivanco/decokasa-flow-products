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
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.Dobles.RolRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearRol;
import com.decokasa.seguimiento.usuarios.application.usecase.CrearRolCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Alta de roles: solo el admin (requisitos 1.3 y 4.6). La lista de permisos sigue pendiente.
class CrearRolTest {

    private final RolRepositorioEnMemoria roles = new RolRepositorioEnMemoria();
    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();

    private CrearRol crearRol;
    private Solicitante admin;

    @BeforeEach
    void preparar() {
        crearRol = new CrearRolCasoDeUso(roles, usuarios);
        admin = adminEn(usuarios);
    }

    // --- Hallazgo A1: la autorización usa el estado actual de la base, no solo los roles del token ---

    @Test
    void unAdminDesactivadoNoCreaRolesConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> crearRol.crear(admin, comandoBodegaQuito())).isInstanceOf(SinPermiso.class);
        assertThat(roles.cantidad()).isEqualTo(7);
    }

    @Test
    void unAdminSinElRolAdminNoCreaRolesConSuTokenAnterior() {
        quitarAdminEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> crearRol.crear(admin, comandoBodegaQuito())).isInstanceOf(SinPermiso.class);
        assertThat(roles.cantidad()).isEqualTo(7);
    }

    private static CrearRol.Comando comandoBodegaQuito() {
        return new CrearRol.Comando(new CodigoRol("bodega_quito"), "Bodega Quito", Set.of());
    }

    @Test
    void elAdminCreaUnRolNuevo() {
        Rol creado = crearRol.crear(admin, new CrearRol.Comando(new CodigoRol("bodega_quito"), "Bodega Quito",
                Set.of()));

        assertThat(creado.id()).isNotNull();
        assertThat(roles.buscarPorCodigo(new CodigoRol("bodega_quito"))).contains(creado);
        assertThat(roles.cantidad()).isEqualTo(8);
    }

    @Test
    void quienNoEsAdminNoPuedeCrearRoles() {
        assertThatThrownBy(() -> crearRol.crear(solicitanteEn(usuarios, "logistica"),
                new CrearRol.Comando(new CodigoRol("bodega_quito"), "Bodega Quito", Set.of())))
                .isInstanceOf(SinPermiso.class);
        assertThat(roles.cantidad()).isEqualTo(7);
    }

    @Test
    void elCodigoNoSeRepite() {
        assertThatThrownBy(() -> crearRol.crear(admin,
                new CrearRol.Comando(new CodigoRol("logistica"), "Otra logística", Set.of())))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("codigo");
    }

    @Test
    void elNombreTieneDeUnoACienCaracteres() {
        assertThatThrownBy(() -> crearRol.crear(admin,
                new CrearRol.Comando(new CodigoRol("bodega_quito"), "r".repeat(101), Set.of())))
                .isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> crearRol.crear(admin,
                new CrearRol.Comando(new CodigoRol("bodega_quito"), " ", Set.of())))
                .isInstanceOf(DatoInvalido.class);
        assertThat(roles.cantidad()).isEqualTo(7);
    }
}
