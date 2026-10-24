package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.BloqueoCopiasEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.AutoresDeCopiasSql;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.CarpetaDriveSqlRepositorio;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.CopiaAdjuntoSqlRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AutoresDeCopias;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.application.usecase.CopiarPendientesCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Revalidación de F3-ADR25-01 (2026-10-24): un evento de subida incoherente deja la copia FALLIDO/CONFIGURACION sin
 * ninguna operación Drive (ni reservar el ID del archivo, ni leer la raíz, ni crear carpetas), también cuando el
 * adjunto ya tiene un ID de Drive reservado. Complementa, sin modificarla, la matriz de Codex (CarpetasDriveAdr25IT),
 * que solo comprobaba la ausencia de carpetas creadas. PostgreSQL 17 propio; el único Drive es un doble que falla ante
 * cualquier llamada.
 */
@Testcontainers
class EventoIncoherenteSinDriveRevalidacionIT {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17").withReuse(false);
    @TempDir Path disco;
    DatosB1 d;
    AlmacenLocal local;
    final List<String> llamadasDrive = new CopyOnWriteArrayList<>();
    final Instant ahora = Instant.parse("2026-10-24T12:00:00Z");
    final byte[] contenido = {21, 22, 23};

    @SuppressWarnings("unchecked")
    <T> T tx(Class<T> tipo, T repo) {
        var plantilla = new TransactionTemplate(new DataSourceTransactionManager(d.ds));
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo}, (p, m, a) -> plantilla.execute(s -> {
            try { return m.invoke(repo, a); }
            catch (InvocationTargetException e) { throw (RuntimeException) e.getCause(); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }));
    }

    @BeforeEach void preparar() throws Exception {
        local = new AlmacenLocalEnDisco(disco);
        var recibido = local.recibir(new ByteArrayInputStream(contenido), 100);
        local.confirmar(recibido, "fixture/archivo");
        d = new DatosB1(PG, null);
        d.poblar(contenido.length, recibido.sha256(), "fixture/archivo");
        // El adjunto del fixture no se reclama: solo cuenta el adjunto incoherente de cada caso.
        d.sql.update("update adjunto set proximo_intento_en = ? where id = ?",
                Timestamp.from(Instant.parse("2099-01-01T00:00:00Z")), d.adjunto);
    }

    /** Doble que registra y rechaza cualquier operación: ninguna debe ocurrir. */
    AlmacenArchivos driveProhibido() {
        return (AlmacenArchivos) Proxy.newProxyInstance(AlmacenArchivos.class.getClassLoader(),
                new Class<?>[] {AlmacenArchivos.class}, (p, m, a) -> {
                    if (m.getDeclaringClass() == Object.class) { return m.invoke(this, a); }
                    llamadasDrive.add(m.getName());
                    throw new AssertionError("Operación Drive no permitida: " + m.getName());
                });
    }

    CopiarPendientesCasoDeUso worker() {
        ProtectorSesionDrive protector = new ProtectorSesionDrive() {
            public byte[] cifrar(UUID id, AlmacenArchivos.Sesion sesion) { return new byte[] {1}; }
            public AlmacenArchivos.Sesion abrir(UUID id, byte[] bytes) { return new AlmacenArchivos.Sesion("sesion-ficticia"); }
        };
        PedidosParaCopias pedidos = id -> d.sql.queryForObject("select pais_codigo, numero from pedido where id = ?",
                (r, n) -> new PedidosParaCopias.Destino(CodigoPais.valueOf(r.getString(1)), r.getString(2)), id);
        return new CopiarPendientesCasoDeUso(true, 1,
                tx(CopiaAdjuntoRepositorio.class, new CopiaAdjuntoSqlRepositorio(d.ds)),
                tx(CarpetaDriveRepositorio.class, new CarpetaDriveSqlRepositorio(d.ds)), pedidos, driveProhibido(), local,
                new BloqueoCopiasEnDisco(disco), protector, () -> ahora,
                new PoliticaCopia(Duration.ofMinutes(3), List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
                        Duration.ofMinutes(15))),
                tx(AutoresDeCopias.class, new AutoresDeCopiasSql(d.ds)), ids -> Map.of());
    }

    UUID otroUsuario() {
        UUID id = UUID.randomUUID();
        d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                + "values (?,?,?,?,?,'hash-ficticio',now(),now(),0)", id, "Otra persona ficticia", id + "@ejemplo.test",
                id.toString(), "+593990000003");
        return id;
    }

    UUID otroPedido() {
        UUID id = UUID.randomUUID();
        UUID flujo = d.sql.queryForObject("select id from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'A' "
                + "order by version desc limit 1", UUID.class);
        d.sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,"
                + "actualizado_en,version) values (?,?,'EC','A',?,'Pedido ficticio',?,now(),1,now(),0)",
                id, "DK-EC-" + id.toString().substring(0, 8), flujo, d.usuario);
        return id;
    }

    /** Adjunto PENDIENTE del pedido del fixture, con el evento SUBIDO incoherente según el caso. */
    UUID adjuntoIncoherente(String caso, boolean conIdReservado) {
        UUID entrada = UUID.randomUUID(), id = UUID.randomUUID(), evento = UUID.randomUUID();
        d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,1,now())", entrada, d.pedido);
        d.sql.update("insert into adjunto(id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,"
                + "clave_local,estado_copia,subido_por,subido_en) select ?,?,1,?,'Ficha','ficticio.bin',tamano_bytes,sha256,"
                + "clave_local,'PENDIENTE',?,now() from adjunto where id=?", id, d.pedido, entrada, d.usuario, d.adjunto);
        if (!caso.equals("ausente")) {
            UUID autorEvento = caso.equals("otro-usuario") ? otroUsuario() : d.usuario;
            UUID pedidoEvento = caso.equals("otro-pedido") ? otroPedido() : d.pedido;
            String tipo = caso.equals("otro-tipo") ? "ETAPA_ENVIADA" : "ARCHIVO_SUBIDO";
            d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) "
                    + "values (?,?,1,?,?,'admin',now(),'{}')", evento, pedidoEvento, tipo, autorEvento);
            d.sql.update("insert into evento_adjunto(evento_id,adjunto_id,relacion) values (?,?,'SUBIDO')", evento, id);
        }
        if (conIdReservado) {
            // Copia que ya había reservado el ID de su archivo en un intento anterior.
            d.sql.update("update adjunto set drive_id_reservado = ? where id = ?", "ficticio_reservado_" + caso, id);
        }
        return id;
    }

    @ParameterizedTest(name = "{0}, ID reservado={1}")
    @CsvSource({"ausente,false", "otro-usuario,false", "otro-pedido,false", "otro-tipo,false",
            "ausente,true", "otro-usuario,true", "otro-pedido,true", "otro-tipo,true"})
    void eventoIncoherenteFallaSinNingunaOperacionDrive(String caso, boolean conIdReservado) {
        UUID adjunto = adjuntoIncoherente(caso, conIdReservado);
        long carpetasAntes = d.sql.queryForObject("select count(*) from carpeta_drive", Long.class);

        assertThat(worker().ejecutar()).isEqualTo(1);

        var fila = d.sql.queryForMap("select estado_copia, ultimo_error, drive_id, drive_enlace, drive_carpeta_id, "
                + "drive_sesion_cifrada, reclamado_por from adjunto where id = ?", adjunto);
        assertThat(llamadasDrive).as("operaciones Drive").isEmpty();
        assertThat(fila.get("estado_copia")).isEqualTo("FALLIDO");
        assertThat(fila.get("ultimo_error")).isEqualTo("CONFIGURACION");
        assertThat(fila.get("drive_id")).isNull();
        assertThat(fila.get("drive_enlace")).isNull();
        assertThat(fila.get("drive_carpeta_id")).isNull();
        assertThat(fila.get("drive_sesion_cifrada")).isNull();
        assertThat(fila.get("reclamado_por")).isNull();
        assertThat(d.sql.queryForObject("select count(*) from carpeta_drive", Long.class)).isEqualTo(carpetasAntes);
        // El fixture no reclamado sigue intacto.
        assertThat(d.sql.queryForObject("select estado_copia from adjunto where id = ?", String.class, d.adjunto))
                .isEqualTo("PENDIENTE");
    }
}
