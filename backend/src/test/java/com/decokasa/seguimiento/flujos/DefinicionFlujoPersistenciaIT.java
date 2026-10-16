package com.decokasa.seguimiento.flujos;

import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** La estructura interna del flujo se comprueba dentro de su módulo, con PostgreSQL temporal. */
@SpringBootTest(properties = {"spring.config.import=", "decokasa.semilla.habilitada=false",
        "spring.jpa.hibernate.ddl-auto=validate"})
@Import(TestcontainersConfiguration.class)
class DefinicionFlujoPersistenciaIT {

    @Autowired DefinicionFlujoRepositorio definiciones;

    @Test
    @Transactional(readOnly = true)
    void adaptadorConservaLasCatorceTransiciones() {
        var flujo = definiciones.buscarVigente(CodigoPais.EC, TipoFlujo.A).orElseThrow();
        assertThat(flujo.transiciones()).hasSize(14);
    }
}
