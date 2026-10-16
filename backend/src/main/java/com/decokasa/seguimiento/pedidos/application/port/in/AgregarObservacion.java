package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;

/**
 * Agrega una observación a la entrada actual de la etapa actual (contrato: POST /pedidos/{id}/observaciones). P-19,
 * provisional del equipo.
 */
public interface AgregarObservacion {

    /** @return el identificador del evento del historial */
    UUID agregar(Solicitante solicitante, Comando comando);

    /**
     * @param etapaEsperada etapa que la persona ve en pantalla
     * @param entradaEsperadaId entrada de esa etapa que la persona ve (P-18)
     * @param adjuntoIds archivos de la entrada actual; vacío si no hay
     */
    record Comando(UUID pedidoId, int etapaEsperada, UUID entradaEsperadaId, String texto, List<UUID> adjuntoIds) {

        public Comando {
            adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        }
    }
}
