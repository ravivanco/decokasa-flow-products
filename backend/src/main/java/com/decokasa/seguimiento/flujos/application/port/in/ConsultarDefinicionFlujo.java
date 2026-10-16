package com.decokasa.seguimiento.flujos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import java.util.UUID;

/**
 * Definición de un flujo para otros módulos. Devuelve el contrato compartido {@link ContratoDeFlujo}, nunca las clases
 * internas de {@code flujos}: la frontera del puerto no expone {@code flujos.domain} (R4; 2.B1-D01). Las transiciones
 * se consultan con el puerto ResolverTransicion. Qué debe exponer GET /flujos/{pais}/{tipoFlujo} sigue bloqueado por
 * las representaciones pendientes del contrato (paso 2.4).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si no hay una versión vigente o la pedida
 */
public interface ConsultarDefinicionFlujo {

    ContratoDeFlujo vigente(CodigoPais pais, TipoFlujo tipo);

    ContratoDeFlujo version(CodigoPais pais, TipoFlujo tipo, int version);

    /** La versión exacta que sigue un pedido (ADR-0004: un pedido no cambia de versión). */
    ContratoDeFlujo porId(UUID flujoId);
}
