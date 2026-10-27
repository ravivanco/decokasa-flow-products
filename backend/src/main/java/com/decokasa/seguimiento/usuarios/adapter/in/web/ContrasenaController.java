package com.decokasa.seguimiento.usuarios.adapter.in.web;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.usuarios.application.port.in.CambiarContrasena;
import com.decokasa.seguimiento.usuarios.application.port.in.RecuperarContrasena;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recuperación y cambio de contraseña (contrato 0.28.0; ADR-0033). Solicitar y restablecer son públicos y tienen un
 * límite por IP; cambiar exige sesión. Ninguna respuesta incluye el enlace, el token ni la contraseña, y ninguno de los
 * dos registra el cuerpo. Tras restablecer o cambiar, la cookie de renovación se borra y hay que iniciar sesión.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class ContrasenaController {

    static final String RESPUESTA_GENERICA = "Si el correo pertenece a una cuenta activa, recibirás un enlace para "
            + "restablecer la contraseña. Revisa también la carpeta de spam. El enlace caduca en poco tiempo.";

    private static final Duration VENTANA = Duration.ofMinutes(15);

    public record SolicitudRecuperacion(
            @NotBlank(message = "Escribe tu correo.") @Size(max = 254) String correo) {
    }

    public record RespuestaRecuperacion(String mensaje) {
    }

    public record SolicitudRestablecer(
            @NotBlank(message = "El enlace no es válido.") @Size(max = 200) String token,
            @NotNull(message = "Escribe la contraseña nueva.") @Size(max = 72) String contrasenaNueva,
            @NotNull(message = "Confirma la contraseña nueva.") @Size(max = 72) String confirmacion) {

        @Override
        public String toString() {
            return "SolicitudRestablecer[PROTEGIDA]";
        }
    }

    public record SolicitudCambioContrasena(
            @NotEmpty(message = "Escribe tu contraseña actual.") @Size(max = 72) String contrasenaActual,
            @NotNull(message = "Escribe la contraseña nueva.") @Size(max = 72) String contrasenaNueva,
            @NotNull(message = "Confirma la contraseña nueva.") @Size(max = 72) String confirmacion) {

        @Override
        public String toString() {
            return "SolicitudCambioContrasena[PROTEGIDA]";
        }
    }

    private final RecuperarContrasena recuperar;
    private final CambiarContrasena cambiar;
    private final LimitadorPorIp limiteSolicitudes;
    private final LimitadorPorIp limiteRestablecer;

    public ContrasenaController(RecuperarContrasena recuperar, CambiarContrasena cambiar, Reloj reloj) {
        this.recuperar = recuperar;
        this.cambiar = cambiar;
        this.limiteSolicitudes = new LimitadorPorIp(10, VENTANA, reloj);
        this.limiteRestablecer = new LimitadorPorIp(20, VENTANA, reloj);
    }

    @PostMapping("/recuperacion")
    public ResponseEntity<RespuestaRecuperacion> solicitar(@Valid @RequestBody SolicitudRecuperacion solicitud,
            HttpServletRequest peticion) {
        limiteSolicitudes.consumir(peticion.getRemoteAddr());
        recuperar.solicitar(solicitud.correo());
        return ResponseEntity.accepted().body(new RespuestaRecuperacion(RESPUESTA_GENERICA));
    }

    @PostMapping("/recuperacion/restablecer")
    public ResponseEntity<Void> restablecer(@Valid @RequestBody SolicitudRestablecer solicitud,
            HttpServletRequest peticion) {
        limiteRestablecer.consumir(peticion.getRemoteAddr());
        recuperar.restablecer(solicitud.token(), solicitud.contrasenaNueva(), solicitud.confirmacion());
        return sinSesion();
    }

    @PostMapping("/contrasena")
    public ResponseEntity<Void> cambiar(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody SolicitudCambioContrasena solicitud) {
        cambiar.cambiar(UUID.fromString(token.getSubject()), solicitud.contrasenaActual(), solicitud.contrasenaNueva(),
                solicitud.confirmacion());
        return sinSesion();
    }

    // Borra la cookie de renovación de este navegador; las de otros ya quedaron revocadas en el servidor.
    private static ResponseEntity<Void> sinSesion() {
        ResponseCookie borrar = ResponseCookie.from(AuthController.COOKIE, "")
                .httpOnly(true).secure(true).sameSite("Strict").path(AuthController.RUTA_COOKIE).maxAge(Duration.ZERO)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, borrar.toString()).build();
    }
}
