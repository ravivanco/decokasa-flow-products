package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Controles de consumo del asistente (ADR-0022; V21). Cada método es una transacción corta propia: la llamada al
 * proveedor ocurre entre {@link #reservar} y su cierre, siempre fuera de una transacción (ADR-0012).
 */
public interface PresupuestoIA {

    /** configuracion_ia vigente. */
    LimitesIA limites();

    /**
     * La tarifa más reciente del modelo con {@code vigente_desde <= dia}, validada o no. Si la más reciente no está
     * validada, el llamador no debe usar una anterior: falla cerrado.
     */
    Optional<Tarifa> tarifaVigente(String modelo, LocalDate dia);

    /**
     * Comprobación previa, **sin reservar**, con los mismos límites aprobados: evita llamar a count_tokens cuando la
     * reserva ya no podría concederse (consulta en curso, consultas del usuario o del día agotadas, presupuesto del mes
     * agotado). No sustituye a {@link #reservar}, que vuelve a comprobarlo todo de forma atómica.
     *
     * @throws com.decokasa.seguimiento.asistente.domain.LimiteConsumo si ya no cabe otra consulta
     */
    void comprobarDisponibilidad(UUID usuarioId, LocalDate dia);

    /**
     * Reserva atómica del peor caso, en una transacción con las filas bloqueadas en orden fijo (presupuesto del mes →
     * uso del día → uso del usuario). Cuenta la consulta del usuario y del día y marca una en curso.
     *
     * @param dia día de Ecuador de la consulta; el mes del presupuesto sale de él
     * @throws com.decokasa.seguimiento.asistente.domain.LimiteConsumo si se supera cualquier límite (nada cambia)
     * @throws com.decokasa.seguimiento.asistente.domain.IaNoDisponible si la IA quedó deshabilitada en la base
     */
    Reserva reservar(UUID usuarioId, long maximoMicroUsd, LocalDate dia, Instant ahora);

    /** Con el costo real del {@code usage}: suma lo consumido y libera la reserva. */
    void liquidar(Reserva reserva, long costoMicroUsd, Instant ahora);

    /** Rechazo confirmado sin cobro: se devuelve todo, también la consulta contada. */
    void liberar(Reserva reserva, Instant ahora);

    /** Resultado dudoso: la reserva entera cuenta como consumida (criterio conservador). */
    void darPorConsumida(Reserva reserva, Instant ahora);

    /**
     * Da por consumidas las reservas abiertas más allá de {@link LimitesIA#esperaReserva()} (caída del servidor a mitad
     * de una consulta). Una liquidación tardía de esas reservas ya no cambia nada.
     *
     * @return reservas cerradas
     */
    int recuperarAbandonadas(Instant ahora);

    /** Apagado automático ante el límite de gasto del proveedor; lo reactiva un admin. */
    void desactivar(String motivo, Instant ahora);

    /** @return false si la versión ya no es la vigente */
    boolean cambiarHabilitada(boolean habilitada, String motivo, long version, UUID actualizadoPor, Instant ahora);

    record Reserva(UUID id, UUID usuarioId, LocalDate dia, LocalDate periodo, long maximoMicroUsd) {
    }
}
