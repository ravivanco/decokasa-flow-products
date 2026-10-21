package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import java.util.UUID;

/**
 * Quién actúa sobre una etapa y a quién se reasigna (R6: se decide en el servidor). Decisiones técnicas del equipo
 * aprobadas por Richard el 2026-10-15 (P-06, P-07); no son respuestas del cliente:
 * <ul>
 *   <li>sin persona asignada, actúa cualquier usuario activo con el rol responsable de la etapa, o el admin;</li>
 *   <li>con persona asignada, solo ella (activa y con el rol responsable) o el admin;</li>
 *   <li>si la persona asignada pierde el rol o se desactiva, solo el admin actúa o reasigna: el resto del rol no gana
 *       permisos de forma automática;</li>
 *   <li>reasignación provisional: solo a personas activas con el rol responsable de la etapa. Si se puede reasignar a
 *       otro rol sigue pendiente del cliente (⏸, arquitectura 8.1).</li>
 * </ul>
 * Todo se decide con el {@link ActorVigente}, el estado actual en la base. El admin cumple las mismas validaciones de
 * la acción (observación, urgencia, documentos): este permiso solo dice quién puede pedirla. Java puro (R1).
 */
public final class PermisosEtapa {

    public static final CodigoRol ADMIN = new CodigoRol("admin");

    private PermisosEtapa() {
    }

    /**
     * Rol con el que la persona interviene en la etapa: el responsable o, si no lo es, {@code admin}.
     *
     * @param rolResponsable rol responsable de la etapa; nulo si es automática
     * @throws TransicionNoPermitida si la etapa es automática: nadie actúa en ella
     * @throws SinPermiso si la persona no puede actuar en la etapa
     */
    public static Interviniente paraActuar(EtapaDePedido etapa, CodigoRol rolResponsable, ActorVigente actor) {
        if (rolResponsable == null) {
            throw new TransicionNoPermitida("Esta etapa avanza sola; nadie actúa en ella.");
        }
        exigirCuentaVigente(actor);
        UUID asignado = etapa.asignadoA();
        boolean libreOAsignadaAEsaPersona = asignado == null || asignado.equals(actor.usuarioId());
        if (libreOAsignadaAEsaPersona && actor.tieneRol(rolResponsable)) {
            return new Interviniente(actor.usuarioId(), rolResponsable);
        }
        if (actor.tieneRol(ADMIN)) {
            return new Interviniente(actor.usuarioId(), ADMIN);
        }
        if (asignado != null && actor.tieneRol(rolResponsable)) {
            throw new SinPermiso("Esta etapa está asignada a otra persona: solo ella o el admin pueden actuar.");
        }
        throw new SinPermiso("Solo el responsable de la etapa o el admin pueden actuar en ella.");
    }

    /** Indica si {@link #paraActuar} aceptaría a la persona (para mostrar las acciones disponibles, paso 2.4). */
    public static boolean puedeActuar(EtapaDePedido etapa, CodigoRol rolResponsable, ActorVigente actor) {
        try {
            paraActuar(etapa, rolResponsable, actor);
            return true;
        } catch (SinPermiso | TransicionNoPermitida noPuede) {
            return false;
        }
    }

    /**
     * Quién crea un pedido: solo quien tiene, vigente en la base, el rol responsable de la primera etapa de la
     * definición del flujo (requisito 4.2 #4). En el flujo A sembrado ese rol es {@code admin} (A1); en el B,
     * {@code compras_china} (B1). A diferencia de actuar en una etapa, el admin no crea por ser admin: si también lo
     * hace en el flujo B sigue pendiente del cliente (P-14, F6-D2). Un admin que además tenga Compras China crea B por
     * ese rol. La regla sale de los datos del flujo, no del tipo A o B.
     */
    public static Interviniente paraCrear(CodigoRol rolResponsableInicial, ActorVigente actor) {
        exigirCuentaVigente(actor);
        if (actor.tieneRol(rolResponsableInicial)) {
            return new Interviniente(actor.usuarioId(), rolResponsableInicial);
        }
        throw new SinPermiso("Solo el responsable de la primera etapa del flujo crea pedidos de ese flujo.");
    }

    /**
     * Solo el admin vigente reasigna (respuesta del cliente: "El admin puede actuar en cualquier etapa o
     * reasignarla").
     */
    static Interviniente paraReasignar(ActorVigente actor) {
        exigirCuentaVigente(actor);
        if (!actor.tieneRol(ADMIN)) {
            throw new SinPermiso("Solo el admin reasigna etapas.");
        }
        return new Interviniente(actor.usuarioId(), ADMIN);
    }

    /**
     * Valor provisional del equipo (P-07): la persona que recibe la etapa está activa y tiene el rol responsable.
     *
     * @throws DatoInvalido si no cumple (400 en la API)
     */
    static void exigirAsignable(CodigoRol rolResponsable, ActorVigente candidato) {
        if (candidato == null || !candidato.puedeActuar() || !candidato.tieneRol(rolResponsable)) {
            throw new DatoInvalido("usuarioId",
                    "La etapa solo se reasigna a una persona activa con el rol responsable de la etapa.");
        }
    }

    private static void exigirCuentaVigente(ActorVigente actor) {
        if (actor == null || !actor.puedeActuar()) {
            throw new SinPermiso("Tu cuenta no está activa o no tiene roles.");
        }
    }
}
