package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.AHORA;
import static com.decokasa.seguimiento.usuarios.application.Dobles.adminEn;
import static com.decokasa.seguimiento.usuarios.application.Dobles.ana;
import static com.decokasa.seguimiento.usuarios.application.Dobles.desactivarEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.quitarAdminEnLaBase;
import static com.decokasa.seguimiento.usuarios.application.Dobles.roles;
import static com.decokasa.seguimiento.usuarios.application.Dobles.solicitanteEn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.adapter.out.RelojFijo;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.usuarios.application.Dobles.RolRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.TokenRenovacionRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarUsuario;
import com.decokasa.seguimiento.usuarios.application.usecase.ActualizarUsuarioCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Edición de usuarios por el admin: roles (al menos uno), desactivación que revoca sesiones (requerimientos 4.7).
class ActualizarUsuarioTest {

    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();
    private final RolRepositorioEnMemoria rolesGuardados = new RolRepositorioEnMemoria();
    private final TokenRenovacionRepositorioEnMemoria tokens = new TokenRenovacionRepositorioEnMemoria();
    private final RelojFijo reloj = new RelojFijo(AHORA);

    private ActualizarUsuario actualizarUsuario;
    private Usuario ana;
    private Solicitante admin;

    @BeforeEach
    void preparar() {
        actualizarUsuario = new ActualizarUsuarioCasoDeUso(usuarios, rolesGuardados, tokens, reloj);
        ana = ana(roles("compras_china"), true);
        usuarios.guardar(ana);
        admin = adminEn(usuarios);
    }

    // --- Hallazgo A1: un token anterior no sirve para recuperar el acceso ni repartir el rol admin ---

