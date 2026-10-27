package com.decokasa.seguimiento.usuarios.config;

import com.decokasa.seguimiento.usuarios.adapter.in.tareas.TareaCorreosRecuperacion;
import com.decokasa.seguimiento.usuarios.application.port.in.EnviarCorreosRecuperacion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Trabajador de los enlaces de recuperación (ADR-0033). Apagado por defecto: hacen falta OAuth, Gmail y
 * {@code decokasa.recuperacion.envio-automatico}; es independiente del trabajador de avisos de etapa.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = {"decokasa.google.oauth.habilitada", "decokasa.google.gmail.habilitada",
        "decokasa.recuperacion.envio-automatico"}, havingValue = "true")
public class RecuperacionAutomaticaConfig {

    @Bean
    TareaCorreosRecuperacion tareaCorreosRecuperacion(EnviarCorreosRecuperacion enviar) {
        return new TareaCorreosRecuperacion(enviar);
    }
}
