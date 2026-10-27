package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;

/** Metadatos de los archivos de una etapa, de todas sus entradas (P-13; ver no autoriza a descargar, P-17). */
public interface ListarAdjuntosDeEtapa {

    List<AdjuntoVista> listar(Solicitante solicitante, UUID pedidoId, int etapa);
}
