package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.io.InputStream;
import java.util.UUID;

/** Sube un archivo real a la entrada actual de la etapa actual (contrato: POST /pedidos/{id}/etapas/{n}/archivos). */
public interface SubirArchivo {

    AdjuntoVista subir(Solicitante solicitante, Comando comando);

    /**
     * @param nombreOriginal solo para mostrar
     * @param contenido el archivo tal como llega; el servidor no confía en su tipo declarado
     */
    record Comando(UUID pedidoId, int etapa, UUID entradaEsperadaId, String tipoDocumento, String nombreOriginal,
            InputStream contenido) {
    }
}
