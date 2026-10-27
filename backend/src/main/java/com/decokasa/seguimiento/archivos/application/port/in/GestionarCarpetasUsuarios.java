package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Carpeta de Drive de cada usuario de DECOKASA (ADR-0026): programarla al darlo de alta, procesar las pendientes y, para
 * el admin vigente, consultar su estado y programarla o reintentarla. La existencia de una carpeta no da acceso a nada:
 * los documentos siguen autorizándose por usuario, país y permisos vigentes.
 */
public interface GestionarCarpetasUsuarios {

    /** Solo el admin vigente en la base. */
    List<Estado> listar(Solicitante solicitante);

    /** Solo el admin vigente; 404 si el usuario no existe. */
    Estado estado(Solicitante solicitante, UUID usuarioId);

    /**
     * Programa la carpeta de un usuario existente o reintenta una FALLIDA. Idempotente: una PENDIENTE o CREADA no
     * cambia. No llama a Google; la procesa la tarea de copias cuando la integración está habilitada.
     */
    Estado programar(Solicitante solicitante, UUID usuarioId);

    /**
     * @param estado SIN_PROGRAMAR, PENDIENTE, CREADA o FALLIDA
     * @param ultimoError causa técnica del último fallo (por ejemplo CUOTA), sin detalles; nula si no hubo
     */
    record Estado(UUID usuarioId, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) { }

    /** Lo usa el oyente del alta, dentro de su transacción (ADR-0026). */
    @FunctionalInterface
    interface ProgramarAlta {
        void programarAlta(UUID usuarioId, Instant ocurridoEn);
    }

    /** Lo usa la tarea programada de copias: procesa un lote de intenciones; 0 si la integración está apagada. */
    @FunctionalInterface
    interface Procesar {
        int ejecutar();
    }
}
