package com.decokasa.seguimiento.notificaciones.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarAvisosDeEtapa;
import com.decokasa.seguimiento.notificaciones.domain.AvisoEtapa;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avisos de etapa de la persona (ADR-0032; contrato 0.27.0-borrador). Sin caché compartida. */
@RestController
public class AvisosController {

    public record AvisoRespuesta(UUID eventoId, UUID pedidoId, String numero, String pais, String tipoFlujo,
            String tipo, Integer etapaOrigen, String nombreOrigen, int etapaDestino, String nombreDestino,
            String actor, String motivo, String ocurridoEn, boolean comoAdmin, boolean comoResponsable,
            boolean leido) {
    }

    public record AvisosRespuesta(List<AvisoRespuesta> avisos, int noLeidos, String leidasHasta) {
    }

    private final ConsultarAvisosDeEtapa avisos;

    public AvisosController(ConsultarAvisosDeEtapa avisos) {
        this.avisos = avisos;
    }

    @GetMapping("/api/v1/avisos")
    public ResponseEntity<AvisosRespuesta> avisos(@AuthenticationPrincipal Jwt token) {
        ConsultarAvisosDeEtapa.Avisos r = avisos.avisos(SolicitantesWeb.de(token));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new AvisosRespuesta(
                r.avisos().stream().map(AvisosController::respuesta).toList(), r.noLeidos(),
                r.leidasHasta().toString()));
    }

    @PostMapping("/api/v1/avisos/leidos")
    public ResponseEntity<Map<String, String>> marcarLeidos(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("leidasHasta", avisos.marcarLeidos(SolicitantesWeb.de(token)).toString()));
    }

    private static AvisoRespuesta respuesta(ConsultarAvisosDeEtapa.Aviso aviso) {
        AvisoEtapa a = aviso.aviso();
        return new AvisoRespuesta(a.eventoId(), a.pedidoId(), a.numeroPedido(), a.pais(), a.tipoFlujo(),
                a.tipo().name(), a.etapaOrigen(), a.nombreOrigen(), a.etapaDestino(), a.nombreDestino(),
                a.actorNombre(), a.motivo(), a.ocurridoEn().toString(), a.comoAdmin(), a.comoResponsable(),
                aviso.leido());
    }
}
