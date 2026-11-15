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
            @Value("${decokasa.notificaciones.solo-a:}") String soloA,
            @Value("${RENDER:}") String render) {
        if (buzonSimulado != null && !buzonSimulado.isBlank()) {
            // Solo pruebas locales: en Render no se simula un envío que parecería real.
            if (render != null && !render.isBlank()) {
                throw new IllegalStateException("NOTIFICACIONES_BUZON_SIMULADO no se usa en Render.");
            }
            return new BuzonSimuladoNotificador(java.nio.file.Path.of(buzonSimulado.strip()));
        }
        Notificador gmail = new GmailNotificador(habilitada,acceso,json,reloj);
        java.util.Set<String> permitidos = java.util.Arrays.stream((soloA == null ? "" : soloA).split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (permitidos.isEmpty()) { return gmail; }
        // Solo pruebas locales con Gmail real: se envía únicamente a las direcciones permitidas; el resto se da por
        // tratado sin salir del servidor, para no escribir a personas reales de una base de prueba. Nunca en Render.
        if (render != null && !render.isBlank()) {
            throw new IllegalStateException("NOTIFICACIONES_SOLO_A no se usa en Render.");
        }
        org.slf4j.LoggerFactory.getLogger("decokasa.notificaciones").warn(
                "correo en modo prueba: solo se envia a {} direccion(es) permitida(s)", permitidos.size());
        return correo -> permitidos.contains(correo.destinatario().toLowerCase(java.util.Locale.ROOT))
                ? gmail.enviar(correo) : new Notificador.Entrega("omitido-prueba-" + correo.notificacionId());
    }
}
