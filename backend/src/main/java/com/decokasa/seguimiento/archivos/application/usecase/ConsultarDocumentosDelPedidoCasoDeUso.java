package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarDocumentosDelPedido;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lista los documentos de un pedido (ADR-0028). Ver exige P-13 (país vigente). {@code puedeAbrir}: P-17 sobre la etapa
 * del archivo o, para quien puede actuar en la etapa actual (o el admin), cualquier documento del pedido. La disponibilidad
 * se deduce del archivo local (solo su tamaño, sin leerlo) o de una copia confirmada; nunca de la existencia de una
 * carpeta. Sin Spring (R2).
 */
public final class ConsultarDocumentosDelPedidoCasoDeUso implements ConsultarDocumentosDelPedido {
    private final PedidosParaArchivos pedidos;
    private final DocumentosDePedido documentos;
    private final AlmacenLocal almacen;
    private final ResumirUsuarios usuarios;

    public ConsultarDocumentosDelPedidoCasoDeUso(PedidosParaArchivos pedidos, DocumentosDePedido documentos,
            AlmacenLocal almacen, ResumirUsuarios usuarios) {
        this.pedidos = pedidos;
        this.documentos = documentos;
        this.almacen = almacen;
        this.usuarios = usuarios;
    }

    @Override public List<Vista> listar(Solicitante solicitante, UUID pedidoId) {
        pedidos.autorizarConsulta(solicitante, pedidoId);
        List<DocumentosDePedido.Documento> lista = documentos.delPedido(pedidoId);
        boolean abreDesdeEtapaActual = permite(() -> pedidos.autorizarAperturaDesdeEtapaActual(solicitante, pedidoId));
        Map<Integer, Boolean> porEtapa = new HashMap<>();
        Set<UUID> autores = lista.stream().map(DocumentosDePedido.Documento::subidoPor).collect(Collectors.toSet());
        Map<UUID, ResumirUsuarios.Resumen> resumenes = usuarios.resumenes(autores);
        Set<UUID> vigentes = vigentes(lista, documentos.transicionesVigentes(pedidoId));
        return lista.stream().map(d -> {
            boolean p17 = porEtapa.computeIfAbsent(d.etapa(),
                    etapa -> permite(() -> pedidos.autorizarDescarga(solicitante, pedidoId, etapa)));
            boolean puedeAbrir = p17 || abreDesdeEtapaActual;
            var resumen = resumenes.get(d.subidoPor());
            Persona autor = resumen == null ? new Persona(d.subidoPor(), "Usuario", Set.of())
                    : new Persona(resumen.id(), resumen.nombre(), resumen.roles());
            return new Vista(d, autor, disponibilidad(d), puedeAbrir, vigentes.contains(d.id()));
        }).toList();
    }

    /**
     * ADR-0029: si la última transición es un rechazo o una devolución, solo lo presentado con ella; si no, lo presentado
     * en la última transición con documentos posterior al último rechazo o devolución. Nunca «el más reciente».
     */
    private static Set<UUID> vigentes(List<DocumentosDePedido.Documento> lista,
            List<DocumentosDePedido.Transicion> transiciones) {
        Map<UUID, Set<UUID>> porEvento = new HashMap<>();
        for (var d : lista) {
            for (var p : d.presentaciones()) { porEvento.computeIfAbsent(p.eventoId(), k -> new java.util.HashSet<>()).add(d.id()); }
        }
        for (int i = 0; i < transiciones.size(); i++) {
            var t = transiciones.get(i);
            boolean devuelve = t.tipo().equals("ETAPA_RECHAZADA") || t.tipo().equals("PEDIDO_DEVUELTO");
            if (devuelve) { return i == 0 ? porEvento.getOrDefault(t.eventoId(), Set.of()) : Set.of(); }
            Set<UUID> presentados = porEvento.get(t.eventoId());
            if (presentados != null && !presentados.isEmpty()) { return presentados; }
        }
        return Set.of();
    }

    private Disponibilidad disponibilidad(DocumentosDePedido.Documento d) {
        var tamano = almacen.tamano(d.claveLocal());
        if (tamano.isPresent() && tamano.getAsLong() == d.tamanoBytes()) { return Disponibilidad.LOCAL; }
        return d.driveId() != null ? Disponibilidad.DRIVE : Disponibilidad.NO_DISPONIBLE;
    }

    private static boolean permite(Runnable autorizacion) {
        try {
            autorizacion.run();
            return true;
        } catch (SinPermiso noPuede) {
            return false;
        }
    }
}
