package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente.EstadoCuenta;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.RolesAsistente;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.UUID;

/**
 * Comprobaciones comunes (ADR-0021), siempre con el estado vigente en la base y nunca con los roles del token:
 * conversación propia y vigente (404), cuenta activa y rol habilitado (403), país asignado hoy (404).
 */
final class AccesoAsistente {

    static final String CONVERSACION_NO_EXISTE = "La conversación no existe.";
    static final CodigoRol ADMIN = new CodigoRol("admin");

    private final CuentasParaAsistente cuentas;
    private final ConversacionesIA conversaciones;

    AccesoAsistente(CuentasParaAsistente cuentas, ConversacionesIA conversaciones) {
        this.cuentas = cuentas;
        this.conversaciones = conversaciones;
    }

    EstadoCuenta habilitado(Solicitante solicitante) {
        if (solicitante == null || solicitante.usuarioId() == null) {
            throw new SinPermiso("Inicia sesión para continuar.");
        }
        EstadoCuenta estado = cuentas.estado(solicitante.usuarioId()).filter(EstadoCuenta::activa)
                .orElseThrow(() -> new SinPermiso("Tu cuenta no está activa o no tiene roles."));
        if (!RolesAsistente.habilita(estado.roles())) {
            throw new SinPermiso("Tu rol no tiene acceso al asistente.");
        }
        return estado;
    }

    /** Admin vigente: cuenta activa con el rol admin en la base. */
    EstadoCuenta admin(Solicitante solicitante) {
        if (solicitante == null || solicitante.usuarioId() == null) {
            throw new SinPermiso("Inicia sesión para continuar.");
        }
        EstadoCuenta estado = cuentas.estado(solicitante.usuarioId()).filter(EstadoCuenta::activa)
                .orElseThrow(() -> new SinPermiso("Tu cuenta no está activa o no tiene roles."));
        if (!estado.roles().contains(ADMIN)) {
            throw new SinPermiso("Solo un administrador puede ver o cambiar la configuración del asistente.");
        }
        return estado;
    }

    /**
     * Primero la propiedad (una conversación ajena es 404 para cualquiera, sin revelar que existe), después la cuenta y
     * el rol, y al final el país de la conversación.
     */
    Vigente vigente(Solicitante solicitante, UUID conversacionId, Instant ahora) {
        if (solicitante == null || solicitante.usuarioId() == null) {
            throw new SinPermiso("Inicia sesión para continuar.");
        }
        ConversacionIA conversacion = conversacionId == null ? null : conversaciones.porId(conversacionId)
                .filter(c -> c.esDe(solicitante.usuarioId()) && c.vigente(ahora)).orElse(null);
        if (conversacion == null) {
            throw new NoEncontrado(CONVERSACION_NO_EXISTE);
        }
        EstadoCuenta estado = habilitado(solicitante);
        if (!estado.paises().contains(conversacion.pais())) {
            throw new NoEncontrado(CONVERSACION_NO_EXISTE);
        }
        return new Vigente(conversacion, estado);
    }

    record Vigente(ConversacionIA conversacion, EstadoCuenta cuenta) {
    }
}
