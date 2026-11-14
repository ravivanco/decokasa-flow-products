package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Trabajador de la cola de correo con el buzón simulado. Solo con NOTIFICACIONES_BUZON_SIMULADO; nunca llega a Gmail. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnExpression("!'${decokasa.notificaciones.buzon-simulado:}'.isBlank()")
public class CorreoSimuladoConfig {

    @Bean
    TareaBuzon tareaBuzon(EnviarNotificacionesPendientes enviar) {
        return new TareaBuzon(enviar);
    }

    public static final class TareaBuzon {
        private final EnviarNotificacionesPendientes enviar;

        TareaBuzon(EnviarNotificacionesPendientes enviar) {
            this.enviar = enviar;
        }

        @Scheduled(fixedDelayString = "${decokasa.notificaciones.buzon-simulado-intervalo-ms:5000}")
        public void ejecutar() {
            try {
                enviar.procesar();
            } catch (RuntimeException error) {
                LoggerFactory.getLogger(TareaBuzon.class).warn("buzon_simulado pasada interrumpida");
            }
        }
    }
}
