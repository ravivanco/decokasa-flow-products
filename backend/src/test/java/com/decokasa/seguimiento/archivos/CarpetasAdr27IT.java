package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.*;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Organizacion vigente. Reutiliza fixtures/doble, sin heredar ni duplicar suites históricas. */
@Testcontainers
class CarpetasAdr27IT {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17").withReuse(false);
    @TempDir java.nio.file.Path disco;
    CarpetasAdr26IT anterior;
    CarpetasDriveAdr25IT h;
    CarpetaPedidoRepositorio intenciones;
    @BeforeEach void preparar() throws Exception {
        h=new CarpetasDriveAdr25IT();h.disco=disco;h.d=new DatosB1(PG,null);
        h.local=new com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco(disco);
        var recibido=h.local.recibir(new java.io.ByteArrayInputStream(h.contenido),100);
        h.local.confirmar(recibido,"fixture/archivo");h.d.poblar(h.contenido.length,recibido.sha256(),"fixture/archivo");
        h.copias=h.tx(CopiaAdjuntoRepositorio.class,new CopiaAdjuntoSqlRepositorio(h.d.ds));
        h.carpetas=h.tx(CarpetaDriveRepositorio.class,new CarpetaDriveSqlRepositorio(h.d.ds));
        h.autores=h.tx(AutoresDeCopias.class,new AutoresDeCopiasSql(h.d.ds));
        h.nombre(h.d.usuario,"Persona ficticia",Set.of(new com.decokasa.seguimiento.compartido.domain.CodigoRol("admin")));
        h.drive=h.new DriveDoble();
        anterior=new CarpetasAdr26IT();anterior.h=h;
        intenciones = h.tx(CarpetaPedidoRepositorio.class, new CarpetaPedidoSqlRepositorio(h.d.ds));
    }
    AprovisionarCarpetasPedidosCasoDeUso provision(boolean habilitado) {
        return new AprovisionarCarpetasPedidosCasoDeUso(habilitado, 1, intenciones, h.carpetas, h.drive,
                anterior.pedidos(), h.reloj, h.politica);
    }
    CopiarPendientesCasoDeUso copia() {
        return new CopiarPendientesCasoDeUso(true,1,h.copias,h.carpetas,anterior.pedidos(),h.drive,h.local,
            new com.decokasa.seguimiento.archivos.adapter.out.almacen.BloqueoCopiasEnDisco(disco),h.protector,h.reloj,
            h.politica,h.autores,ids->Map.copyOf(h.nombres),id->Optional.of(anterior.usuario(id)),
            h.tx(FechasDeSubida.class,new FechasDeSubidaSql(h.d.ds)));
    }
    void programar() { intenciones.programar(h.d.pedido,CarpetaPedidoRepositorio.Origen.ALTA,false,h.ahora); }
    String estado() { return intenciones.estado(h.d.pedido).orElseThrow().estado(); }
    String clave() { return "d/o/"+CarpetasDriveAdr25IT.compacto(h.d.pedido); }
    @Test void raizNoLegibleConDriveFileUsaAliasRootSinCambiarElAmbito() {
        h.drive=spy(h.drive);doReturn(Optional.empty()).when(h.drive).metadatos("root");
        assertThat(copia().ejecutar()).isEqualTo(1);assertThat(h.estado(h.d.adjunto)).isEqualTo("COPIADO");
        assertThat(h.carpetas.buscar("d").orElseThrow().padreId()).isEqualTo("root");
        assertThat(h.drive.creaciones).hasSize(4);
    }
    @Test void uuidCompletoSeparaAutoresConElMismoNombreYSufijo() {
        assertThat(copia().ejecutar()).isEqualTo(1);
        UUID a=UUID.fromString("11111111-1111-4111-8111-1111deadbeef"),b=UUID.fromString("22222222-2222-4222-8222-2222deadbeef");
        int i=0;for(UUID id:List.of(a,b)) {
            dAutor(id,++i);h.nombre(id,"Homónimo ficticio",Set.of(new com.decokasa.seguimiento.compartido.domain.CodigoRol("admin")));
            h.archivo(h.d.pedido,id,"admin");
        }
        var worker=new CopiarPendientesCasoDeUso(true,2,h.copias,h.carpetas,anterior.pedidos(),h.drive,h.local,
            new com.decokasa.seguimiento.archivos.adapter.out.almacen.BloqueoCopiasEnDisco(disco),h.protector,h.reloj,h.politica,
            h.autores,ids->Map.copyOf(h.nombres),id->Optional.of("homonimo"),h.tx(FechasDeSubida.class,new FechasDeSubidaSql(h.d.ds)));
        assertThat(worker.ejecutar()).isEqualTo(2);
        var aa=h.carpetas.buscar(clave()+"/"+CarpetasDriveAdr25IT.compacto(a)).orElseThrow();
        var bb=h.carpetas.buscar(clave()+"/"+CarpetasDriveAdr25IT.compacto(b)).orElseThrow();
        assertThat(aa.nombre()).isEqualTo(bb.nombre());assertThat(aa.driveId()).isNotEqualTo(bb.driveId());
    }
    void dAutor(UUID id,int i) {
        h.d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Homónimo',?,?,?,'ficticio',now(),now(),0)",id,id+"@ejemplo.test",id.toString(),"+59399900000"+i);
    }
    @Test void apagadoConservaIntencionIdempotenteSinLlamarAlProveedor() {
        programar(); programar(); assertThat(provision(false).ejecutar()).isZero();
        assertThat(estado()).isEqualTo("PENDIENTE"); assertThat(h.drive.reservas.get()).isZero();
        assertThat(h.d.sql.queryForObject("select count(*) from carpeta_pedido_drive",Long.class)).isEqualTo(1);
    }
    @Test void creaPedidoAutomaticamenteSinProgramarPorHttpYSinAutoresVacios() {
        programar(); assertThat(provision(true).ejecutar()).isEqualTo(1);
        assertThat(estado()).isEqualTo("CREADA");
        assertThat(h.drive.creaciones).extracting(AlmacenArchivos.Remoto::nombre)
            .containsExactly("DECOKASA",anterior.pedidos().destino(h.d.pedido).numero());
        assertThat(provision(true).ejecutar()).isZero();
        assertThat(h.carpetas.buscar(clave()).orElseThrow().confirmada()).isTrue();
    }
    @Test void respuestaInciertaSeConciliaConElIdReservado() {
        programar(); h.drive.falloCrear=Causa.RESULTADO_INCIERTO;
        assertThat(provision(true).ejecutar()).isEqualTo(1); assertThat(estado()).isEqualTo("CREADA");
        assertThat(h.drive.creaciones).hasSize(2); assertThat(h.drive.reservas.get()).isEqualTo(2);
    }
    @Test void reintentosPersistidosNoHacenUnQuintoIntentoTrasReinicio() {
        programar(); h.drive.fallarAntesDeCrear=true; long[] espera={60,300,900};
        for(int i=1;i<=4;i++) {
            assertThat(provision(true).ejecutar()).isEqualTo(1);
            var fila=h.d.sql.queryForMap("select estado,intentos,proximo_intento_en from carpeta_pedido_drive where pedido_id=?",h.d.pedido);
            assertThat(fila.get("intentos")).isEqualTo(i);
            if(i<4) {
                assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
                assertThat(((Timestamp)fila.get("proximo_intento_en")).toInstant()).isEqualTo(h.ahora.plusSeconds(espera[i-1]));
                assertThat(provision(true).ejecutar()).isZero(); h.ahora=h.ahora.plusSeconds(espera[i-1]);
            } else assertThat(fila.get("estado")).isEqualTo("FALLIDA");
        }
        intenciones=h.tx(CarpetaPedidoRepositorio.class,new CarpetaPedidoSqlRepositorio(h.d.ds));
        h.ahora=h.ahora.plusSeconds(86400); assertThat(provision(true).ejecutar()).isZero();
        intenciones.programar(h.d.pedido,CarpetaPedidoRepositorio.Origen.ADMIN,true,h.ahora);
        assertThat(estado()).isEqualTo("PENDIENTE");
    }
    @Test void leaseYFencingImpidenConfirmarAlPropietarioAntiguo() {
        programar();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),a)).hasSize(1);
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),b)).isEmpty();
        h.ahora=h.ahora.plusSeconds(11);
        intenciones=h.tx(CarpetaPedidoRepositorio.class,new CarpetaPedidoSqlRepositorio(h.d.ds));
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),b)).hasSize(1);
        assertThat(intenciones.renovar(h.d.pedido,a,h.ahora,h.ahora.plusSeconds(10))).isFalse();
        assertThat(intenciones.confirmar(h.d.pedido,a,"antiguo",h.ahora)).isFalse();
        assertThat(intenciones.registrarFallo(h.d.pedido,a,Causa.CONFIGURACION,null,true,h.ahora)).isFalse();
        assertThat(intenciones.confirmar(h.d.pedido,b,"confirmado-ficticio",h.ahora)).isTrue();
    }
    @Test void dosWorkersSoloReclamanUnaVez() throws Exception {
        programar();var barrera=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{barrera.await();return provision(true).ejecutar();});
            var b=pool.submit(()->{barrera.await();return provision(true).ejecutar();});
            assertThat(a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(h.drive.creaciones).hasSize(2);assertThat(estado()).isEqualTo("CREADA");
    }
    @Test void provisionYCopiaConcurrentesCompartenElPedidoSinDuplicados() throws Exception {
        programar();h.drive.carrera=true;
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->provision(true).ejecutar());var b=pool.submit(()->copia().ejecutar());
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(1);assertThat(b.get(30,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado()).isEqualTo("CREADA");assertThat(h.estado(h.d.adjunto)).isEqualTo("COPIADO");
        assertThat(h.drive.creaciones).hasSize(4);assertThat(h.drive.completos).hasSize(1);
    }
    @ParameterizedTest @CsvSource({"EC,2027-01-01T04:59:59Z,2026-12-31","EC,2026-03-01T05:00:00Z,2026-03-01",
        "CO,2027-01-01T04:59:59Z,2026-12-31","CO,2026-03-01T05:00:00Z,2026-03-01"})
    void diaHistoricoDeSubidaEnZonaDelPaisYAsciiAunqueSeReintenteOtroDia(String pais,String instante,String dia) {
        UUID pedido=h.d.pedido,adjunto=h.d.adjunto;
        if(pais.equals("CO")) { assertThat(copia().ejecutar()).isEqualTo(1);pedido=h.pedido("CO");adjunto=h.archivo(pedido,h.d.usuario,"admin"); }
        h.d.sql.update("update adjunto set subido_en=? where id=?",Timestamp.from(Instant.parse(instante)),adjunto);
        h.ahora=Instant.parse("2028-07-20T15:00:00Z");
        Locale anteriorLocale=Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("ar-SA-u-nu-arab"));assertThat(copia().ejecutar()).isEqualTo(1); }
        finally {Locale.setDefault(anteriorLocale);}
        assertThat(h.estado(adjunto)).isEqualTo("COPIADO");
        assertThat(h.drive.remotos.get(h.destino(adjunto)).nombre()).isEqualTo(dia).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}");
        assertThat(copia().ejecutar()).isZero();
    }
    @Test void v24ActualizaV23SinPerderFilasYCierraSoloIntencionesGlobalesPendientes() {
        var d=new DatosB1(PG,"23");d.poblar(3,"a".repeat(64),"historico/archivo");
        d.sql.update("insert into carpeta_usuario_drive(usuario_id,estado,origen,programada_en,actualizada_en) values(?,'PENDIENTE','ALTA',now(),now())",d.usuario);
        var pedido=d.sql.queryForMap("select * from pedido where id=?",d.pedido);
        var archivo=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);
        var f=Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration").load();
        assertThat(f.migrate().migrationsExecuted).isEqualTo(1);f.validate();assertThat(f.migrate().migrationsExecuted).isZero();
        assertThat(d.sql.queryForMap("select * from pedido where id=?",d.pedido)).isEqualTo(pedido);
        assertThat(d.sql.queryForMap("select * from adjunto where id=?",d.adjunto)).isEqualTo(archivo);
        assertThat(d.sql.queryForMap("select estado,ultimo_error from carpeta_usuario_drive where usuario_id=?",d.usuario))
            .containsEntry("estado","FALLIDA").containsEntry("ultimo_error","OBSOLETA");
        assertThat(d.sql.queryForObject("select count(*) from carpeta_pedido_drive",Long.class)).isZero();
    }
}
