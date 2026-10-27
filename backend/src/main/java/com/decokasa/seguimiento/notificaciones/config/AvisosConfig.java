package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarAvisosDeEtapa;
import com.decokasa.seguimiento.notificaciones.application.port.out.AvisoRepositorio;
import com.decokasa.seguimiento.notificaciones.application.usecase.ConsultarAvisosDeEtapaCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** ADR-0032: consultar avisos es de solo lectura; marcar como leídos escribe solo aviso_lectura. */
@Configuration(proxyBeanMethods = false)
public class AvisosConfig {
    @Bean
    ConsultarAvisosDeEtapa consultarAvisosDeEtapa(PlatformTransactionManager gestor, AvisoRepositorio avisos,
            ConsultarResponsables responsables, Reloj reloj) {
        TransactionTemplate lectura = new TransactionTemplate(gestor);
        lectura.setReadOnly(true);
        TransactionTemplate escritura = new TransactionTemplate(gestor);
        var caso = new ConsultarAvisosDeEtapaCasoDeUso(avisos, responsables, reloj);
        return new ConsultarAvisosDeEtapa() {
            @Override public Avisos avisos(Solicitante solicitante) {
                return lectura.execute(e -> caso.avisos(solicitante));
            }

            @Override public Instant marcarLeidos(Solicitante solicitante) {
                return escritura.execute(e -> caso.marcarLeidos(solicitante));
            }
        };
    }
}
