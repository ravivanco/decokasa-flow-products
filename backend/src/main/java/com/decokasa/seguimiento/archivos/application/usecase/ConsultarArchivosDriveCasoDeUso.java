package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.ConsultarArchivosDrive;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.net.URI;
import java.util.List;
import java.util.UUID;

public final class ConsultarArchivosDriveCasoDeUso implements ConsultarArchivosDrive {
    private final AdjuntoRepositorio adjuntos;
    private final ConsultaCopias copias;
    private final PedidosParaArchivos pedidos;
    private final ConsultarResponsables responsables;
    private final ResumirUsuarios usuarios;
    public ConsultarArchivosDriveCasoDeUso(AdjuntoRepositorio adjuntos,ConsultaCopias copias,
            PedidosParaArchivos pedidos,ConsultarResponsables responsables,ResumirUsuarios usuarios) {
        this.adjuntos=adjuntos;this.copias=copias;this.pedidos=pedidos;this.responsables=responsables;this.usuarios=usuarios;
    }
    public String enlace(Solicitante s,UUID id) {
        var a=adjuntos.porId(id).orElseThrow(()->new NoEncontrado("El archivo no existe."));
        pedidos.autorizarDescarga(s,a.pedidoId(),a.etapa()); // P-17 vigente, igual que contenido.
        String url=copias.enlaceConfirmado(id).orElseThrow(Pendiente::new);
        try {
            URI uri=URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"drive.google.com".equals(uri.getHost())
                    || uri.getUserInfo()!=null || uri.getPort()!=-1 || url.length()>2048) { throw new Pendiente(); }
        } catch (IllegalArgumentException error) { throw new Pendiente(); }
        return url; // Nunca construir un enlace a partir de un ID reservado.
    }
    public List<AdjuntoVista> fallidos(Solicitante s,int pagina,int tamano) {
        if (responsables.estado(s.usuarioId()).filter(u -> u.activo() && u.roles().contains(new CodigoRol("admin"))).isEmpty()) {
            throw new SinPermiso("Solo el admin vigente puede consultar copias fallidas.");
        }
        if (pagina<0 || tamano<1 || tamano>100) { throw new DatoInvalido("pagina","Paginación inválida."); }
        var ids=copias.fallidos(pagina,tamano);
        var lista=adjuntos.porIds(ids);
        var eventos=adjuntos.eventosDeSubida(ids);
        var personas=usuarios.resumenes(lista.stream().map(a->a.subidoPor()).distinct().toList());
        var porId=lista.stream().collect(java.util.stream.Collectors.toMap(a->a.id(),a->a));
        return ids.stream().filter(porId::containsKey).map(porId::get).map(a -> new AdjuntoVista(a,eventos.get(a.id()),
            PersonasDeArchivos.persona(a.subidoPor(),personas.get(a.subidoPor())))).toList();
    }
    private static final class Pendiente extends ErrorDeDominio {
        private static final long serialVersionUID=1L;
        Pendiente() { super(CodigoError.COPIA_PENDIENTE,"No hay enlace de Drive confirmado para este archivo."); }
    }
}
