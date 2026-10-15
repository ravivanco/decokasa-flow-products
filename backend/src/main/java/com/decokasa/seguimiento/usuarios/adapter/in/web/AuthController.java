package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.usuarios.application.port.in.CerrarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual;
import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual.UsuarioActual;
import com.decokasa.seguimiento.usuarios.application.port.in.RenovarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.SesionIniciada;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Sesión (contrato: /auth/login, /auth/renovar, /auth/cerrar-sesion y /auth/yo; ADR-0002). El token de acceso va en
 * el cuerpo y vive en la memoria del navegador; el de renovación, solo en la cookie HttpOnly (D3b). /renovar y
 * /cerrar-sesion exigen un Origin permitido: si falta o no está en la lista, 403 (aprobado el 2026-10-13).
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String COOKIE = "decokasa_renovacion";
    static final String RUTA_COOKIE = "/api/v1/auth";

    public record SolicitudInicioSesion(
            @NotBlank(message = "Escribe tu correo, usuario o teléfono.") @Size(max = 254) String identificador,
            @NotEmpty(message = "Escribe tu contraseña.") @Size(max = 72) String contrasena) {
    }

    public record RespuestaSesion(String tokenAcceso, long expiraEn, UsuarioRespuesta usuario) {

        static RespuestaSesion de(SesionIniciada sesion) {
            return new RespuestaSesion(sesion.tokenAcceso(), sesion.expiraEnSegundos(),
                    UsuarioRespuesta.de(sesion.usuario()));
        }
    }

    public record UsuarioActualRespuesta(UUID id, String nombre, String correo, String usuario, String telefono,
            List<String> roles, List<String> paises, boolean activo, long version, List<String> permisos) {

        static UsuarioActualRespuesta de(UsuarioActual actual) {
            UsuarioRespuesta u = UsuarioRespuesta.de(actual.usuario());
            return new UsuarioActualRespuesta(u.id(), u.nombre(), u.correo(), u.usuario(), u.telefono(), u.roles(),
                    u.paises(), u.activo(), u.version(), actual.permisos().stream().sorted().toList());
        }
    }

    private final IniciarSesion iniciarSesion;
    private final RenovarSesion renovarSesion;
    private final CerrarSesion cerrarSesion;
    private final ObtenerUsuarioActual obtenerUsuarioActual;
    private final Reloj reloj;
    private final List<String> origenesPermitidos;

    public AuthController(IniciarSesion iniciarSesion, RenovarSesion renovarSesion, CerrarSesion cerrarSesion,
            ObtenerUsuarioActual obtenerUsuarioActual, Reloj reloj,
            @Value("${decokasa.cors.origenes}") List<String> origenesPermitidos) {
        this.iniciarSesion = iniciarSesion;
        this.renovarSesion = renovarSesion;
        this.cerrarSesion = cerrarSesion;
        this.obtenerUsuarioActual = obtenerUsuarioActual;
        this.reloj = reloj;
        this.origenesPermitidos = List.copyOf(origenesPermitidos);
    }

    @PostMapping("/login")
    public ResponseEntity<RespuestaSesion> iniciarSesion(@Valid @RequestBody SolicitudInicioSesion solicitud) {
        return conCookie(iniciarSesion.iniciar(solicitud.identificador(), solicitud.contrasena()));
    }

    @PostMapping("/renovar")
    public ResponseEntity<RespuestaSesion> renovar(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origen,
            @CookieValue(name = COOKIE, required = false) String tokenRenovacion) {
        exigirOrigenPermitido(origen);
        return conCookie(renovarSesion.renovar(tokenRenovacion));
    }

    @PostMapping("/cerrar-sesion")
    public ResponseEntity<Void> cerrarSesion(
            @RequestHeader(value = HttpHeaders.ORIGIN, required = false) String origen,
            @CookieValue(name = COOKIE, required = false) String tokenRenovacion) {
        exigirOrigenPermitido(origen);
        cerrarSesion.cerrar(tokenRenovacion);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString()).build();
    }

    @GetMapping("/yo")
    public UsuarioActualRespuesta yo(@AuthenticationPrincipal Jwt token) {
        return UsuarioActualRespuesta.de(obtenerUsuarioActual.obtener(UUID.fromString(token.getSubject())));
    }

    private ResponseEntity<RespuestaSesion> conCookie(SesionIniciada sesion) {
        Duration restante = Duration.between(reloj.ahora(), sesion.renovableHasta());
        ResponseCookie cookie = cookie(sesion.tokenRenovacion(), restante.isNegative() ? Duration.ZERO : restante);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(RespuestaSesion.de(sesion));
    }

    private static ResponseCookie cookie(String valor, Duration vida) {
        return ResponseCookie.from(COOKIE, valor)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(RUTA_COOKIE)
                .maxAge(vida)
                .build();
    }

    private void exigirOrigenPermitido(String origen) {
        if (origen == null || !origenesPermitidos.contains(origen)) {
            throw new SinPermiso("El origen de la solicitud no está permitido.");
        }
    }
}
