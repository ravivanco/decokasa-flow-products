package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;

/** Archivos de un pedido, por los puertos de entrada de archivos (R4). Solo del almacenamiento local: sin Drive. */
public interface DocumentosParaAsistente {

    /** Metadatos de los archivos de una etapa (ver el pedido basta; descargar no, P-17). */
    List<DocumentoPedido> deEtapa(Solicitante solicitante, UUID pedidoId, int etapa);

    /** P-17 vigente para ese archivo; false también si ya no existe o es de otro país. */
    boolean puedeDescargar(Solicitante solicitante, UUID archivoId);

    /**
     * Contenido verificado (tamaño y huella) del almacenamiento local, con el permiso de descarga comprobado de nuevo.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si ya no puede descargarlo
     * @throws com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible si falta, supera el límite o no coincide
     */
    byte[] leer(Solicitante solicitante, UUID archivoId, long limiteBytes);

    /** @param tipoDetectado tipo por el contenido (firma), nunca por el nombre; nulo si no se reconoce */
    record DocumentoPedido(UUID id, int etapa, String nombre, String tipoDetectado, String sha256, long tamanoBytes) {

        /** Sin el nombre original del archivo. */
        @Override
        public String toString() {
            return "DocumentoPedido[id=" + id + ", etapa=" + etapa + "]";
        }
    }
}
