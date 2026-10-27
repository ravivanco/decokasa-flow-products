package com.decokasa.seguimiento.flujos.config;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarAccionesDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.DescribirFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import com.decokasa.seguimiento.flujos.application.usecase.ConsultarDefinicionFlujoCasoDeUso;
import com.decokasa.seguimiento.flujos.application.usecase.DescribirFlujoCasoDeUso;
import com.decokasa.seguimiento.flujos.application.usecase.ResolverTransicionCasoDeUso;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Raíz de composición del módulo flujos (arquitectura.md, sección 1). Las lecturas abren una transacción de solo
 * lectura, o se unen a la de la acción que las llama (propagación REQUIRED; ADR-0012).
 */
@Configuration(proxyBeanMethods = false)
public class FlujosConfig {

    @Bean
    ResolverTransicion resolverTransicion(PlatformTransactionManager gestor, DefinicionFlujoRepositorio definiciones) {
        TransactionTemplate tx = soloLectura(gestor);
        ResolverTransicion caso = new ResolverTransicionCasoDeUso(definiciones);
        return (flujoId, etapaOrigen, accion) -> tx.execute(estado -> caso.resolver(flujoId, etapaOrigen, accion));
    }

    @Bean
    ConsultarAccionesDeEtapa consultarAccionesDeEtapa(PlatformTransactionManager gestor,
            DefinicionFlujoRepositorio definiciones) {
        TransactionTemplate tx = soloLectura(gestor);
        ConsultarAccionesDeEtapa caso = new ResolverTransicionCasoDeUso(definiciones);
        return (flujoId, etapa) -> tx.execute(estado -> caso.accionesDe(flujoId, etapa));
    }

    @Bean
    ConsultarDefinicionFlujo consultarDefinicionFlujo(PlatformTransactionManager gestor,
            DefinicionFlujoRepositorio definiciones) {
        TransactionTemplate tx = soloLectura(gestor);
        ConsultarDefinicionFlujo caso = new ConsultarDefinicionFlujoCasoDeUso(definiciones);
        return new ConsultarDefinicionFlujo() {
            @Override
            public ContratoDeFlujo vigente(CodigoPais pais, TipoFlujo tipo) {
                return tx.execute(estado -> caso.vigente(pais, tipo));
            }

            @Override
            public ContratoDeFlujo version(CodigoPais pais, TipoFlujo tipo, int version) {
                return tx.execute(estado -> caso.version(pais, tipo, version));
            }

            @Override
            public ContratoDeFlujo porId(UUID flujoId) {
                return tx.execute(estado -> caso.porId(flujoId));
            }
        };
    }

    @Bean
    DescribirFlujo describirFlujo(PlatformTransactionManager gestor, DefinicionFlujoRepositorio definiciones) {
        TransactionTemplate tx = soloLectura(gestor);
        DescribirFlujo caso = new DescribirFlujoCasoDeUso(definiciones);
        return (pais, tipo, version) -> tx.execute(estado -> caso.describir(pais, tipo, version));
    }

    // Dentro de una transacción de escritura, readOnly no la cambia: solo se une a ella.
    private static TransactionTemplate soloLectura(PlatformTransactionManager gestor) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        return tx;
    }
}
