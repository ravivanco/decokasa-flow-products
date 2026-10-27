package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que un pedido nuevo necesita de la versión vigente de su flujo. El caso de uso (paso 2.4) lo arma con el puerto
 * ConsultarDefinicionFlujo de {@code flujos}; el dominio de pedidos no conoce el de flujos (R4).
 *
 * @param numerosDeEtapa números de todas las etapas de esa versión
 * @param rolResponsableInicial rol responsable de la primera etapa
 * @param plazoInicial plazo de la primera etapa
 */
public record FlujoDelPedido(UUID flujoId, TipoFlujo tipo, List<Integer> numerosDeEtapa,
        CodigoRol rolResponsableInicial, PlazoDeEtapa plazoInicial) {

    public FlujoDelPedido {
        Objects.requireNonNull(flujoId, "flujoId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(rolResponsableInicial, "rolResponsableInicial");
        Objects.requireNonNull(plazoInicial, "plazoInicial");
        numerosDeEtapa = List.copyOf(numerosDeEtapa);
    }
}
