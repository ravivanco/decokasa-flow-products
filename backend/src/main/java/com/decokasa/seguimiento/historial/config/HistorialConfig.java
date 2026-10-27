package com.decokasa.seguimiento.historial.config;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import com.decokasa.seguimiento.historial.application.port.in.RegistrarEventoPedido;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.application.port.out.PedidosParaHistorial;
import com.decokasa.seguimiento.historial.application.usecase.ConsultarHistorialPedidoCasoDeUso;
import com.decokasa.seguimiento.historial.application.usecase.RegistrarEventoPedidoCasoDeUso;
import com.decokasa.seguimiento.historial.application.usecase.ResumirEtapasDePedidoCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.port.in.NombrarRoles;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición del módulo historial (arquitectura.md, sección 1). El registro no abre transacción propia: corre
 * en la de la acción, y el oyente exige que exista. La consulta del historial (permisos P-13 y endpoint) es del paso
 * 2.4.
 */
@Configuration(proxyBeanMethods = false)
public class HistorialConfig {

    @Bean
    RegistrarEventoPedido registrarEventoPedido(HistorialRepositorio historial,
            com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventosRegistrados publicador) {
        return new RegistrarEventoPedidoCasoDeUso(historial, publicador);
    }

    /** Consulta del historial: transacción de solo lectura. El permiso (P-13) lo da el adaptador de pedidos. */
    @Bean
    ConsultarHistorialPedido consultarHistorialPedido(PlatformTransactionManager gestor, PedidosParaHistorial pedidos,
            HistorialRepositorio historial, ResumirUsuarios usuarios) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        ConsultarHistorialPedido caso = new ConsultarHistorialPedidoCasoDeUso(pedidos, historial, usuarios);
        return new ConsultarHistorialPedido() {
            @Override
            public List<Entrada> historial(Solicitante solicitante, UUID pedidoId) {
                return tx.execute(estado -> caso.historial(solicitante, pedidoId));
            }

            @Override
            public Entrada evento(Solicitante solicitante, UUID pedidoId, UUID eventoId) {
                return tx.execute(estado -> caso.evento(solicitante, pedidoId, eventoId));
            }
        };
    }

    /** Resumen de etapas para el detalle del pedido: se une a la transacción de solo lectura de la consulta. */
    @Bean
    ResumirEtapasDePedido resumirEtapasDePedido(PlatformTransactionManager gestor, HistorialRepositorio historial,
            ResumirUsuarios usuarios, NombrarRoles roles) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        ResumirEtapasDePedido caso = new ResumirEtapasDePedidoCasoDeUso(historial, usuarios, roles);
        return pedidoId -> tx.execute(estado -> caso.ultimosCierres(pedidoId));
    }
}
