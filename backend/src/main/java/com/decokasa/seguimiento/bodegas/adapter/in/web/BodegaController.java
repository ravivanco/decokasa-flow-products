package com.decokasa.seguimiento.bodegas.adapter.in.web;

import com.decokasa.seguimiento.bodegas.application.port.in.ActualizarBodega;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegas;
import com.decokasa.seguimiento.bodegas.application.port.in.CrearBodega;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * Catálogo de bodegas (contrato: /bodegas). La lectura es para cualquier usuario autenticado, como dice el contrato. El
 * alta y la edición las decide el caso de uso con el estado vigente (F4-D1: Logística o admin). No hay borrado: una
 * bodega se desactiva con {@code activa: false}.
 */
@RestController
@RequestMapping("/api/v1/bodegas")
public class BodegaController {

    public record SolicitudBodega(
            @NotBlank(message = "Escribe el nombre de la bodega.") @Size(max = 100) String nombre,
            @NotBlank(message = "Escribe la ciudad de la bodega.") @Size(max = 100) String ciudad,
            Boolean activa) {
    }

    public record SolicitudActualizarBodega(
            @Size(min = 1, max = 100) String nombre,
            @Size(min = 1, max = 100) String ciudad,
            Boolean activa,
            // Se recibe como nodo JSON y se valida aquí, como en usuarios (D10): Jackson convertiría "3", 1.5 o true en
            // un número.
            JsonNode version) {
    }

    private final ConsultarBodegas consultar;
    private final CrearBodega crear;
    private final ActualizarBodega actualizar;

    public BodegaController(ConsultarBodegas consultar, CrearBodega crear, ActualizarBodega actualizar) {
        this.consultar = consultar;
        this.crear = crear;
        this.actualizar = actualizar;
    }

    @GetMapping
    public List<BodegaRespuesta> listar(@RequestParam(required = false) Boolean activa) {
        return consultar.listar(activa).stream().map(BodegaRespuesta::de).toList();
    }

    @PostMapping
    public ResponseEntity<BodegaRespuesta> crear(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody SolicitudBodega solicitud) {
        Bodega creada = crear.crear(SolicitantesWeb.de(token),
                new CrearBodega.Comando(solicitud.nombre(), solicitud.ciudad(), solicitud.activa()));
        return ResponseEntity.created(URI.create("/api/v1/bodegas/" + creada.id())).body(BodegaRespuesta.de(creada));
    }

    @PatchMapping("/{bodegaId}")
    public BodegaRespuesta actualizar(@AuthenticationPrincipal Jwt token, @PathVariable UUID bodegaId,
            @Valid @RequestBody SolicitudActualizarBodega solicitud) {
        long version = versionLeida(solicitud.version());
        if (solicitud.nombre() == null && solicitud.ciudad() == null && solicitud.activa() == null) {
            throw new DatoInvalido("Envía al menos un campo para cambiar.");
        }
        return BodegaRespuesta.de(actualizar.actualizar(SolicitantesWeb.de(token), new ActualizarBodega.Comando(
                bodegaId, solicitud.nombre(), solicitud.ciudad(), solicitud.activa(), version)));
    }

    // La versión que leyó el cliente: un entero JSON de 0 en adelante. Ausente, null, texto, booleano, con decimales,
    // negativa o fuera de rango: 400 en el campo version.
    private static long versionLeida(JsonNode version) {
        if (version == null || !version.isIntegralNumber() || !version.canConvertToLong() || version.asLong() < 0) {
            throw new DatoInvalido("version", "Envía la versión de la bodega que leíste: un número entero desde 0.");
        }
        return version.asLong();
    }
}
