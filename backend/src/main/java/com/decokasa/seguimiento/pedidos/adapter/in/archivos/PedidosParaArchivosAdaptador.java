package com.decokasa.seguimiento.pedidos.adapter.in.archivos;

import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Expone las reglas de pedidos al módulo de archivos a través del puerto de salida de archivos. Vive en pedidos para
 * que la dependencia vaya de pedidos a archivos y nunca al revés (R9).
 */
@Component
public class PedidosParaArchivosAdaptador implements PedidosParaArchivos {

    private final AutorizarArchivosDePedido autorizar;

    public PedidosParaArchivosAdaptador(AutorizarArchivosDePedido autorizar) {
        this.autorizar = autorizar;
    }

    @Override
    public AutorizacionSubida autorizarSubida(Solicitante solicitante, UUID pedidoId, int etapa,
            UUID entradaEsperadaId) {
        AutorizarArchivosDePedido.AutorizacionSubida autorizacion = autorizar.autorizarSubida(solicitante, pedidoId,
                etapa, entradaEsperadaId);
        return new AutorizacionSubida(autorizacion.pedido(), autorizacion.actor(), autorizacion.entradaId());
    }

    @Override
    public void autorizarConsulta(Solicitante solicitante, UUID pedidoId) {
        autorizar.autorizarConsulta(solicitante, pedidoId);
    }

    @Override
    public void autorizarDescarga(Solicitante solicitante, UUID pedidoId, int etapaDelArchivo) {
        autorizar.autorizarDescarga(solicitante, pedidoId, etapaDelArchivo);
    }

    @Override
    public void autorizarAperturaDesdeEtapaActual(Solicitante solicitante, UUID pedidoId) {
        autorizar.autorizarAperturaDesdeEtapaActual(solicitante, pedidoId);
    }
}
