package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Intenciones duraderas de crear la carpeta de Drive de cada pedido ({@code carpeta_pedido_drive}, V24; ADR-0027), con
 * el mismo patrón de lease, intentos y fencing que la cola de copias y que {@link CarpetaUsuarioRepositorio}. Las
 * actualizaciones son condicionales al lease vigente: un trabajador que lo perdió no cambia nada.
 */
public interface CarpetaPedidoRepositorio {

    /**
     * Registra la intención si no existe. Con {@code reabrirFallida}, una intención FALLIDA vuelve a PENDIENTE con los
     * intentos a cero; PENDIENTE y CREADA no cambian (idempotente).
     */
    void programar(UUID pedidoId, Origen origen, boolean reabrirFallida, Instant ahora);

    /** Reclama hasta {@code limite} intenciones listas, con {@code FOR UPDATE SKIP LOCKED}. */
    List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario);

    boolean renovar(UUID pedidoId, UUID propietario, Instant ahora, Instant hasta);

    /** CREADA con el ID confirmado en Drive. */
    boolean confirmar(UUID pedidoId, UUID propietario, String driveId, Instant ahora);

    boolean registrarFallo(UUID pedidoId, UUID propietario, Causa causa, Instant siguiente, boolean terminal,
            Instant ahora);

    /** Estado de la carpeta de un pedido existente, con su país; vacío si el pedido no existe. */
    Optional<Estado> estado(UUID pedidoId);

    enum Origen { ALTA, ADMIN }

    record Pendiente(UUID pedidoId, int intentos) { }

    /**
     * @param estado SIN_PROGRAMAR, PENDIENTE, CREADA o FALLIDA. CREADA también si la carpeta ya está confirmada en
     *        {@code carpeta_drive} (por ejemplo, la creó la copia de un documento del pedido)
     * @param ultimoError causa técnica del último fallo, sin detalles; nula si no hubo
     */
    record Estado(UUID pedidoId, CodigoPais pais, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) { }
}
