package com.decokasa.seguimiento.pedidos.config;

import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegaActiva;
import com.decokasa.seguimiento.bodegas.application.port.in.DescribirBodega;
import com.decokasa.seguimiento.archivos.application.port.in.ConsultarAdjuntosDeEntrada;
import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarAccionesDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.AgregarObservacion;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarCronograma;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.pedidos.application.port.in.CrearPedido;
import com.decokasa.seguimiento.pedidos.application.port.in.EjecutarAccionEtapa;
import com.decokasa.seguimiento.pedidos.application.port.in.ReasignarEtapa;
import com.decokasa.seguimiento.pedidos.application.port.in.RegistrarMotivoRetraso;
import com.decokasa.seguimiento.pedidos.application.port.in.VerificarConsultaDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.VistasDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.GeneradorNumeroPedido;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.application.usecase.AgregarObservacionCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.AutorizarArchivosCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.ConsultarCronogramaCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.ConsultarPedidosCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.CrearPedidoCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.EjecutarAccionEtapaCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.ReasignarEtapaCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.RegistrarMotivoRetrasoCasoDeUso;
import com.decokasa.seguimiento.pedidos.application.usecase.VerificarConsultaCasoDeUso;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición del módulo pedidos (arquitectura.md, sección 1). Cada caso de uso corre dentro de una
 * transacción abierta aquí, fuera del núcleo (ADR-0012). En ella pasan el bloqueo del pedido, el número, las lecturas
 * de flujos y usuarios, el guardado, la publicación de eventos y la inserción del historial: si algo falla, no queda
 * nada a medias (R7). Sin outbox (ADR-0012 propuesto).
 *
 * <p>Dirección de dependencias (R9): pedidos → flujos, plazos, usuarios, archivos y compartido. Ningún módulo depende
 * de pedidos.
 */
@Configuration(proxyBeanMethods = false)
public class PedidosConfig {

    @Bean
    EtapasHabilitadas etapasHabilitadas() {
        // Paso 6.4 (F6-D3): las etapas manuales de la definición del flujo de cada pedido (A1–A10, B1–B11). Las
        // automáticas (A11, B12) nunca admiten acciones manuales.
        return EtapasHabilitadas.segunDefinicion();
    }

    @Bean
    CrearPedido crearPedido(PlatformTransactionManager gestor, ConsultarDefinicionFlujo flujos,
            PedidoRepositorio pedidos, GeneradorNumeroPedido generador, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador, Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        CrearPedido caso = new CrearPedidoCasoDeUso(flujos, pedidos, generador, responsables, vencimientos,
                publicador, reloj);
        return (solicitante, comando) -> tx.execute(estado -> caso.crear(solicitante, comando));
    }

