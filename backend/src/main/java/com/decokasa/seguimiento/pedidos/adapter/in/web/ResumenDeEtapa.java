package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido.Cierre;
import com.decokasa.seguimiento.pedidos.domain.EstadoEtapa;
import java.time.format.DateTimeFormatter;

/**
 * Texto de {@code EtapaEnLinea.resumen} (contrato: string | null; requisito 4.2 #17). Regla provisional del equipo
 * (2026-10-16), pendiente del cliente:
 * <ul>
 * <li>resume la última entrada cerrada de la etapa; si la etapa está activa otra vez, empieza con "Entrada anterior";
 * <li>solo con datos del evento que la cerró: acción, persona (nombre vigente o, si no se resuelve, su identificador),
 * rol con el que intervino, fecha y hora de Ecuador, etapa de destino, observación y archivos presentados;
 * <li>la observación se recorta a 200 caracteres con "…"; completa queda en el historial;
 * <li>sin un evento de cierre, el resumen es nulo: no se rellena ni se deduce de otras etapas.
 * </ul>
 */
final class ResumenDeEtapa {

    static final int OBSERVACION_MAXIMA = 200;

    private static final DateTimeFormatter FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(Reloj.ZONA_ECUADOR);

    private ResumenDeEtapa() {
    }

    /** @return el texto, o nulo si la etapa no tiene una entrada cerrada que contar */
    static String texto(String tipoFlujo, EstadoEtapa estado, Cierre cierre) {
        if (cierre == null) {
            return null;
        }
        StringBuilder texto = new StringBuilder();
        if (estado == EstadoEtapa.ACTUAL) {
            texto.append("Entrada anterior: ");
        }
        texto.append(accion(cierre));
        String actor = actor(cierre);
        if (actor != null) {
            texto.append(" por ").append(actor);
        }
        texto.append(" el ").append(FECHA.format(cierre.ocurridoEn()));
        if (cierre.etapaDestino() != null) {
            texto.append(cierre.etapaDestino() < cierre.etapa() ? " · vuelve a " : " · pasa a ")
                    .append(tipoFlujo).append(cierre.etapaDestino());
        }
        texto.append('.');
        if (cierre.observacion() != null && !cierre.observacion().isBlank()) {
            texto.append(" Observación: «").append(recortar(cierre.observacion().strip())).append("».");
        }
        if (cierre.archivos() > 0) {
            texto.append(' ').append(cierre.archivos())
                    .append(cierre.archivos() == 1 ? " archivo presentado." : " archivos presentados.");
        }
        return texto.toString();
    }

    private static String accion(Cierre cierre) {
        return switch (cierre.tipo()) {
            case ENVIADA -> "Enviada";
            case APROBADA -> "Aprobada";
            case RECHAZADA -> "Rechazada";
            case DEVUELTA -> cierre.motivo() == MotivoDevolucion.PRODUCTO_NUEVO ? "Devuelta por producto nuevo"
                    : cierre.motivo() == MotivoDevolucion.ERROR ? "Devuelta por error" : "Devuelta";
        };
    }

    /** Nombre vigente o, si no se puede resolver, el identificador; el rol es el histórico. Nulo sin actor. */
    private static String actor(Cierre cierre) {
        if (cierre.actorId() == null) {
            return null;
        }
        String nombre = cierre.nombreActor() != null ? cierre.nombreActor() : cierre.actorId().toString();
        if (cierre.rol() == null) {
            return nombre;
        }
        String rol = cierre.nombreRol() != null ? cierre.nombreRol() : cierre.rol().valor();
        return nombre + " (" + rol + ")";
    }

    /** Recorta por puntos de código, para no partir un carácter formado por dos unidades UTF-16. */
    static String recortar(String observacion) {
        if (observacion.codePointCount(0, observacion.length()) <= OBSERVACION_MAXIMA) {
            return observacion;
        }
        return observacion.substring(0, observacion.offsetByCodePoints(0, OBSERVACION_MAXIMA)).stripTrailing() + "…";
    }
}
