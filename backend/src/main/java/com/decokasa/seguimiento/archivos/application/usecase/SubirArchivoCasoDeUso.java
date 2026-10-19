package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.ArchivoRecibido;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos.AutorizacionSubida;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.archivos.domain.EstadoCopia;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.ArchivoSubido;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.List;
import java.util.UUID;

/**
 * Pasos de la subida (P-05 opción A). La configuración los ordena (ADR-0012):
 * <ol>
 *   <li>{@link #prevalidar}: en una transacción corta, permiso y entrada actual, antes de copiar nada;</li>
 *   <li>{@link #recibir}: fuera de la transacción, copia lo recibido a un temporal midiendo tamaño y huella;</li>
 *   <li>{@link #registrar}: en la transacción de la subida, bloquea el pedido, revalida, mueve el archivo a su clave
 *       definitiva, inserta el adjunto y publica ARCHIVO_SUBIDO (el historial lo inserta en la misma transacción);</li>
 *   <li>{@link #descartar}: si algo falla, borra el temporal y el definitivo.</li>
 * </ol>
 * El adjunto solo se confirma con el archivo ya escrito en disco. Si el proceso se cae entre mover el archivo y
 * confirmar, queda un archivo sin fila (nunca una fila sin archivo); su limpieza es de la fase 3. Sin Spring (R2).
 */
public final class SubirArchivoCasoDeUso {

    private final PedidosParaArchivos pedidos;
    private final AlmacenLocal almacen;
    private final AdjuntoRepositorio adjuntos;
    private final PublicadorEventos publicador;
    private final ResumirUsuarios usuarios;
    private final Reloj reloj;
    private final LimiteDeTamano limite;

    public SubirArchivoCasoDeUso(PedidosParaArchivos pedidos, AlmacenLocal almacen, AdjuntoRepositorio adjuntos,
            PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite) {
        this.pedidos = pedidos;
        this.almacen = almacen;
        this.adjuntos = adjuntos;
        this.publicador = publicador;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.limite = limite;
    }

    public void prevalidar(Solicitante solicitante, SubirArchivo.Comando comando) {
        Adjunto.validarTipoDocumento(comando.tipoDocumento());
        pedidos.autorizarSubida(solicitante, comando.pedidoId(), comando.etapa(), comando.entradaEsperadaId());
    }

    public ArchivoRecibido recibir(SubirArchivo.Comando comando) {
        return almacen.recibir(comando.contenido(), limite.bytes());
    }

    public AdjuntoVista registrar(Solicitante solicitante, SubirArchivo.Comando comando, ArchivoRecibido recibido,
            UUID adjuntoId) {
        AutorizacionSubida autorizacion = pedidos.autorizarSubida(solicitante, comando.pedidoId(), comando.etapa(),
                comando.entradaEsperadaId());
        String claveFinal = claveFinal(comando, adjuntoId);
        almacen.confirmar(recibido, claveFinal);
        Adjunto adjunto = new Adjunto(adjuntoId, comando.pedidoId(), comando.etapa(), autorizacion.entradaId(),
                comando.tipoDocumento(), comando.nombreOriginal(), recibido.tamanoBytes(), recibido.sha256(),
                recibido.tipoDetectado(), claveFinal, EstadoCopia.PENDIENTE, autorizacion.actor().usuarioId(),
                reloj.ahora());
        adjuntos.insertar(adjunto);
        publicador.publicar(new ArchivoSubido(autorizacion.pedido(), adjunto.etapa(), autorizacion.actor(),
                adjunto.subidoEn(), adjunto.id(), adjunto.entradaId(), adjunto.tipoDocumento()));
        UUID eventoId = adjuntos.eventosDeSubida(List.of(adjunto.id())).get(adjunto.id());
        UUID subidoPor = adjunto.subidoPor();
        return new AdjuntoVista(adjunto, eventoId,
                PersonasDeArchivos.persona(subidoPor, usuarios.resumenes(List.of(subidoPor)).get(subidoPor)));
    }

    public void descartar(SubirArchivo.Comando comando, ArchivoRecibido recibido, UUID adjuntoId) {
        if (recibido != null) {
            almacen.descartar(recibido.claveTemporal());
        }
        almacen.descartar(claveFinal(comando, adjuntoId));
    }

    /** Generada por el servidor; nunca lleva el nombre original. */
    public static String claveFinal(SubirArchivo.Comando comando, UUID adjuntoId) {
        return comando.pedidoId() + "/" + comando.etapa() + "/" + adjuntoId;
    }
}
