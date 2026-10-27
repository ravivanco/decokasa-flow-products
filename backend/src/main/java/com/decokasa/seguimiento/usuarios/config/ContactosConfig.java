package com.decokasa.seguimiento.usuarios.config;

import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import com.decokasa.seguimiento.usuarios.application.port.out.Contactos;
import com.decokasa.seguimiento.usuarios.application.usecase.ConsultarContactosCasoDeUso;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** ADR-0032: contactos vigentes para los avisos. Sin transacción propia: usa la del llamador, si la hay. */
@Configuration(proxyBeanMethods = false)
public class ContactosConfig {
    @Bean
    ConsultarContactos consultarContactos(Contactos contactos) {
        return new ConsultarContactosCasoDeUso(contactos);
    }
}
