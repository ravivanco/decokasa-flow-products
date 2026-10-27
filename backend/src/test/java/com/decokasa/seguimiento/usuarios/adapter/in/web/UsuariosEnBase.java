package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prepara usuarios ficticios directamente en PostgreSQL, sin pasar por el código de producción, y ofrece ayudas
 * para iniciar sesión por la API. Las contraseñas de prueba se codifican aquí con BCrypt (fuerza 4, solo pruebas).
 */
final class UsuariosEnBase {

    static final String CLAVE = "Clave-Prueba-1";

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(4);

    private final JdbcTemplate jdbc;

    UsuariosEnBase(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Deja la base como la deja la migración V1: sin usuarios, sesiones ni roles creados por las pruebas. */
    void limpiar() {
        jdbc.update("delete from token_renovacion");
        jdbc.update("delete from usuario_rol");
        jdbc.update("delete from usuario_pais");
        jdbc.update("delete from usuario");
        jdbc.update("delete from rol_permiso");
        jdbc.update("delete from rol where id::text not like '01a12d68-b800-7000-8000-%'");
    }

    UUID crear(String nombre, String correo, String usuario, String telefono, String clave, boolean activo,
            String... roles) {
        UUID id = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("""
                insert into usuario (id, nombre, correo, usuario, telefono, contrasena_hash, activo,
                                     creado_en, actualizado_en, version)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, 0)""",
                id, nombre, correo, usuario, telefono, BCRYPT.encode(clave), activo, ahora, ahora);
        for (String rol : roles) {
            jdbc.update("insert into usuario_rol (usuario_id, rol_id, asignado_en) "
                    + "select ?, id, ? from rol where codigo = ?", id, ahora, rol);
        }
        jdbc.update("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", id);
        return id;
    }

    int intentosFallidos(UUID usuarioId) {
        return jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class, usuarioId);
    }

    static String cuerpoLogin(String identificador, String contrasena) {
        return """
                {"identificador": "%s", "contrasena": "%s"}""".formatted(identificador, contrasena);
    }

    /** Inicia sesión por la API y devuelve el token de acceso; falla si la respuesta no es 200. */
    static String iniciarSesion(MockMvc mvc, String identificador, String contrasena) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoLogin(identificador, contrasena)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(respuesta, "$.tokenAcceso");
    }

    /** Contenido (payload) de un JWT, sin verificar la firma: solo para revisar sus claims en las pruebas. */
    static String claims(String jwt) {
        String[] partes = jwt.split("\\.");
        return new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
    }
}
