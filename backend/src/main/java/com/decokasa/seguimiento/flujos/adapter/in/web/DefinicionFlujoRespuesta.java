package com.decokasa.seguimiento.flujos.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.DescribirFlujo;
import java.util.List;

/** Esquemas DefinicionFlujo, DefinicionEtapa, Plazo y DefinicionAccion del contrato 0.7.0. */
public record DefinicionFlujoRespuesta(String pais, String tipoFlujo, int version, List<Etapa> etapas) {

    public record Etapa(int numero, String nombre, String rolResponsable, String estadoPlazo, Integer revisaEtapa,
            Plazo plazo, List<Accion> acciones, boolean automatica, boolean registraLlegada) {
    }

    public record Plazo(int cantidad, String tipo) {
    }

    public record Accion(String accion, Integer etapaDestino, boolean requiereObservacion, boolean requiereUrgencia,
            boolean requiereBodega, String requisitosDocumentales, List<String> documentosObligatorios) {
    }

    static DefinicionFlujoRespuesta de(DescribirFlujo.Flujo flujo) {
        return new DefinicionFlujoRespuesta(flujo.pais().name(), flujo.tipo().name(), flujo.version(),
                flujo.etapas().stream().map(DefinicionFlujoRespuesta::etapa).toList());
    }

    private static Etapa etapa(DescribirFlujo.Etapa etapa) {
        String estado;
        Plazo plazo;
        switch (etapa.plazo()) {
            case PlazoDeEtapa.Definido definido -> {
                estado = "DEFINIDO";
                plazo = new Plazo(definido.cantidad(), definido.tipo().name());
            }
            case PlazoDeEtapa.SinPlazo sinPlazo -> {
                estado = "SIN_PLAZO";
                plazo = null;
            }
            case PlazoDeEtapa.Pendiente pendiente -> {
                estado = "PENDIENTE";
                plazo = null;
            }
        }
        return new Etapa(etapa.numero(), etapa.nombre(),
                etapa.rolResponsable() == null ? null : etapa.rolResponsable().valor(), estado, etapa.revisaEtapa(),
                plazo, etapa.acciones().stream().map(DefinicionFlujoRespuesta::accion).toList(), etapa.automatica(),
                etapa.registraLlegada());
    }

    private static Accion accion(DescribirFlujo.Accion accion) {
        return new Accion(accion.accion().name(), accion.etapaDestino(), accion.requiereObservacion(),
                accion.requiereUrgencia(), accion.requiereBodega(),
                accion.documentosPendientes() ? "PENDIENTES" : "DEFINIDOS", accion.documentosObligatorios());
    }
}
