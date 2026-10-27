package com.decokasa.seguimiento.asistente.adapter.out.modulos;

import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.application.port.in.AutorizarArchivosDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.pedidos.application.port.in.VistasDePedido;
import java.util.UUID;

/**
 * Lectura del pedido (P-13 y país, ADR-0024) y permiso de descarga por etapa (P-17) desde los puertos de entrada de
 * pedidos. No copia ninguna regla: las decide pedidos con el estado vigente.
 */
public final class PedidosParaAsistenteAdaptador implements PedidosParaAsistente {

    private final ConsultarPedidos consultar;
    private final AutorizarArchivosDePedido autorizar;

    public PedidosParaAsistenteAdaptador(ConsultarPedidos consultar, AutorizarArchivosDePedido autorizar) {
        this.consultar = consultar;
        this.autorizar = autorizar;
    }

    @Override
    public DatosPedido leer(Solicitante solicitante, UUID pedidoId) {
        VistasDePedido.Detalle detalle = consultar.detalle(solicitante, pedidoId);
        VistasDePedido.Resumen resumen = detalle.resumen();
        return new DatosPedido(resumen.id(), resumen.numero(), resumen.pais(), resumen.tipoFlujo().name(),
                resumen.etapaActual(), resumen.nombreEtapaActual(), resumen.finalizado(), detalle.descripcion(),
                detalle.lineaDeTiempo().stream()
                        .map(etapa -> new EtapaPedido(etapa.numero(), etapa.nombre(), etapa.estado().name(),
                                etapa.entrada(), etapa.salida(), etapa.venceEn()))
                        .toList());
    }

    @Override
    public boolean puedeDescargarEtapa(Solicitante solicitante, UUID pedidoId, int etapa) {
        try {
            autorizar.autorizarDescarga(solicitante, pedidoId, etapa);
            return true;
        } catch (SinPermiso | NoEncontrado sinAcceso) {
            return false;
        }
    }
}
