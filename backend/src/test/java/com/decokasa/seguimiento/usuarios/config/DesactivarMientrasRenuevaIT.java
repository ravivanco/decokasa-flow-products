package com.decokasa.seguimiento.usuarios.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarUsuario;
import com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.RenovarSesion;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * N1 (revisión de seguridad del paso 1.7): el admin desactiva a un usuario mientras ese usuario renueva su sesión. La
 * desactivación bloquea al usuario y después revoca sus tokens; la renovación consume un token y después inserta su
 * sucesor, que por la clave foránea también necesita la fila del usuario. Con el orden de bloqueos invertido, las dos
 * transacciones pueden esperarse entre sí y PostgreSQL cancela una (interbloqueo).
 *
 * <p>No depende de la suerte: los repositorios se envuelven (solo en esta prueba) con puntos de espera que fuerzan el
 * orden exacto en que cada transacción llega a la base. Se llaman los casos de uso reales, con sus transacciones,
 * contra PostgreSQL 17. Se comprueba el resultado de cada operación y el estado final de la cuenta y de las sesiones:
 * la cuenta queda desactivada y ningún token de renovación queda vigente. Datos ficticios. Requiere Docker.
 */
@SpringBootTest
@Import({TestcontainersConfiguration.class, DesactivarMientrasRenuevaIT.Envoltorios.class})
class DesactivarMientrasRenuevaIT {

    private static final String CLAVE = "Clave-Prueba-1";
    private static final long ESPERA_SEGUNDOS = 3;

    @Autowired
    private IniciarSesion iniciarSesion;

    @Autowired
    private RenovarSesion renovarSesion;

    @Autowired
    private ActualizarUsuario actualizarUsuario;

    @Autowired
    private JdbcTemplate jdbc;

    private final ExecutorService hilos = Executors.newFixedThreadPool(2);
    private UUID admin;
    private UUID ana;
    private String tokenRenovacion;

    @BeforeEach
    void preparar() {
        Coreografia.limpiar();
        jdbc.update("delete from token_renovacion");
        jdbc.update("delete from usuario_rol");
        jdbc.update("delete from usuario_pais");
        jdbc.update("delete from usuario");
        admin = crear("Admin Prueba", "admin@ejemplo.test", "adminprueba", "+593990000010", "admin");
        ana = crear("Ana Prueba", "ana@ejemplo.test", "anaprueba", "+593990000001", "compras_china");
        tokenRenovacion = iniciarSesion.iniciar("ana@ejemplo.test", CLAVE).tokenRenovacion();
    }

    @AfterEach
    void cerrar() {
        Coreografia.limpiar();
        hilos.shutdownNow();
    }

    @Test
    void laRenovacionConsumeElTokenAntesDeQueLaDesactivacionBloqueeAlUsuario() throws Exception {
        CountDownLatch consumido = new CountDownLatch(1);
        CountDownLatch usuarioBloqueado = new CountDownLatch(1);
        // Renovación: consume el token y espera a que la desactivación tenga al usuario; luego guarda el sucesor.
        Coreografia.despues("RENOVAR", "consumir", () -> {
            consumido.countDown();
            esperar(usuarioBloqueado);
        });
        // Desactivación: bloquea al usuario solo después del consumo; luego revoca los tokens.
        Coreografia.antes("EDITAR", "buscarParaActualizar", () -> esperar(consumido));
        Coreografia.despues("EDITAR", "buscarParaActualizar", usuarioBloqueado::countDown);

        Resultados resultados = desactivarYRenovarALaVez();

        assertThat(resultados.desactivacion()).as("desactivación").isNull();
        assertThat(resultados.renovacion()).as("renovación").isNull();
        comprobarCuentaDesactivadaSinSesionesVigentes();
    }

    @Test
    void laDesactivacionBloqueaAlUsuarioAntesDeQueLaRenovacionConsumaElToken() throws Exception {
        CountDownLatch usuarioBloqueado = new CountDownLatch(1);
        CountDownLatch consumido = new CountDownLatch(1);
        // Desactivación: bloquea al usuario y espera a que la renovación consuma el token; luego revoca los tokens.
        Coreografia.despues("EDITAR", "buscarParaActualizar", () -> {
            usuarioBloqueado.countDown();
            esperar(consumido);
        });
        // Renovación: no toca al usuario ni consume el token hasta que la desactivación ya tiene al usuario. El primer
        // acceso al usuario es buscarPorId (sin bloqueo) o buscarParaActualizar (con bloqueo), según el código.
        Coreografia.antes("RENOVAR", "buscarPorId", () -> esperar(usuarioBloqueado));
        Coreografia.antes("RENOVAR", "buscarParaActualizar", () -> esperar(usuarioBloqueado));
        Coreografia.antes("RENOVAR", "consumir", () -> esperar(usuarioBloqueado));
        Coreografia.despues("RENOVAR", "consumir", consumido::countDown);

        Resultados resultados = desactivarYRenovarALaVez();

        assertThat(resultados.desactivacion()).as("desactivación").isNull();
        // La cuenta ya estaba desactivada cuando la renovación pudo seguir: 401, nunca un error técnico.
        assertThat(resultados.renovacion()).as("renovación").isInstanceOf(SesionInvalida.class);
        comprobarCuentaDesactivadaSinSesionesVigentes();
    }

