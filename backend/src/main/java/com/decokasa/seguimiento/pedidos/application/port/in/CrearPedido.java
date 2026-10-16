package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.pedidos.domain.Pedido;

/**
 * Crea un pedido en la primera etapa de la versión vigente de su flujo (contrato: POST /pedidos). El número lo
 * genera el servidor (requerimientos 4.2 #5).
 */
public interface CrearPedido {

    Pedido crear(Solicitante solicitante, Comando comando);

    /** @param pais nulo equivale a EC (contrato: {@code default: EC}) */
    record Comando(TipoFlujo tipoFlujo, CodigoPais pais, String nombre, String descripcion,
            boolean incluyeProductosNuevos) {
    }
}
