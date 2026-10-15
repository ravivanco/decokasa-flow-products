package com.decokasa.seguimiento.usuarios.application;

import static com.decokasa.seguimiento.usuarios.application.Dobles.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.decokasa.seguimiento.compartido.adapter.out.RelojFijo;
import com.decokasa.seguimiento.usuarios.application.Dobles.GeneradorTokenFalso;
import com.decokasa.seguimiento.usuarios.application.Dobles.TokenRenovacionRepositorioEnMemoria;
import com.decokasa.seguimiento.usuarios.application.port.in.CerrarSesion;
import com.decokasa.seguimiento.usuarios.application.usecase.CerrarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Cierre de sesión (ADR-0002, D3b): revoca la familia del token; sin token o con uno desconocido no falla.
class CerrarSesionTest {

    private static final UUID FAMILIA = UUID.randomUUID();

    private final TokenRenovacionRepositorioEnMemoria tokens = new TokenRenovacionRepositorioEnMemoria();
    private final GeneradorTokenFalso generador = new GeneradorTokenFalso();

    private CerrarSesion cerrarSesion;

    @BeforeEach
    void preparar() {
        cerrarSesion = new CerrarSesionCasoDeUso(tokens, generador, new RelojFijo(AHORA));
        tokens.guardar(new SesionRenovable(UUID.randomUUID(), UUID.randomUUID(), FAMILIA, generador.hash("rotado"),
                AHORA, AHORA.plus(Duration.ofHours(8)), AHORA));
        tokens.guardar(new SesionRenovable(UUID.randomUUID(), UUID.randomUUID(), FAMILIA, generador.hash("vigente"),
                AHORA, AHORA.plus(Duration.ofHours(8)), null));
    }

    @Test
    void revocaTodaLaFamiliaDelToken() {
        cerrarSesion.cerrar("vigente");

        assertThat(tokens.deFamilia(FAMILIA)).allSatisfy(s -> assertThat(s.estaRevocada()).isTrue());
    }

    @Test
    void sinTokenOConUnoDesconocidoNoFalla() {
        assertThatCode(() -> cerrarSesion.cerrar(null)).doesNotThrowAnyException();
        assertThatCode(() -> cerrarSesion.cerrar("desconocido")).doesNotThrowAnyException();
        assertThat(tokens.buscarPorHash(generador.hash("vigente")).orElseThrow().estaRevocada()).isFalse();
    }
}
