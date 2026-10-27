package com.decokasa.seguimiento.bodegas.config;

import com.decokasa.seguimiento.bodegas.application.port.in.ActualizarBodega;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegaActiva;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegasPorPais;
import com.decokasa.seguimiento.bodegas.application.port.in.CrearBodega;
import com.decokasa.seguimiento.bodegas.application.port.in.DescribirBodega;
import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.application.usecase.ActualizarBodegaCasoDeUso;
import com.decokasa.seguimiento.bodegas.application.usecase.ConsultarBodegasCasoDeUso;
import com.decokasa.seguimiento.bodegas.application.usecase.CrearBodegaCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición del módulo bodegas. Las transacciones se abren aquí (ADR-0012). Dirección de dependencias (R9):
 * bodegas → usuarios (puerto de entrada) y compartido; bodegas no depende de pedidos.
 */
@Configuration(proxyBeanMethods = false)
public class BodegasConfig {

    // Fase 7 (ADR-0024): no se publica ConsultarBodegas (lista sin filtro de país). GET /bodegas usa
    // ConsultarBodegasPorPais.

    /** GET /bodegas desde la fase 7 (ADR-0024): solo las de los países asignados hoy. */
    @Bean
    ConsultarBodegasPorPais consultarBodegasPorPais(PlatformTransactionManager gestor, BodegaRepositorio bodegas,
            ConsultarResponsables usuarios) {
        TransactionTemplate tx = soloLectura(gestor);
        ConsultarBodegasPorPais caso = new ConsultarBodegasCasoDeUso(bodegas, usuarios);
        return (solicitante, pais, activa) -> tx.execute(estado -> caso.listar(solicitante, pais, activa));
    }

    /** Para pedidos (A9, todavía sin uso): se une a la transacción de quien la llama. */
    @Bean
    ConsultarBodegaActiva consultarBodegaActiva(PlatformTransactionManager gestor, BodegaRepositorio bodegas) {
        TransactionTemplate tx = soloLectura(gestor);
        ConsultarBodegaActiva caso = new ConsultarBodegasCasoDeUso(bodegas);
        return bodegaId -> tx.execute(estado -> caso.activa(bodegaId));
    }

    /** Para el detalle de pedidos: la bodega asignada, activa o no. */
    @Bean
    DescribirBodega describirBodega(PlatformTransactionManager gestor, BodegaRepositorio bodegas) {
        TransactionTemplate tx = soloLectura(gestor);
        DescribirBodega caso = new ConsultarBodegasCasoDeUso(bodegas);
        return bodegaId -> tx.execute(estado -> caso.describir(bodegaId));
    }

    @Bean
    CrearBodega crearBodega(PlatformTransactionManager gestor, BodegaRepositorio bodegas,
            ConsultarResponsables usuarios) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        CrearBodega caso = new CrearBodegaCasoDeUso(bodegas, usuarios);
        return (solicitante, comando) -> tx.execute(estado -> caso.crear(solicitante, comando));
    }

    @Bean
    ActualizarBodega actualizarBodega(PlatformTransactionManager gestor, BodegaRepositorio bodegas,
            ConsultarResponsables usuarios) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        ActualizarBodega caso = new ActualizarBodegaCasoDeUso(bodegas, usuarios);
        return (solicitante, comando) -> tx.execute(estado -> caso.actualizar(solicitante, comando));
    }

    // Dentro de una transacción de escritura, readOnly no la cambia: solo se une a ella.
    private static TransactionTemplate soloLectura(PlatformTransactionManager gestor) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        return tx;
    }
}
