package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import java.util.UUID;

/**
 * Lo que el módulo de archivos necesita de los pedidos. Lo implementa un adaptador de {@code pedidos}: la dependencia
 * va de pedidos a archivos y no al revés (R9). Las reglas de permisos viven en pedidos (P-06, P-13, P-17).
 */
public interface PedidosParaArchivos {

    /**
     * Bloquea el pedido y comprueba que la persona puede subir a la entrada actual de la etapa actual (P-06, P-18).
     *
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el pedido no existe
     * @throws com.decokasa.seguimiento.compartido.domain.EtapaCambio si la etapa o su entrada ya no son las actuales
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si no puede actuar en la etapa
     */
    AutorizacionSubida autorizarSubida(Solicitante solicitante, UUID pedidoId, int etapa, UUID entradaEsperadaId);

    /**
     * P-13: la persona puede ver el pedido.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si el pedido no existe
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la cuenta está inactiva o sin roles
     */
    void autorizarConsulta(Solicitante solicitante, UUID pedidoId);

    /**
     * P-17: la persona puede descargar un archivo de esa etapa del pedido.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si no puede
     */
    void autorizarDescarga(Solicitante solicitante, UUID pedidoId, int etapaDelArchivo);

    /** Pedido, rol con el que interviene la persona y entrada actual de la etapa. */
    record AutorizacionSubida(DatosDelPedido pedido, Interviniente actor, UUID entradaId) {
    }
}
