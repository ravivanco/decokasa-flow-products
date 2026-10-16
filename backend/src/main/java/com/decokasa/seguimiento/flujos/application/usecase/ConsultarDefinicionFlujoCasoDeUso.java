package com.decokasa.seguimiento.flujos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import java.util.UUID;

/**
 * Lectura de definiciones de flujo. Entrega la definición del dominio como {@link ContratoDeFlujo}, el contrato que
 * puede cruzar la frontera del módulo (R4). Sin Spring (R2).
 */
public final class ConsultarDefinicionFlujoCasoDeUso implements ConsultarDefinicionFlujo {

    private final DefinicionFlujoRepositorio definiciones;

    public ConsultarDefinicionFlujoCasoDeUso(DefinicionFlujoRepositorio definiciones) {
        this.definiciones = definiciones;
    }

    @Override
    public ContratoDeFlujo vigente(CodigoPais pais, TipoFlujo tipo) {
        return definiciones.buscarVigente(pais, tipo)
                .orElseThrow(() -> new NoEncontrado("No hay un flujo vigente para ese país y tipo."));
    }

    @Override
    public ContratoDeFlujo version(CodigoPais pais, TipoFlujo tipo, int version) {
        return definiciones.buscarVersion(pais, tipo, version)
                .orElseThrow(() -> new NoEncontrado("Esa versión del flujo no existe."));
    }

    @Override
    public ContratoDeFlujo porId(UUID flujoId) {
        return definiciones.buscarPorId(flujoId)
                .orElseThrow(() -> new NoEncontrado("El flujo del pedido no existe."));
    }
}
