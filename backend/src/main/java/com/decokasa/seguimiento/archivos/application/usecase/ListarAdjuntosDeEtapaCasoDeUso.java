package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Lista los archivos de una etapa (todas sus entradas) para quien puede ver el pedido (P-13). Sin Spring (R2). */
public final class ListarAdjuntosDeEtapaCasoDeUso implements ListarAdjuntosDeEtapa {

    private final PedidosParaArchivos pedidos;
    private final AdjuntoRepositorio adjuntos;
    private final ResumirUsuarios usuarios;

    public ListarAdjuntosDeEtapaCasoDeUso(PedidosParaArchivos pedidos, AdjuntoRepositorio adjuntos,
            ResumirUsuarios usuarios) {
        this.pedidos = pedidos;
        this.adjuntos = adjuntos;
        this.usuarios = usuarios;
    }

    @Override
    public List<AdjuntoVista> listar(Solicitante solicitante, UUID pedidoId, int etapa) {
        pedidos.autorizarConsulta(solicitante, pedidoId);
        List<Adjunto> lista = adjuntos.deEtapa(pedidoId, etapa);
        Map<UUID, UUID> eventos = adjuntos.eventosDeSubida(lista.stream().map(Adjunto::id).toList());
        Map<UUID, ResumirUsuarios.Resumen> personas = usuarios.resumenes(
                lista.stream().map(Adjunto::subidoPor).distinct().toList());
        return lista.stream()
                .map(adjunto -> new AdjuntoVista(adjunto, eventos.get(adjunto.id()),
                        PersonasDeArchivos.persona(adjunto.subidoPor(), personas.get(adjunto.subidoPor()))))
                .toList();
    }
}
