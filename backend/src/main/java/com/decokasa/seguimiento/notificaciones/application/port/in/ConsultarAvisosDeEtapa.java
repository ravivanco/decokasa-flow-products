package com.decokasa.seguimiento.notificaciones.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.domain.AvisoEtapa;
import java.time.Instant;
import java.util.List;

/**
 * Avisos de etapa de la persona (ADR-0032): los mismos en la campana y en Mi bandeja. Solo de los países que tiene
 * asignados hoy (ADR-0024).
 */
public interface ConsultarAvisosDeEtapa {

    /** Avisos de los últimos 30 días (como máximo 50), del más reciente al más antiguo, y cuántos no ha leído. */
    Avisos avisos(Solicitante solicitante);

    /** Marca como leídos los avisos hasta la hora del servidor y la devuelve. */
    Instant marcarLeidos(Solicitante solicitante);

    record Aviso(AvisoEtapa aviso, boolean leido) {
    }

    record Avisos(List<Aviso> avisos, int noLeidos, Instant leidasHasta) {
        public Avisos {
            avisos = List.copyOf(avisos);
        }
    }
}
