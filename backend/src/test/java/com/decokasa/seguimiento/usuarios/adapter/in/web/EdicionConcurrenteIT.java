package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * D10 y M2 (versión) con solicitudes simultáneas: dos ediciones con la misma versión dan una 200 y otra 409
 * VERSION_DESACTUALIZADA sin sobrescribir nada; una edición a la vez que varios logins fallidos no hace fallar la
 * edición ni pierde intentos ni quita un bloqueo (los contadores del login están fuera de la versión). Servidor real,
 * varios hilos y PostgreSQL 17; cada carrera se repite varias rondas. Datos ficticios. Requiere Docker.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class EdicionConcurrenteIT {

    private static final int RONDAS = 10;

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ExecutorService hilos = Executors.newFixedThreadPool(4);
    private UUID ana;
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        UsuariosEnBase base = new UsuariosEnBase(jdbc);
        base.limpiar();
        base.crear("Admin Prueba", "admin@ejemplo.test", "adminprueba", "+593990000010", CLAVE, true, "admin");
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china");
        HttpResponse<String> login = login("admin@ejemplo.test", CLAVE);
        assertThat(login.statusCode()).isEqualTo(200);
        tokenAdmin = JsonPath.read(login.body(), "$.tokenAcceso");
    }

    @AfterEach
    void cerrar() {
        hilos.shutdownNow();
    }

    @Test
    void dosEdicionesSimultaneasConLaMismaVersionGuardanUnaYLaOtraRecibe409() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            long version = versionDe(ana);
            String nombreA = "Ronda " + ronda + " A";
            String nombreB = "Ronda " + ronda + " B";

            List<HttpResponse<String>> respuestas = aLaVez(List.of(
                    () -> editar(version, nombreA), () -> editar(version, nombreB)));

            assertThat(respuestas.stream().map(HttpResponse::statusCode).toList())
                    .as("respuestas de la ronda %d", ronda).containsExactlyInAnyOrder(200, 409);
            HttpResponse<String> ganadora = respuestas.stream().filter(r -> r.statusCode() == 200).findFirst()
                    .orElseThrow();
            HttpResponse<String> rechazada = respuestas.stream().filter(r -> r.statusCode() == 409).findFirst()
                    .orElseThrow();
            assertThat((String) JsonPath.read(rechazada.body(), "$.codigo")).isEqualTo("VERSION_DESACTUALIZADA");
            // Solo quedó el cambio de la ganadora, con una sola versión más.
            assertThat(jdbc.queryForObject("select nombre from usuario where id = ?", String.class, ana))
                    .isEqualTo(JsonPath.<String>read(ganadora.body(), "$.nombre"));
            assertThat(versionDe(ana)).as("versión tras la ronda %d", ronda).isEqualTo(version + 1);
            assertThat(JsonPath.<Integer>read(ganadora.body(), "$.version")).isEqualTo((int) (version + 1));
        }
    }

    @Test
    void unaEdicionALaVezQueLoginsFallidosNoFallaNiPierdeIntentosNiQuitaElBloqueo() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            // Rondas impares: sin fallos previos. Pares: 3 previos, así el bloqueo se forma durante la carrera.
            int previos = ronda % 2 == 1 ? 0 : 3;
            jdbc.update("update usuario set intentos_fallidos = ?, bloqueado_hasta = null where id = ?", previos, ana);
            long version = versionDe(ana);
            String nombre = "Edición " + ronda;

            List<Callable<HttpResponse<String>>> envios = new ArrayList<>();
            envios.add(() -> editar(version, nombre));
            for (int i = 0; i < 3; i++) {
                envios.add(() -> login("ana@ejemplo.test", "Clave-Incorrecta-1"));
            }
            List<HttpResponse<String>> respuestas = aLaVez(envios);

            assertThat(respuestas.get(0).statusCode()).as("edición de la ronda %d", ronda).isEqualTo(200);
            List<Integer> logins = respuestas.subList(1, 4).stream().map(HttpResponse::statusCode).toList();
            int intentos = jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class,
                    ana);
            if (previos == 0) {
                assertThat(logins).as("logins de la ronda %d", ronda).containsOnly(401);
                assertThat(intentos).as("intentos de la ronda %d", ronda).isEqualTo(3);
            } else {
                assertThat(Collections.frequency(logins, 401)).as("fallos contados en la ronda %d", ronda)
                        .isEqualTo(2);
                assertThat(Collections.frequency(logins, 423)).as("bloqueados en la ronda %d", ronda).isEqualTo(1);
                assertThat(intentos).as("intentos de la ronda %d", ronda).isEqualTo(5);
                assertThat(jdbc.queryForObject("select bloqueado_hasta is not null from usuario where id = ?",
                        Boolean.class, ana)).as("bloqueo de la ronda %d", ronda).isTrue();
            }
            // La edición se guardó y solo ella cambió la versión.
            assertThat(jdbc.queryForObject("select nombre from usuario where id = ?", String.class, ana))
                    .isEqualTo(nombre);
            assertThat(versionDe(ana)).as("versión tras la ronda %d", ronda).isEqualTo(version + 1);
        }
    }

    private HttpResponse<String> editar(long version, String nombre) throws Exception {
        return enviar(HttpRequest.newBuilder(uri("/api/v1/usuarios/" + ana))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + tokenAdmin)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(
                        "{\"version\": " + version + ", \"nombre\": \"" + nombre + "\"}")));
    }

    private HttpResponse<String> login(String identificador, String contrasena) throws Exception {
        return enviar(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoLogin(identificador, contrasena))));
    }

    // Todas las solicitudes esperan la misma señal y salen a la vez; las respuestas vuelven en el mismo orden.
    private List<HttpResponse<String>> aLaVez(List<Callable<HttpResponse<String>>> solicitudes) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> envios = new ArrayList<>();
        for (Callable<HttpResponse<String>> solicitud : solicitudes) {
            envios.add(hilos.submit(() -> {
                salida.await();
                return solicitud.call();
            }));
        }
        salida.countDown();
        List<HttpResponse<String>> respuestas = new ArrayList<>();
        for (Future<HttpResponse<String>> envio : envios) {
            respuestas.add(envio.get(60, TimeUnit.SECONDS));
        }
        return respuestas;
    }

    private long versionDe(UUID usuarioId) {
        return jdbc.queryForObject("select version from usuario where id = ?", Long.class, usuarioId);
    }

    private HttpResponse<String> enviar(HttpRequest.Builder solicitud) throws Exception {
        return cliente.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
