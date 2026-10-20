package com.decokasa.seguimiento.flujos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarAccionesDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import com.decokasa.seguimiento.flujos.domain.DefinicionFlujo;
import com.decokasa.seguimiento.flujos.domain.TransicionPermitida;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Resuelve una acción con los datos de la versión del flujo del pedido (ADR-0004). Sin Spring (R2). */
public final class ResolverTransicionCasoDeUso implements ResolverTransicion, ConsultarAccionesDeEtapa {

    private final DefinicionFlujoRepositorio definiciones;

    public ResolverTransicionCasoDeUso(DefinicionFlujoRepositorio definiciones) {
        this.definiciones = definiciones;
    }

    @Override
    public Resolucion resolver(UUID flujoId, int etapaOrigen, AccionEtapa accion) {
        DefinicionFlujo flujo = flujo(flujoId);
        return resolucion(flujo, flujo.resolver(etapaOrigen, accion));
    }

    @Override
    public List<Resolucion> accionesDe(UUID flujoId, int etapa) {
        DefinicionFlujo flujo = flujo(flujoId);
        return flujo.transiciones().stream()
                .filter(transicion -> transicion.etapaOrigen() == etapa)
                .sorted(Comparator.comparing(TransicionPermitida::accion))
                .map(transicion -> resolucion(flujo, transicion))
                .toList();
    }

    private DefinicionFlujo flujo(UUID flujoId) {
        return definiciones.buscarPorId(flujoId)
                .orElseThrow(() -> new NoEncontrado("El flujo del pedido no existe."));
    }

    private static Resolucion resolucion(DefinicionFlujo flujo, TransicionPermitida transicion) {
        Integer destino = transicion.etapaDestino();
        PlazoDeEtapa plazoDestino = destino == null ? null : flujo.etapa(destino).plazo();
        boolean retornoDeUrgencia = destino != null && flujo.esRetornoDeUrgencia(destino);
        return new Resolucion(transicion.etapaOrigen(), transicion.accion(), destino, transicion.requiereObservacion(),
                transicion.requiereUrgencia(), transicion.requiereBodega(), transicion.documentosPendientes(),
                transicion.documentosObligatorios(), plazoDestino, retornoDeUrgencia,
                flujo.etapa(transicion.etapaOrigen()).rolResponsable(),
                flujo.etapa(transicion.etapaOrigen()).registraLlegada(),
                destino != null && flujo.etapa(destino).automatica());
    }
}
