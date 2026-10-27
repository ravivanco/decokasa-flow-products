package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.io.InputStream;
import java.util.UUID;

/** Sube un archivo real a la entrada actual de la etapa actual (contrato: POST /pedidos/{id}/etapas/{n}/archivos). */
public interface SubirArchivo {

    AdjuntoVista subir(Solicitante solicitante, Comando comando);

    /**
     * @param nombreOriginal para mostrar y para su extensión (formatos admitidos, ADR-0026)
     * @param contenido el archivo tal como llega; el servidor comprueba su contenido técnico
     * @param tipoDeclarado {@code Content-Type} que declaró el navegador; nulo si no hay. Solo debe ser coherente con la
     *        extensión: no decide el formato
     */
    record Comando(UUID pedidoId, int etapa, UUID entradaEsperadaId, String tipoDocumento, String nombreOriginal,
            InputStream contenido, String tipoDeclarado) {

        /** Sin tipo declarado (cargas reanudables y pruebas anteriores al 2026-10-24). */
        public Comando(UUID pedidoId, int etapa, UUID entradaEsperadaId, String tipoDocumento, String nombreOriginal,
                InputStream contenido) {
            this(pedidoId, etapa, entradaEsperadaId, tipoDocumento, nombreOriginal, contenido, null);
        }
    }
}
