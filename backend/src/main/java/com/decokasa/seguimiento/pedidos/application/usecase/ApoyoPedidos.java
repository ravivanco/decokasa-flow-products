package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.Vencimientos;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Piezas comunes de los casos de uso de pedidos: el estado vigente de las personas (usuarios, por su puerto), los
 * vencimientos (plazos, por su puerto) y la publicación de eventos. Sin Spring (R2).
 */
final class ApoyoPedidos {

    /** Mismo mensaje que un pedido inexistente: un pedido de otro país no revela que existe (ADR-0024). */
    static final String PEDIDO_NO_EXISTE = "El pedido no existe.";

    private final ConsultarResponsables responsables;
    private final CalcularVencimiento vencimientos;
    private final PublicadorEventos publicador;

    ApoyoPedidos(ConsultarResponsables responsables, CalcularVencimiento vencimientos, PublicadorEventos publicador) {
        this.responsables = responsables;
        this.vencimientos = vencimientos;
        this.publicador = publicador;
    }

    /**
     * Estado actual en la base de quien pide la operación, nunca los roles del token (lección A1).
     *
     * @throws SinPermiso si no hay solicitante o su usuario no existe
     */
    ActorVigente solicitante(Solicitante solicitante) {
        if (solicitante == null || solicitante.usuarioId() == null) {
            throw new SinPermiso("Inicia sesión para continuar.");
        }
        return persona(solicitante.usuarioId())
                .orElseThrow(() -> new SinPermiso("Tu cuenta no está activa o no tiene roles."));
    }

    /**
     * P-13 (provisional del equipo): solo una persona activa con al menos un rol vigente consulta pedidos.
     *
     * @throws SinPermiso si la cuenta está inactiva o sin roles
     */
    ActorVigente lector(Solicitante solicitante) {
        ActorVigente actor = solicitante(solicitante);
        if (!actor.puedeActuar()) {
            throw new SinPermiso("Tu cuenta no está activa o no tiene roles.");
        }
        return actor;
    }

    /**
     * ADR-0024: un pedido de un país que la persona no tiene asignado hoy no existe para ella (404, como un pedido
     * inexistente), tampoco para el admin. Se comprueba después de la cuenta (P-13) y antes de cualquier otra regla.
     *
     * @throws NoEncontrado si el país del pedido no está entre los asignados
     */
    void exigirPais(ActorVigente actor, Pedido pedido) {
        if (!actor.veePais(pedido.pais())) {
            throw new NoEncontrado(PEDIDO_NO_EXISTE);
        }
    }

    /** Estado actual de cualquier persona; vacío si no existe. */
    Optional<ActorVigente> persona(UUID usuarioId) {
        return responsables.estado(usuarioId)
                .map(estado -> new ActorVigente(estado.usuarioId(), estado.activo(), estado.roles(), estado.paises()));
    }

    /** Vencimientos con el calendario del país del pedido (fase 7: configuración separada por país). */
    Vencimientos vencimientos(CodigoPais pais) {
        CalcularVencimiento delPais = vencimientos.paraPais(pais);
        return new Vencimientos() {
            @Override
            public Optional<Instant> deEtapa(Instant entrada, PlazoDeEtapa plazo) {
                return delPais.deEtapa(entrada, plazo);
            }

            @Override
            public Instant deUrgencia(Instant activada, NivelUrgencia nivel) {
                return delPais.deUrgencia(activada, nivel);
            }
        };
    }

    /**
     * Publica los eventos pendientes del pedido después de guardarlo y dentro de la misma transacción: el historial
     * los inserta en ella, así que acción e historial se confirman o se revierten juntos (R7).
     */
    void publicarEventos(Pedido pedido) {
        pedido.retirarEventos().forEach(publicador::publicar);
    }
}
