package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import com.decokasa.seguimiento.pedidos.application.port.in.AgregarObservacion;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Observaciones de la etapa actual (contrato: POST /pedidos/{id}/observaciones). P-19, provisional del equipo: quien
 * puede actuar en la etapa (P-06) o el admin, decidido en el caso de uso con el estado vigente (R6). La respuesta es
 * el evento del historial, leído después del commit.
 */
@RestController
public class ObservacionController {

    private final AgregarObservacion agregar;
    private final ConsultarHistorialPedido historial;

    public ObservacionController(AgregarObservacion agregar, ConsultarHistorialPedido historial) {
        this.agregar = agregar;
        this.historial = historial;
    }

    @PostMapping("/api/v1/pedidos/{pedidoId}/observaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public ObservacionRespuesta agregar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @Valid @RequestBody SolicitudObservacion solicitud) {
        Solicitante solicitante = SolicitantesWeb.de(token);
        UUID eventoId = agregar.agregar(solicitante, new AgregarObservacion.Comando(pedidoId,
                solicitud.etapaEsperada(), solicitud.entradaEsperadaId(), solicitud.texto(), solicitud.adjuntoIds()));
        return ObservacionRespuesta.de(historial.evento(solicitante, pedidoId, eventoId));
    }
}
