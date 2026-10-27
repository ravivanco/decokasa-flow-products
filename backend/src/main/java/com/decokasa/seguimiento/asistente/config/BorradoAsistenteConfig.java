package com.decokasa.seguimiento.asistente.config;

import com.decokasa.seguimiento.asistente.adapter.in.tareas.TareaBorradoConversaciones;
import com.decokasa.seguimiento.asistente.application.port.in.BorrarConversacionesVencidas;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * F7-D5 (piloto): borrado diario de las conversaciones vencidas. Activo por defecto, también con la IA desactivada,
 * para que nada guardado sobreviva a su plazo. Se apaga solo con {@code decokasa.ia.borrado.automatico=false}.
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "decokasa.ia.borrado.automatico", havingValue = "true", matchIfMissing = true)
public class BorradoAsistenteConfig {

    @Bean
    TareaBorradoConversaciones tareaBorradoConversaciones(BorrarConversacionesVencidas borrar) {
        return new TareaBorradoConversaciones(borrar);
    }
}
