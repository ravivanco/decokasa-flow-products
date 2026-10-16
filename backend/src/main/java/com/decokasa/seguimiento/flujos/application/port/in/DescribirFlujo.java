package com.decokasa.seguimiento.flujos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.util.List;

/**
 * Definición completa de un flujo para mostrarla (contrato: GET /flujos/{pais}/{tipoFlujo}, esquema DefinicionFlujo).
 * Devuelve una vista propia del puerto, sin clases internas del dominio de flujos. Los valores pendientes del cliente
 * se muestran como pendientes: plazo PENDIENTE y documentos PENDIENTES no equivalen a "sin plazo" ni a "sin
 * documentos".
 */
public interface DescribirFlujo {

    /**
     * @param version nula para la vigente
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si no hay vigente o la versión no existe
     */
    Flujo describir(CodigoPais pais, TipoFlujo tipo, Integer version);

    record Flujo(CodigoPais pais, TipoFlujo tipo, int version, List<Etapa> etapas) {
    }

    /**
     * @param rolResponsable nulo solo en etapas automáticas
     * @param revisaEtapa etapa cuyos archivos revisa (P-17); nula si ninguna
     */
    record Etapa(int numero, String nombre, CodigoRol rolResponsable, PlazoDeEtapa plazo, Integer revisaEtapa,
            boolean automatica, List<Accion> acciones) {
    }

    /** @param documentosPendientes el cliente no definió los documentos de la acción */
    record Accion(AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion, boolean requiereUrgencia,
            boolean requiereBodega, boolean documentosPendientes, List<String> documentosObligatorios) {
    }
}
