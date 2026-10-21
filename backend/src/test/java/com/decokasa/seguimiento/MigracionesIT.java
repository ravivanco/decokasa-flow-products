package com.decokasa.seguimiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Migración inicial (paso 1.5) contra PostgreSQL 17 real: esquema, datos sembrados y restricciones de
 * modelo-datos.md §3.1. Cada prueba corre en una transacción que se deshace al terminar, así no se ensucian
 * entre sí. Solo usa datos ficticios (ADR-0015). Requiere Docker encendido.
 */
@Testcontainers
class MigracionesIT {

    // Códigos SQLSTATE de PostgreSQL.
    private static final String UNICIDAD = "23505";
    private static final String CLAVE_FORANEA = "23503";
    private static final String CHECK = "23514";
    private static final String NO_NULO = "23502";

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"));

    private static MigrateResult resultado;
    private static MigrateResult resultadoV1;
    private static MigrateResult resultadoV2;
    private static List<String> tablasV1;

    private Connection conexion;

    @BeforeAll
    static void migrar() {
        resultadoV1 = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("1").load().migrate();
        try (Connection inicial = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
                Statement consulta = inicial.createStatement();
                ResultSet filas = consulta.executeQuery("select table_name from information_schema.tables "
                        + "where table_schema = 'public' and table_name <> 'flyway_schema_history' order by table_name")) {
            tablasV1 = new ArrayList<>();
            while (filas.next()) tablasV1.add(filas.getString(1));
        } catch (SQLException error) {
            throw new IllegalStateException("No se pudo verificar el esquema V1 antes de aplicar V2", error);
        }
        resultadoV2 = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").target("2").load().migrate();
        resultado = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @BeforeEach
    void abrirTransaccion() throws SQLException {
        conexion = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        conexion.setAutoCommit(false);
    }

    @AfterEach
    void deshacer() throws SQLException {
        conexion.rollback();
        conexion.close();
    }

    // --- Esquema ---

    @Test
    void flywayAplicaLaVersionUnoSobreUnaBaseVacia() {
        assertThat(resultadoV1.success).isTrue();
        assertThat(resultadoV1.targetSchemaVersion).isEqualTo("1");
        assertThat(resultadoV1.migrationsExecuted).isEqualTo(1);
        assertThat(tablasV1).containsExactly(
                "pais", "rol", "rol_permiso", "token_renovacion", "usuario", "usuario_pais", "usuario_rol");
    }

    @Test
    void flywayCompletaVersionDosSinModificarLaUno() {
        assertThat(resultadoV2.success).isTrue();
        assertThat(resultadoV2.targetSchemaVersion).isEqualTo("2");
        assertThat(resultadoV2.migrationsExecuted).isEqualTo(1);
        assertThat(resultado.success).isTrue();
        assertThat(resultado.targetSchemaVersion).isEqualTo("18");
        assertThat(resultado.migrationsExecuted).isEqualTo(16);
    }

    @Test
    void reiniciarFlywayValidaLaMigracionSinReaplicarla() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration").load();

        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        MigrateResult segunda = flyway.migrate();

        assertThat(segunda.success).isTrue();
        assertThat(segunda.migrationsExecuted).isZero();
        assertThat(flyway.info().current().getVersion().toString()).isEqualTo("18");
        assertThat(flyway.info().applied()).extracting(m -> m.getVersion().toString()).containsExactly("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18");
        assertThat(flyway.info().pending()).isEmpty();
    }

