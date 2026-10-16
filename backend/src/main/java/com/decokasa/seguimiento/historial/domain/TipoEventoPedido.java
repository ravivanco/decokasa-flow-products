package com.decokasa.seguimiento.historial.domain;

/**
 * Tipos de evento del historial (columna evento_pedido.tipo de V2; contrato: TipoEvento). En la fase 2 se registran
 * PEDIDO_CREADO, ETAPA_ENVIADA, ETAPA_APROBADA, ETAPA_RECHAZADA, PEDIDO_DEVUELTO y ETAPA_REASIGNADA; los demás llegan
 * en sus fases.
 */
public enum TipoEventoPedido {
    PEDIDO_CREADO,
    ETAPA_ENVIADA,
    ETAPA_APROBADA,
    ETAPA_RECHAZADA,
    PEDIDO_DEVUELTO,
    DEMORA_REPORTADA,
    BODEGA_ASIGNADA,
    OBSERVACION_AGREGADA,
    ARCHIVO_SUBIDO,
    ETAPA_REASIGNADA,
    PLAZO_VENCIDO,
    PEDIDO_FINALIZADO
}
