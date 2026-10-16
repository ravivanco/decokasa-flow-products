package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que la definición del flujo dice de una acción en la etapa actual. El caso de uso (paso 2.4) la arma con la
 * respuesta del puerto ResolverTransicion de {@code flujos}; el dominio de pedidos no conoce el de flujos (R4).
 *
 * @param etapaDestino nulo si el pedido se queda en la etapa
 * @param documentosPendientes el cliente no definió los documentos obligatorios: la acción no se puede ejecutar
 * @param plazoDestino plazo de la etapa de destino; nulo solo si no hay destino
 * @param destinoEsRetornoDeUrgencia al entrar en el destino termina la urgencia (A4 / B5)
 * @param rolResponsableOrigen rol responsable de la etapa actual; nulo si es automática y nadie actúa en ella
 */
public record ReglaDeTransicion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino,
        boolean requiereObservacion, boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
        Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
        CodigoRol rolResponsableOrigen) {

    public ReglaDeTransicion {
        Objects.requireNonNull(accion, "accion");
        documentosObligatorios = Set.copyOf(documentosObligatorios);
        if (etapaDestino != null) {
            Objects.requireNonNull(plazoDestino, "plazoDestino");
        }
    }
}
