package com.decokasa.seguimiento.historial.application.port.out;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/**
 * Lo que el historial necesita de los pedidos para mostrarse. Lo implementa un adaptador de {@code pedidos}: la
 * dependencia va de pedidos a historial y no al revés (R9).
 */
public interface PedidosParaHistorial {

    /**
     * P-13 (provisional del equipo): la persona puede ver el pedido. Devuelve su número.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la cuenta está inactiva o sin roles
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el pedido no existe
     */
    String autorizarConsulta(Solicitante solicitante, UUID pedidoId);
}
