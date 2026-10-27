package com.decokasa.seguimiento.historial.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.historial.application.port.in.ConsultarHistorialPedido;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Historial del pedido (contrato: GET /pedidos/{id}/historial). Permiso P-13, decidido en el caso de uso (R6). */
@RestController
public class HistorialController {

    private final ConsultarHistorialPedido consultar;

    public HistorialController(ConsultarHistorialPedido consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/api/v1/pedidos/{pedidoId}/historial")
    public List<EventoHistorialRespuesta> historial(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return consultar.historial(SolicitantesWeb.de(token), pedidoId).stream()
                .map(EventoHistorialRespuesta::de)
                .toList();
    }
}
