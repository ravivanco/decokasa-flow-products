package com.decokasa.seguimiento.compartido.domain.flujo;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.util.List;
import java.util.UUID;

/**
 * Lo que otro módulo puede leer de una versión de un flujo (R4: contrato compartido, como los eventos de P-15). Lo
 * implementa el dominio de {@code flujos} y lo entrega su puerto de entrada ConsultarDefinicionFlujo; {@code pedidos}
 * trabaja solo con este contrato y no depende de las clases internas de {@code flujos} (2.B1-D01). Java puro (R1).
 */
public interface ContratoDeFlujo {

    UUID id();

    CodigoPais pais();

    TipoFlujo tipo();

    /** Versión del flujo (ADR-0004: un pedido sigue la versión con la que se creó). */
    int version();

    /** Etapas de la versión, en orden. */
    List<? extends ContratoDeEtapa> etapas();

    /** La etapa con ese número; si no existe, un error de dominio. */
    ContratoDeEtapa etapa(int numero);

    /** La primera etapa (A1 / B1), donde nace un pedido. */
    ContratoDeEtapa etapaInicial();
}
