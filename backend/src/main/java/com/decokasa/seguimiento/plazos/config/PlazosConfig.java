package com.decokasa.seguimiento.plazos.config;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.plazos.application.port.in.ConfigurarMeta;
import com.decokasa.seguimiento.plazos.application.port.in.ConsultarConfiguracionPlazos;
import com.decokasa.seguimiento.plazos.application.port.in.DetectarVencimientos;
import com.decokasa.seguimiento.plazos.application.port.in.RegistrarVencimientosDePedido;
import com.decokasa.seguimiento.plazos.application.port.out.AvisosVencimientoRepositorio;
import com.decokasa.seguimiento.plazos.application.port.out.ConfiguracionPlazosRepositorio;
import com.decokasa.seguimiento.plazos.application.port.out.DiagnosticoVencimientos;
import com.decokasa.seguimiento.plazos.application.port.out.PedidosConEtapaActiva;
import com.decokasa.seguimiento.plazos.application.usecase.CalcularCronogramaCasoDeUso;
import com.decokasa.seguimiento.plazos.application.usecase.CalcularVencimientoCasoDeUso;
import com.decokasa.seguimiento.plazos.application.usecase.ConfiguracionPlazosCasoDeUso;
import com.decokasa.seguimiento.plazos.application.usecase.DetectarVencimientosCasoDeUso;
import com.decokasa.seguimiento.plazos.application.usecase.RegistrarVencimientosCasoDeUso;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición del módulo plazos. Calcula con la hora de Ecuador (ADR-0006, R5). El vencimiento no toca la base;
 * el cronograma y la meta sí (configuracion_plazos, V14), con la transacción abierta aquí (ADR-0012).
 *
 * <p>Dirección de dependencias (R9): plazos → usuarios (puerto de entrada) y compartido. Plazos no depende de pedidos:
 * pedidos le entrega los datos del pedido por {@link CalcularCronograma}.
 */
@Configuration(proxyBeanMethods = false)
public class PlazosConfig {

    private static CalendarioLaboral calendario() {
        return new CalendarioLaboral(Reloj.ZONA_ECUADOR);
    }

    @Bean
    CalcularVencimiento calcularVencimiento() {
        return new CalcularVencimientoCasoDeUso(calendario());
    }

    /** Para pedidos (GET /pedidos/{id}/cronograma): se une a la transacción de solo lectura de quien la llama. */
    @Bean
    CalcularCronograma calcularCronograma(PlatformTransactionManager gestor,
            ConfiguracionPlazosRepositorio configuracion) {
        TransactionTemplate tx = soloLectura(gestor);
        CalcularCronograma caso = new CalcularCronogramaCasoDeUso(configuracion, calendario());
        return datos -> tx.execute(estado -> caso.calcular(datos));
    }

    @Bean
    ConsultarConfiguracionPlazos consultarConfiguracionPlazos(PlatformTransactionManager gestor,
            ConfiguracionPlazosRepositorio configuracion, ConsultarResponsables usuarios, Reloj reloj) {
        TransactionTemplate tx = soloLectura(gestor);
        ConsultarConfiguracionPlazos caso = new ConfiguracionPlazosCasoDeUso(configuracion, usuarios, reloj);
        return () -> tx.execute(estado -> caso.consultar());
    }

    @Bean
    ConfigurarMeta configurarMeta(PlatformTransactionManager gestor, ConfiguracionPlazosRepositorio configuracion,
            ConsultarResponsables usuarios, Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        ConfigurarMeta caso = new ConfiguracionPlazosCasoDeUso(configuracion, usuarios, reloj);
        return (solicitante, comando) -> tx.execute(estado -> caso.configurar(solicitante, comando));
    }

    /**
     * Paso 5.2: cada pedido en su propia transacción (REQUIRES_NEW). En ella pasan el bloqueo, la relectura, el evento
     * PLAZO_VENCIDO (historial y outbox, oyentes síncronos con MANDATORY) y el aviso único: todo o nada.
     */
    @Bean
    RegistrarVencimientosDePedido registrarVencimientosDePedido(PlatformTransactionManager gestor,
            PedidosConEtapaActiva pedidos, AvisosVencimientoRepositorio avisos, PublicadorEventos publicador) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        RegistrarVencimientosDePedido caso = new RegistrarVencimientosCasoDeUso(pedidos, avisos, publicador);
        return (pedidoId, ahora) -> tx.execute(estado -> caso.registrar(pedidoId, ahora));
    }

    /** La lectura de candidatos no bloquea; cada pedido se registra con {@link RegistrarVencimientosDePedido}. */
    @Bean
    DetectarVencimientos detectarVencimientos(PedidosConEtapaActiva pedidos, RegistrarVencimientosDePedido registrar,
            Reloj reloj, @Value("${decokasa.plazos.vencimientos.lote:100}") int lote,
            DiagnosticoVencimientos diagnostico) {
        return new DetectarVencimientosCasoDeUso(pedidos, registrar, reloj, lote, diagnostico);
    }

    // Dentro de una transacción de escritura, readOnly no la cambia: solo se une a ella.
    private static TransactionTemplate soloLectura(PlatformTransactionManager gestor) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        return tx;
    }
}
