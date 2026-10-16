package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import java.util.List;
import java.util.UUID;

/**
 * Ejecuta una acción sobre la etapa actual (contrato: POST /pedidos/{id}/acciones). La acción, su evento y la entrada
 * del historial se confirman juntos.
 *
 * <p>Documentos (P-05 opción A, P-18): solo cuentan los {@code adjuntoIds} de archivos reales subidos a la entrada
 * actual de la etapa. Un identificador inválido responde 400 y nunca se ignora; un tipo obligatorio que falta, 422.
 */
public interface EjecutarAccionEtapa {

    Pedido ejecutar(Solicitante solicitante, Comando comando);

    /**
     * @param etapaEsperada la etapa que la persona ve en pantalla; si ya no es la actual, 409 ETAPA_CAMBIO
     * @param adjuntoIds archivos que acompañan la acción; vacío si no hay
     */
    record Comando(UUID pedidoId, AccionEtapa accion, int etapaEsperada, String observacion,
            NivelUrgencia nivelUrgencia, List<UUID> adjuntoIds) {

        public Comando {
            adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
        }

        /** Sin archivos. */
        public Comando(UUID pedidoId, AccionEtapa accion, int etapaEsperada, String observacion,
                NivelUrgencia nivelUrgencia) {
            this(pedidoId, accion, etapaEsperada, observacion, nivelUrgencia, List.of());
        }
    }
}