    @Test
    void creaLasTablasDeV1MotorArchivosYBodegasHastaV15() throws SQLException {
        List<String> tablas = textos("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_name <> 'flyway_schema_history'
                order by table_name""");

        // Conserva V1–V3 y exige también las tablas nuevas de V4–V6; no acepta tablas inesperadas.
        assertThat(tablas).containsExactly(
                "adjunto", "archivo_cuota", "archivo_cuota_cerrojo", "aviso_vencimiento", "bodega", "carga_archivo", "carga_fragmento", "carpeta_drive", "configuracion_plazos", "definicion_etapa", "definicion_flujo", "documento_requerido", "entrada_etapa", "etapa_pedido", "evento_adjunto", "evento_pedido",
                "notificacion", "pais", "pedido", "rol", "rol_permiso", "secuencia_pedido", "token_renovacion", "transicion_permitida",
                "usuario", "usuario_pais", "usuario_rol");
    }

    @Test
    void elUsuarioNoGuardaElRolEnUnaColumna() throws SQLException {
        // Uno o varios roles por usuario (decisión del equipo, requerimientos 4.7): la relación vive en usuario_rol.
        assertThat(textos("""
                select column_name from information_schema.columns
                where table_name = 'usuario' and column_name like 'rol%'""")).isEmpty();
    }

    // --- Datos sembrados ---

    @Test
    void siembraEcuadorActivoYColombiaInactivo() throws SQLException {
        Map<String, Boolean> paises = new HashMap<>();
        try (Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("select codigo, nombre, activo from pais")) {
            while (filas.next()) {
                assertThat(filas.getString("nombre")).isNotBlank();
                paises.put(filas.getString("codigo"), filas.getBoolean("activo"));
            }
        }

        assertThat(paises).containsExactlyInAnyOrderEntriesOf(Map.of("EC", true, "CO", false));
    }

    @Test
    void siembraLosSieteRolesAcordados() throws SQLException {
        assertThat(textos("select codigo from rol")).containsExactlyInAnyOrder(
                "admin", "asistente_compras", "marketing", "compras_china", "logistica", "asistente_china", "consulta");
        assertThat(entero("select count(*) from rol where char_length(nombre) = 0")).isZero();
    }

    @Test
    void losRolesSembradosTienenIdentificadoresUuidVersion7Fijos() throws SQLException {
        // UUIDv7 válidos con la fecha de la decisión (2026-10-12T00:00:00Z), iguales en todos los entornos.
        long fechaDecision = Instant.parse("2026-10-12T00:00:00Z").toEpochMilli();
        List<UUID> ids = new ArrayList<>();
        try (Statement sentencia = conexion.createStatement();
                ResultSet filas = sentencia.executeQuery("select id from rol order by id")) {
            while (filas.next()) {
                ids.add(filas.getObject("id", UUID.class));
            }
        }

        assertThat(ids).hasSize(7).allSatisfy(id -> {
            assertThat(id.version()).isEqualTo(7);
            assertThat(id.variant()).isEqualTo(2);
            assertThat(id.getMostSignificantBits() >>> 16).isEqualTo(fechaDecision);
        });
        assertThat(idRol("admin")).isEqualTo(UUID.fromString("01a12d68-b800-7000-8000-000000000001"));
    }

    @Test
    void noSiembraUsuariosContrasenasNiSesiones() throws SQLException {
        // Los usuarios reales se cargan fuera de Git (ADR-0015).
        assertThat(entero("select count(*) from usuario")).isZero();
        assertThat(entero("select count(*) from usuario_rol")).isZero();
        assertThat(entero("select count(*) from usuario_pais")).isZero();
        assertThat(entero("select count(*) from token_renovacion")).isZero();
    }

    // --- Usuario ---

    @Test
    void unUsuarioNuevoQuedaActivoYSinIntentosFallidos() throws SQLException {
        UUID id = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        try (ResultSet fila = consultar("select activo, intentos_fallidos, bloqueado_hasta from usuario where id = ?", id)) {
            assertThat(fila.next()).isTrue();
            assertThat(fila.getBoolean("activo")).isTrue();
            assertThat(fila.getInt("intentos_fallidos")).isZero();
            assertThat(fila.getObject("bloqueado_hasta")).isNull();
        }
    }

    @Test
    void elCorreoEsUnicoSinDistinguirMayusculas() throws SQLException {
        insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(UNICIDAD, () -> insertarUsuario("ANA@Ejemplo.test", "ana2", "+593990000002"));
    }

    @Test
    void elNombreDeUsuarioEsUnicoSinDistinguirMayusculas() throws SQLException {
        insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(UNICIDAD, () -> insertarUsuario("otra@ejemplo.test", "ANA", "+593990000002"));
    }

    @Test
    void elTelefonoEsUnico() throws SQLException {
        insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(UNICIDAD, () -> insertarUsuario("otra@ejemplo.test", "otra", "+593990000001"));
    }

    @Test
    void elTelefonoSeGuardaNormalizado() {
        assertViolacion(CHECK, () -> insertarUsuario("ana@ejemplo.test", "ana", "+593 99 000 0001"));
    }

    @Test
    void elCorreoDebeTenerArroba() {
        assertViolacion(CHECK, () -> insertarUsuario("sin-arroba.test", "ana", "+593990000001"));
    }

    @Test
    void elNombreDeUsuarioTieneAlMenosTresCaracteres() {
        assertViolacion(CHECK, () -> insertarUsuario("ana@ejemplo.test", "an", "+593990000001"));
    }

    // Límites de campos: los mismos que openapi.yaml y los formularios (modelo-datos.md §3.1, "Límites de campos").

    @Test
    void losTextosDelUsuarioAceptanSuLongitudMaxima() throws SQLException {
        String correo = "a".repeat(254 - "@ejemplo.test".length()) + "@ejemplo.test";
        UUID id = insertarUsuario(correo, "u".repeat(50), "+123456789012345");

        ejecutar("update usuario set nombre = ? where id = ?", "n".repeat(200), id);
    }

    @Test
    void elNombreDelUsuarioNoPasaDeDoscientosCaracteres() throws SQLException {
        UUID id = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(CHECK, () -> ejecutar("update usuario set nombre = ? where id = ?", "n".repeat(201), id));
    }

    @Test
    void elCorreoNoPasaDeDoscientosCincuentaYCuatroCaracteres() {
        String correo = "a".repeat(255 - "@ejemplo.test".length()) + "@ejemplo.test";

        assertViolacion(CHECK, () -> insertarUsuario(correo, "ana", "+593990000001"));
    }

    @Test
    void elNombreDeUsuarioNoPasaDeCincuentaCaracteres() {
        assertViolacion(CHECK, () -> insertarUsuario("ana@ejemplo.test", "u".repeat(51), "+593990000001"));
    }

    // Una violación aborta la transacción de la prueba: cada caso va en su propia prueba.
    @Test
    void elTelefonoNoPasaDeQuinceDigitos() {
        assertViolacion(CHECK, () -> insertarUsuario("ana@ejemplo.test", "ana", "+1234567890123456"));
    }

    @Test
    void elTelefonoTieneAlMenosSieteDigitos() {
        assertViolacion(CHECK, () -> insertarUsuario("ana@ejemplo.test", "ana", "+123456"));
    }

    @Test
    void elNombreDelRolNoPasaDeCienCaracteres() {
        assertViolacion(CHECK, () -> ejecutar(
                "insert into rol (id, codigo, nombre, creado_en) values (?, 'rol_largo', ?, now())",
                UUID.randomUUID(), "r".repeat(101)));
    }

    @Test
    void losIntentosFallidosNoPuedenSerNegativos() throws SQLException {
        UUID id = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(CHECK, () -> ejecutar("update usuario set intentos_fallidos = -1 where id = ?", id));
    }

    @Test
    void laContrasenaEsObligatoria() {
        assertViolacion(NO_NULO, () -> ejecutar("""
                insert into usuario (id, nombre, correo, usuario, telefono, contrasena_hash, creado_en, actualizado_en, version)
                values (?, 'Ana Prueba', 'ana@ejemplo.test', 'ana', '+593990000001', null, now(), now(), 0)""",
                UUID.randomUUID()));
    }

    // --- Roles del usuario (usuario_rol) ---

    @Test
    void unUsuarioPuedeTenerVariosRoles() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        asignarRol(usuario, "compras_china");
        asignarRol(usuario, "logistica");

        assertThat(textos("""
                select r.codigo from usuario_rol ur join rol r on r.id = ur.rol_id
                where ur.usuario_id = '%s' order by r.codigo""".formatted(usuario)))
                .containsExactly("compras_china", "logistica");
    }

    @Test
    void elMismoRolNoSeAsignaDosVeces() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        asignarRol(usuario, "marketing");

        assertViolacion(UNICIDAD, () -> asignarRol(usuario, "marketing"));
    }

    @Test
    void noSeAsignaUnRolQueNoExiste() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(CLAVE_FORANEA, () -> ejecutar(
                "insert into usuario_rol (usuario_id, rol_id, asignado_en) values (?, ?, now())",
                usuario, UUID.randomUUID()));
    }

    @Test
    void unUsuarioSinRolesSeGuardaPeroNoTieneFilasEnUsuarioRol() throws SQLException {
        // La base lo permite (p. ej. una carga externa); el dominio y el login le niegan el acceso (paso 1.6).
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertThat(entero("select count(*) from usuario_rol where usuario_id = '%s'".formatted(usuario))).isZero();
    }

    @Test
    void noSeBorraUnRolAsignado() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        asignarRol(usuario, "logistica");

        assertViolacion(CLAVE_FORANEA, () -> ejecutar("delete from rol where codigo = 'logistica'"));
    }

    @Test
    void noSeBorraUnUsuarioConRolesOSesiones() throws SQLException {
        // Los usuarios se desactivan, no se borran; sin ON DELETE CASCADE nada de su rastro desaparece.
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        asignarRol(usuario, "logistica");

        assertViolacion(CLAVE_FORANEA, () -> ejecutar("delete from usuario where id = ?", usuario));
    }

    // --- Países del usuario ---

    @Test
    void unUsuarioTrabajaEnPaisesExistentes() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        ejecutar("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", usuario);

        assertViolacion(CLAVE_FORANEA, () -> ejecutar("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'PE')", usuario));
    }

    @Test
    void elMismoPaisNoSeAsignaDosVeces() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        ejecutar("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", usuario);

        assertViolacion(UNICIDAD, () -> ejecutar("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", usuario));
    }

    // --- Catálogos ---

    @Test
    void elCodigoDePaisSonDosMayusculas() {
        assertViolacion(CHECK, () -> ejecutar("insert into pais (codigo, nombre, activo) values ('ecu', 'Prueba', false)"));
    }

    @Test
    void elCodigoDeRolTieneElFormatoDelDominio() {
        // Mismo patrón que CodigoRol: ^[a-z_]{3,40}$.
        assertViolacion(CHECK, () -> ejecutar(
                "insert into rol (id, codigo, nombre, creado_en) values (?, 'Rol-Nuevo', 'Prueba', now())", UUID.randomUUID()));
    }

    @Test
    void elCodigoDeRolEsUnico() {
        assertViolacion(UNICIDAD, () -> ejecutar(
                "insert into rol (id, codigo, nombre, creado_en) values (?, 'admin', 'Otro admin', now())", UUID.randomUUID()));
    }

    @Test
    void unPermisoNoSeRepiteEnElMismoRol() throws SQLException {
        UUID rol = idRol("admin");
        ejecutar("insert into rol_permiso (rol_id, permiso) values (?, 'usuarios.gestionar')", rol);

        assertViolacion(UNICIDAD, () -> ejecutar("insert into rol_permiso (rol_id, permiso) values (?, 'usuarios.gestionar')", rol));
    }

    // --- Renovación de sesión (token_renovacion) ---

    @Test
    void elHashDelTokenDeRenovacionEsUnico() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");
        insertarToken(usuario, "hash-ficticio-1", "now() + interval '8 hours'");

        assertViolacion(UNICIDAD, () -> insertarToken(usuario, "hash-ficticio-1", "now() + interval '8 hours'"));
    }

    @Test
    void elTokenDeRenovacionVenceDespuesDeCrearse() throws SQLException {
        UUID usuario = insertarUsuario("ana@ejemplo.test", "ana", "+593990000001");

        assertViolacion(CHECK, () -> insertarToken(usuario, "hash-ficticio-2", "now() - interval '1 minute'"));
    }

    @Test
    void elTokenDeRenovacionPerteneceAUnUsuarioExistente() {
        assertViolacion(CLAVE_FORANEA, () -> insertarToken(UUID.randomUUID(), "hash-ficticio-3", "now() + interval '8 hours'"));
    }

    // --- Ayudas ---

    private UUID insertarUsuario(String correo, String usuario, String telefono) throws SQLException {
        UUID id = UUID.randomUUID();
        ejecutar("""
                insert into usuario (id, nombre, correo, usuario, telefono, contrasena_hash, creado_en, actualizado_en, version)
                values (?, 'Usuario Ficticio', ?, ?, ?, '$2a$12$hash-de-prueba', now(), now(), 0)""",
                id, correo, usuario, telefono);
        return id;
    }

    private void asignarRol(UUID usuario, String codigoRol) throws SQLException {
        ejecutar("insert into usuario_rol (usuario_id, rol_id, asignado_en) values (?, ?, now())", usuario, idRol(codigoRol));
    }

    private void insertarToken(UUID usuario, String hash, String expiraEn) throws SQLException {
        ejecutar("""
                insert into token_renovacion (id, usuario_id, familia_id, token_hash, creado_en, expira_en)
                values (?, ?, ?, ?, now(), %s)""".formatted(expiraEn),
                UUID.randomUUID(), usuario, UUID.randomUUID(), hash);
    }

    private UUID idRol(String codigo) throws SQLException {
        try (ResultSet fila = consultar("select id from rol where codigo = ?", codigo)) {
            assertThat(fila.next()).as("rol %s sembrado", codigo).isTrue();
            return fila.getObject("id", UUID.class);
        }
    }

    private void ejecutar(String sql, Object... parametros) throws SQLException {
        try (PreparedStatement sentencia = preparar(sql, parametros)) {
            sentencia.executeUpdate();
        }
    }

    // La sentencia se cierra con la conexión al terminar cada prueba.
    private ResultSet consultar(String sql, Object... parametros) throws SQLException {
        return preparar(sql, parametros).executeQuery();
    }

    private PreparedStatement preparar(String sql, Object... parametros) throws SQLException {
        PreparedStatement sentencia = conexion.prepareStatement(sql);
        for (int i = 0; i < parametros.length; i++) {
            sentencia.setObject(i + 1, parametros[i]);
        }
        return sentencia;
    }

    private List<String> textos(String sql) throws SQLException {
        List<String> valores = new ArrayList<>();
        try (Statement sentencia = conexion.createStatement(); ResultSet filas = sentencia.executeQuery(sql)) {
            while (filas.next()) {
                valores.add(filas.getString(1));
            }
        }
        return valores;
    }

    private long entero(String sql) throws SQLException {
        try (Statement sentencia = conexion.createStatement(); ResultSet fila = sentencia.executeQuery(sql)) {
            fila.next();
            return fila.getLong(1);
        }
    }

    private static void assertViolacion(String sqlState, AccionSql accion) {
        assertThatThrownBy(accion::ejecutar)
                .isInstanceOf(SQLException.class)
                .extracting(error -> ((SQLException) error).getSQLState())
                .isEqualTo(sqlState);
    }

    @FunctionalInterface
    private interface AccionSql {
        void ejecutar() throws SQLException;
    }
}
