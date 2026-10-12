package com.decokasa.seguimiento.compartido.config;

import com.decokasa.seguimiento.compartido.adapter.out.RelojSistema;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raíz de composición del módulo compartido (arquitectura.md, sección 1): decide qué adaptador implementa cada
 * puerto. Las clases del núcleo no llevan anotaciones de Spring (R2); se registran aquí.
 */
@Configuration(proxyBeanMethods = false)
public class CompartidoConfig {

    @Bean
    Reloj reloj() {
        return new RelojSistema();
    }
}