    private record Resultados(Throwable desactivacion, Throwable renovacion) {
    }

    // Cada operación corre en su hilo, marcada con su papel; devuelve el error que lanzó, o null si terminó bien.
    private Resultados desactivarYRenovarALaVez() throws Exception {
        long version = jdbc.queryForObject("select version from usuario where id = ?", Long.class, ana);
        Solicitante solicitante = new Solicitante(admin, Set.of(new CodigoRol("admin")));
        Future<Throwable> desactivacion = hilos.submit(enPapel("EDITAR", () -> actualizarUsuario.actualizar(
                solicitante, new ActualizarUsuario.Comando(ana, null, null, null, null, null, false, version))));
        Future<Throwable> renovacion = hilos.submit(enPapel("RENOVAR", () -> renovarSesion.renovar(tokenRenovacion)));
        return new Resultados(desactivacion.get(30, TimeUnit.SECONDS), renovacion.get(30, TimeUnit.SECONDS));
    }

    private void comprobarCuentaDesactivadaSinSesionesVigentes() {
        assertThat(jdbc.queryForObject("select activo from usuario where id = ?", Boolean.class, ana))
                .as("cuenta desactivada").isFalse();
        assertThat(jdbc.queryForObject("select count(*) from token_renovacion where usuario_id = ? "
                + "and revocado_en is null", Integer.class, ana)).as("tokens de renovación vigentes").isZero();
    }

    private static Callable<Throwable> enPapel(String papel, Callable<?> operacion) {
        return () -> {
            Coreografia.PAPEL.set(papel);
            try {
                operacion.call();
                return null;
            } catch (Exception error) {
                return error;
            } finally {
                Coreografia.PAPEL.remove();
            }
        };
    }

    // Espera con límite: si la otra transacción quedó retenida por un bloqueo de la base, se sigue igual.
    private static void esperar(CountDownLatch senal) {
        try {
            senal.await(ESPERA_SEGUNDOS, TimeUnit.SECONDS);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
        }
    }

    private UUID crear(String nombre, String correo, String usuario, String telefono, String rol) {
        UUID id = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("""
                insert into usuario (id, nombre, correo, usuario, telefono, contrasena_hash, activo,
                                     creado_en, actualizado_en, version)
                values (?, ?, ?, ?, ?, ?, true, ?, ?, 0)""",
                id, nombre, correo, usuario, telefono, new BCryptPasswordEncoder(4).encode(CLAVE), ahora, ahora);
        jdbc.update("insert into usuario_rol (usuario_id, rol_id, asignado_en) "
                + "select ?, id, ? from rol where codigo = ?", id, ahora, rol);
        jdbc.update("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", id);
        return id;
    }

    /** Puntos de espera por papel del hilo y método del repositorio. Cada uno se usa una sola vez. */
    static final class Coreografia {

        static final ThreadLocal<String> PAPEL = new ThreadLocal<>();
        private static final Map<String, Runnable> PUNTOS = new ConcurrentHashMap<>();

        private Coreografia() {
        }

        static void antes(String papel, String metodo, Runnable accion) {
            PUNTOS.put(papel + ":antes:" + metodo, accion);
        }

        static void despues(String papel, String metodo, Runnable accion) {
            PUNTOS.put(papel + ":despues:" + metodo, accion);
        }

        static void limpiar() {
            PUNTOS.clear();
        }

        static void ejecutar(String momento, String metodo) {
            String papel = PAPEL.get();
            if (papel == null) {
                return;
            }
            Runnable accion = PUNTOS.remove(papel + ":" + momento + ":" + metodo);
            if (accion != null) {
                accion.run();
            }
        }
    }

    /** Envuelve los dos repositorios de usuarios con los puntos de espera. Solo existe en el contexto de esta prueba. */
    @TestConfiguration(proxyBeanMethods = false)
    static class Envoltorios {

        @Bean
        static BeanPostProcessor puntosDeEspera() {
            return new BeanPostProcessor() {
                @Override
                public Object postProcessAfterInitialization(Object bean, String nombre) {
                    if (bean instanceof TokenRenovacionRepositorio) {
                        return envolver(bean, TokenRenovacionRepositorio.class);
                    }
                    if (bean instanceof UsuarioRepositorio) {
                        return envolver(bean, UsuarioRepositorio.class);
                    }
                    return bean;
                }
            };
        }

        private static Object envolver(Object real, Class<?> puerto) {
            return Proxy.newProxyInstance(puerto.getClassLoader(), new Class<?>[] {puerto}, (proxy, metodo, args) -> {
                Coreografia.ejecutar("antes", metodo.getName());
                try {
                    return metodo.invoke(real, args);
                } catch (InvocationTargetException error) {
                    throw error.getCause();
                } finally {
                    Coreografia.ejecutar("despues", metodo.getName());
                }
            });
        }
    }
}
