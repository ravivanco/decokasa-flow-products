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
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Hallazgo M2 (parte de duplicados) de la revisión de seguridad del paso 1.7: dos altas simultáneas que comparten el
 * correo, el nombre de usuario o el teléfono. Las dos pasan la comprobación previa del caso de uso; la base solo deja
 * guardar una. La otra debe responder 409 DUPLICADO con el campo repetido (D9, `openapi.yaml`), nunca 500 ni detalles
 * de SQL. Servidor real, dos hilos y PostgreSQL 17; la carrera se repite varias rondas. Datos ficticios.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class AltasConcurrentesIT {

    private static final int RONDAS = 10;

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ExecutorService hilos = Executors.newFixedThreadPool(2);
    private String tokenAdmin;

    @BeforeEach
    void preparar() throws Exception {
        UsuariosEnBase base = new UsuariosEnBase(jdbc);
        base.limpiar();
        base.crear("Admin Prueba", "admin@ejemplo.test", "adminprueba", "+593990000010", CLAVE, true, "admin");
        HttpResponse<String> login = enviar(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpoLogin("admin@ejemplo.test", CLAVE))));
        assertThat(login.statusCode()).isEqualTo(200);
        tokenAdmin = JsonPath.read(login.body(), "$.tokenAcceso");
    }

    @AfterEach
    void cerrar() {
        hilos.shutdownNow();
    }

    @ParameterizedTest(name = "mismo {0}")
    @ValueSource(strings = {"correo", "usuario", "telefono"})
    void dosAltasSimultaneasConElMismoDatoGuardanUnSoloUsuarioYLaOtraRecibe409(String campo) throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            // Las dos altas comparten solo el campo probado; los otros dos son distintos.
            String a = alta(campo, ronda, 'a');
            String b = alta(campo, ronda, 'b');

            List<HttpResponse<String>> respuestas = crearALaVez(a, b);

            List<Integer> estados = respuestas.stream().map(HttpResponse::statusCode).toList();
            assertThat(estados).as("respuestas de la ronda %d (%s)", ronda, campo).containsExactlyInAnyOrder(201, 409);
            HttpResponse<String> rechazo = respuestas.stream().filter(r -> r.statusCode() == 409).findFirst()
                    .orElseThrow();
            assertThat(rechazo.headers().firstValue("Content-Type")).hasValueSatisfying(
                    tipo -> assertThat(tipo).startsWith("application/problem+json"));
            assertThat((String) JsonPath.read(rechazo.body(), "$.codigo")).isEqualTo("DUPLICADO");
            assertThat((String) JsonPath.read(rechazo.body(), "$.errores[0].campo"))
                    .as("campo repetido en la ronda %d", ronda).isEqualTo(campo);
            // Sin detalles internos de la base en la respuesta.
            assertThat(rechazo.body()).doesNotContainIgnoringCase("duplicate key")
                    .doesNotContain("usuario_" + campo + "_unico").doesNotContainIgnoringCase("sql")
                    .doesNotContainIgnoringCase("constraint");
            // Se guardó un solo usuario con ese dato.
            assertThat(guardadosCon(campo, ronda)).as("usuarios guardados en la ronda %d", ronda).isEqualTo(1);
        }
    }

    private int guardadosCon(String campo, int ronda) {
        return switch (campo) {
            case "correo" -> jdbc.queryForObject("select count(*) from usuario where lower(correo) = ?",
                    Integer.class, correo(ronda, 'x'));
            case "usuario" -> jdbc.queryForObject("select count(*) from usuario where lower(usuario) = ?",
                    Integer.class, usuario(ronda, 'x'));
            default -> jdbc.queryForObject("select count(*) from usuario where telefono = ?", Integer.class,
                    telefono(ronda, 'x'));
        };
    }

    // Valor compartido ('x') en el campo probado; en los demás, uno propio de cada alta ('a' o 'b').
    private static String alta(String campo, int ronda, char lado) {
        return """
                {"nombre": "Carrera Prueba", "correo": "%s", "usuario": "%s", "telefono": "%s",
                 "roles": ["marketing"], "paises": ["EC"], "contrasenaInicial": "Clave-Nueva-1"}"""
                .formatted(correo(ronda, campo.equals("correo") ? 'x' : lado),
                        usuario(ronda, campo.equals("usuario") ? 'x' : lado),
                        telefono(ronda, campo.equals("telefono") ? 'x' : lado));
    }

    private static String correo(int ronda, char lado) {
        return "carrera" + lado + ronda + "@ejemplo.test";
    }

    private static String usuario(int ronda, char lado) {
        return "carrera" + lado + ronda;
    }

    private static String telefono(int ronda, char lado) {
        int digito = switch (lado) {
            case 'a' -> 1;
            case 'b' -> 2;
            default -> 3;
        };
        return "+593993%d%04d".formatted(digito, ronda);
    }

    // Las dos altas esperan la misma señal y se envían a la vez.
    private List<HttpResponse<String>> crearALaVez(String... cuerpos) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> envios = new ArrayList<>();
        for (String cuerpo : cuerpos) {
            envios.add(hilos.submit(() -> {
                salida.await();
                return enviar(HttpRequest.newBuilder(uri("/api/v1/usuarios"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpo)));
            }));
        }
        salida.countDown();
        List<HttpResponse<String>> respuestas = new ArrayList<>();
        for (Future<HttpResponse<String>> envio : envios) {
            respuestas.add(envio.get(60, TimeUnit.SECONDS));
        }
        return respuestas;
    }

    private HttpResponse<String> enviar(HttpRequest.Builder solicitud) throws Exception {
        return cliente.send(solicitud.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
