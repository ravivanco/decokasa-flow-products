package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.iniciarSesion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * D10 (aprobado el 2026-10-13; `openapi.yaml` 0.5.0): `Usuario.version` en las respuestas de usuario y de sesión;
 * `PATCH /usuarios/{id}` exige la versión leída y, si ya no es la vigente, responde 409 VERSION_DESACTUALIZADA sin
 * aplicar ningún cambio. El inicio de sesión no cambia la versión y una edición no pisa sus contadores. Contra
 * PostgreSQL 17, en orden; la concurrencia está en `EdicionConcurrenteIT`. Datos ficticios. Requiere Docker.
 */
@SpringBootTest(properties = "CORS_ORIGENES=" + VersionUsuarioIT.ORIGEN)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class VersionUsuarioIT {

    static final String ORIGEN = "http://localhost:3000";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuariosEnBase base;
    private UUID ana;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        base = new UsuariosEnBase(jdbc);
        base.limpiar();
        base.crear("Admin Prueba", "admin@ejemplo.test", "adminprueba", "+593990000010", CLAVE, true, "admin");
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china");
        tokenAdmin = iniciarSesion(mvc, "admin@ejemplo.test", CLAVE);
    }

    // --- La versión en las respuestas ---

    @Test
    void lasRespuestasDeUsuarioIncluyenLaVersion() throws Exception {
        conToken(get("/api/v1/usuarios/{id}", ana)).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
        conToken(get("/api/v1/usuarios")).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.correo == 'ana@ejemplo.test')].version").value(0));
        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombre": "Carla Prueba", "correo": "carla@ejemplo.test", "usuario": "carlaprueba",
                 "telefono": "+593990000009", "roles": ["marketing"], "contrasenaInicial": "Clave-Nueva-1"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void lasRespuestasDeSesionIncluyenLaVersion() throws Exception {
        editar(ana, 0, "\"nombre\": \"Ana María Prueba\"").andExpect(status().isOk());

        MockHttpServletResponse login = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("ana@ejemplo.test", CLAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.version").value(1))
                .andReturn().getResponse();
        Cookie renovacion = login.getCookie("decokasa_renovacion");
        String acceso = JsonPath.read(login.getContentAsString(), "$.tokenAcceso");

        mvc.perform(get("/api/v1/auth/yo").header(HttpHeaders.AUTHORIZATION, "Bearer " + acceso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mvc.perform(post("/api/v1/auth/renovar").cookie(renovacion).header(HttpHeaders.ORIGIN, ORIGEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.version").value(1));
    }

    // --- PATCH con la versión vigente o vieja ---

    @Test
    void conLaVersionVigenteLaRespuestaLlevaLaVersionGuardada() throws Exception {
        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana Uno"));
        assertThat(versionDe(ana)).isEqualTo(1);

        editar(ana, 1, "\"roles\": [\"logistica\"]").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        assertThat(versionDe(ana)).isEqualTo(2);
    }

    @Test
    void conUnaVersionViejaResponde409SinCambiarNada() throws Exception {
        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk());

        editar(ana, 0, "\"nombre\": \"Ana Dos\", \"roles\": [\"logistica\"]")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"))
                .andExpect(jsonPath("$.errores").doesNotExist());

        assertThat(nombreDe(ana)).isEqualTo("Ana Uno");
        assertThat(versionDe(ana)).isEqualTo(1);
        assertThat(jdbc.queryForList("select r.codigo from usuario_rol ur join rol r on r.id = ur.rol_id "
                + "where ur.usuario_id = ?", String.class, ana)).containsExactly("compras_china");
    }

    @Test
    void desactivarConUnaVersionViejaNoDesactivaNiRevocaSesiones() throws Exception {
        iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);
        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk());

        editar(ana, 0, "\"activo\": false").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));

        assertThat(jdbc.queryForObject("select activo from usuario where id = ?", Boolean.class, ana)).isTrue();
        assertThat(jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ? "
                + "and revocado_en is null", Integer.class, ana)).isEqualTo(1);
        iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);
    }

    @Test
    void unaVersionViejaGanaAUnDatoRepetido() throws Exception {
        base.crear("Beto Prueba", "beto@ejemplo.test", "betoprueba", "+593990000002", CLAVE, true, "logistica");
        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk());

        editar(ana, 0, "\"correo\": \"beto@ejemplo.test\"").andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("VERSION_DESACTUALIZADA"));
    }

    // --- Versión ausente o inválida, o solo la versión: 400 sin cambios ---

    @ParameterizedTest(name = "version = {0}")
    @ValueSource(strings = {"AUSENTE", "null", "-1", "1.5", "0.0", "\"0\"", "true", "{}", "[0]",
        "99999999999999999999999"})
    void unaVersionAusenteOInvalidaResponde400SinCambios(String version) throws Exception {
        String cuerpo = version.equals("AUSENTE")
                ? "{\"nombre\": \"Otra Ana\"}"
                : "{\"version\": " + version + ", \"nombre\": \"Otra Ana\"}";

        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("version"));

        assertThat(nombreDe(ana)).isEqualTo("Ana Prueba");
        assertThat(versionDe(ana)).isZero();
    }

    @Test
    void unCuerpoConSoloLaVersionResponde400SinCambios() throws Exception {
        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));

        assertThat(versionDe(ana)).isZero();
    }

    // --- Login y edición, en los dos órdenes: ni se pierden intentos ni se pisa el bloqueo ---

    @Test
    void losLoginsNoCambianLaVersionYLaEdicionPosteriorConservaLosIntentos() throws Exception {
        fallarLogin(3);
        assertThat(versionDe(ana)).isZero();

        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        assertThat(base.intentosFallidos(ana)).isEqualTo(3);
    }

    @Test
    void trasUnaEdicionLosLoginsCuentanSinCambiarLaVersion() throws Exception {
        editar(ana, 0, "\"nombre\": \"Ana Uno\"").andExpect(status().isOk());

        fallarLogin(2);
        iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);
        fallarLogin(1);

        assertThat(versionDe(ana)).isEqualTo(1);
        assertThat(base.intentosFallidos(ana)).isEqualTo(1);
    }

    @Test
    void unaEdicionNoQuitaUnBloqueoActivo() throws Exception {
        fallarLogin(5);
        Timestamp bloqueadoHasta = bloqueadoHasta(ana);
        assertThat(bloqueadoHasta).isNotNull();

        editar(ana, 0, "\"nombre\": \"Ana Uno\", \"roles\": [\"logistica\"]").andExpect(status().isOk());

        assertThat(bloqueadoHasta(ana)).isEqualTo(bloqueadoHasta);
        assertThat(base.intentosFallidos(ana)).isEqualTo(5);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("ana@ejemplo.test", CLAVE)))
                .andExpect(status().is(423));
    }

    private void fallarLogin(int veces) throws Exception {
        for (int i = 0; i < veces; i++) {
            mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpoLogin("ana@ejemplo.test", "Clave-Incorrecta-1")))
                    .andExpect(status().isUnauthorized());
        }
    }

    private ResultActions editar(UUID usuarioId, long version, String campos) throws Exception {
        return conToken(patch("/api/v1/usuarios/{id}", usuarioId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": " + version + ", " + campos + "}"));
    }

    private ResultActions conToken(MockHttpServletRequestBuilder solicitud) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAdmin));
    }

    private long versionDe(UUID usuarioId) {
        return jdbc.queryForObject("select version from usuario where id = ?", Long.class, usuarioId);
    }

    private String nombreDe(UUID usuarioId) {
        return jdbc.queryForObject("select nombre from usuario where id = ?", String.class, usuarioId);
    }

    private Timestamp bloqueadoHasta(UUID usuarioId) {
        return jdbc.queryForObject("select bloqueado_hasta from usuario where id = ?", Timestamp.class, usuarioId);
    }
}