    @Test
    void unAdminDesactivadoNoPuedeReactivarseConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(admin.usuarioId(), null, null, null, null, null, true, 0L)))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorId(admin.usuarioId()).orElseThrow().activo()).isFalse();
    }

    @Test
    void unAdminSinElRolAdminNoPuedeRecuperarloConSuTokenAnterior() {
        quitarAdminEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(admin.usuarioId(), null, null, null, roles("admin"), null, null, 0L)))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorId(admin.usuarioId()).orElseThrow().roles()).isEqualTo(roles("marketing"));
    }

    @Test
    void unAdminDesactivadoOSinElRolAdminNoConvierteAOtroEnAdmin() {
        quitarAdminEnLaBase(usuarios, admin);
        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles("admin"))))
                .isInstanceOf(SinPermiso.class);

        desactivarEnLaBase(usuarios, admin);
        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles("admin"))))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().roles()).isEqualTo(roles("compras_china"));
    }

    // --- D10: versión administrativa; con una versión vieja no cambia nada ---

    @Test
    void conLaVersionVigenteGuardaYDevuelveLaVersionGuardada() {
        Usuario actualizado = actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), "Ana María Prueba", null, null, null, null, null, 0L));

        assertThat(actualizado.version()).isEqualTo(1);
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().version()).isEqualTo(actualizado.version());
        assertThat(actualizado.nombre()).isEqualTo("Ana María Prueba");
    }

    @Test
    void conUnaVersionViejaRespondeVersionDesactualizadaSinCambiarNada() {
        otroAdminGuardaAAna();

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles("logistica"))))
                .isInstanceOf(VersionDesactualizada.class);

        Usuario guardada = usuarios.buscarPorId(ana.id()).orElseThrow();
        assertThat(guardada.roles()).isEqualTo(roles("compras_china"));
        assertThat(guardada.version()).isEqualTo(1);
    }

    @Test
    void desactivarConUnaVersionViejaNoDesactivaNiRevocaSesiones() {
        tokens.guardar(sesion(UUID.randomUUID(), "sha256:a"));
        otroAdminGuardaAAna();

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), null, null, null, null, null, false, 0L)))
                .isInstanceOf(VersionDesactualizada.class);

        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().activo()).isTrue();
        assertThat(tokens.todas()).singleElement().satisfies(s -> assertThat(s.estaRevocada()).isFalse());
    }

    @Test
    void laVersionSeComparaConElEstadoReleidoAlBloquearLaFila() {
        // Otra edición se confirma justo antes de que esta obtenga la fila: la versión leída por el cliente ya no vale.
        usuarios.otraSolicitudConfirmaAntesDeBloquear(this::otroAdminGuardaAAna);

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles("logistica"))))
                .isInstanceOf(VersionDesactualizada.class);

        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().roles()).isEqualTo(roles("compras_china"));
    }

    @Test
    void unaVersionViejaGanaAUnDatoRepetido() {
        usuarios.guardar(new Usuario(UUID.randomUUID(), "Beto Prueba", "beto@ejemplo.test", "betoprueba",
                new Telefono("+593990000002"), "hash-ficticio", roles("logistica"), Set.of(CodigoPais.EC), true, 0,
                null, 0));
        otroAdminGuardaAAna();

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), null, "beto@ejemplo.test", null, null, null, null, 0L)))
                .isInstanceOf(VersionDesactualizada.class);
    }

    // Otro admin ya guardó a Ana: su versión pasa de 0 a 1.
    private void otroAdminGuardaAAna() {
        usuarios.guardar(usuarios.buscarPorId(ana.id()).orElseThrow());
    }

    @Test
    void unAdminDesactivadoNoDesactivaAOtroConSuTokenAnterior() {
        desactivarEnLaBase(usuarios, admin);

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), null, null, null, null, null, false, 0L)))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().activo()).isTrue();
    }

    @Test
    void elAdminReemplazaLosRolesPorVarios() {
        Usuario actualizado = actualizarUsuario.actualizar(admin, soloRoles(roles("logistica", "marketing")));

        assertThat(actualizado.roles())
                .containsExactlyInAnyOrder(new CodigoRol("logistica"), new CodigoRol("marketing"));
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().roles()).isEqualTo(roles("logistica", "marketing"));
    }

    @Test
    void noSePuedeDejarAUnUsuarioSinRoles() {
        // Para retirar el acceso se desactiva la cuenta (decisión del equipo, requerimientos 4.7).
        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles())))
                .isInstanceOf(DatoInvalido.class);
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().roles()).isEqualTo(roles("compras_china"));
    }

    @Test
    void rechazaUnRolQueNoExiste() {
        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin, soloRoles(roles("rol_inventado"))))
                .isInstanceOf(DatoInvalido.class);
    }

    @Test
    void quienNoEsAdminNoPuedeCambiarRoles() {
        assertThatThrownBy(() -> actualizarUsuario.actualizar(solicitanteEn(usuarios, "compras_china"),
                soloRoles(roles("admin"))))
                .isInstanceOf(SinPermiso.class);
        assertThat(usuarios.buscarPorId(ana.id()).orElseThrow().roles()).isEqualTo(roles("compras_china"));
    }

    @Test
    void desactivarRevocaTodasSusSesionesYConservaAlUsuario() {
        UUID familia1 = UUID.randomUUID();
        UUID familia2 = UUID.randomUUID();
        tokens.guardar(sesion(familia1, "sha256:a"));
        tokens.guardar(sesion(familia2, "sha256:b"));

        Usuario actualizado = actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), null, null, null, null, null, false, 0L));

        assertThat(actualizado.activo()).isFalse();
        assertThat(usuarios.buscarPorId(ana.id())).isPresent();
        assertThat(tokens.todas()).hasSize(2).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
    }

    @Test
    void unUsuarioInexistenteNoSeEncuentra() {
        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(UUID.randomUUID(), "Nadie", null, null, null, null, null, 0L)))
                .isInstanceOf(NoEncontrado.class);
    }

    @Test
    void noPuedeTomarElCorreoDeOtroUsuario() {
        usuarios.guardar(new Usuario(UUID.randomUUID(), "Beto Prueba", "beto@ejemplo.test", "betoprueba",
                new Telefono("+593990000002"), "hash-ficticio", roles("logistica"), Set.of(CodigoPais.EC), true, 0,
                null, 0));

        assertThatThrownBy(() -> actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), null, "BETO@ejemplo.test", null, null, null, null, 0L)))
                .isInstanceOf(DatoDuplicado.class)
                .extracting(error -> ((DatoDuplicado) error).campo()).isEqualTo("correo");
    }

    @Test
    void puedeConservarSuPropioCorreo() {
        Usuario actualizado = actualizarUsuario.actualizar(admin,
                new ActualizarUsuario.Comando(ana.id(), "Ana María Prueba", "ana@ejemplo.test", null, null, null,
                        null, 0L));

        assertThat(actualizado.nombre()).isEqualTo("Ana María Prueba");
    }

    private ActualizarUsuario.Comando soloRoles(Set<CodigoRol> nuevosRoles) {
        return new ActualizarUsuario.Comando(ana.id(), null, null, null, nuevosRoles, null, null, 0L);
    }

    private SesionRenovable sesion(UUID familia, String hash) {
        return new SesionRenovable(UUID.randomUUID(), ana.id(), familia, hash, AHORA,
                AHORA.plus(Duration.ofHours(8)), null);
    }
}
