package com.decokasa.seguimiento.asistente.adapter.out.modulos;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.asistente.application.port.out.DocumentosParaAsistente;
import com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Archivos desde los puertos de entrada de archivos: metadatos por etapa y descarga verificada (P-17, tamaño y huella)
 * del almacenamiento local. Nunca lee Drive (F7-D3: sin Google en el piloto).
 */
public final class DocumentosParaAsistenteAdaptador implements DocumentosParaAsistente {

    private static final String NO_DISPONIBLE = "El archivo no está disponible para el asistente.";

    private final ListarAdjuntosDeEtapa listar;
    private final DescargarArchivo descargar;

    public DocumentosParaAsistenteAdaptador(ListarAdjuntosDeEtapa listar, DescargarArchivo descargar) {
        this.listar = listar;
        this.descargar = descargar;
    }

    @Override
    public List<DocumentoPedido> deEtapa(Solicitante solicitante, UUID pedidoId, int etapa) {
        return listar.listar(solicitante, pedidoId, etapa).stream()
                .map(AdjuntoVista::adjunto)
                .map(DocumentosParaAsistenteAdaptador::documento)
                .toList();
    }

    @Override
    public boolean puedeDescargar(Solicitante solicitante, UUID archivoId) {
        try {
            descargar.autorizar(solicitante, archivoId);
            return true;
        } catch (SinPermiso | NoEncontrado sinAcceso) {
            return false;
        }
    }

    @Override
    public byte[] leer(Solicitante solicitante, UUID archivoId, long limiteBytes) {
        DescargarArchivo.Autorizada autorizada = descargar.autorizar(solicitante, archivoId);
        if (autorizada.tamanoBytes() > limiteBytes) {
            throw new ArchivoNoDisponible(NO_DISPONIBLE);
        }
        DescargarArchivo.Contenido contenido = descargar.abrir(autorizada);
        try (InputStream flujo = contenido.flujo()) {
            byte[] datos = flujo.readNBytes(Math.toIntExact(limiteBytes) + 1);
            if (datos.length > limiteBytes || datos.length != autorizada.tamanoBytes()) {
                throw new ArchivoNoDisponible(NO_DISPONIBLE);
            }
            return datos;
        } catch (IOException error) {
            throw new ArchivoNoDisponible(NO_DISPONIBLE);
        }
    }

    private static DocumentoPedido documento(Adjunto adjunto) {
        return new DocumentoPedido(adjunto.id(), adjunto.etapa(), adjunto.nombre(), adjunto.tipoDetectado(),
                adjunto.sha256(), adjunto.tamanoBytes());
    }
}
