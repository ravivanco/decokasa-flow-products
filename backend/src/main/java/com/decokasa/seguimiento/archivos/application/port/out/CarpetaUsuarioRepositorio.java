package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Intenciones duraderas de crear la carpeta de Drive de cada usuario ({@code carpeta_usuario_drive}, V23; ADR-0026),
 * con el mismo patrón de lease, intentos y fencing que la cola de copias. Las actualizaciones son condicionales al lease
 * vigente: un trabajador que lo perdió no cambia nada.
 */
public interface CarpetaUsuarioRepositorio {

    /**
     * Registra la intención si no existe. Con {@code reabrirFallida}, una intención FALLIDA vuelve a PENDIENTE con los
     * intentos a cero; PENDIENTE y CREADA no cambian (idempotente).
     */
    void programar(UUID usuarioId, Origen origen, boolean reabrirFallida, Instant ahora);

    /** Reclama hasta {@code limite} intenciones listas, con {@code FOR UPDATE SKIP LOCKED}. */
    List<Pendiente> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario);

    boolean renovar(UUID usuarioId, UUID propietario, Instant ahora, Instant hasta);

    /** CREADA con el ID confirmado en Drive. */
    boolean confirmar(UUID usuarioId, UUID propietario, String driveId, Instant ahora);

    boolean registrarFallo(UUID usuarioId, UUID propietario, Causa causa, Instant siguiente, boolean terminal,
            Instant ahora);

    /** Estado de la carpeta de un usuario existente; vacío si el usuario no existe. */
    Optional<Estado> estado(UUID usuarioId);

    /** Estado de la carpeta de todos los usuarios, para la administración. */
    List<Estado> estados();

    enum Origen { ALTA, ADMIN }

    record Pendiente(UUID usuarioId, int intentos) { }

    /**
     * @param estado SIN_PROGRAMAR, PENDIENTE, CREADA o FALLIDA. CREADA también si la carpeta ya está confirmada en
     *        {@code carpeta_drive} (por ejemplo, la creó la copia de un documento del usuario)
     * @param ultimoError causa técnica del último fallo, sin detalles; nula si no hubo
     */
    record Estado(UUID usuarioId, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) { }
}
