package com.decokasa.seguimiento.pedidos.config;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.PlanesVencidos;
import com.decokasa.seguimiento.pedidos.application.usecase.AlertarPlanVencidoCasoDeUso;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.util.List;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Alerta de fecha planificada vencida: va con los vencimientos automáticos (VENCIMIENTOS_AUTOMATICO=true). */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "decokasa.plazos.vencimientos.automatico", havingValue = "true")
public class PlanVencidoConfig {

    @Bean
    TareaPlanVencido tareaPlanVencido(PlatformTransactionManager gestor, ConsultaDePedidos consultas,
            ConsultarDefinicionFlujo flujos, CalcularCronograma cronogramas, PlanesVencidos planes,
            PublicadorEventos publicador, Reloj reloj) {
        return new TareaPlanVencido(new TransactionTemplate(gestor),
                new AlertarPlanVencidoCasoDeUso(consultas, flujos, cronogramas, planes, publicador, reloj));
    }

    public static final class TareaPlanVencido {
        private final TransactionTemplate tx;
        private final AlertarPlanVencidoCasoDeUso caso;

        TareaPlanVencido(TransactionTemplate tx, AlertarPlanVencidoCasoDeUso caso) {
            this.tx = tx;
            this.caso = caso;
        }

        @Scheduled(fixedDelayString = "${decokasa.plazos.plan-vencido.intervalo-ms:300000}")
        public void ejecutar() {
            List<UUID> pedidos = tx.execute(estado -> caso.candidatos(500));
            int avisos = 0;
            int fallidos = 0;
            for (UUID pedido : pedidos == null ? List.<UUID>of() : pedidos) {
                try {
                    // Una transacción por pedido: el evento, el historial y los avisos se confirman juntos.
                    if (Boolean.TRUE.equals(tx.execute(estado -> caso.revisar(pedido)))) {
                        avisos++;
                    }
                } catch (RuntimeException error) {
                    fallidos++;
                }
            }
            if (avisos > 0 || fallidos > 0) {
                LoggerFactory.getLogger(TareaPlanVencido.class).info("plan_vencido avisos={} fallidos={}", avisos,
                        fallidos);
            }
        }
    }
}
