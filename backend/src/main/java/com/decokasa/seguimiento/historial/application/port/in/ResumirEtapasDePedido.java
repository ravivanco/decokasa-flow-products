package com.decokasa.seguimiento.historial.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Lo ocurrido al cerrar la última entrada de cada etapa (requisito 4.2 #17; regla provisional del equipo del
 * 2026-10-16, pendiente del cliente). Solo sale de eventos reales del historial: el evento con el que alguien sacó el
 * pedido de esa etapa (enviar, aprobar, rechazar o devolver). Una etapa que quedó atrás cuando el pedido se devolvió
 * desde otra no recibe ese evento: conserva el de su propio cierre. Sin control de acceso: lo usa pedidos después de
 * autorizar la consulta (P-13).
 */
public interface ResumirEtapasDePedido {

    /** @return el último cierre por número de etapa; una etapa sin cierre no aparece */
    Map<Integer, Cierre> ultimosCierres(UUID pedidoId);

    enum TipoCierre {
        ENVIADA,
        APROBADA,
        RECHAZADA,
        DEVUELTA
    }

    /**
     * @param actorId quien ejecutó la acción; nulo solo si el evento no tiene actor
     * @param nombreActor nombre vigente al consultar; nulo si ya no se puede resolver
     * @param rol rol con el que intervino en ese momento (histórico)
     * @param nombreRol nombre del rol en la tabla rol; nulo si ya no existe
     * @param motivo solo en una devolución
     * @param observacion completa, tal como quedó en el historial; nula si no hubo
     * @param archivos cantidad de archivos presentados con la acción
     */
    record Cierre(int etapa, TipoCierre tipo, UUID actorId, String nombreActor, CodigoRol rol, String nombreRol,
            Instant ocurridoEn, Integer etapaDestino, MotivoDevolucion motivo, String observacion, int archivos) {
    }
}
