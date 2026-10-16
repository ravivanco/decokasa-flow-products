package com.decokasa.seguimiento.plazos.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.plazos.application.usecase.CalcularVencimientoCasoDeUso;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Raíz de composición del módulo plazos. Calcula con la hora de Ecuador (ADR-0006, R5); no toca la base, así que no
 * necesita transacción.
 */
@Configuration(proxyBeanMethods = false)
public class PlazosConfig {

    @Bean
    CalcularVencimiento calcularVencimiento() {
        return new CalcularVencimientoCasoDeUso(new CalendarioLaboral(Reloj.ZONA_ECUADOR));
    }
}
