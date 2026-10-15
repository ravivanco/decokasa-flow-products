package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Adaptador JPA de usuarios y D10: un conflicto de `@Version` se traduce a VersionDesactualizada (red de seguridad
 * detrás del bloqueo de fila); `guardar` no escribe los contadores del login, y `guardarIntentos` no cambia la
 * versión. Contra PostgreSQL 17. Datos ficticios. Requiere Docker.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UsuarioJpaRepositorioIT {

    @Autowired
    private UsuarioRepositorio usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager gestor;

    private UUID ana;

    @BeforeEach
    void preparar() {
        jdbc.update("delete from token_renovacion");
        jdbc.update("delete from usuario_rol");
        jdbc.update("delete from usuario_pais");
        jdbc.update("delete from usuario");
        ana = UUID.randomUUID();
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("""
                insert into usuario (id, nombre, correo, usuario, telefono, contrasena_hash, activo,
                                     creado_en, actualizado_en, version)
                values (?, 'Ana Prueba', 'ana@ejemplo.test', 'anaprueba', '+593990000001', 'hash-ficticio', true,
                        ?, ?, 0)""", ana, ahora, ahora);
        jdbc.update("insert into usuario_rol (usuario_id, rol_id, asignado_en) "
                + "select ?, id, ? from rol where codigo = 'compras_china'", ana, ahora);
        jdbc.update("insert into usuario_pais (usuario_id, pais_codigo) values (?, 'EC')", ana);
    }

    @Test
    void unConflictoDeVersionAlGuardarSeTraduceAVersionDesactualizada() {
        new TransactionTemplate(gestor).executeWithoutResult(estado -> {
            Usuario leido = usuarios.buscarPorId(ana).orElseThrow();
            // Otra escritura sube la versión después de la lectura, sin pasar por el bloqueo de fila.
            jdbc.update("update usuario set version = version + 1 where id = ?", ana);
            leido.cambiarDatos("Otra Ana", null, null);

            assertThatThrownBy(() -> usuarios.guardar(leido)).isInstanceOf(VersionDesactualizada.class);
            estado.setRollbackOnly();
        });
    }

    @Test
    void guardarNoEscribeLosContadoresDelLogin() {
        new TransactionTemplate(gestor).executeWithoutResult(estado -> {
            Usuario leido = usuarios.buscarParaActualizar(ana).orElseThrow();
            // Un login guardó 4 fallos y un bloqueo después de que el admin leyera al usuario.
            jdbc.update("update usuario set intentos_fallidos = 4, bloqueado_hasta = now() + interval '15 minutes' "
                    + "where id = ?", ana);
            leido.cambiarDatos("Otra Ana", null, null);
            usuarios.guardar(leido);
        });

        assertThat(jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class, ana))
                .isEqualTo(4);
        assertThat(jdbc.queryForObject("select bloqueado_hasta is not null from usuario where id = ?", Boolean.class,
                ana)).isTrue();
        assertThat(jdbc.queryForObject("select nombre from usuario where id = ?", String.class, ana))
                .isEqualTo("Otra Ana");
        assertThat(jdbc.queryForObject("select version from usuario where id = ?", Long.class, ana)).isEqualTo(1);
    }

    @Test
    void guardarIntentosNoCambiaLaVersion() {
        new TransactionTemplate(gestor).executeWithoutResult(estado -> {
            Usuario usuario = usuarios.buscarParaActualizar(ana).orElseThrow();
            usuario.registrarFallo(Instant.now(), PoliticaBloqueo.ACORDADA);
            usuarios.guardarIntentos(usuario);
        });

        assertThat(jdbc.queryForObject("select intentos_fallidos from usuario where id = ?", Integer.class, ana))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("select version from usuario where id = ?", Long.class, ana)).isZero();
    }
}
