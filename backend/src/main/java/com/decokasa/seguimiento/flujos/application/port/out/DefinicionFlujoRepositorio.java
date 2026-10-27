package com.decokasa.seguimiento.flujos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.flujos.domain.DefinicionFlujo;
import java.util.Optional;
import java.util.UUID;

/**
 * Lectura de las definiciones de flujo (tablas de V2). Las definiciones se cargan con migraciones; este puerto no las
 * escribe. El adaptador JPA llega en el paso 2.4.
 */
public interface DefinicionFlujoRepositorio {

    Optional<DefinicionFlujo> buscarPorId(UUID flujoId);

    Optional<DefinicionFlujo> buscarVigente(CodigoPais pais, TipoFlujo tipo);

    Optional<DefinicionFlujo> buscarVersion(CodigoPais pais, TipoFlujo tipo, int version);
}
