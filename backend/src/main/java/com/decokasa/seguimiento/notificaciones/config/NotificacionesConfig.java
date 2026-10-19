package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.application.usecase.EncolarNotificacionCasoDeUso;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NotificacionesConfig {
    /** No deducir que todos los admins ni la cuenta remitente son destinatarios. F3-D8 pendiente. */
    @Bean DirectorioDestinatarios directorioDestinatarios() { return pais -> Set.of(); }
    @Bean EncolarNotificacion encolarNotificacion(NotificacionRepositorio repositorio,
            DirectorioDestinatarios directorio) {
        return new EncolarNotificacionCasoDeUso(repositorio, directorio);
    }
}
