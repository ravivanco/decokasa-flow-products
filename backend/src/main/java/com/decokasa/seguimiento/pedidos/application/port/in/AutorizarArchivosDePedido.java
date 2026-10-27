package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import java.util.UUID;

/**
 * Reglas de pedidos que necesita el módulo de archivos: subir (P-06, P-18), ver (P-13) y descargar (P-17). El
 * adaptador {@code PedidosParaArchivosAdaptador} lo expone como el puerto de salida de archivos.
 */
public interface AutorizarArchivosDePedido {

    /** Bloquea el pedido (como una acción) y comprueba etapa, entrada actual y permiso. */
    AutorizacionSubida autorizarSubida(Solicitante solicitante, UUID pedidoId, int etapa, UUID entradaEsperadaId);

    void autorizarConsulta(Solicitante solicitante, UUID pedidoId);

    void autorizarDescarga(Solicitante solicitante, UUID pedidoId, int etapaDelArchivo);

    /**
     * ADR-0028: abrir cualquier documento del pedido desde la etapa actual (quien puede actuar en ella, o el admin).
     * Por defecto deniega.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si no puede
     */
    default void autorizarAperturaDesdeEtapaActual(Solicitante solicitante, UUID pedidoId) {
        throw new com.decokasa.seguimiento.compartido.domain.SinPermiso("No puedes abrir este archivo.");
    }

    record AutorizacionSubida(DatosDelPedido pedido, Interviniente actor, UUID entradaId) {
    }
}
