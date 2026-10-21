package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import com.decokasa.seguimiento.pedidos.application.port.in.RegistrarMotivoRetraso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Motivo del retraso de la etapa actual (contrato 0.12.0-borrador; F5-D3, provisional del equipo). Quién puede y
 * cuándo lo decide el caso de uso con el estado vigente (R6). La respuesta es el evento del historial, leído después
 * del commit, como en las observaciones.
 */
@RestController
public class MotivoRetrasoController {

    /** Cuerpo del PUT: la entrada que la persona ve (P-18) y el motivo. */
    public record SolicitudMotivoRetraso(@NotNull UUID entradaEsperadaId,
            @NotBlank @Size(max = 4000) String motivo) {
    }

    private final RegistrarMotivoRetraso registrar;
    private final ConsultarHistorialPedido historial;

    public MotivoRetrasoController(RegistrarMotivoRetraso registrar, ConsultarHistorialPedido historial) {
        this.registrar = registrar;
        this.historial = historial;
    }

    @PutMapping("/api/v1/pedidos/{pedidoId}/etapas/{numeroEtapa}/motivo-retraso")
    public ObservacionRespuesta registrar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable int numeroEtapa, @Valid @RequestBody SolicitudMotivoRetraso solicitud) {
        Solicitante solicitante = SolicitantesWeb.de(token);
        UUID eventoId = registrar.registrar(solicitante, new RegistrarMotivoRetraso.Comando(pedidoId, numeroEtapa,
                solicitud.entradaEsperadaId(), solicitud.motivo()));
        return ObservacionRespuesta.de(historial.evento(solicitante, pedidoId, eventoId));
    }
}
