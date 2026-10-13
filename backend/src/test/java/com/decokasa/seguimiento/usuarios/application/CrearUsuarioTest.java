package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.adminEn;
import static com.decokasa.seguimiento.usuarios.application.Dobles.ana;
import static com.decokasa.seguimiento.usuarios.application.Dobles.desactivarEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.quitarAdminEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.roles;
import static com.decokasa.seguimiento.usuarios.application.Dobles.solicitanteEn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.Dobles.CodificadorFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.RolRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import com.decokasa.seguimiento.usuarios.application.usecase.CrearUsuarioCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Alta de usuarios: solo el admin, uno o varios roles, identificadores únicos y contraseña de 8 caracteres a 72 bytes.
class CrearUsuarioTest {

    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();
    private final RolRepositorioEnMemoria rolesGuardados = new RolRepositorioEnMemoria();
    private final CodificadorFalso codificador = new CodificadorFalso();

    private CrearUsuario crearUsuario;
    private Solicitante admin;

    @BeforeEach
    void preparar() {
        crearUsuario = new CrearUsuarioCasoDeUso(usuarios, rolesGuardados, codificador);
        admin = adminEn(usuarios);
    }

    // --- Hallazgo A1: un token anterior no sirve para crear otro administrador ---

    @Test
    void unAdminDesactivadoNoCreaOtroAdministradorConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles("admin"), "Clave-Nueva-1")))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void unAdminSinElRolAdminNoCreaOtroAdministradorConSuTokenAnterior() {
        quitarAdminEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles("admin"), "Clave-Nueva-1")))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void unSolicitanteQueNoExisteEnLaBaseNoCreaUsuariosAunqueSuTokenDigaAdmin() {
        Solicitante desconocido = new Solicitante(UUID.randomUUID(), roles("admin"));

        assertThatThrownBy(() -> crearUsuario.crear(desconocido, comando(roles("admin"), "Clave-Nueva-1")))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void laCargaInicialCreaUsuariosSinUnAdminEnLaBase() {
        // ADR-0015: el cargador de arranque trabaja con la tabla vacía, sin ningún admin que autorice.
        UsuarioRepositorioEnMemoria vacia = new UsuarioRepositorioEnMemoria();
        CrearUsuario cargaInicial = CrearUsuarioCasoDeUso.paraCargaInicial(vacia, rolesGuardados, codificador);

        Usuario creado = cargaInicial.crear(new Solicitante(new UUID(0, 0), roles("admin")),
                comando(roles("admin"), "Clave-Nueva-1"));

        assertThat(vacia.buscarPorId(creado.id())).isPresent();
        assertThat(creado.roles()).containsExactly(new CodigoRol("admin"));
    }

    @Test
    void elAdminCreaUnUsuarioConVariosRoles() {
        Usuario creado = crearUsuario.crear(admin, comando(roles("compras_china", "logistica"), "Clave-Nueva-1"));

        assertThat(creado.roles()).containsExactlyInAnyOrder(new CodigoRol("compras_china"), new CodigoRol("logistica"));
        assertThat(creado.activo()).isTrue();
        assertThat(creado.intentosFallidos()).isZero();
        assertThat(creado.telefono()).isEqualTo(new Telefono("+593990000009"));
        assertThat(usuarios.buscarPorId(creado.id())).contains(creado);
    }

    @Test
    void guardaSoloElHashDeLaContrasena() {
        Usuario creado = crearUsuario.crear(admin, comando(roles("marketing"), "Clave-Nueva-1"));

        assertThat(creado.contrasenaHash()).isEqualTo(CodificadorFalso.PREFIJO + "Clave-Nueva-1");
        assertThat(creado.contrasenaHash()).isNotEqualTo("Clave-Nueva-1");
    }

    @Test
    void unAdminConVariosRolesTambienPuedeCrear() {
        Usuario creado = crearUsuario.crear(solicitanteEn(usuarios, "compras_china", "admin"),
                comando(roles("marketing"), "Clave-Nueva-1"));

        assertThat(usuarios.buscarPorId(creado.id())).isPresent();
    }

    @Test
    void quienNoEsAdminNoPuedeCrearUsuarios() {
        assertThatThrownBy(() -> crearUsuario.crear(solicitanteEn(usuarios, "marketing", "logistica"),
                comando(roles("marketing"), "Clave-Nueva-1")))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void exigeAlMenosUnRol() {
        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles(), "Clave-Nueva-1")))
                .isInstanceOf(DatoInvalido.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void rechazaUnRolQueNoExiste() {
        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles("rol_inventado"), "Clave-Nueva-1")))
                .isInstanceOf(DatoInvalido.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    @Test
    void elCorreoNoSeRepiteSinDistinguirMayusculas() {
        usuarios.guardar(ana(roles("logistica"), true));

        assertThatThrownBy(() -> crearUsuario.crear(admin, new CrearUsuario.Comando("Otra Ana", "ANA@Ejemplo.test",
                "otraana", "+593 99 000 0009", roles("marketing"), Set.of(CodigoPais.EC), "Clave-Nueva-1")))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("correo");
    }

    @Test
    void elNombreDeUsuarioNoSeRepiteSinDistinguirMayusculas() {
        usuarios.guardar(ana(roles("logistica"), true));

        assertThatThrownBy(() -> crearUsuario.crear(admin, new CrearUsuario.Comando("Otra Ana", "otra@ejemplo.test",
                "AnaPrueba", "+593 99 000 0009", roles("marketing"), Set.of(CodigoPais.EC), "Clave-Nueva-1")))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("usuario");
    }

    @Test
    void elTelefonoNoSeRepiteAunqueSeEscribaConEspacios() {
        usuarios.guardar(ana(roles("logistica"), true));

        assertThatThrownBy(() -> crearUsuario.crear(admin, new CrearUsuario.Comando("Otra Ana", "otra@ejemplo.test",
                "otraana", "+593 99 000 0001", roles("marketing"), Set.of(CodigoPais.EC), "Clave-Nueva-1")))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("telefono");
    }

    @Test
    void aceptaUnaContrasenaDeSetentaYDosBytes() {
        Usuario creado = crearUsuario.crear(admin, comando(roles("marketing"), "ñ".repeat(36)));

        assertThat(creado.contrasenaHash()).isEqualTo(CodificadorFalso.PREFIJO + "ñ".repeat(36));
    }

    @Test
    void rechazaUnaContrasenaDeSetentaYTresBytesSinGuardarNiCodificar() {
        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles("marketing"), "ñ".repeat(36) + "a")))
                .isInstanceOf(DatoInvalido.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
        assertThat(codificador.recibidas).isEmpty();
    }

    @Test
    void rechazaUnaContrasenaDeMenosDeOchoCaracteres() {
        assertThatThrownBy(() -> crearUsuario.crear(admin, comando(roles("marketing"), "Corta-1")))
                .isInstanceOf(DatoInvalido.class);
        assertThat(usuarios.buscarPorCorreo("carla@ejemplo.test")).isEmpty();
    }

    private static CrearUsuario.Comando comando(Set<CodigoRol> roles, String contrasena) {
        return new CrearUsuario.Comando("Carla Prueba", "carla@ejemplo.test", "carlaprueba", "+593 99 000 0009", roles,
                Set.of(CodigoPais.EC), contrasena);
    }
}
