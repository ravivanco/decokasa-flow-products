package com.decokasa.seguimiento.archivos.application.port.out;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lecturas de los documentos de un pedido para consultarlos desde DECOKASA (ADR-0028): adjuntos con su entrada, las
 * transiciones en las que se presentaron ({@code evento_adjunto} PRESENTADO) y el ID de Drive solo si la copia está
 * confirmada. Nunca sale de este módulo hacia el navegador el ID de Drive, la clave local ni la huella.
 */
public interface DocumentosDePedido {

    /** Todos los adjuntos del pedido, del más antiguo al más reciente. */
    List<Documento> delPedido(UUID pedidoId);

    /**
     * Transiciones del pedido (envío, aprobación, rechazo, devolución), de la más reciente a la más antigua, para decidir
     * qué documentos son vigentes (ADR-0029). Vacía si el pedido terminó.
     */
    List<Transicion> transicionesVigentes(UUID pedidoId);

    /** ID de Drive persistido de un adjunto COPIADO; vacío si la copia no está confirmada. */
    Optional<String> driveIdCopiado(UUID adjuntoId);

    /**
     * @param tipoDetectado MIME canónico validado al subir (ADR-0026), o el registrado antes; puede ser nulo
     * @param driveId ID de Drive solo con estado COPIADO; nulo en otro caso
     * @param entradaEn inicio de la entrada de la etapa a la que se subió (cada paso por la etapa es una entrada)
     */
    record Documento(UUID id, UUID pedidoId, int etapa, UUID entradaId, Instant entradaEn, String nombre,
            String tipoDocumento, String tipoDetectado, long tamanoBytes, String claveLocal, String estadoCopia,
            String driveId, UUID subidoPor, Instant subidoEn, List<Presentacion> presentaciones) {
        public Documento {
            presentaciones = List.copyOf(presentaciones);
        }
    }

    record Transicion(UUID eventoId, String tipo) { }

    /** Transición en la que se presentó el documento. */
    record Presentacion(UUID eventoId, String tipo, int etapa, Instant fecha) { }
}
