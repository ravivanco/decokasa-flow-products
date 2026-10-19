package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.LoggerFactory;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name={"decokasa.google.oauth.habilitada","decokasa.google.gmail.habilitada",
    "decokasa.google.gmail.worker.automatico"},havingValue="true")
public class CorreoAutomaticoConfig {
    @Bean TareaCorreo tareaCorreo(EnviarNotificacionesPendientes enviar) { return new TareaCorreo(enviar); }
    public static final class TareaCorreo {
        private final EnviarNotificacionesPendientes enviar;
        TareaCorreo(EnviarNotificacionesPendientes enviar) { this.enviar=enviar; }
        @Scheduled(fixedDelayString="${decokasa.google.gmail.worker.intervalo-ms:30000}")
        public void ejecutar() {
            try { enviar.procesar(); }
            catch (RuntimeException error) {
                // Sin excepción/causa, destinatario, cuerpo o token en logs.
                LoggerFactory.getLogger(TareaCorreo.class).warn("correo_pasada_interrumpida; revisar registro persistente");
            }
        }
    }
}
