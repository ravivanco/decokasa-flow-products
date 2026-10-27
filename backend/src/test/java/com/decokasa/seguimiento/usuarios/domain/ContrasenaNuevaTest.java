package com.decokasa.seguimiento.usuarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

// Límite de BCrypt: como máximo 72 bytes en UTF-8, no 72 caracteres; nunca se recorta (modelo-datos.md 3.1).
class ContrasenaNuevaTest {

    private static final String ENE = "ñ"; // 2 bytes en UTF-8
    private static final String EMOJI = "😀"; // 4 bytes en UTF-8

    @Test
    void aceptaOchoCaracteres() {
        assertThat(new ContrasenaNueva("Clave-12").valor()).isEqualTo("Clave-12");
    }

    @Test
    void rechazaMenosDeOchoCaracteres() {
        assertThatThrownBy(() -> new ContrasenaNueva("Clave-1")).isInstanceOf(DatoInvalido.class);
    }

    @Test
    void rechazaUnaContrasenaNula() {
        assertThatThrownBy(() -> new ContrasenaNueva(null)).isInstanceOf(DatoInvalido.class);
    }

    @Test
    void aceptaSetentaYDosBytesAscii() {
        String clave = "a".repeat(72);

        assertThat(new ContrasenaNueva(clave).valor()).isEqualTo(clave);
    }

    @Test
    void rechazaSetentaYTresBytesAscii() {
        assertThatThrownBy(() -> new ContrasenaNueva("a".repeat(73))).isInstanceOf(DatoInvalido.class);
    }

    @Test
    void aceptaTreintaYSeisEnesQueSonSetentaYDosBytes() {
        String clave = ENE.repeat(36);
        assertThat(clave.getBytes(StandardCharsets.UTF_8)).hasSize(72);

        assertThat(new ContrasenaNueva(clave).valor()).isEqualTo(clave);
    }

    @Test
    void rechazaTreintaYSieteCaracteresQueSonSetentaYTresBytes() {
        // Cumple maxLength: 72 en caracteres, pero no el límite real en bytes.
        String clave = ENE.repeat(36) + "a";
        assertThat(clave).hasSize(37);
        assertThat(clave.getBytes(StandardCharsets.UTF_8)).hasSize(73);

        assertThatThrownBy(() -> new ContrasenaNueva(clave))
                .isInstanceOf(DatoInvalido.class)
                .hasMessageContaining("72 bytes");
    }

    @Test
    void aceptaDieciochoEmojiQueSonSetentaYDosBytes() {
        String clave = EMOJI.repeat(18);

        assertThat(new ContrasenaNueva(clave).valor()).isEqualTo(clave);
    }

    @Test
    void rechazaDiecinueveEmojiQueSonSetentaYSeisBytes() {
        assertThatThrownBy(() -> new ContrasenaNueva(EMOJI.repeat(19))).isInstanceOf(DatoInvalido.class);
    }

    @Test
    void noRecortaLaContrasena() {
        String clave = ENE.repeat(36);

        assertThat(new ContrasenaNueva(clave).valor().getBytes(StandardCharsets.UTF_8)).hasSize(72);
    }
}
