package com.decokasa.seguimiento.flujos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.Objects;
import java.util.Set;

/**
 * Una acción permitida en una etapa, con su destino y sus requisitos (modelo-datos.md §3.2; contrato:
 * DefinicionAccion). {@code etapaDestino} es nulo solo si el pedido se queda en la etapa (REPORTAR_DEMORA,
 * ELEGIR_BODEGA). Con requisitos documentales PENDIENTES, {@code documentosObligatorios} no es la lista completa y la
 * transición no se puede ejecutar. Java puro (R1).
 */
public record TransicionPermitida(int etapaOrigen, AccionEtapa accion, Integer etapaDestino,
        boolean requiereObservacion, boolean requiereUrgencia, boolean requiereBodega,
        RequisitosDocumentales requisitosDocumentales, Set<String> documentosObligatorios) {

    public TransicionPermitida {
        Objects.requireNonNull(accion, "accion");
        Objects.requireNonNull(requisitosDocumentales, "requisitosDocumentales");
        boolean sinDestino = accion == AccionEtapa.REPORTAR_DEMORA || accion == AccionEtapa.ELEGIR_BODEGA;
        if ((etapaDestino == null) != sinDestino) {
            throw new DatoInvalido("Solo REPORTAR_DEMORA y ELEGIR_BODEGA dejan el pedido en la misma etapa.");
        }
        if (requiereUrgencia && accion != AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO) {
            throw new DatoInvalido("Solo la devolución por producto nuevo pide urgencia.");
        }
        documentosObligatorios = Set.copyOf(documentosObligatorios);
    }

    public boolean documentosPendientes() {
        return requisitosDocumentales == RequisitosDocumentales.PENDIENTES;
    }
}
