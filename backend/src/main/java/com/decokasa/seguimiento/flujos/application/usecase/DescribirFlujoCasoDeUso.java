package com.decokasa.seguimiento.flujos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.DescribirFlujo;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import com.decokasa.seguimiento.flujos.domain.DefinicionEtapa;
import com.decokasa.seguimiento.flujos.domain.DefinicionFlujo;
import com.decokasa.seguimiento.flujos.domain.TransicionPermitida;
import java.util.Comparator;
import java.util.List;

/** Arma la vista completa de una versión del flujo (ADR-0004). Sin Spring (R2). */
public final class DescribirFlujoCasoDeUso implements DescribirFlujo {

    private final DefinicionFlujoRepositorio definiciones;

    public DescribirFlujoCasoDeUso(DefinicionFlujoRepositorio definiciones) {
        this.definiciones = definiciones;
    }

    @Override
    public Flujo describir(CodigoPais pais, TipoFlujo tipo, Integer version) {
        DefinicionFlujo flujo = (version == null ? definiciones.buscarVigente(pais, tipo)
                : definiciones.buscarVersion(pais, tipo, version))
                .orElseThrow(() -> new NoEncontrado("No hay una definición de flujo para esos datos."));
        List<Etapa> etapas = flujo.etapas().stream()
                .sorted(Comparator.comparingInt(DefinicionEtapa::numero))
                .map(etapa -> etapa(flujo, etapa))
                .toList();
        return new Flujo(flujo.pais(), flujo.tipo(), flujo.version(), etapas);
    }

    private static Etapa etapa(DefinicionFlujo flujo, DefinicionEtapa etapa) {
        List<Accion> acciones = flujo.transiciones().stream()
                .filter(transicion -> transicion.etapaOrigen() == etapa.numero())
                .sorted(Comparator.comparing(TransicionPermitida::accion))
                .map(transicion -> new Accion(transicion.accion(), transicion.etapaDestino(),
                        transicion.requiereObservacion(), transicion.requiereUrgencia(), transicion.requiereBodega(),
                        transicion.documentosPendientes(),
                        transicion.documentosObligatorios().stream().sorted().toList()))
                .toList();
        return new Etapa(etapa.numero(), etapa.nombre(), etapa.rolResponsable(), etapa.plazo(), etapa.revisaEtapa(),
                etapa.automatica(), etapa.registraLlegada(), acciones);
    }
}
