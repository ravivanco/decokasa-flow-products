package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.UUID;

/**
 * Carpeta de Drive de cada pedido (ADR-0027): programarla al crear el pedido, procesar las pendientes y, para el admin
 * vigente con el país del pedido, consultar su estado y programarla o reintentarla. La existencia de una carpeta no da
 * acceso a nada: los documentos siguen autorizándose por usuario, país y permisos vigentes.
 */
public interface GestionarCarpetasPedidos {

    /** Solo el admin vigente en la base con el país del pedido; 404 si no existe o no es de sus países. */
    Estado estado(Solicitante solicitante, UUID pedidoId);

    /**
     * Programa la carpeta de un pedido existente o reintenta una FALLIDA. Idempotente: una PENDIENTE o CREADA no cambia.
     * No llama a Google; la procesa la tarea de copias cuando la integración está habilitada.
     */
    Estado programar(Solicitante solicitante, UUID pedidoId);

    /**
     * @param estado SIN_PROGRAMAR, PENDIENTE, CREADA o FALLIDA
     * @param ultimoError causa técnica del último fallo (por ejemplo CUOTA), sin detalles; nula si no hubo
     */
    record Estado(UUID pedidoId, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) { }

    /** Lo usa el oyente del alta del pedido, dentro de su transacción (ADR-0027). */
    @FunctionalInterface
    interface ProgramarAlta {
        void programarAlta(UUID pedidoId, Instant ocurridoEn);
    }

    /** Lo usa la tarea programada de copias: procesa un lote de intenciones; 0 si la integración está apagada. */
    @FunctionalInterface
    interface Procesar {
        int ejecutar();
    }
}
