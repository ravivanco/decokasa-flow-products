package com.decokasa.seguimiento.notificaciones.adapter.in.usuarios;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.decokasa.seguimiento.usuarios.application.port.out.CorreoRecuperacion;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entrega el enlace de recuperación con el mismo adaptador de Gmail que los avisos (ADR-0033), pero sin pasar por el
 * outbox de pedidos ni por sus cortes. No incluye contraseñas; el enlace solo vive en el cuerpo enviado.
 */
public final class CorreoRecuperacionGmail implements CorreoRecuperacion {

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy, HH:mm", Locale.forLanguageTag("es-EC"));

    private static final Logger LOG = LoggerFactory.getLogger("decokasa.recuperacion");

    private final Notificador gmail;

    public CorreoRecuperacionGmail(Notificador gmail) {
        this.gmail = gmail;
    }

    @Override
    public void enviar(UUID solicitudId, String destinatario, String nombre, String enlace, Instant expiraEn) {
        String texto = "Hola" + (nombre == null || nombre.isBlank() ? "" : " " + nombre.strip()) + ":\n\n"
                + "Recibimos una solicitud para restablecer la contraseña de tu cuenta de DECOKASA. Abre este enlace "
                + "para elegir una nueva:\n\n" + enlace + "\n\n"
                + "El enlace sirve una sola vez y caduca el " + FECHA.format(expiraEn.atZone(Reloj.ZONA_ECUADOR))
                + " (hora de Ecuador). Si no lo pediste, ignora este correo: tu contraseña no cambia.\n\n"
                + "DECOKASA nunca te pedirá tu contraseña por correo.";
        try {
            gmail.enviar(new Notificador.Correo(solicitudId, destinatario, "DECOKASA: restablece tu contraseña", texto,
                    "<notificacion-" + solicitudId + "@decokasa.invalid>"));
        } catch (FalloIntegracionGoogle error) {
            // Solo el id de la solicitud y la causa técnica: ni destinatario, ni enlace, ni token.
            LOG.warn("recuperacion.correo fallido solicitud={} causa={}", solicitudId, error.causa());
            throw error;
        } catch (RuntimeException error) {
            LOG.warn("recuperacion.correo fallido solicitud={} causa={}", solicitudId, error.getClass().getSimpleName());
            throw error;
        }
        LOG.info("recuperacion.correo enviado solicitud={}", solicitudId);
    }

    @Override
    public String toString() {
        return "CorreoRecuperacionGmail";
    }
}
