package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.ArchivoRecibido;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos.AutorizacionSubida;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.VerificadorFormato;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.archivos.domain.EstadoCopia;
import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.eventos.ArchivoSubido;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.List;
import java.util.Objects;
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
 * Formatos (ADR-0026): la extensión y el tipo declarado se comprueban en {@link #prevalidar}, antes de reservar cuota y
 * de recibir bytes; el contenido técnico, en {@link #recibir}, sobre el archivo completo. Un rechazo responde 415
 * TIPO_ARCHIVO_NO_ACEPTADO y borra el temporal.
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
    private final VerificadorFormato verificador;

    /**
     * Solo para las pruebas anteriores al 2026-10-24: comprueba extensión y tipo declarado, pero no el contenido. La
     * configuración de la aplicación usa el constructor con {@link VerificadorFormato}.
     */
    public SubirArchivoCasoDeUso(PedidosParaArchivos pedidos, AlmacenLocal almacen, AdjuntoRepositorio adjuntos,
            PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite) {
        this(pedidos, almacen, adjuntos, publicador, usuarios, reloj, limite, (clave, formato) -> { });
    }

    public SubirArchivoCasoDeUso(PedidosParaArchivos pedidos, AlmacenLocal almacen, AdjuntoRepositorio adjuntos,
            PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite,
            VerificadorFormato verificador) {
        this.verificador = Objects.requireNonNull(verificador, "verificador");
        this.pedidos = pedidos;
        this.almacen = almacen;
        this.adjuntos = adjuntos;
        this.publicador = publicador;
        this.usuarios = usuarios;
        this.reloj = reloj;
        this.limite = limite;
    }

    public void prevalidar(Solicitante solicitante, SubirArchivo.Comando comando) {
        FormatoArchivo.desdeNombre(comando.nombreOriginal()).exigirTipoDeclarado(comando.tipoDeclarado());
        Adjunto.validarTipoDocumento(comando.tipoDocumento());
        pedidos.autorizarSubida(solicitante, comando.pedidoId(), comando.etapa(), comando.entradaEsperadaId());
    }

    /** Fuera de transacción. El tipo guardado es el MIME canónico del formato validado, no el que declaró el cliente. */
    public ArchivoRecibido recibir(SubirArchivo.Comando comando) {
        FormatoArchivo formato = FormatoArchivo.desdeNombre(comando.nombreOriginal());
        ArchivoRecibido recibido = almacen.recibir(comando.contenido(), limite.bytes());
        try {
            verificador.verificar(recibido.claveTemporal(), formato);
        } catch (RuntimeException rechazo) {
            almacen.descartar(recibido.claveTemporal());
            throw rechazo;
        }
        return new ArchivoRecibido(recibido.claveTemporal(), recibido.tamanoBytes(), recibido.sha256(), formato.mime());
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

    /**
     * ADR-0034: registra un documento que está únicamente en Drive y ya se confirmó íntegro (tamaño, SHA-256 de cada
     * fragmento y del total, verificación técnica). Mismo permiso, evento ARCHIVO_SUBIDO e historial que
     * {@link #registrar}; el adjunto nace COPIADO, sin archivo local.
     */
    public AdjuntoVista registrarEnDrive(Solicitante solicitante, SubirArchivo.Comando comando, long tamanoBytes,
            String sha256, String tipoDetectado, UUID adjuntoId,
            com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio.EnDrive drive) {
        AutorizacionSubida autorizacion = pedidos.autorizarSubida(solicitante, comando.pedidoId(), comando.etapa(),
                comando.entradaEsperadaId());
        Adjunto adjunto = new Adjunto(adjuntoId, comando.pedidoId(), comando.etapa(), autorizacion.entradaId(),
                comando.tipoDocumento(), comando.nombreOriginal(), tamanoBytes, sha256, tipoDetectado, null,
                EstadoCopia.COPIADO, autorizacion.actor().usuarioId(), reloj.ahora());
        adjuntos.insertarEnDrive(adjunto, drive);
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
