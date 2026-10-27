package com.decokasa.seguimiento.asistente.domain;

import java.time.Duration;

/**
 * Configuración vigente del asistente en la base (configuracion_ia, V21). Los valores son los de F7-D2, aprobados por
 * Richard para el piloto; {@code esperaReserva} es el "tiempo de espera + margen" de ADR-0022 (propuesta del equipo).
 *
 * @param habilitada interruptor de la base: el apagado automático lo pone en false y lo reactiva un admin
 */
public record LimitesIA(boolean habilitada, String motivoDeshabilitada, int tokensEntradaMax, int tokensSalidaMax,
        int preguntasPorConversacion, int consultasUsuarioDia, int enCursoUsuario, int consultasDia,
        long presupuestoMesMicroUsd, Duration esperaReserva, long version) {
}
