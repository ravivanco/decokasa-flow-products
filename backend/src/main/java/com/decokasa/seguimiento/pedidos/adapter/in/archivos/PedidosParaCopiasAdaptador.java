package com.decokasa.seguimiento.pedidos.adapter.in.archivos;

import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Identidad y fecha de creación de un pedido ya aceptado (ADR-0026). Dirección pedidos -> archivos, igual al adaptador de fase 2. */
@Component
@Transactional(propagation=Propagation.MANDATORY)
public class PedidosParaCopiasAdaptador implements PedidosParaCopias {
    private final PedidoRepositorio pedidos;
    public PedidosParaCopiasAdaptador(PedidoRepositorio pedidos) { this.pedidos=pedidos; }
    @Override public Destino destino(UUID id) {
        var pedido=pedidos.buscarParaActualizar(id).orElseThrow(() -> new FalloIntegracionGoogle(Causa.CONFIGURACION));
        return new Destino(pedido.pais(),pedido.numero().valor(),pedido.creadoEn());
    }
}
