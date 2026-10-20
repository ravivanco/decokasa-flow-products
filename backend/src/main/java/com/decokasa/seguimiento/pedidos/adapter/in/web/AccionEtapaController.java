package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.pedidos.application.port.in.EjecutarAccionEtapa;
import com.decokasa.seguimiento.pedidos.application.port.in.ReasignarEtapa;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Acciones sobre la etapa actual (contrato: POST /pedidos/{id}/acciones) y reasignación (PUT …/asignacion). La
 * acción, su evento y el historial se confirman juntos; la respuesta es el pedido leído después del commit. Dos
 * avances simultáneos: el segundo recibe 409 ETAPA_CAMBIO (P-11).
 */
@RestController
public class AccionEtapaController {

    private final EjecutarAccionEtapa ejecutar;
    private final ReasignarEtapa reasignar;
    private final ConsultarPedidos consultar;

    public AccionEtapaController(EjecutarAccionEtapa ejecutar, ReasignarEtapa reasignar, ConsultarPedidos consultar) {
        this.ejecutar = ejecutar;
        this.reasignar = reasignar;
        this.consultar = consultar;
    }

    @PostMapping("/api/v1/pedidos/{pedidoId}/acciones")
    public PedidoRespuestas.Detalle ejecutar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @Valid @RequestBody SolicitudAccion solicitud) {
        Solicitante solicitante = SolicitantesWeb.de(token);
        ejecutar.ejecutar(solicitante, new EjecutarAccionEtapa.Comando(pedidoId, solicitud.accion(),
                solicitud.etapaEsperada(), solicitud.observacion(), solicitud.nivelUrgencia(), solicitud.adjuntoIds(),
                solicitud.nuevaFechaEstimadaLlegada(), solicitud.bodegaId()));
        return PedidoRespuestas.de(consultar.detalle(solicitante, pedidoId));
    }

    /**
     * Solo el admin vigente reasigna, y solo la etapa actual; la persona que la recibe debe estar activa y tener el rol
     * responsable (P-07, provisional del equipo; reasignar a otro rol sigue pendiente del cliente). Lo decide el caso
     * de uso con el estado actual de la base, no los roles del JWT.
     */
    @PutMapping("/api/v1/pedidos/{pedidoId}/etapas/{numeroEtapa}/asignacion")
    public PedidoRespuestas.Detalle reasignar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable int numeroEtapa, @Valid @RequestBody SolicitudReasignar solicitud) {
        Solicitante solicitante = SolicitantesWeb.de(token);
        reasignar.reasignar(solicitante, new ReasignarEtapa.Comando(pedidoId, numeroEtapa, solicitud.usuarioId(),
                solicitud.motivo()));
        return PedidoRespuestas.de(consultar.detalle(solicitante, pedidoId));
    }
}
