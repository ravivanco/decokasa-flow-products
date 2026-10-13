package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarUsuario;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
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
 * Gestión de usuarios (contrato: /usuarios). Solo el admin: lo exige la seguridad web y, de nuevo, cada caso de uso
 * (R6). Los límites de los campos son los del contrato y de la base (modelo-datos.md 3.1).
 */
@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private static final String TELEFONO = "^\\+?[0-9 ()-]{7,20}$";
    private static final String CODIGO_ROL = "^[a-z_]{3,40}$";

    public record SolicitudCrearUsuario(
            @NotBlank(message = "Escribe el nombre.") @Size(max = 200) String nombre,
            @NotBlank(message = "Escribe un correo válido.") @Email(message = "Escribe un correo válido.")
            @Size(max = 254) String correo,
            @NotNull @Size(min = 3, max = 50, message = "El usuario debe tener entre 3 y 50 caracteres.")
            String usuario,
            @NotNull @Size(min = 7, max = 20) @Pattern(regexp = TELEFONO,
                    message = "Escribe un teléfono válido, de 7 a 15 dígitos.") String telefono,
            @NotNull @Size(min = 1, message = "Elige al menos un rol.") Set<@Pattern(regexp = CODIGO_ROL) String> roles,
            Set<CodigoPais> paises,
            @NotEmpty @Size(min = 8, max = 72, message = "La contraseña debe tener de 8 caracteres a 72 bytes.")
            String contrasenaInicial) {
    }

    public record SolicitudActualizarUsuario(
            @Size(min = 1, max = 200) String nombre,
            @Email(message = "Escribe un correo válido.") @Size(max = 254) String correo,
            @Size(min = 7, max = 20) @Pattern(regexp = TELEFONO,
                    message = "Escribe un teléfono válido, de 7 a 15 dígitos.") String telefono,
            @Size(min = 1, message = "Elige al menos un rol.") Set<@Pattern(regexp = CODIGO_ROL) String> roles,
            Set<CodigoPais> paises,
            Boolean activo,
            // D10. Se recibe como nodo JSON y se valida aquí: Jackson convertiría "3", 1.5 o true en un número.
            JsonNode version) {
    }

    private final ConsultarUsuarios consultarUsuarios;
    private final CrearUsuario crearUsuario;
    private final ActualizarUsuario actualizarUsuario;

    public UsuarioController(ConsultarUsuarios consultarUsuarios, CrearUsuario crearUsuario,
            ActualizarUsuario actualizarUsuario) {
        this.consultarUsuarios = consultarUsuarios;
        this.crearUsuario = crearUsuario;
        this.actualizarUsuario = actualizarUsuario;
    }

    @GetMapping
    public List<UsuarioRespuesta> listar(@AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) Boolean activo) {
        return consultarUsuarios.listar(Solicitantes.de(token), activo).stream().map(UsuarioRespuesta::de).toList();
    }

    @GetMapping("/{usuarioId}")
    public UsuarioRespuesta obtener(@AuthenticationPrincipal Jwt token, @PathVariable UUID usuarioId) {
        return UsuarioRespuesta.de(consultarUsuarios.obtener(Solicitantes.de(token), usuarioId));
    }

    @PostMapping
    public ResponseEntity<UsuarioRespuesta> crear(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody SolicitudCrearUsuario solicitud) {
        Usuario creado = crearUsuario.crear(Solicitantes.de(token), new CrearUsuario.Comando(solicitud.nombre(),
                solicitud.correo(), solicitud.usuario(), solicitud.telefono(), roles(solicitud.roles()),
                solicitud.paises(), solicitud.contrasenaInicial()));
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + creado.id()))
                .body(UsuarioRespuesta.de(creado));
    }

    @PatchMapping("/{usuarioId}")
    public UsuarioRespuesta actualizar(@AuthenticationPrincipal Jwt token, @PathVariable UUID usuarioId,
            @Valid @RequestBody SolicitudActualizarUsuario solicitud) {
        long version = versionLeida(solicitud.version());
        if (solicitud.nombre() == null && solicitud.correo() == null && solicitud.telefono() == null
                && solicitud.roles() == null && solicitud.paises() == null && solicitud.activo() == null) {
            throw new DatoInvalido("Envía al menos un campo para cambiar.");
        }
        return UsuarioRespuesta.de(actualizarUsuario.actualizar(Solicitantes.de(token),
                new ActualizarUsuario.Comando(usuarioId, solicitud.nombre(), solicitud.correo(), solicitud.telefono(),
                        roles(solicitud.roles()), solicitud.paises(), solicitud.activo(), version)));
    }

    // La versión que leyó el cliente (D10): un entero JSON de 0 en adelante. Ausente, null, texto, booleano, con
    // decimales (también 1.0), negativa o fuera de rango: 400 en el campo version.
    private static long versionLeida(JsonNode version) {
        if (version == null || !version.isIntegralNumber() || !version.canConvertToLong() || version.asLong() < 0) {
            throw new DatoInvalido("version", "Envía la versión del usuario que leíste: un número entero desde 0.");
        }
        return version.asLong();
    }

    private static Set<CodigoRol> roles(Set<String> codigos) {
        return codigos == null ? null : codigos.stream().map(CodigoRol::new).collect(Collectors.toSet());
    }
}
