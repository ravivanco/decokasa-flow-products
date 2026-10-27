package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
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
 * Hallazgo A2 de la revisión de seguridad del paso 1.7: dos renovaciones simultáneas con el mismo token de
 * renovación no pueden emitir dos sucesores válidos (ADR-0002, D3). Usa el servidor real y dos hilos que envían la
 * misma cookie a la vez, contra PostgreSQL 17. Sin margen de reutilización: la segunda solicitud cuenta como
 * reutilización y revoca toda la familia. La carrera se repite varias rondas porque el solapamiento depende del
 * planificador. Datos ficticios. Requiere Docker encendido.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
        properties = "CORS_ORIGENES=" + RenovacionConcurrenteIT.ORIGEN_PERMITIDO)
@Import(TestcontainersConfiguration.class)
class RenovacionConcurrenteIT {

    static final String ORIGEN_PERMITIDO = "http://localhost:3000";
    private static final String COOKIE = "decokasa_renovacion";
    private static final int RONDAS = 25;

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ExecutorService hilos = Executors.newFixedThreadPool(2);

    @BeforeEach
    void preparar() {
        UsuariosEnBase base = new UsuariosEnBase(jdbc);
        base.limpiar();
        base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true, "compras_china");
    }

    @AfterEach
    void cerrar() {
        hilos.shutdownNow();
    }

    @Test
    void dosRenovacionesSimultaneasConElMismoTokenNoEmitenDosSucesoresValidos() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            jdbc.update("delete from token_renovacion");
            String token = iniciarSesion();
            Timestamp venceLaSesion = jdbc.queryForObject("select expira_en from token_renovacion",
                    Timestamp.class);

            List<Integer> estados = renovarALaVez(token);

            // Una sola gana; la otra llega con el token ya consumido: es reutilización (401).
            assertThat(estados).as("respuestas de la ronda %d", ronda).containsExactlyInAnyOrder(200, 401);
            // Estado guardado: el token original y un único sucesor, ambos revocados por la reutilización.
            assertThat(jdbc.queryForObject("select count(*) from token_renovacion", Integer.class))
                    .as("tokens guardados en la ronda %d", ronda).isEqualTo(2);
            assertThat(jdbc.queryForObject("select count(*) from token_renovacion where revocado_en is null",
                    Integer.class)).as("tokens vigentes en la ronda %d", ronda).isZero();
            assertThat(jdbc.queryForObject("select count(distinct familia_id) from token_renovacion", Integer.class))
                    .isEqualTo(1);
            // El sucesor hereda el límite absoluto de 8 horas del inicio de sesión.
            assertThat(jdbc.queryForList("select distinct expira_en from token_renovacion", Timestamp.class))
                    .as("vencimiento de la familia en la ronda %d", ronda).containsExactly(venceLaSesion);
        }
    }

    private String iniciarSesion() throws Exception {
        HttpResponse<String> respuesta = cliente.send(HttpRequest.newBuilder(uri("/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpoLogin("ana@ejemplo.test", CLAVE)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return respuesta.headers().allValues("Set-Cookie").stream()
                .filter(cabecera -> cabecera.startsWith(COOKIE + "="))
                .map(cabecera -> cabecera.substring(COOKIE.length() + 1, cabecera.indexOf(';')))
                .findFirst()
                .orElseThrow();
    }

    // Los dos hilos esperan la misma señal y envían la misma cookie a la vez.
    private List<Integer> renovarALaVez(String token) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> envios = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            envios.add(hilos.submit(() -> {
                salida.await();
                return cliente.send(HttpRequest.newBuilder(uri("/api/v1/auth/renovar"))
                                .header("Cookie", COOKIE + "=" + token)
                                .header("Origin", ORIGEN_PERMITIDO)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build(),
                        HttpResponse.BodyHandlers.discarding()).statusCode();
            }));
        }
        salida.countDown();
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> envio : envios) {
            estados.add(envio.get(30, TimeUnit.SECONDS));
        }
        return estados;
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
