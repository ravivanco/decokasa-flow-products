package com.decokasa.seguimiento.usuarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

// Reglas del usuario (requerimientos 4.7 y ADR-0002). Datos ficticios.
class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-10-12T14:00:00Z");
    private static final CodigoRol COMPRAS_CHINA = new CodigoRol("compras_china");
    private static final CodigoRol LOGISTICA = new CodigoRol("logistica");

    // --- Acceso ---

    @Test
    void unUsuarioActivoConUnRolPuedeIniciarSesion() {
        assertThat(usuario(Set.of(COMPRAS_CHINA), true).puedeIniciarSesion()).isTrue();
    }

    @Test
    void unUsuarioConVariosRolesPuedeIniciarSesionYLosTieneTodos() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA, LOGISTICA), true);

        assertThat(usuario.puedeIniciarSesion()).isTrue();
        assertThat(usuario.tieneRol(COMPRAS_CHINA)).isTrue();
        assertThat(usuario.tieneRol(LOGISTICA)).isTrue();
        assertThat(usuario.tieneRol(new CodigoRol("admin"))).isFalse();
    }

    @Test
    void unUsuarioSinRolesNoPuedeIniciarSesion() {
        assertThat(usuario(Set.of(), true).puedeIniciarSesion()).isFalse();
    }

    @Test
    void unUsuarioDesactivadoNoPuedeIniciarSesion() {
        assertThat(usuario(Set.of(COMPRAS_CHINA), false).puedeIniciarSesion()).isFalse();
    }

    @Test
    void desactivarLeQuitaElAccesoSinBorrarSusRoles() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);

        usuario.desactivar();

        assertThat(usuario.activo()).isFalse();
        assertThat(usuario.puedeIniciarSesion()).isFalse();
        assertThat(usuario.roles()).containsExactly(COMPRAS_CHINA);
    }

    // --- Roles ---

    @Test
    void asignarRolesReemplazaLaListaCompleta() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);

        usuario.asignarRoles(Set.of(LOGISTICA, new CodigoRol("marketing")));

        assertThat(usuario.roles()).containsExactlyInAnyOrder(LOGISTICA, new CodigoRol("marketing"));
    }

    @Test
    void noSePuedeDejarAUnUsuarioSinRoles() {
        // Para retirar el acceso se desactiva la cuenta (decisión del equipo, requerimientos 4.7).
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);

        assertThatThrownBy(() -> usuario.asignarRoles(Set.of())).isInstanceOf(DatoInvalido.class);
        assertThat(usuario.roles()).containsExactly(COMPRAS_CHINA);
    }

    @Test
    void unUsuarioNuevoNecesitaAlMenosUnRol() {
        assertThatThrownBy(() -> Usuario.nuevo("Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), "hash-ficticio", Set.of(), Set.of(CodigoPais.EC)))
                .isInstanceOf(DatoInvalido.class);
    }

    @Test
    void unUsuarioNuevoQuedaActivoSinIntentosYConSusRoles() {
        Usuario usuario = Usuario.nuevo("Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), "hash-ficticio", Set.of(COMPRAS_CHINA, LOGISTICA),
                Set.of(CodigoPais.EC));

        assertThat(usuario.id()).isNotNull();
        assertThat(usuario.activo()).isTrue();
        assertThat(usuario.intentosFallidos()).isZero();
        assertThat(usuario.bloqueadoHasta()).isNull();
        assertThat(usuario.roles()).containsExactlyInAnyOrder(COMPRAS_CHINA, LOGISTICA);
    }

    // --- Bloqueo (D2: 5 intentos, 15 minutos) ---

    @Test
    void cuatroFallosNoBloquean() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);

        fallar(usuario, 4);

        assertThat(usuario.intentosFallidos()).isEqualTo(4);
        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
    }

    @Test
    void elQuintoFalloBloqueaQuinceMinutos() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);

        fallar(usuario, 5);

        assertThat(usuario.bloqueadoHasta()).isEqualTo(AHORA.plus(Duration.ofMinutes(15)));
        assertThat(usuario.estaBloqueado(AHORA)).isTrue();
        assertThat(usuario.estaBloqueado(AHORA.plus(Duration.ofMinutes(14)))).isTrue();
    }

    @Test
    void elBloqueoTerminaALosQuinceMinutos() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);
        fallar(usuario, 5);

        assertThat(usuario.estaBloqueado(AHORA.plus(Duration.ofMinutes(15)))).isFalse();
    }

    @Test
    void unAciertoReiniciaElContador() {
        Usuario usuario = usuario(Set.of(COMPRAS_CHINA), true);
        fallar(usuario, 4);

        usuario.registrarAcierto();
        fallar(usuario, 4);

        assertThat(usuario.intentosFallidos()).isEqualTo(4);
        assertThat(usuario.estaBloqueado(AHORA)).isFalse();
    }

    private static void fallar(Usuario usuario, int veces) {
        for (int i = 0; i < veces; i++) {
            usuario.registrarFallo(AHORA, PoliticaBloqueo.ACORDADA);
        }
    }

    private static Usuario usuario(Set<CodigoRol> roles, boolean activo) {
        return new Usuario(UUID.randomUUID(), "Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), "hash-ficticio", roles, Set.of(CodigoPais.EC), activo, 0, null, 0);
    }
}
