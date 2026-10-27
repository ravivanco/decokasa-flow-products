package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle;
import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle.Inicio;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;
import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** El callback GET es público solo a nivel de JWT: exige estado vivo y cookie del mismo navegador. */
@RestController
@RequestMapping("/api/v1/integraciones/google/oauth2")
@ConditionalOnProperty(name = "decokasa.google.oauth.habilitada", havingValue = "true")
public class GoogleOAuthController {
    private static final String COOKIE = "decokasa_google_inicio";
    private static final String RUTA = "/api/v1/integraciones/google/oauth2";
    private final AutorizarCuentaGoogle autorizar;
    private final SecureRandom azar = new SecureRandom();

    public GoogleOAuthController(AutorizarCuentaGoogle autorizar) { this.autorizar = autorizar; }

    @PostMapping("/inicio")
    public ResponseEntity<Inicio> iniciar(@AuthenticationPrincipal Jwt token) {
        byte[] bytes = new byte[32]; azar.nextBytes(bytes);
        String vinculo = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Inicio inicio = autorizar.iniciar(SolicitantesWeb.de(token), vinculo);
        return proteger(ResponseEntity.ok()).header(HttpHeaders.SET_COOKIE,
                cookie(vinculo, Duration.ofMinutes(5)).toString()).body(inicio);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam MultiValueMap<String, String> parametros,
            @CookieValue(name = COOKIE, required = false) String vinculo, HttpServletResponse respuesta) {
        // Elimina la cookie también ante error; una solicitud fallida requiere un nuevo inicio administrativo.
        respuesta.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
        try {
            String estado = unico(parametros, "state");
            String codigo = unico(parametros, "code");
            boolean denegado = parametros.containsKey("error");
            autorizar.completar(estado, vinculo, codigo, denegado);
            return resultado(200);
        } catch (ErrorDeDominio error) {
            int estado = error.codigo() == CodigoError.GOOGLE_NO_DISPONIBLE ? 503
                    : error.codigo() == CodigoError.SIN_PERMISO ? 403 : 400;
            return resultado(estado);
        } catch (RuntimeException error) {
            // Nada del proveedor o la query acaba en el manejador global y sus logs.
            return resultado(503);
        }
    }

    @GetMapping("/resultado")
    public ResponseEntity<String> mostrarResultado(@RequestParam(defaultValue = "400") String codigo) {
        if ("200".equals(codigo)) {
            return html(200, "Autorización Google guardada", "Puedes cerrar esta pestaña. No se ha enviado correo "
                    + "ni subido ningún archivo. En modo En pruebas la conexión deberá renovarse como máximo a los siete días.");
        }
        int estado = "403".equals(codigo) ? 403 : "503".equals(codigo) ? 503 : 400;
        return html(estado, "No se completó la conexión Google", "Revisa la configuración privada y vuelve a "
                + "iniciar desde una sesión de admin vigente. No compartas la URL de retorno.");
    }

    private static ResponseEntity<Void> resultado(int estado) {
        // Saca código y state de la página de resultado y evita propagar la query a navegación posterior.
        return proteger(ResponseEntity.status(303)).header(HttpHeaders.LOCATION,
                RUTA + "/resultado?codigo=" + estado).build();
    }

    private static String unico(MultiValueMap<String, String> parametros, String nombre) {
        List<String> valores = parametros.get(nombre);
        if (valores == null) { return null; }
        if (valores.size() != 1) { throw new DatoInvalido("Retorno OAuth inválido."); }
        return valores.getFirst();
    }

    private static ResponseCookie cookie(String valor, Duration duracion) {
        // HTTP es una excepción SOLO para localhost en este cliente. Host-only; no Domain.
        // Lax permite el retorno GET de Google; no cambia la cookie Strict de renovación del login.
        return ResponseCookie.from(COOKIE, valor).httpOnly(true).secure(false).sameSite("Lax")
                .path(RUTA).maxAge(duracion).build();
    }

    private static ResponseEntity.BodyBuilder proteger(ResponseEntity.BodyBuilder builder) {
        return builder.header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header("Referrer-Policy", "no-referrer")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; base-uri 'none'; frame-ancestors 'none'");
    }

    private static ResponseEntity<String> html(int estado, String titulo, String texto) {
        // Todos los textos son constantes locales, nunca parámetros Google o mensajes de excepción.
        return proteger(ResponseEntity.status(estado)).contentType(MediaType.TEXT_HTML)
                .body("<!doctype html><html lang=\"es\"><meta charset=\"utf-8\"><title>Conexión Google</title>"
                        + "<h1>" + titulo + "</h1><p>" + texto + "</p></html>");
    }
}
