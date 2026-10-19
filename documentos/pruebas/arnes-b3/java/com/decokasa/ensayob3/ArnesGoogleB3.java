package com.decokasa.ensayob3;

import com.decokasa.seguimiento.SeguimientoApplication;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.env.MapPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Main exclusivamente opt-in del source set ensayoB3; nunca se descubre desde produccion. */
public final class ArnesGoogleB3 {
    private final Path raiz;
    private final UUID id;
    private final UUID admin;
    private final Properties privado;
    private final Properties origen;
    private PostgreSQLContainer postgres;
    private ConfigurableApplicationContext contexto;
    private final AtomicBoolean detenido = new AtomicBoolean();

    private ArnesGoogleB3(Path raiz, Path repo) throws IOException {
        this.raiz = GuardasB3.externo(raiz, repo);
        this.id = UUID.fromString(raiz.getFileName().toString());
        admin = UUID.nameUUIDFromBytes((id + ":admin").getBytes(StandardCharsets.UTF_8));
        // Solo extrae rutas/cuenta del archivo ignorado existente; nunca usa su DB/JWT/tokens habituales.
        Path configuracion = repo.resolve("backend/.env.local");
        GuardasB3.exigir(Files.isRegularFile(configuracion, LinkOption.NOFOLLOW_LINKS));
        origen = new Properties();
        try (var entrada = Files.newBufferedReader(configuracion, StandardCharsets.UTF_8)) { origen.load(entrada); }
        GuardasB3.receptor(origen.getProperty("GOOGLE_CUENTA_EMAIL"));
        for (String k : List.of("GOOGLE_OAUTH_CLIENTE_RUTA", "GOOGLE_OAUTH_LLAVE_RUTA")) {
            Path p = Path.of(origen.getProperty(k, ""));
            GuardasB3.externo(p.getParent(), repo); GuardasB3.privado(p);
        }
        Path archivo = raiz.resolve("ensayo-privado.properties");
        if (!Files.exists(archivo, LinkOption.NOFOLLOW_LINKS)) {
            Properties p = new Properties();
            p.setProperty("ensayo", id.toString());
            p.setProperty("db-password", UUID.randomUUID().toString() + UUID.randomUUID());
            p.setProperty("jwt-secret", UUID.randomUUID().toString() + UUID.randomUUID());
            crearPrivado(archivo);
            try (var salida = Files.newOutputStream(archivo)) { p.store(salida, "Privado B3; no copiar a Git o evidencia"); }
        }
        privado = leer(archivo);
        GuardasB3.exigir(id.toString().equals(privado.getProperty("ensayo")));
        GuardasB3.exigir(privado.getProperty("db-password", "").length() >= 32
                && privado.getProperty("jwt-secret", "").length() >= 32);
        Path disco = raiz.resolve("archivos");
        if (!Files.exists(disco)) {
            if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
                Files.createDirectory(disco, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            else Files.createDirectory(disco);
        }
        GuardasB3.externo(disco, repo);
    }

    public static void main(String[] args) {
        // Incluso con argumentos validos, stdin redirigido/Gradle no puede iniciar operaciones.
        Console consola = System.console();
        if (consola == null || args.length != 2) {
            System.err.println("Bloqueado: requiere consola interactiva, directorio externo UUID y ruta del repositorio.");
            return;
        }
        if (!GuardasB3.confirmar(consola.readLine("Escribe INICIAR ENTORNO AISLADO 3.B3: "),
                "INICIAR ENTORNO AISLADO 3.B3")) return;
        ArnesGoogleB3 arnes = null;
        try {
            Path root = GuardasB3.externo(Path.of(args[0]), Path.of(args[1]));
            try (FileChannel canal = FileChannel.open(root.resolve(".b3.lock"),
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
                    FileLock bloqueo = canal.tryLock()) {
                GuardasB3.exigir(bloqueo != null);
                try (ServerSocket probe = new ServerSocket()) {
                    probe.bind(new InetSocketAddress("127.0.0.1", 8080));
                }
                arnes = new ArnesGoogleB3(root, Path.of(args[1]));
                ArnesGoogleB3 actual = arnes;
                Runtime.getRuntime().addShutdownHook(new Thread(actual::detener));
                try {
                    arnes.iniciarBase();
                    arnes.abrir(false, false, false);
                    arnes.prepararAdmin(consola);
                    arnes.menu(consola);
                } finally { actual.detener(); } // Detener antes de liberar el cerrojo exclusivo del directorio.
            }
        } catch (Exception error) {
            // Ningun mensaje/stack/cause ajeno se imprime: puede contener credenciales o URL privada.
            System.err.println("Arnes bloqueado o interrumpido. Conservar datos; revisar precondiciones sin compartir secretos.");
        } finally { if (arnes != null) arnes.detener(); }
    }

    private void iniciarBase() {
        String volumen = "decokasa-b3-" + id;
        String propietario = UUID.nameUUIDFromBytes(raiz.toString().getBytes(StandardCharsets.UTF_8)).toString();
        var docker = DockerClientFactory.instance().client();
        GuardasB3.exigir(docker.listContainersCmd().withShowAll(false).exec().stream()
                .noneMatch(c -> c.getLabels() != null && id.toString().equals(c.getLabels().get("decokasa.b3"))));
        try {
            var existente = docker.inspectVolumeCmd(volumen).exec();
            GuardasB3.exigir(existente.getLabels() != null
                    && id.toString().equals(existente.getLabels().get("decokasa.b3"))
                    && propietario.equals(existente.getLabels().get("decokasa.b3.root")));
        } catch (NotFoundException ausente) {
            docker.createVolumeCmd().withName(volumen)
                    .withLabels(Map.of("decokasa.b3", id.toString(), "decokasa.b3.root", propietario)).exec();
        }
        postgres = new PostgreSQLContainer("postgres:17")
                .withDatabaseName("b3_" + id.toString().replace("-", ""))
                .withUsername("b3_admin").withPassword(privado.getProperty("db-password"))
                .withReuse(false).withLabel("decokasa.b3", id.toString())
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withBinds(new Bind(volumen, new Volume("/var/lib/postgresql/data")))
                        .withPortBindings(new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 0), new ExposedPort(5432))));
        postgres.start();
        GuardasB3.exigir(Set.of("127.0.0.1", "localhost").contains(postgres.getHost()));
    }

    private void abrir(boolean oauth, boolean drive, boolean gmail) throws IOException {
        var modo = GuardasB3.modo(oauth, drive, gmail);
        cerrarContexto();
        Map<String, Object> valores = new HashMap<>();
        valores.put("server.address", "127.0.0.1"); valores.put("server.port", 8080);
        valores.put("spring.datasource.url", postgres.getJdbcUrl());
        valores.put("spring.datasource.username", postgres.getUsername());
        valores.put("spring.datasource.password", privado.getProperty("db-password"));
        valores.put("decokasa.jwt.secreto", privado.getProperty("jwt-secret"));
        valores.put("decokasa.semilla.habilitada", false);
        valores.put("decokasa.archivos.directorio", raiz.resolve("archivos").toString());
        valores.put("decokasa.archivos.tamano-maximo", "100MB");
        valores.put("decokasa.cors.origenes", "http://localhost:3000");
        valores.putAll(modo);
        valores.put("decokasa.google.drive.worker.lote", 1);
        valores.put("decokasa.google.gmail.worker.lote", 1);
        valores.put("decokasa.google.oauth.cuenta-email", GuardasB3.CUENTA);
        valores.put("decokasa.google.oauth.cliente-ruta", origen.getProperty("GOOGLE_OAUTH_CLIENTE_RUTA"));
        valores.put("decokasa.google.oauth.llave-ruta", origen.getProperty("GOOGLE_OAUTH_LLAVE_RUTA"));
        valores.put("decokasa.google.oauth.tokens-ruta", raiz.resolve("tokens-3b3.enc").toString());
        valores.put("b3.admin-id", admin.toString());
        valores.put("logging.level.root", "WARN");
        valores.put("server.tomcat.accesslog.enabled", false);
        var app = new SpringApplication(SeguimientoApplication.class, DirectorioB3.class);
        app.setRegisterShutdownHook(false); app.setLogStartupInfo(false); app.setBannerMode(Banner.Mode.OFF);
        app.addInitializers(c -> c.getEnvironment().getPropertySources().addFirst(new MapPropertySource("b3-aislado", valores)));
        // No importa el .env.local de produccion. Secretos solo en memoria, nunca en argv del SO.
        contexto = app.run("--spring.config.import=");
    }

    private JdbcTemplate sql() { return contexto.getBean(JdbcTemplate.class); }
    private void prepararAdmin(Console consola) {
        long usuarios = sql().queryForObject("select count(*) from usuario", Long.class);
        GuardasB3.exigir(usuarios <= 1);
        char[] caracteres = consola.readPassword(usuarios == 0
                ? "Nueva contrasena del admin exclusivo b3-admin (12-72 bytes): "
                : "Contrasena del admin exclusivo b3-admin: ");
        GuardasB3.exigir(caracteres != null);
        try {
            String clave = new String(caracteres);
            new com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva(clave);
            GuardasB3.exigir(clave.getBytes(StandardCharsets.UTF_8).length >= 12
                    && clave.getBytes(StandardCharsets.UTF_8).length <= 72);
            var encoder = new BCryptPasswordEncoder(12);
            if (usuarios == 0) {
                var tx = new org.springframework.transaction.support.TransactionTemplate(
                        contexto.getBean(org.springframework.transaction.PlatformTransactionManager.class));
                tx.executeWithoutResult(s -> {
                    sql().update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Admin ensayo B3',?,'b3-admin','+593990000099',?,now(),now(),0)", admin, GuardasB3.CUENTA, encoder.encode(clave));
                    sql().update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo='admin'", admin);
                    sql().update("insert into usuario_pais(usuario_id,pais_codigo) values (?,'EC')", admin);
                });
            } else {
                String hash = sql().queryForObject("select contrasena_hash from usuario where id=? and usuario='b3-admin' and activo", String.class, admin);
                GuardasB3.exigir(encoder.matches(clave, hash));
            }
        } finally { Arrays.fill(caracteres, '\0'); }
    }

    private void menu(Console consola) throws IOException {
        System.out.println("Entorno aislado listo. Usuario b3-admin; http://localhost:8080. Todos los adaptadores apagados.");
        while (true) {
            String accion = consola.readLine("Accion [estado, autorizar, copia, correo, salir]: ");
            if (accion == null || "salir".equals(accion)) return;
            try {
                switch (accion) {
                    case "estado" -> estado();
                    case "autorizar" -> {
                        confirmar(consola, "AUTORIZAR GOOGLE SOLO 3.B3");
                        abrir(true, false, false);
                        System.out.println("Solo OAuth habilitado: seguir procedimiento manual, mismo navegador/localhost. Nada se copia o envia.");
                        consola.readLine("Enter para apagar OAuth y volver al entorno local: ");
                        abrir(false, false, false);
                    }
                    case "copia" -> {
                        confirmar(consola, "COPIAR UN ARCHIVO EN DRIVE 3.B3");
                        exigirArchivo(); exigirTokens();
                        abrir(true, true, false);
                        contexto.getBean(ReintentarCopiasPendientes.class).ejecutar();
                        evidencia("copia");
                        abrir(false, false, false);
                    }
                    case "correo" -> {
                        confirmar(consola, "ENVIAR UN CORREO SOLO 3.B3");
                        exigirArchivo(); exigirTokens(); exigirCorreo();
                        abrir(true, false, true);
                        exigirCorreo();
                        GuardasB3.consumirCorreo(raiz.resolve("correo-invocado.NO-BORRAR"));
                        contexto.getBean(EnviarNotificacionesPendientes.class).procesar();
                        evidencia("correo");
                        abrir(false, false, false);
                    }
                    default -> System.out.println("Comando no reconocido. No se ejecuto ninguna operacion.");
                }
            } catch (Exception error) {
                System.err.println("Operacion detenida. No repetir un correo ni borrar su marca; conservar estados y evidencias.");
                abrir(false, false, false);
            }
        }
    }
    private static void confirmar(Console c, String frase) {
        GuardasB3.exigir(GuardasB3.confirmar(c.readLine("Escribe exactamente " + frase + ": "), frase));
    }
    private void exigirTokens() throws IOException {
        Path p = raiz.resolve("tokens-3b3.enc");
        GuardasB3.exigir(Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS)); GuardasB3.privado(p);
    }
    private void exigirArchivo() {
        long pedidos = sql().queryForObject("select count(*) from pedido", Long.class);
        long archivos = sql().queryForObject("select count(*) from adjunto", Long.class);
        GuardasB3.exigir(archivos == 1);
        var fila = sql().queryForMap("select a.tamano_bytes,a.nombre from adjunto a join pedido p on p.id=a.pedido_id where p.pais_codigo='EC'");
        GuardasB3.contenido(pedidos, archivos, ((Number)fila.get("tamano_bytes")).longValue(), (String)fila.get("nombre"));
        GuardasB3.exigir(sql().queryForObject("select count(*) from carga_archivo where estado<>'COMPLETADA'", Long.class) == 0);
    }
    private void exigirCorreo() {
        GuardasB3.exigir(sql().queryForObject("select count(*) from adjunto where estado_copia='COPIADO'", Long.class) == 1);
        long total = sql().queryForObject("select count(*) from notificacion", Long.class);
        GuardasB3.exigir(total == 1);
        var fila = sql().queryForMap("select n.destinatario,n.estado,n.intentos,n.resultado_incierto from notificacion n join evento_pedido e on e.id=n.evento_id join adjunto a on a.pedido_id=n.pedido_id where e.tipo='ETAPA_ENVIADA' and e.usuario_id=?", admin);
        GuardasB3.correo(total, (String)fila.get("destinatario"), (String)fila.get("estado"),
                ((Number)fila.get("intentos")).intValue(), (Boolean)fila.get("resultado_incierto"));
    }
    private void estado() {
        System.out.println("Pedidos=" + sql().queryForObject("select count(*) from pedido", Long.class)
                + "; archivos=" + sql().queryForObject("select count(*) from adjunto", Long.class)
                + "; avisos=" + sql().queryForObject("select count(*) from notificacion", Long.class)
                + "; tokenExterno=" + Files.exists(raiz.resolve("tokens-3b3.enc")));
    }
    private void evidencia(String operacion) throws IOException {
        Properties resultado = new Properties(); resultado.setProperty("operacion", operacion);
        resultado.setProperty("hora", Instant.now().toString());
        var archivos = sql().queryForList("select estado_copia,intentos from adjunto");
        var correos = sql().queryForList("select estado,intentos,resultado_incierto from notificacion");
        resultado.setProperty("archivos", archivos.toString()); resultado.setProperty("correos", correos.toString());
        Path destino = raiz.resolve("evidencia-" + UUID.randomUUID() + ".properties"); crearPrivado(destino);
        try (var salida = Files.newOutputStream(destino)) { resultado.store(salida, "Solo estados, sin IDs remotos o secretos"); }
        System.out.println("Pasada terminada; revisar estados y recepcion manual. La aceptacion no demuestra lectura.");
    }
    private void cerrarContexto() { if (contexto != null) { contexto.close(); contexto = null; } }
    private void detener() {
        if (!detenido.compareAndSet(false, true)) return;
        try { cerrarContexto(); }
        catch (RuntimeException ignorada) { System.err.println("Revisar parada del contexto del ensayo."); }
        try {
            // Solo STOP del contenedor que este arnes creo. Nunca removeVolume/prune/down -v.
            if (postgres != null && postgres.getContainerId() != null)
                DockerClientFactory.instance().client().stopContainerCmd(postgres.getContainerId()).exec();
        } catch (RuntimeException ignorada) { System.err.println("Revisar parada del contenedor de este ensayo; conservar volumen."); }
    }
    private static Properties leer(Path p) throws IOException {
        GuardasB3.privado(p);
        Properties props = new Properties();
        try (var entrada = Files.newBufferedReader(p, StandardCharsets.UTF_8)) { props.load(entrada); }
        return props;
    }
    private static void crearPrivado(Path p) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            Files.createFile(p, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        else Files.createFile(p);
        GuardasB3.privado(p);
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class DirectorioB3 {
        @Bean @Primary
        DirectorioDestinatarios receptorUnicoB3(ConsultarResponsables usuarios,
                JdbcTemplate sql,
                @org.springframework.beans.factory.annotation.Value("${b3.admin-id}") UUID admin) {
            return pais -> pais == CodigoPais.EC && usuarios.estado(admin)
                    .filter(u -> u.activo() && u.roles().contains(new CodigoRol("admin"))).isPresent()
                    && sql.queryForObject("select count(*) from usuario_pais where usuario_id=? and pais_codigo='EC'", Long.class, admin) == 1
                    ? Set.of(GuardasB3.CUENTA) : Set.of();
        }
    }
}
