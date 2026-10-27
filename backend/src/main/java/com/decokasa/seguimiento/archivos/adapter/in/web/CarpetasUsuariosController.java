package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasUsuarios;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Historial de la carpeta global de cada usuario (ADR-0026), solo para el admin vigente y solo lectura. Desde ADR-0027
 * no se crean carpetas globales por usuario: se retiró la programación y V24 cerró las intenciones pendientes como
 * OBSOLETA. No llama a Google ni devuelve IDs o enlaces de Drive.
 */
@RestController
public class CarpetasUsuariosController {
    private final GestionarCarpetasUsuarios carpetas;

    public CarpetasUsuariosController(GestionarCarpetasUsuarios carpetas) { this.carpetas = carpetas; }

    @GetMapping("/api/v1/usuarios/carpetas-drive")
    public ResponseEntity<List<EstadoCarpeta>> listar(@AuthenticationPrincipal Jwt token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(carpetas.listar(SolicitantesWeb.de(token)).stream().map(EstadoCarpeta::de).toList());
    }

    @GetMapping("/api/v1/usuarios/{usuarioId}/carpeta-drive")
    public ResponseEntity<EstadoCarpeta> estado(@AuthenticationPrincipal Jwt token, @PathVariable UUID usuarioId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(EstadoCarpeta.de(carpetas.estado(SolicitantesWeb.de(token), usuarioId)));
    }

    public record EstadoCarpeta(UUID usuarioId, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) {
        static EstadoCarpeta de(GestionarCarpetasUsuarios.Estado e) {
            return new EstadoCarpeta(e.usuarioId(), e.estado(), e.intentos(), e.ultimoError(), e.programadaEn(),
                    e.creadaEn());
        }
    }
}
