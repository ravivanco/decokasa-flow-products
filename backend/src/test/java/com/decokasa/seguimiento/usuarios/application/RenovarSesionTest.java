package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.AHORA;
import static com.decokasa.seguimiento.usuarios.application.Dobles.roles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.adapter.out.RelojFijo;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.Dobles.EmisorTokensFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.GeneradorTokenFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.TokenRenovacionRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.Dobles.UsuarioRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.RenovarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.SesionIniciada;
import com.decokasa.seguimiento.usuarios.application.usecase.RenovarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.PoliticaSesion;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Renovación con rotación y límite absoluto de 8 horas (ADR-0002, D3); sin roles o desactivado no renueva (D4).
class RenovarSesionTest {

    private static final UUID ID_ANA = UUID.randomUUID();
    private static final UUID FAMILIA = UUID.randomUUID();
    private static final String TOKEN_INICIAL = "renovacion-inicial";

    private final UsuarioRepositorioEnMemoria usuarios = new UsuarioRepositorioEnMemoria();
    private final TokenRenovacionRepositorioEnMemoria tokens = new TokenRenovacionRepositorioEnMemoria();
    private final EmisorTokensFalso emisor = new EmisorTokensFalso();
    private final GeneradorTokenFalso generador = new GeneradorTokenFalso();
    private final RelojFijo reloj = new RelojFijo(AHORA);

    private RenovarSesion renovarSesion;

    @BeforeEach
    void preparar() {
        renovarSesion = new RenovarSesionCasoDeUso(usuarios, tokens, emisor, generador, reloj, PoliticaSesion.ACORDADA);
        guardarAna(roles("compras_china"), true);
        // Sesión iniciada a las 14:00 (AHORA): renovable hasta las 22:00.
        tokens.guardar(new SesionRenovable(UUID.randomUUID(), ID_ANA, FAMILIA, generador.hash(TOKEN_INICIAL), AHORA,
                AHORA.plus(Duration.ofHours(8)), null));
    }

    @Test
    void unTokenValidoEntregaUnAccesoNuevoYRotaElTokenDeRenovacion() {
        reloj.adelantar(Duration.ofMinutes(20));

        SesionIniciada sesion = renovarSesion.renovar(TOKEN_INICIAL);

        assertThat(sesion.tokenAcceso()).isEqualTo("acceso-1");
        assertThat(sesion.expiraEnSegundos()).isEqualTo(900);
        assertThat(sesion.tokenRenovacion()).isNotEqualTo(TOKEN_INICIAL);
        assertThat(guardada(TOKEN_INICIAL).estaRevocada()).isTrue();
        assertThat(guardada(sesion.tokenRenovacion()).estaRevocada()).isFalse();
        assertThat(guardada(sesion.tokenRenovacion()).familiaId()).isEqualTo(FAMILIA);
    }

    @Test
    void rotarNoExtiendeElLimiteDeOchoHoras() {
        reloj.adelantar(Duration.ofHours(7));

        SesionIniciada sesion = renovarSesion.renovar(TOKEN_INICIAL);

        assertThat(sesion.renovableHasta()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
        assertThat(guardada(sesion.tokenRenovacion()).expiraEn()).isEqualTo(AHORA.plus(Duration.ofHours(8)));
    }

    @Test
    void pasadasOchoHorasDesdeElInicioDeSesionYaNoRenuevaAunqueSeHayaRotado() {
        reloj.adelantar(Duration.ofHours(7));
        String rotado = renovarSesion.renovar(TOKEN_INICIAL).tokenRenovacion();

        reloj.adelantar(Duration.ofHours(1));

        assertThatThrownBy(() -> renovarSesion.renovar(rotado)).isInstanceOf(SesionInvalida.class);
    }

    @Test
    void reutilizarUnTokenRevocadoRevocaTodaLaFamilia() {
        String rotado = renovarSesion.renovar(TOKEN_INICIAL).tokenRenovacion();

        assertThatThrownBy(() -> renovarSesion.renovar(TOKEN_INICIAL)).isInstanceOf(SesionInvalida.class);

        assertThat(tokens.deFamilia(FAMILIA)).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
        assertThatThrownBy(() -> renovarSesion.renovar(rotado)).isInstanceOf(SesionInvalida.class);
    }

    @Test
    void siOtraRenovacionConsumeElTokenAntesEsReutilizacionYNoEmiteSucesor() {
        // Hallazgo A2: las dos solicitudes leen el token vigente; solo una puede consumirlo. Sin margen de reutilización.
        tokens.otraRenovacionSeAdelantaraAlBuscar();

        assertThatThrownBy(() -> renovarSesion.renovar(TOKEN_INICIAL)).isInstanceOf(SesionInvalida.class);

        assertThat(tokens.deFamilia(FAMILIA)).hasSize(1).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void siLaCuentaSeDesactivaAntesDeBloquearSuFilaNoRenuevaYRevocaLaFamilia() {
        // N1: la renovación toma la fila del usuario (el mismo orden que la desactivación: usuario y después tokens)
        // y decide con lo confirmado; una desactivación que ganó la fila ya no deja renovar.
        usuarios.otraSolicitudConfirmaAntesDeBloquear(() -> guardarAna(roles("compras_china"), false));

        assertThatThrownBy(() -> renovarSesion.renovar(TOKEN_INICIAL)).isInstanceOf(SesionInvalida.class);

        assertThat(tokens.deFamilia(FAMILIA)).hasSize(1).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unTokenDesconocidoNoRenueva() {
        assertThatThrownBy(() -> renovarSesion.renovar("token-inventado")).isInstanceOf(SesionInvalida.class);
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unUsuarioDesactivadoNoRenuevaYSeRevocaLaFamilia() {
        guardarAna(roles("compras_china"), false);

        assertThatThrownBy(() -> renovarSesion.renovar(TOKEN_INICIAL)).isInstanceOf(SesionInvalida.class);

        assertThat(tokens.deFamilia(FAMILIA)).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void unUsuarioSinRolesNoRenuevaYSeRevocaLaFamilia() {
        guardarAna(roles(), true);

        assertThatThrownBy(() -> renovarSesion.renovar(TOKEN_INICIAL)).isInstanceOf(SesionInvalida.class);

        assertThat(tokens.deFamilia(FAMILIA)).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
        assertThat(emisor.emisiones).isEmpty();
    }

    @Test
    void elAccesoRenovadoLlevaLosRolesActualesDelUsuario() {
        // El admin le cambió los roles después del inicio de sesión.
        guardarAna(roles("logistica", "marketing"), true);

        renovarSesion.renovar(TOKEN_INICIAL);

        assertThat(emisor.ultima().roles()).isEqualTo(roles("logistica", "marketing"));
        assertThat(emisor.ultima().duracion()).isEqualTo(Duration.ofMinutes(15));
    }

    private SesionRenovable guardada(String token) {
        return tokens.buscarPorHash(generador.hash(token)).orElseThrow();
    }

    private void guardarAna(Set<CodigoRol> roles, boolean activo) {
        usuarios.guardar(new Usuario(ID_ANA, "Ana Prueba", "ana@ejemplo.test", "anaprueba",
                new Telefono("+593990000001"), "hash-ficticio", roles, Set.of(CodigoPais.EC), activo, 0, null, 0));
    }
}
