package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.notificaciones.adapter.out.gmail.GmailNotificador;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class GmailConfig {
    @Bean Notificador notificador(AccesoCuentaGoogle acceso, JsonMapper json,
            com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj,
            @Value("${decokasa.google.gmail.habilitada:false}") boolean habilitada) {
        return new GmailNotificador(habilitada,acceso,json,reloj);
    }
}
