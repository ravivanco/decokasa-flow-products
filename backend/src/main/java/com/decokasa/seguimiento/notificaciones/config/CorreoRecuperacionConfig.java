package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.notificaciones.adapter.in.usuarios.CorreoRecuperacionGmail;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.decokasa.seguimiento.usuarios.application.port.out.CorreoRecuperacion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Gmail para los enlaces de recuperación de {@code usuarios} (ADR-0033). Con Gmail apagado, el envío falla y no sale. */
@Configuration(proxyBeanMethods = false)
public class CorreoRecuperacionConfig {

    @Bean
    CorreoRecuperacion correoRecuperacion(Notificador gmail) {
        return new CorreoRecuperacionGmail(gmail);
    }
}
