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
    MOTIVO_RETRASO_REGISTRADO,
    PEDIDO_FINALIZADO,
    /** BL, Packing Lists, envíos y decisiones de embarque (V32). */
    EMBARQUE_REGISTRADO,
    /** La etapa actual superó la fecha planificada al crear el pedido (V34). */
    PLAN_VENCIDO
}
