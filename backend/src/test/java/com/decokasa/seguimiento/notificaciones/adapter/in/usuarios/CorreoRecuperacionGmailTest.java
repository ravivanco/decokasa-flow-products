package com.decokasa.seguimiento.notificaciones.adapter.in.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** ADR-0033: el enlace va al correo registrado, por el mismo Gmail central, y los fallos se propagan sin reintento. */
class CorreoRecuperacionGmailTest {

    private static final String ENLACE = "https://decokasa.example.test/restablecer-contrasena#token=ficticio";

    @Test
    void enviaAlCorreoRegistradoConElEnlaceYLaCaducidad() {
        List<Notificador.Correo> enviados = new ArrayList<>();
        var correo = new CorreoRecuperacionGmail(c -> { enviados.add(c); return new Notificador.Entrega("id-1"); });
        UUID solicitud = UUID.randomUUID();

        correo.enviar(solicitud, "Persona.Ficticia@Ejemplo.test", "Persona Ficticia", ENLACE,
                Instant.parse("2026-11-03T15:30:00Z"));

        assertThat(enviados).singleElement().satisfies(c -> {
            assertThat(c.destinatario()).isEqualTo("persona.ficticia@ejemplo.test");
            assertThat(c.asunto()).isEqualTo("DECOKASA: restablece tu contraseña");
            assertThat(c.texto()).contains("Hola Persona Ficticia", ENLACE, "sirve una sola vez",
                    "3 de noviembre de 2026, 10:30", "hora de Ecuador");
            assertThat(c.messageId()).isEqualTo("<notificacion-" + solicitud + "@decokasa.invalid>");
        });
    }

    @Test
    void propagaElFalloDeGoogleSinReintentar() {
        int[] llamadas = {0};
        var correo = new CorreoRecuperacionGmail(c -> {
            llamadas[0]++;
            throw new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.AUTORIZACION_REQUERIDA);
        });

        assertThatThrownBy(() -> correo.enviar(UUID.randomUUID(), "persona@ejemplo.test", null, ENLACE, Instant.now()))
                .isInstanceOf(FalloIntegracionGoogle.class);
        assertThat(llamadas[0]).isEqualTo(1);
    }
}
