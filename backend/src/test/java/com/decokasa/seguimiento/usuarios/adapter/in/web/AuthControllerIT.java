package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.claims;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.iniciarSesion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * POST /api/v1/auth/login contra PostgreSQL 17 real (contrato: SolicitudInicioSesion, RespuestaSesion, Problema).
 * Cubre D2 (bloqueo), D3 (acceso de 15 minutos) y D4 (roles, cuentas sin roles o desactivadas). Los atributos de la
 * cookie de renovación y /auth/renovar por HTTP dependen de D3b (propuesta pendiente) y no se prueban aquí; la
 * renovación se prueba en RenovarSesionTest. Datos ficticios. Requiere Docker encendido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AuthControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuariosEnBase base;
    private UUID ana;

    @BeforeEach
    void preparar() {
        base = new UsuariosEnBase(jdbc);
        base.limpiar();
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china", "logistica");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ANA@Ejemplo.test", "AnaPrueba", "+593 99 000 0001"})
    void entraConCorreoUsuarioOTelefono(String identificador) throws Exception {
        login(identificador, CLAVE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").isNotEmpty())
                .andExpect(jsonPath("$.expiraEn").value(900))
                .andExpect(jsonPath("$.usuario.id").value(ana.toString()))
                .andExpect(jsonPath("$.usuario.correo").value("ana@ejemplo.test"))
                .andExpect(jsonPath("$.usuario.roles", containsInAnyOrder("compras_china", "logistica")))
                .andExpect(jsonPath("$.tokenRenovacion").doesNotExist())
                .andExpect(jsonPath("$.usuario.contrasenaHash").doesNotExist());
    }

    @Test
    void elTokenDeAccesoLlevaElIdYTodosLosRolesYDuraQuinceMinutos() throws Exception {
        String claims = claims(iniciarSesion(mvc, "ana@ejemplo.test", CLAVE));

        String sujeto = JsonPath.read(claims, "$.sub");
        List<String> roles = JsonPath.read(claims, "$.roles");
        Number emitido = JsonPath.read(claims, "$.iat");
        Number vence = JsonPath.read(claims, "$.exp");
        assertThat(sujeto).isEqualTo(ana.toString());
        assertThat(roles).containsExactlyInAnyOrder("compras_china", "logistica");
        assertThat(vence.longValue() - emitido.longValue()).isEqualTo(900);
    }

    @Test
    void unaContrasenaIncorrectaResponde401YSumaUnIntento() throws Exception {
        login("ana@ejemplo.test", "Clave-Mala-1")
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));

        assertThat(base.intentosFallidos(ana)).isEqualTo(1);
    }

    @Test
    void desactivadosSinRolesEInexistentesRecibenLaMismaRespuestaQueUnaContrasenaIncorrecta() throws Exception {
        base.crear("Beto Prueba", "beto@ejemplo.test", "betoprueba", "+593990000002", CLAVE, false, "logistica");
        base.crear("Carla Prueba", "carla@ejemplo.test", "carlaprueba", "+593990000003", CLAVE, true);
        // Control: la misma contraseña sí funciona para una cuenta activa con roles.
        login("ana@ejemplo.test", CLAVE).andExpect(status().isOk());

        String claveMala = cuerpo(login("ana@ejemplo.test", "Clave-Mala-1").andExpect(status().isUnauthorized()));
        String desactivado = cuerpo(login("beto@ejemplo.test", CLAVE).andExpect(status().isUnauthorized()));
        String sinRoles = cuerpo(login("carla@ejemplo.test", CLAVE).andExpect(status().isUnauthorized()));
        String inexistente = cuerpo(login("nadie@ejemplo.test", CLAVE).andExpect(status().isUnauthorized()));

        String codigo = JsonPath.read(claveMala, "$.codigo");
        assertThat(codigo).isEqualTo("NO_AUTENTICADO");
        assertThat(List.of(desactivado, sinRoles, inexistente)).allSatisfy(c -> assertThat(c).isEqualTo(claveMala));
    }

    @Test
    void trasCincoFallosLaCuentaSeBloqueaQuinceMinutos() throws Exception {
        for (int i = 0; i < 5; i++) {
            login("ana@ejemplo.test", "Clave-Mala-1").andExpect(status().isUnauthorized());
        }

        login("ana@ejemplo.test", CLAVE)
                .andExpect(status().is(423))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("CUENTA_BLOQUEADA"));
        assertThat(jdbc.queryForObject(
                "select bloqueado_hasta - now() between interval '14 minutes' and interval '15 minutes' "
                        + "from usuario where id = ?", Boolean.class, ana)).isTrue();
    }

    @Test
    void unaContrasenaDeMasDeSetentaYDosBytesNoEntraNiSeRecorta() throws Exception {
        String clave72 = "ñ".repeat(36);
        base.crear("Dina Prueba", "dina@ejemplo.test", "dinaprueba", "+593990000004", clave72, true, "marketing");

        login("dina@ejemplo.test", clave72).andExpect(status().isOk());
        login("dina@ejemplo.test", clave72 + "x")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void unIdentificadorVacioEsUnErrorDeValidacion() throws Exception {
        login("   ", CLAVE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("identificador"));
    }

    private ResultActions login(String identificador, String contrasena) throws Exception {
        return mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpoLogin(identificador, contrasena)));
    }

    @Test
    void yoDevuelveElUsuarioConTodosSusRolesYSusPermisos() throws Exception {
        String token = iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);

        mvc.perform(get("/api/v1/auth/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ana.toString()))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("compras_china", "logistica")))
                .andExpect(jsonPath("$.permisos").isArray());
    }

    @Test
    void unTokenManipuladoNoAutentica() throws Exception {
        String token = iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);
        String manipulado = token.substring(0, token.length() - 2) + (token.endsWith("AA") ? "BB" : "AA");

        mvc.perform(get("/api/v1/auth/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + manipulado))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    private static String cuerpo(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
