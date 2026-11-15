package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** V37: quién ve los documentos confidenciales de un pedido y dar o quitar acceso (solo Admin; lo decide el caso de uso). */
@RestController
@RequestMapping("/api/v1/pedidos/{pedidoId}/acceso-confidencial")
public class AccesoConfidencialController {

    private final AccesoConfidencial acceso;

    public AccesoConfidencialController(AccesoConfidencial acceso) {
        this.acceso = acceso;
    }

    public record SolicitudAcceso(@NotNull UUID usuarioId) {
    }

    /** Para cualquiera que vea el pedido: cuántos confidenciales hay por etapa y si los puede ver. */
    @GetMapping("/resumen")
    public AccesoConfidencial.Resumen resumen(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return acceso.resumen(SolicitantesWeb.de(token), pedidoId);
    }

    @GetMapping
    public List<AccesoConfidencial.ConAcceso> listar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return acceso.conAcceso(SolicitantesWeb.de(token), pedidoId);
    }

    @PostMapping
    public ResponseEntity<Void> otorgar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @Valid @RequestBody SolicitudAcceso solicitud) {
        acceso.otorgar(SolicitantesWeb.de(token), pedidoId, solicitud.usuarioId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{usuarioId}")
    public ResponseEntity<Void> revocar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable UUID usuarioId) {
        acceso.revocar(SolicitantesWeb.de(token), pedidoId, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