    @Bean
    EjecutarAccionEtapa ejecutarAccionEtapa(PlatformTransactionManager gestor, PedidoRepositorio pedidos,
            ResolverTransicion transiciones, EtapasHabilitadas habilitadas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador, Reloj reloj,
            ValidarAdjuntosParaAccion adjuntos, ConsultarBodegaActiva bodegas) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        EjecutarAccionEtapa caso = new EjecutarAccionEtapaCasoDeUso(pedidos, transiciones, habilitadas, responsables,
                vencimientos, publicador, reloj, adjuntos, bodegas);
        return (solicitante, comando) -> tx.execute(estado -> caso.ejecutar(solicitante, comando));
    }

    @Bean
    ReasignarEtapa reasignarEtapa(PlatformTransactionManager gestor, PedidoRepositorio pedidos,
            ConsultarDefinicionFlujo flujos, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        ReasignarEtapa caso = new ReasignarEtapaCasoDeUso(pedidos, flujos, responsables, vencimientos, publicador,
                reloj);
        return (solicitante, comando) -> tx.execute(estado -> caso.reasignar(solicitante, comando));
    }

    /** P-13: consultas en una transacción de solo lectura. */
    @Bean
    ConsultarPedidos consultarPedidos(PlatformTransactionManager gestor, ConsultaDePedidos consultas,
            ConsultarDefinicionFlujo flujos, ConsultarAccionesDeEtapa acciones, ConsultarAdjuntosDeEntrada adjuntos,
            ResumirUsuarios usuarios, EtapasHabilitadas habilitadas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador, Reloj reloj,
            ResumirEtapasDePedido cierres, DescribirBodega bodegas) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        ConsultarPedidos caso = new ConsultarPedidosCasoDeUso(consultas, flujos, acciones, adjuntos, usuarios,
                habilitadas, responsables, vencimientos, publicador, reloj, cierres, bodegas);
        return new ConsultarPedidos() {
            @Override
            public VistasDePedido.Detalle detalle(Solicitante solicitante, UUID pedidoId) {
                return tx.execute(estado -> caso.detalle(solicitante, pedidoId));
            }

            @Override
            public VistasDePedido.Detalle porNumero(Solicitante solicitante, String numero) {
                return tx.execute(estado -> caso.porNumero(solicitante, numero));
            }

            @Override
            public VistasDePedido.Pagina listar(Solicitante solicitante, Filtro filtro, int pagina, int tamano) {
                return tx.execute(estado -> caso.listar(solicitante, filtro, pagina, tamano));
            }

            @Override
            public VistasDePedido.Pagina bandeja(Solicitante solicitante, FiltroBandeja filtro, int pagina,
                    int tamano) {
                return tx.execute(estado -> caso.bandeja(solicitante, filtro, pagina, tamano));
            }
        };
    }

    /** Motivo del retraso (fase 5, paso 5.2; F5-D3): con el pedido bloqueado, como las acciones. */
    @Bean
    RegistrarMotivoRetraso registrarMotivoRetraso(PlatformTransactionManager gestor, PedidoRepositorio pedidos,
            ConsultarDefinicionFlujo flujos, EtapasHabilitadas habilitadas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador, Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        RegistrarMotivoRetraso caso = new RegistrarMotivoRetrasoCasoDeUso(pedidos, flujos, habilitadas, responsables,
                vencimientos, publicador, reloj);
        return (solicitante, comando) -> tx.execute(estado -> caso.registrar(solicitante, comando));
    }

    /** Cronograma del pedido (fase 5, paso 5.1; P-13): transacción de solo lectura; el cálculo es de plazos. */
    @Bean
    ConsultarCronograma consultarCronograma(PlatformTransactionManager gestor, ConsultaDePedidos consultas,
            ConsultarDefinicionFlujo flujos, CalcularCronograma cronogramas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        ConsultarCronograma caso = new ConsultarCronogramaCasoDeUso(consultas, flujos, cronogramas, responsables,
                vencimientos, publicador);
        return (solicitante, pedidoId) -> tx.execute(estado -> caso.cronograma(solicitante, pedidoId));
    }

    /**
     * Reglas de archivos de pedidos (P-06, P-13, P-17, P-18). Se une a la transacción de archivos que lo llama: la
     * subida bloquea el pedido hasta confirmar el adjunto.
     */
    @Bean
    AutorizarArchivosDePedido autorizarArchivosDePedido(PlatformTransactionManager gestor, PedidoRepositorio pedidos,
            ConsultaDePedidos consultas, ConsultarDefinicionFlujo flujos, EtapasHabilitadas habilitadas,
            ConsultarResponsables responsables, CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        AutorizarArchivosDePedido caso = new AutorizarArchivosCasoDeUso(pedidos, consultas, flujos, habilitadas,
                responsables, vencimientos, publicador);
        return new AutorizarArchivosDePedido() {
            @Override
            public AutorizacionSubida autorizarSubida(Solicitante solicitante, UUID pedidoId, int etapa,
                    UUID entradaEsperadaId) {
                return tx.execute(estado -> caso.autorizarSubida(solicitante, pedidoId, etapa, entradaEsperadaId));
            }

            @Override
            public void autorizarConsulta(Solicitante solicitante, UUID pedidoId) {
                tx.executeWithoutResult(estado -> caso.autorizarConsulta(solicitante, pedidoId));
            }

            @Override
            public void autorizarDescarga(Solicitante solicitante, UUID pedidoId, int etapaDelArchivo) {
                tx.executeWithoutResult(estado -> caso.autorizarDescarga(solicitante, pedidoId, etapaDelArchivo));
            }
        };
    }

    /** P-13 para el historial: transacción de solo lectura. */
    @Bean
    VerificarConsultaDePedido verificarConsultaDePedido(PlatformTransactionManager gestor, ConsultaDePedidos consultas,
            ConsultarResponsables responsables, CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        VerificarConsultaDePedido caso = new VerificarConsultaCasoDeUso(consultas, responsables, vencimientos,
                publicador);
        return (solicitante, pedidoId) -> tx.execute(estado -> caso.numeroVisible(solicitante, pedidoId));
    }

    /** P-19: observación e historial en la misma transacción, con el pedido bloqueado. */
    @Bean
    AgregarObservacion agregarObservacion(PlatformTransactionManager gestor, PedidoRepositorio pedidos,
            ConsultarDefinicionFlujo flujos, EtapasHabilitadas habilitadas, ConsultarResponsables responsables,
            CalcularVencimiento vencimientos, PublicadorEventos publicador, Reloj reloj,
            ValidarAdjuntosParaAccion adjuntos) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        AgregarObservacion caso = new AgregarObservacionCasoDeUso(pedidos, flujos, habilitadas, responsables,
                vencimientos, publicador, reloj, adjuntos);
        return (solicitante, comando) -> tx.execute(estado -> caso.agregar(solicitante, comando));
    }
}
