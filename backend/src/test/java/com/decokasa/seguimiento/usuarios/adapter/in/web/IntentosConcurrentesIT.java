package com.decokasa.seguimiento.usuarios.adapter.in.web;

import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.CLAVE;
import static com.decokasa.seguimiento.usuarios.adapter.in.web.UsuariosEnBase.cuerpoLogin;
import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
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
 * Hallazgo A4 de la revisión de seguridad del paso 1.7: logins fallidos simultáneos contra la misma cuenta. Cada
 * intento procesado antes del bloqueo se cuenta y se guarda, y los que llegan después respetan el bloqueo (política
 * D2: 5 fallos, 15 minutos; ADR-0002). Ningún intento puede perderse en un conflicto ni responder 500. Usa el
 * servidor real, varios hilos y PostgreSQL 17; la carrera se repite varias rondas. Datos ficticios. Requiere Docker.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class IntentosConcurrentesIT {

    private static final int SIMULTANEOS = 8;
    private static final int MAXIMO = 5;
    private static final int RONDAS = 5;

    @LocalServerPort
    private int puerto;

    @Autowired
    private JdbcTemplate jdbc;

    private final HttpClient cliente = HttpClient.newHttpClient();
    private final ExecutorService hilos = Executors.newFixedThreadPool(SIMULTANEOS);
    private UUID ana;

    @BeforeEach
    void preparar() {
        UsuariosEnBase base = new UsuariosEnBase(jdbc);
        base.limpiar();
        ana = base.crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", CLAVE, true,
                "compras_china");
    }

    @AfterEach
    void cerrar() {
        hilos.shutdownNow();
    }

    @Test
    void losFallosSimultaneosSeCuentanTodosYLosPosterioresRespetanElBloqueo() throws Exception {
        for (int ronda = 1; ronda <= RONDAS; ronda++) {
            jdbc.update("update usuario set intentos_fallidos = 0, bloqueado_hasta = null where id = ?", ana);

            List<Integer> estados = iniciarSesionALaVez("Clave-Incorrecta-1");

            // Los 5 primeros que llegan a la base cuentan como fallo (401); el 5.º bloquea y el resto recibe 423.
            // Ninguno se pierde en un conflicto (500).
            assertThat(estados).as("respuestas de la ronda %d", ronda).doesNotContain(500)
                    .containsOnly(401, 423);
            assertThat(Collections.frequency(estados, 401)).as("fallos contados en la ronda %d", ronda)
                    .isEqualTo(MAXIMO);
            assertThat(Collections.frequency(estados, 423)).as("bloqueados en la ronda %d", ronda)
                    .isEqualTo(SIMULTANEOS - MAXIMO);
            // El contador guardado coincide con los fallos contados, y la cuenta queda bloqueada 15 minutos.
            assertThat(jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class, ana))
                    .as("intentos guardados en la ronda %d", ronda).isEqualTo(MAXIMO);
            assertThat(jdbc.queryForObject("select bloqueado_hasta - now() between interval '14 minutes' "
                    + "and interval '15 minutes' from usuario where id = ?", Boolean.class, ana))
                    .as("bloqueo de 15 minutos en la ronda %d", ronda).isTrue();
            // Después del bloqueo, ni la contraseña correcta entra.
            assertThat(iniciarSesion(CLAVE)).as("contraseña correcta bloqueada en la ronda %d", ronda)
                    .isEqualTo(423);
        }
    }

    // Todos los hilos esperan la misma señal y envían el login a la vez.
    private List<Integer> iniciarSesionALaVez(String contrasena) throws Exception {
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> envios = new ArrayList<>();
        for (int i = 0; i < SIMULTANEOS; i++) {
            envios.add(hilos.submit(() -> {
                salida.await();
                return iniciarSesion(contrasena);
            }));
        }
        salida.countDown();
        List<Integer> estados = new ArrayList<>();
        for (Future<Integer> envio : envios) {
            estados.add(envio.get(60, TimeUnit.SECONDS));
        }
        return estados;
    }

    private int iniciarSesion(String contrasena) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + "/api/v1/auth/login"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(cuerpoLogin("ana@ejemplo.test", contrasena)))
                        .build(),
                HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
