package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.application.usecase.EncolarNotificacionCasoDeUso;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NotificacionesConfig {

    /**
     * Único destinatario que Richard autorizó para la prueba real de Gmail (2026-10-24). No es un destinatario
     * definitivo de producción (F3-D8 sigue pendiente).
     */
    static final String DESTINATARIO_PRUEBA_AUTORIZADO = "importadora.latinoamericaworld@gmail.com";

    /**
     * No deducir que todos los admins ni la cuenta remitente son destinatarios. F3-D8 pendiente.
     *
     * <p>Solo para la prueba real, en la configuración local aislada: {@code NOTIFICACIONES_DESTINATARIO_PRUEBA}. Vacía
     * por defecto, el directorio sigue vacío y no se envía nada. El único valor admitido es el buzón autorizado; con
     * cualquier otro la aplicación no arranca. Encolar y enviar siguen revalidando el destinatario contra este
     * directorio.
     */
    @Bean DirectorioDestinatarios directorioDestinatarios(
            @Value("${decokasa.notificaciones.destinatario-prueba:}") String destinatarioPrueba) {
        return desde(destinatarioPrueba);
    }

    static DirectorioDestinatarios desde(String destinatarioPrueba) {
        String valor = destinatarioPrueba == null ? "" : destinatarioPrueba.strip();
        if (valor.isEmpty()) {
            return pais -> Set.of();
        }
        if (!DESTINATARIO_PRUEBA_AUTORIZADO.equals(valor.toLowerCase(Locale.ROOT))) {
            // Sin el valor en el mensaje: no se registran direcciones en los logs.
            throw new IllegalStateException(
                    "decokasa.notificaciones.destinatario-prueba solo admite el buzón autorizado.");
        }
        Set<String> unico = Set.of(DESTINATARIO_PRUEBA_AUTORIZADO);
        return pais -> unico;
    }

    @Bean EncolarNotificacion encolarNotificacion(NotificacionRepositorio repositorio,
            DirectorioDestinatarios directorio) {
        return new EncolarNotificacionCasoDeUso(repositorio, directorio);
    }
}
