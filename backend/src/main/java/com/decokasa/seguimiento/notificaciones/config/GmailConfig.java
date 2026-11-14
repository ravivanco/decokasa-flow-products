package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.notificaciones.adapter.out.buzon.BuzonSimuladoNotificador;
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
            @Value("${decokasa.google.gmail.habilitada:false}") boolean habilitada,
            @Value("${decokasa.notificaciones.buzon-simulado:}") String buzonSimulado,
            @Value("${RENDER:}") String render) {
        if (buzonSimulado != null && !buzonSimulado.isBlank()) {
            // Solo pruebas locales: en Render no se simula un envío que parecería real.
            if (render != null && !render.isBlank()) {
                throw new IllegalStateException("NOTIFICACIONES_BUZON_SIMULADO no se usa en Render.");
            }
            return new BuzonSimuladoNotificador(java.nio.file.Path.of(buzonSimulado.strip()));
        }
        return new GmailNotificador(habilitada,acceso,json,reloj);
    }
}
