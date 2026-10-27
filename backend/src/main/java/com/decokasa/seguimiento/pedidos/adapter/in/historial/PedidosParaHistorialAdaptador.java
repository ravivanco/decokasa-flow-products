package com.decokasa.seguimiento.pedidos.adapter.in.historial;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.historial.application.port.out.PedidosParaHistorial;
import com.decokasa.seguimiento.pedidos.application.port.in.VerificarConsultaDePedido;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Expone el permiso de consulta de pedidos (P-13) al historial; la dependencia va de pedidos a historial (R9). */
@Component
public class PedidosParaHistorialAdaptador implements PedidosParaHistorial {

    private final VerificarConsultaDePedido verificar;

    public PedidosParaHistorialAdaptador(VerificarConsultaDePedido verificar) {
        this.verificar = verificar;
    }

    @Override
    public String autorizarConsulta(Solicitante solicitante, UUID pedidoId) {
        return verificar.numeroVisible(solicitante, pedidoId);
    }
}
