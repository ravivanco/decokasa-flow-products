package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarRoles;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearRol;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Roles como datos (contrato: /roles). Solo el admin (R6). La lista de permisos sigue pendiente de definir; las
 * etapas que atiende cada rol se derivan de las definiciones de flujo (fase 2) y por ahora van vacías.
 */
@RestController
@RequestMapping("/api/v1/roles")
public class RolController {

    public record SolicitudRol(
            @NotNull @Pattern(regexp = "^[a-z_]{3,40}$") String codigo,
            @NotBlank @Size(max = 100) String nombre,
            @NotNull Set<@NotBlank @Size(max = 100) String> permisos) {
    }

    public record RolRespuesta(UUID id, String codigo, String nombre, List<String> permisos,
            List<Object> etapasQueAtiende) {

        static RolRespuesta de(Rol rol) {
            return new RolRespuesta(rol.id(), rol.codigo().valor(), rol.nombre(),
                    rol.permisos().stream().sorted().toList(), List.of());
        }
    }

    private final ConsultarRoles consultarRoles;
    private final CrearRol crearRol;
    private final ActualizarRol actualizarRol;

    public RolController(ConsultarRoles consultarRoles, CrearRol crearRol, ActualizarRol actualizarRol) {
        this.consultarRoles = consultarRoles;
        this.crearRol = crearRol;
        this.actualizarRol = actualizarRol;
    }

    @GetMapping
    public List<RolRespuesta> listar(@AuthenticationPrincipal Jwt token) {
        return consultarRoles.listar(Solicitantes.de(token)).stream().map(RolRespuesta::de).toList();
    }

    @PostMapping
    public ResponseEntity<RolRespuesta> crear(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody SolicitudRol solicitud) {
        Rol creado = crearRol.crear(Solicitantes.de(token),
                new CrearRol.Comando(new CodigoRol(solicitud.codigo()), solicitud.nombre(), solicitud.permisos()));
        return ResponseEntity.created(URI.create("/api/v1/roles/" + creado.id())).body(RolRespuesta.de(creado));
    }

    @PutMapping("/{rolId}")
    public RolRespuesta actualizar(@AuthenticationPrincipal Jwt token, @PathVariable UUID rolId,
            @Valid @RequestBody SolicitudRol solicitud) {
        return RolRespuesta.de(actualizarRol.actualizar(Solicitantes.de(token), new ActualizarRol.Comando(rolId,
                new CodigoRol(solicitud.codigo()), solicitud.nombre(), solicitud.permisos())));
    }
}
