package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasPedidos;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Carpeta de Drive de cada pedido (ADR-0027), solo para el admin vigente con el país del pedido: estado y programación
 * o reintento. No llama a Google ni devuelve IDs o enlaces de Drive; la carpeta la crea la tarea de copias cuando la
 * integración está habilitada. La existencia de una carpeta no da acceso a ningún documento.
 */
@RestController
public class CarpetasPedidosController {
    private final GestionarCarpetasPedidos carpetas;

    public CarpetasPedidosController(GestionarCarpetasPedidos carpetas) { this.carpetas = carpetas; }

    @GetMapping("/api/v1/pedidos/{pedidoId}/carpeta-drive")
    public ResponseEntity<EstadoCarpeta> estado(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(EstadoCarpeta.de(carpetas.estado(SolicitantesWeb.de(token), pedidoId)));
    }

    /** Programa o reintenta; idempotente. 202: queda pendiente hasta que Drive confirme la carpeta. */
    @PostMapping("/api/v1/pedidos/{pedidoId}/carpeta-drive")
    public ResponseEntity<EstadoCarpeta> programar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(EstadoCarpeta.de(carpetas.programar(SolicitantesWeb.de(token), pedidoId)));
    }

    public record EstadoCarpeta(UUID pedidoId, String estado, int intentos, String ultimoError, Instant programadaEn,
            Instant creadaEn) {
        static EstadoCarpeta de(GestionarCarpetasPedidos.Estado e) {
            return new EstadoCarpeta(e.pedidoId(), e.estado(), e.intentos(), e.ultimoError(), e.programadaEn(),
                    e.creadaEn());
        }
    }
}
