package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.claims;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.iniciarSesion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * /api/v1/usuarios y /api/v1/roles contra PostgreSQL 17 real: solo el admin gestiona (R6), uno o varios roles con al
 * menos uno, desactivar en lugar de quitar roles, límites de campos y 72 bytes (requerimientos 4.7, modelo-datos 3.1).
 * Datos ficticios. Requiere Docker encendido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UsuarioControllerIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    private UsuariosEnBase base;
    private UUID ana;
    private UUID adminId;

    @BeforeEach
    void preparar() {
        base = new UsuariosEnBase(jdbc);
        base.limpiar();
        adminId = base.crear("Admin Prueba", "admin@ejemplo.test", "adminprueba", "+593990000010", CLAVE, true,
                "admin");
        base.crear("Mario Prueba", "mario@ejemplo.test", "marioprueba", "+593990000011", CLAVE, true, "marketing");
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china");
    }

    // --- Solo el admin (R6) ---

    @Test
    void quienNoEsAdminNoGestionaUsuariosNiRoles() throws Exception {
        String token = iniciarSesion(mvc, "mario@ejemplo.test", CLAVE);

        conToken(get("/api/v1/usuarios"), token).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[\"marketing\"]", "Clave-Nueva-1")), token)
                .andExpect(status().isForbidden());
        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"roles\": [\"admin\"]}"), token)
                .andExpect(status().isForbidden());
        conToken(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"bodega_quito\", \"nombre\": \"Bodega Quito\", \"permisos\": []}"), token)
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("select count(*) from usuario_rol ur join rol r on r.id = ur.rol_id "
                + "where ur.usuario_id = ? and r.codigo = 'admin'", Integer.class, ana)).isZero();
    }

    @Test
    void elAdminListaLosUsuariosConSusRoles() throws Exception {
        conToken(get("/api/v1/usuarios"), tokenAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.correo == 'ana@ejemplo.test')].roles[*]").value(hasItem("compras_china")))
                .andExpect(jsonPath("$[*].contrasenaHash").isEmpty());
    }

    @Test
    void unUsuarioConVariosRolesQueIncluyenAdminTambienGestiona() throws Exception {
        base.crear("Eva Prueba", "eva@ejemplo.test", "evaprueba", "+593990000012", CLAVE, true, "compras_china",
                "admin");

        conToken(get("/api/v1/usuarios"), iniciarSesion(mvc, "eva@ejemplo.test", CLAVE)).andExpect(status().isOk());
    }

    // --- Alta con varios roles ---

    @Test
    void elAdminCreaUnUsuarioConVariosRolesQueLuegoPuedeEntrar() throws Exception {
        String respuesta = conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[\"compras_china\", \"logistica\"]", "Clave-Nueva-1")), tokenAdmin())
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("compras_china", "logistica")))
                .andExpect(jsonPath("$.telefono").value("+593990000009"))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String id = JsonPath.read(respuesta, "$.id");
        UUID creado = UUID.fromString(id);

        assertThat(jdbc.queryForObject("select count(*) from usuario_rol where usuario_id = ?", Integer.class, creado))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("select contrasena_hash from usuario where id = ?", String.class, creado))
                .startsWith("$2").isNotEqualTo("Clave-Nueva-1");
        List<String> roles = JsonPath.read(claims(iniciarSesion(mvc, "carla@ejemplo.test", "Clave-Nueva-1")),
                "$.roles");
        assertThat(roles).containsExactlyInAnyOrder("compras_china", "logistica");
    }

    @Test
    void crearSinRolesEsUnErrorDeValidacion() throws Exception {
        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[]", "Clave-Nueva-1")), tokenAdmin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("roles")));

        assertThat(cantidadDe("carla@ejemplo.test")).isZero();
    }

    @Test
    void elCorreoDuplicadoRespondeConflictoEnElCampoCorreo() throws Exception {
        String duplicado = """
                {"nombre": "Otra Ana", "correo": "ANA@Ejemplo.test", "usuario": "otraana",
                 "telefono": "+593 99 000 0009", "roles": ["marketing"], "paises": ["EC"],
                 "contrasenaInicial": "Clave-Nueva-1"}""";

        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON).content(duplicado), tokenAdmin())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("DUPLICADO"))
                .andExpect(jsonPath("$.errores[0].campo").value("correo"));
    }

    // --- Contraseña: 8 caracteres a 72 bytes en UTF-8 ---

    @Test
    void unaContrasenaInicialDeSetentaYTresBytesSeRechazaEnSuCampo() throws Exception {
        // 37 caracteres: pasa maxLength 72, pero son 73 bytes en UTF-8.
        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[\"marketing\"]", "ñ".repeat(36) + "a")), tokenAdmin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[*].campo", hasItem("contrasenaInicial")));

        assertThat(cantidadDe("carla@ejemplo.test")).isZero();
    }

    @Test
    void unaContrasenaInicialDeSetentaYDosBytesSirveSinRecortes() throws Exception {
        String clave72 = "ñ".repeat(36);
        conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[\"marketing\"]", clave72)), tokenAdmin())
                .andExpect(status().isCreated());

        iniciarSesion(mvc, "carla@ejemplo.test", clave72);
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("carla@ejemplo.test", clave72 + "x")))
                .andExpect(status().isUnauthorized());
    }

    // --- Edición: al menos un rol; para retirar el acceso se desactiva ---

    @Test
    void elAdminReemplazaLosRolesPorVarios() throws Exception {
        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"roles\": [\"logistica\", \"marketing\"]}"), tokenAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", containsInAnyOrder("logistica", "marketing")));
    }

    @Test
    void noSePuedeDejarAUnUsuarioSinRoles() throws Exception {
        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"roles\": []}"), tokenAdmin())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores[*].campo", hasItem("roles")));

        assertThat(jdbc.queryForObject("select count(*) from usuario_rol where usuario_id = ?", Integer.class, ana))
                .isEqualTo(1);
    }

    @Test
    void desactivarImpideEntrarYConservaAlUsuario() throws Exception {
        iniciarSesion(mvc, "ana@ejemplo.test", CLAVE);

        conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"activo\": false}"), tokenAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin("ana@ejemplo.test", CLAVE)))
                .andExpect(status().isUnauthorized());
        assertThat(jdbc.queryForObject("select count(*) from usuario where id = ?", Integer.class, ana)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ? "
                + "and revocado_en is null", Integer.class, ana)).isZero();
    }

    // --- Roles ---

    @Test
    void elAdminCreaUnRolYLoVeEnLaLista() throws Exception {
        String token = tokenAdmin();

        conToken(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"bodega_quito\", \"nombre\": \"Bodega Quito\", \"permisos\": []}"), token)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("bodega_quito"));
        conToken(get("/api/v1/roles"), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo", hasItem("bodega_quito")))
                .andExpect(jsonPath("$[*].codigo", hasItem("consulta")));
    }

    @Test
    void elAdminActualizaUnRol() throws Exception {
        String logistica = "01a12d68-b800-7000-8000-000000000005";

        conToken(put("/api/v1/roles/{id}", logistica).contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"logistica\", \"nombre\": \"Logística Ecuador\", "
                        + "\"permisos\": [\"bodegas.gestionar\"]}"), tokenAdmin())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(logistica))
                .andExpect(jsonPath("$.nombre").value("Logística Ecuador"))
                .andExpect(jsonPath("$.permisos[0]").value("bodegas.gestionar"));
        assertThat(jdbc.queryForObject("select nombre from rol where codigo = 'logistica'", String.class))
                .isEqualTo("Logística Ecuador");
        // Deja el catálogo como lo sembró V1 para las demás pruebas.
        jdbc.update("update rol set nombre = 'Logística' where codigo = 'logistica'");
    }

    // --- Detalle de un usuario ---

    @Test
    void elAdminConsultaUnUsuarioYUnoInexistenteDa404() throws Exception {
        String token = tokenAdmin();

        conToken(get("/api/v1/usuarios/{id}", ana), token)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@ejemplo.test"))
                .andExpect(jsonPath("$.roles", containsInAnyOrder("compras_china")))
                .andExpect(jsonPath("$.paises", containsInAnyOrder("EC")));
        conToken(get("/api/v1/usuarios/{id}", UUID.randomUUID()), token)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NO_ENCONTRADO"));
    }

    // --- Hallazgo A1: un token de acceso anterior al cambio ya no administra (estado y roles de la base) ---

    @Test
    void unAdminDesactivadoNoAdministraNiSeReactivaConSuTokenAnterior() throws Exception {
        String tokenAnterior = tokenAdmin();
        conToken(patch("/api/v1/usuarios/{id}", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"activo\": false}"), tokenOtroAdmin())
                .andExpect(status().isOk());

        comprobarQueNoAdministra(tokenAnterior);
        assertThat(jdbc.queryForObject("select activo from usuario where id = ?", Boolean.class, adminId)).isFalse();
        assertThat(rolesDe(adminId)).containsExactly("admin");
    }

    @Test
    void unAdminSinElRolAdminNoAdministraNiLoRecuperaConSuTokenAnterior() throws Exception {
        String tokenAnterior = tokenAdmin();
        conToken(patch("/api/v1/usuarios/{id}", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": 0, \"roles\": [\"marketing\"]}"), tokenOtroAdmin())
                .andExpect(status().isOk());

        comprobarQueNoAdministra(tokenAnterior);
        assertThat(jdbc.queryForObject("select activo from usuario where id = ?", Boolean.class, adminId)).isTrue();
        assertThat(rolesDe(adminId)).containsExactly("marketing");
    }

    /**
     * Con el token anterior (que todavía dice "admin" y no ha vencido): no se reactiva, no recupera el rol admin, no
     * crea ni convierte a otro en administrador y no consulta ni cambia usuarios o roles. Todo responde 403.
     */
    private void comprobarQueNoAdministra(String token) throws Exception {
        assertThat(claims(token)).contains("\"admin\"");

        // Con la versión vigente (D10): el 403 se debe al permiso, no a la versión.
        sinPermiso(conToken(patch("/api/v1/usuarios/{id}", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": %d, \"activo\": true}".formatted(versionDe(adminId))), token));
        sinPermiso(conToken(patch("/api/v1/usuarios/{id}", adminId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": %d, \"roles\": [\"admin\"]}".formatted(versionDe(adminId))), token));
        sinPermiso(conToken(post("/api/v1/usuarios").contentType(MediaType.APPLICATION_JSON)
                .content(nuevoUsuario("[\"admin\"]", "Clave-Nueva-1")), token));
        sinPermiso(conToken(patch("/api/v1/usuarios/{id}", ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\": %d, \"roles\": [\"admin\"]}".formatted(versionDe(ana))), token));
        sinPermiso(conToken(get("/api/v1/usuarios"), token));
        sinPermiso(conToken(get("/api/v1/usuarios/{id}", ana), token));
        sinPermiso(conToken(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"bodega_quito\", \"nombre\": \"Bodega Quito\", \"permisos\": []}"), token));
        sinPermiso(conToken(put("/api/v1/roles/{id}", "01a12d68-b800-7000-8000-000000000005")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"codigo\": \"logistica\", \"nombre\": \"Otra\", \"permisos\": []}"), token));
        sinPermiso(conToken(get("/api/v1/roles"), token));

        assertThat(cantidadDe("carla@ejemplo.test")).isZero();
        assertThat(rolesDe(ana)).containsExactly("compras_china");
        assertThat(jdbc.queryForObject("select count(*) from rol where codigo = 'bodega_quito'", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("select nombre from rol where codigo = 'logistica'", String.class))
                .isEqualTo("Logística");
    }

    private static void sinPermiso(ResultActions respuesta) throws Exception {
        respuesta.andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("SIN_PERMISO"));
    }

    private long versionDe(UUID usuarioId) {
        return jdbc.queryForObject("select version from usuario where id = ?", Long.class, usuarioId);
    }

    private List<String> rolesDe(UUID usuarioId) {
        return jdbc.queryForList("select r.codigo from usuario_rol ur join rol r on r.id = ur.rol_id "
                + "where ur.usuario_id = ? order by r.codigo", String.class, usuarioId);
    }

    private String tokenOtroAdmin() throws Exception {
        base.crear("Eva Prueba", "eva@ejemplo.test", "evaprueba", "+593990000012", CLAVE, true, "admin");
        return iniciarSesion(mvc, "eva@ejemplo.test", CLAVE);
    }

    private String tokenAdmin() throws Exception {
        return iniciarSesion(mvc, "admin@ejemplo.test", CLAVE);
    }

    private ResultActions conToken(MockHttpServletRequestBuilder solicitud, String token) throws Exception {
        return mvc.perform(solicitud.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    private int cantidadDe(String correo) {
        return jdbc.queryForObject("select count(*) from usuario where lower(correo) = lower(?)", Integer.class,
                correo);
    }

    private static String nuevoUsuario(String roles, String contrasena) {
        return """
                {"nombre": "Carla Prueba", "correo": "carla@ejemplo.test", "usuario": "carlaprueba",
                 "telefono": "+593 99 000 0009", "roles": %s, "paises": ["EC"], "contrasenaInicial": "%s"}"""
                .formatted(roles, contrasena);
    }
}
