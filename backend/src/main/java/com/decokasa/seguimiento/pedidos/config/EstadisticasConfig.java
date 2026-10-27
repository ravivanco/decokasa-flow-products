package com.decokasa.seguimiento.pedidos.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarEstadisticas;
import com.decokasa.seguimiento.pedidos.application.port.out.EstadisticasDePedidos;
import com.decokasa.seguimiento.pedidos.application.usecase.ConsultarEstadisticasCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** ADR-0029: estadísticas y alertas en una transacción de solo lectura. */
@Configuration(proxyBeanMethods = false)
public class EstadisticasConfig {
    @Bean
    ConsultarEstadisticas consultarEstadisticas(PlatformTransactionManager gestor, EstadisticasDePedidos datos,
            ConsultarResponsables responsables, Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        var caso = new ConsultarEstadisticasCasoDeUso(datos, responsables, reloj);
        return new ConsultarEstadisticas() {
            @Override public Estadisticas estadisticas(com.decokasa.seguimiento.compartido.domain.Solicitante s,
                    Filtro f) {
                return tx.execute(e -> caso.estadisticas(s, f));
            }

            @Override public java.util.List<Alerta> alertas(com.decokasa.seguimiento.compartido.domain.Solicitante s,
                    boolean todas) {
                return tx.execute(e -> caso.alertas(s, todas));
            }
        };
    }
}
