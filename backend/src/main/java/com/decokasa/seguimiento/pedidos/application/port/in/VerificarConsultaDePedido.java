package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/** P-13 (provisional del equipo): la persona puede ver el pedido. Lo usan otros módulos (historial). */
public interface VerificarConsultaDePedido {

    /**
     * Devuelve el número del pedido si la persona puede verlo.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la cuenta está inactiva o sin roles
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el pedido no existe
     */
    String numeroVisible(Solicitante solicitante, UUID pedidoId);
}
