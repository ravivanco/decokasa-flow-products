package com.decokasa.seguimiento.notificaciones.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.notificaciones.adapter.out.persistence.NotificacionSqlRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.decokasa.seguimiento.notificaciones.application.port.out.TrabajoCorreo;
import com.decokasa.seguimiento.notificaciones.application.usecase.EncolarNotificacionCasoDeUso;
import com.decokasa.seguimiento.notificaciones.application.usecase.EnviarNotificacionesCasoDeUso;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/**
 * Destinatario de prueba de Gmail (2026-10-24). Vacío por defecto: ningún destinatario, nada se reclama ni se envía.
 * Solo admite el buzón autorizado. La revalidación al encolar y al enviar se conserva. PostgreSQL 17 propio con un
 * esquema por caso; Gmail es un doble.
 */
@Testcontainers
class DestinatarioPruebaRevalidacionIT {
    static final String AUTORIZADO = "importadora.latinoamericaworld@gmail.com";
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17").withReuse(false);
    final Instant ahora = Instant.parse("2026-10-24T15:00:00Z");
    JdbcTemplate sql;
    NotificacionSqlRepositorio repo;
    TransactionTemplate tx;
    final UUID usuario = UUID.randomUUID(), pedido = UUID.randomUUID();

    @BeforeEach void preparar() {
        String esquema = "dp_" + UUID.randomUUID().toString().replace("-", "");
        var ds = new DriverManagerDataSource(PG.getJdbcUrl() + (PG.getJdbcUrl().contains("?") ? "&" : "?")
                + "currentSchema=" + esquema, PG.getUsername(), PG.getPassword());
        Flyway.configure().dataSource(ds).locations("classpath:db/migration").schemas(esquema).load().migrate();
        sql = new JdbcTemplate(ds);
        repo = new NotificacionSqlRepositorio(ds, JsonMapper.builder().build());
        tx = new TransactionTemplate(new DataSourceTransactionManager(ds));
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                + "values (?,'Persona ficticia',?,?,'+593990000071','hash-ficticio',now(),now(),0)",
                usuario, usuario + "@ejemplo.test", usuario.toString());
        sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,"
                + "actualizado_en,version) values (?,'DK-EC-2026-0001','EC','A','01a13cdb-cc00-7000-8000-000000000001',"
                + "'Pedido ficticio',?,now(),2,now(),0)", pedido, usuario);
    }

    /** Evento ETAPA_ENVIADA persistido (FK de la notificación) y su registro para encolar. */
    EventoPedidoRegistrado envio() {
        UUID evento = UUID.randomUUID();
        sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) "
                + "values (?,?,1,'ETAPA_ENVIADA',?,'admin',now(),'{}')", evento, pedido, usuario);
        var datos = new DatosDelPedido(pedido, "DK-EC-2026-0001", CodigoPais.EC, TipoFlujo.A);
        return new EventoPedidoRegistrado(evento, new EtapaEnviada(datos, 1,
                new Interviniente(usuario, new CodigoRol("admin")), ahora, 2, "Envío ficticio", false));
    }

    void encolar(DirectorioDestinatarios directorio, EventoPedidoRegistrado registrado) {
        tx.executeWithoutResult(s -> new EncolarNotificacionCasoDeUso(repo, directorio).encolar(registrado));
    }

    EnviarNotificacionesCasoDeUso enviar(DirectorioDestinatarios directorio, Notificador gmail) {
        TrabajoCorreo trabajo = new TrabajoCorreo() {
            public Optional<Notificacion> reclamar(Instant a, Instant h, UUID p) { return tx.execute(s -> repo.reclamar(1, a, h, p).stream().findFirst()); }
            public boolean vigente(UUID i, UUID p, Instant a) { return Boolean.TRUE.equals(tx.execute(s -> repo.vigente(i, p, a))); }
            public boolean enviada(UUID i, UUID p, String proveedor, Instant a) { return Boolean.TRUE.equals(tx.execute(s -> repo.marcarEnviada(i, p, proveedor, a))); }
            public boolean fallo(UUID i, UUID p, Causa c, Instant a) { return Boolean.TRUE.equals(tx.execute(s -> repo.registrarFallo(i, p, c, a, true, a))); }
        };
        return new EnviarNotificacionesCasoDeUso(trabajo, gmail, directorio, () -> ahora, true, 1, Duration.ofMinutes(3));
    }

    @ParameterizedTest @ValueSource(strings = {"", "   "})
    void vacioNoTieneDestinatarios(String valor) {
        for (var directorio : new DirectorioDestinatarios[] {NotificacionesConfig.desde(valor), NotificacionesConfig.desde(null)}) {
            for (CodigoPais pais : CodigoPais.values()) { assertThat(directorio.administradores(pais)).isEmpty(); }
        }
    }

    @ParameterizedTest @ValueSource(strings = {AUTORIZADO, "  IMPORTADORA.LatinoAmericaWorld@gmail.com  "})
    void soloElBuzonAutorizado(String valor) {
        for (CodigoPais pais : CodigoPais.values()) {
            assertThat(NotificacionesConfig.desde(valor).administradores(pais)).containsExactly(AUTORIZADO);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin@ejemplo.test", "importadora.latinoamericaworld@gmail.com.ejemplo.test",
            "importadora.latinoamericaworld@gmail.co", "importadora.latinoamericaworld@gmail.com,otro@ejemplo.test"})
    void cualquierOtroValorImpideArrancarSinRevelarlo(String valor) {
        assertThatThrownBy(() -> NotificacionesConfig.desde(valor))
                .isInstanceOf(IllegalStateException.class)
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain(valor.strip()));
    }

    /** Arranque real de la configuración: sin la propiedad, directorio vacío; con un valor no autorizado, no arranca. */
    @Test void contextoDeSpringArrancaVacioYRechazaValorNoAutorizadoSinRevelarlo() {
        var base = new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(NotificacionesConfig.class)
                .withBean(com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio.class,
                        () -> mock(com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio.class));
        base.run(contexto -> {
            assertThat(contexto).hasNotFailed();
            for (CodigoPais pais : CodigoPais.values()) {
                assertThat(contexto.getBean(DirectorioDestinatarios.class).administradores(pais)).isEmpty();
            }
        });
        base.withPropertyValues("decokasa.notificaciones.destinatario-prueba=" + AUTORIZADO).run(contexto ->
                assertThat(contexto.getBean(DirectorioDestinatarios.class).administradores(CodigoPais.EC))
                        .containsExactly(AUTORIZADO));
        String noAutorizado = "otra.persona.ficticia@ejemplo.test";
        base.withPropertyValues("decokasa.notificaciones.destinatario-prueba=" + noAutorizado).run(contexto -> {
            assertThat(contexto).hasFailed();
            for (Throwable t = contexto.getStartupFailure(); t != null; t = t.getCause()) {
                assertThat(String.valueOf(t.getMessage())).doesNotContain(noAutorizado);
                assertThat(String.valueOf(t)).doesNotContain(noAutorizado);
            }
        });
    }

    @Test void vacioEncolaSinDestinatarioYNoEnviaNada() {
        var vacio = NotificacionesConfig.desde("");
        encolar(vacio, envio());
        var gmail = mock(Notificador.class);
        assertThat(enviar(vacio, gmail).procesar()).isZero();
        verifyNoInteractions(gmail);
        assertThat(repo.listar(null, 0, 20)).singleElement().satisfies(n -> {
            assertThat(n.destinatario()).isNull();
            assertThat(n.estado()).isEqualTo(Notificacion.Estado.PENDIENTE);
            assertThat(n.intentos()).isZero();
        });
    }

    @Test void configuradoEncolaYEnviaUnaSolaVezAlBuzonAutorizado() {
        var prueba = NotificacionesConfig.desde(AUTORIZADO);
        encolar(prueba, envio());
        var gmail = mock(Notificador.class);
        when(gmail.enviar(any())).thenReturn(new Notificador.Entrega("gmail-ficticio-1"));
        var caso = enviar(prueba, gmail);
        assertThat(caso.procesar()).isEqualTo(1);
        assertThat(caso.procesar()).isZero();
        var correo = ArgumentCaptor.forClass(Notificador.Correo.class);
        verify(gmail, times(1)).enviar(correo.capture());
        assertThat(correo.getValue().destinatario()).isEqualTo(AUTORIZADO);
        assertThat(repo.listar(null, 0, 20)).singleElement().satisfies(n -> {
            assertThat(n.estado()).isEqualTo(Notificacion.Estado.ENVIADA);
            assertThat(n.intentos()).isEqualTo(1);
        });
    }

    @Test void encoladoConElBuzonPeroConfiguracionVaciaAlEnviarNoSeReclamaNiEnvia() {
        encolar(NotificacionesConfig.desde(AUTORIZADO), envio());
        var gmail = mock(Notificador.class);
        assertThat(enviar(NotificacionesConfig.desde(""), gmail).procesar()).isZero();
        verifyNoInteractions(gmail);
        assertThat(repo.listar(null, 0, 20)).singleElement().satisfies(n -> {
            assertThat(n.destinatario()).isEqualTo(AUTORIZADO);
            assertThat(n.estado()).isEqualTo(Notificacion.Estado.PENDIENTE);
            assertThat(n.intentos()).isZero();
        });
    }

    @Test void avisoParaOtraDireccionNoSeEnviaAunqueElBuzonEsteConfigurado() {
        var otro = Notificacion.pendiente(pedido, envio().eventoId(), "admin@ejemplo.test",
                Map.of("pais", "EC", "pedido", "DK-EC-2026-0001", "tipoEvento", "EtapaEnviada"), ahora);
        tx.executeWithoutResult(s -> assertThat(repo.insertarSiAusente(otro)).isTrue());
        var gmail = mock(Notificador.class);
        enviar(NotificacionesConfig.desde(AUTORIZADO), gmail).procesar();
        verifyNoInteractions(gmail);
        assertThat(repo.listar(null, 0, 20)).singleElement().satisfies(n -> {
            assertThat(n.estado()).isEqualTo(Notificacion.Estado.FALLIDA);
            assertThat(n.ultimoError()).isEqualTo("CONFIGURACION");
        });
    }
}
