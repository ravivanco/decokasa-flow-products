package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.claims;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Sesión por HTTP según D3b (ADR-0002, aprobado el 2026-10-12): cookie de renovación HttpOnly, Secure, SameSite=Strict
 * en /api/v1/auth; rotación, reutilización, límite de 8 horas y cuentas desactivadas o sin roles en /auth/renovar;
 * /auth/cerrar-sesion; y Origin obligatoria y permitida. Datos ficticios. Requiere Docker encendido.
 */
@SpringBootTest(properties = "CORS_ORIGENES=" + SesionHttpIT.ORIGEN_PERMITIDO)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SesionHttpIT {

    static final String ORIGEN_PERMITIDO = "http://localhost:3000";
    private static final String ORIGEN_AJENO = "http://ajeno.test";
    private static final String COOKIE = "decokasa_renovacion";
    private static final int OCHO_HORAS = 8 * 60 * 60;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID ana;

    @BeforeEach
    void preparar() {
        UsuariosEnBase base = new UsuariosEnBase(jdbc);
        base.limpiar();
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china", "logistica");
    }

    // --- Cookie al iniciar sesión ---

    @Test
    void elLoginEntregaLaCookieDeRenovacionConSusAtributos() throws Exception {
        login()
                .andExpect(status().isOk())
                .andExpect(cookie().exists(COOKIE))
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().secure(COOKIE, true))
                .andExpect(cookie().sameSite(COOKIE, "Strict"))
                .andExpect(cookie().path(COOKIE, "/api/v1/auth"))
                .andExpect(cookie().maxAge(COOKIE, allOf(greaterThan(OCHO_HORAS - 60), lessThanOrEqualTo(OCHO_HORAS))))
                .andExpect(jsonPath("$.tokenRenovacion").doesNotExist());
    }

    @Test
    void elTokenDeAccesoNoViajaEnNingunaCookie() throws Exception {
        MockHttpServletResponse respuesta = login().andExpect(status().isOk()).andReturn().getResponse();
        String acceso = JsonPath.read(respuesta.getContentAsString(StandardCharsets.UTF_8), "$.tokenAcceso");

        assertThat(respuesta.getCookies()).extracting(Cookie::getName).containsExactly(COOKIE);
        assertThat(respuesta.getCookie(COOKIE).getValue()).isNotBlank().isNotEqualTo(acceso);
    }

    // --- Renovación por HTTP ---

    @Test
    void renovarConLaCookieEntregaUnAccesoNuevoYRotaLaCookie() throws Exception {
        String inicial = cookieDeLogin();

        MockHttpServletResponse respuesta = renovar(inicial, ORIGEN_PERMITIDO)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").value(900))
                .andExpect(jsonPath("$.usuario.roles", containsInAnyOrder("compras_china", "logistica")))
                .andExpect(jsonPath("$.tokenRenovacion").doesNotExist())
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andExpect(cookie().secure(COOKIE, true))
                .andExpect(cookie().sameSite(COOKIE, "Strict"))
                .andExpect(cookie().path(COOKIE, "/api/v1/auth"))
                .andExpect(cookie().maxAge(COOKIE, lessThanOrEqualTo(OCHO_HORAS)))
                .andReturn().getResponse();

        assertThat(respuesta.getCookie(COOKIE).getValue()).isNotBlank().isNotEqualTo(inicial);
    }

    @Test
    void reutilizarLaCookieAnteriorRevocaTodaLaSesion() throws Exception {
        String inicial = cookieDeLogin();
        String rotada = renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(COOKIE).getValue();

        renovar(inicial, ORIGEN_PERMITIDO)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        renovar(rotada, ORIGEN_PERMITIDO).andExpect(status().isUnauthorized());
        assertThat(sesionesAbiertas()).isZero();
    }

    // Control en las dos siguientes: hay una sesión abierta con cookie válida; lo que falla es la petición sin ella.
    @Test
    void sinCookieNoRenueva() throws Exception {
        cookieDeLogin();

        mvc.perform(post("/api/v1/auth/renovar").header(HttpHeaders.ORIGIN, ORIGEN_PERMITIDO))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        assertThat(sesionesAbiertas()).isEqualTo(1);
    }

    @Test
    void unaCookieInventadaNoRenueva() throws Exception {
        cookieDeLogin();

        renovar("cookie-inventada", ORIGEN_PERMITIDO).andExpect(status().isUnauthorized());
        assertThat(sesionesAbiertas()).isEqualTo(1);
    }

    @Test
    void variasRotacionesConservanElVencimientoAbsolutoEnPostgres() throws Exception {
        String actual = cookieDeLogin();
        java.time.Instant limite = jdbc.queryForObject(
                "select expira_en from token_renovacion where usuario_id = ?",
                java.sql.Timestamp.class, ana).toInstant();

        for (int i = 0; i < 3; i++) {
            String anterior = actual;
            actual = renovar(anterior, ORIGEN_PERMITIDO).andExpect(status().isOk())
                    .andReturn().getResponse().getCookie(COOKIE).getValue();
            assertThat(actual).isNotBlank().isNotEqualTo(anterior);
            assertThat(sesionesAbiertas()).isEqualTo(1);
            assertThat(jdbc.queryForList("select expira_en from token_renovacion where usuario_id = ?",
                    java.sql.Timestamp.class, ana)).hasSize(i + 2)
                    .allSatisfy(fecha -> assertThat(fecha.toInstant()).isEqualTo(limite));
            assertThat(jdbc.queryForObject("select count(distinct familia_id) from token_renovacion "
                    + "where usuario_id = ?", Integer.class, ana)).isEqualTo(1);
        }

        jdbc.update("update token_renovacion set creado_en = now() - interval '9 hours', "
                + "expira_en = now() - interval '1 second' where usuario_id = ?", ana);
        int cantidad = jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ?",
                Integer.class, ana);
        renovar(actual, ORIGEN_PERMITIDO).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        assertThat(jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ?",
                Integer.class, ana)).isEqualTo(cantidad);
    }

    @Test
    void pasadasLasOchoHorasNoRenuevaAunqueLaCookieNoSeHayaRotado() throws Exception {
        String inicial = cookieDeLogin();
        // Simula que el inicio de sesión fue hace más de 8 horas.
        jdbc.update("update token_renovacion set creado_en = now() - interval '9 hours', "
                + "expira_en = now() - interval '1 hour' where usuario_id = ?", ana);

        renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isUnauthorized());
    }

    @Test
    void unaCuentaDesactivadaNoRenuevaYSuSesionSeRevoca() throws Exception {
        String inicial = cookieDeLogin();
        jdbc.update("update usuario set activo = false where id = ?", ana);

        renovar(inicial, ORIGEN_PERMITIDO)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        assertThat(sesionesAbiertas()).isZero();
    }

    @Test
    void unaCuentaSinRolesNoRenuevaYSuSesionSeRevoca() throws Exception {
        String inicial = cookieDeLogin();
        jdbc.update("delete from usuario_rol where usuario_id = ?", ana);

        renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isUnauthorized());
        assertThat(sesionesAbiertas()).isZero();
    }

    @Test
    void elAccesoRenovadoLlevaLosRolesActuales() throws Exception {
        String inicial = cookieDeLogin();
        jdbc.update("delete from usuario_rol where usuario_id = ?", ana);
        jdbc.update("insert into usuario_rol (usuario_id, rol_id, asignado_en) "
                + "select ?, id, now() from rol where codigo = 'marketing'", ana);

        String respuesta = renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        String acceso = JsonPath.read(respuesta, "$.tokenAcceso");
        List<String> roles = JsonPath.read(claims(acceso), "$.roles");
        assertThat(roles).containsExactly("marketing");
    }

    // --- Comprobación de Origin ---

    @Test
    void renovarDesdeUnOrigenAjenoResponde403SinTocarLaSesion() throws Exception {
        String inicial = cookieDeLogin();

        renovar(inicial, ORIGEN_AJENO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));

        renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isOk());
    }

    @Test
    void renovarSinOriginResponde403() throws Exception {
        String inicial = cookieDeLogin();

        mvc.perform(post("/api/v1/auth/renovar").cookie(new Cookie(COOKIE, inicial)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        assertThat(sesionesAbiertas()).isEqualTo(1);
    }

    // --- Cierre de sesión ---

    @Test
    void cerrarSesionRevocaLaRenovacionYBorraLaCookie() throws Exception {
        String inicial = cookieDeLogin();

        cerrarSesion(new Cookie(COOKIE, inicial), ORIGEN_PERMITIDO)
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0))
                .andExpect(cookie().path(COOKIE, "/api/v1/auth"))
                .andExpect(cookie().httpOnly(COOKIE, true));

        renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isUnauthorized());
        assertThat(sesionesAbiertas()).isZero();
    }

    @Test
    void cerrarSesionSinCookieTambienResponde204() throws Exception {
        cerrarSesion(null, ORIGEN_PERMITIDO).andExpect(status().isNoContent());
    }

    @Test
    void cerrarSesionDesdeUnOrigenAjenoResponde403YLaSesionSigue() throws Exception {
        String inicial = cookieDeLogin();

        cerrarSesion(new Cookie(COOKIE, inicial), ORIGEN_AJENO)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));

        renovar(inicial, ORIGEN_PERMITIDO).andExpect(status().isOk());
    }

    private ResultActions login() throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoLogin("ana@ejemplo.test", CLAVE)));
    }

    private String cookieDeLogin() throws Exception {
        Cookie cookie = login().andExpect(status().isOk()).andReturn().getResponse().getCookie(COOKIE);
        assertThat(cookie).as("cookie %s del login", COOKIE).isNotNull();
        return cookie.getValue();
    }

    private ResultActions renovar(String valorCookie, String origen) throws Exception {
        return mvc.perform(post("/api/v1/auth/renovar")
                .cookie(new Cookie(COOKIE, valorCookie))
                .header(HttpHeaders.ORIGIN, origen));
    }

    private ResultActions cerrarSesion(Cookie cookie, String origen) throws Exception {
        MockHttpServletRequestBuilder solicitud = post("/api/v1/auth/cerrar-sesion").header(HttpHeaders.ORIGIN, origen);
        if (cookie != null) {
            solicitud.cookie(cookie);
        }
        return mvc.perform(solicitud);
    }

    private int sesionesAbiertas() {
        return jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ? and revocado_en is null",
                Integer.class, ana);
    }
}
