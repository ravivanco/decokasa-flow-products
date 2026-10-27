package com.decokasa.seguimiento.plazos.config;

import com.decokasa.seguimiento.plazos.adapter.in.tareas.TareaVencimientos;
import com.decokasa.seguimiento.plazos.application.port.in.DetectarVencimientos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Registra la tarea de vencimientos solo con {@code decokasa.plazos.vencimientos.automatico=true} (paso 5.2). Queda
 * desactivada por defecto hasta que Codex la verifique; sin ella, nada se detecta ni se encola.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "decokasa.plazos.vencimientos.automatico", havingValue = "true")
public class VencimientosAutomaticosConfig {

    @Bean
    TareaVencimientos tareaVencimientos(DetectarVencimientos detectar,
            @Value("${decokasa.plazos.vencimientos.intervalo-ms:300000}") long intervalo) {
        if (intervalo < 1000 || intervalo > 86400000) {
            throw new IllegalArgumentException("Intervalo de vencimientos inválido");
        }
        return new TareaVencimientos(detectar);
    }
}
