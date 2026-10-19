package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarArchivosDrive;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
@RestController
public class ConsultaDriveController {
    private final ConsultarArchivosDrive consultar;
    public ConsultaDriveController(ConsultarArchivosDrive consultar) { this.consultar=consultar; }
    @GetMapping("/api/v1/archivos/{adjuntoId}/enlace")
    public ResponseEntity<Enlace> enlace(@AuthenticationPrincipal Jwt token,@PathVariable UUID adjuntoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new Enlace(consultar.enlace(SolicitantesWeb.de(token),adjuntoId)));
    }
    @GetMapping("/api/v1/archivos/copias-fallidas")
    public ResponseEntity<List<AdjuntoRespuesta>> fallidos(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
            consultar.fallidos(SolicitantesWeb.de(token),pagina,tamano).stream().map(AdjuntoRespuesta::de).toList());
    }
    public record Enlace(String url) {}
}
