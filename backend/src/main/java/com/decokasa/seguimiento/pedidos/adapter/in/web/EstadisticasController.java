package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarEstadisticas;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Inicio común (ADR-0029): estadísticas del alcance de la persona y sus alertas. Sin caché compartida. */
@RestController
public class EstadisticasController {
    private final ConsultarEstadisticas consultar;

    public EstadisticasController(ConsultarEstadisticas consultar) { this.consultar = consultar; }

    @GetMapping("/api/v1/estadisticas/pedidos")
    public ResponseEntity<ConsultarEstadisticas.Estadisticas> estadisticas(@AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) Integer anio, @RequestParam(required = false) CodigoPais pais,
            @RequestParam(required = false) String tipoFlujo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(consultar.estadisticas(
                SolicitantesWeb.de(token), new ConsultarEstadisticas.Filtro(anio, pais, tipoFlujo, desde, hasta)));
    }

    @GetMapping("/api/v1/alertas")
    public ResponseEntity<List<ConsultarEstadisticas.Alerta>> alertas(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "MIAS") String ambito) {
        if (!ambito.equals("MIAS") && !ambito.equals("TODAS")) {
            throw new DatoInvalido("ambito", "El ámbito es MIAS o TODAS.");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(consultar.alertas(SolicitantesWeb.de(token), ambito.equals("TODAS")));
    }
}
