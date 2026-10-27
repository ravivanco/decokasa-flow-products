package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.*;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.*;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
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

/** Reutiliza el doble y fixtures históricos sin heredar ni duplicar sus treinta pruebas. */
@Testcontainers
class CarpetasAdr26IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17").withReuse(false);
    @TempDir Path disco;
    CarpetasDriveAdr25IT h;
    CarpetaUsuarioRepositorio intenciones;
    @BeforeEach void preparar()throws Exception{
        h=new CarpetasDriveAdr25IT();h.disco=disco;h.d=new DatosB1(PG,null);
        h.local=new AlmacenLocalEnDisco(disco);
        var recibido=h.local.recibir(new ByteArrayInputStream(h.contenido),100);h.local.confirmar(recibido,"fixture/archivo");
        // Adjunto histórico previo a los formatos cerrados: la copia no revalida ni elimina archivos antiguos.
        h.d.poblar(h.contenido.length,recibido.sha256(),"fixture/archivo");
        h.copias=h.tx(CopiaAdjuntoRepositorio.class,new CopiaAdjuntoSqlRepositorio(h.d.ds));
        h.carpetas=h.tx(CarpetaDriveRepositorio.class,new CarpetaDriveSqlRepositorio(h.d.ds));
        h.autores=h.tx(AutoresDeCopias.class,new AutoresDeCopiasSql(h.d.ds));
        intenciones=h.tx(CarpetaUsuarioRepositorio.class,new CarpetaUsuarioSqlRepositorio(h.d.ds));
        h.nombre(h.d.usuario,"Persona ficticia",Set.of(new CodigoRol("admin")));
        h.drive=h.new DriveDoble();
    }
    String usuario(UUID id){return h.d.sql.queryForObject("select usuario from usuario where id=?",String.class,id);}
    String clave(UUID id){return "d/u/"+CarpetasDriveAdr25IT.compacto(id);}
    PedidosParaCopias pedidos(){return id->h.d.sql.queryForObject("select pais_codigo,numero,creado_en from pedido where id=?",
        (r,n)->new PedidosParaCopias.Destino(CodigoPais.valueOf(r.getString(1)),r.getString(2),r.getTimestamp(3).toInstant()),id);}
    CopiarPendientesCasoDeUso copia(){return new CopiarPendientesCasoDeUso(true,1,h.copias,h.carpetas,pedidos(),h.drive,h.local,
        new BloqueoCopiasEnDisco(disco),h.protector,h.reloj,h.politica,h.autores,ids->Map.copyOf(h.nombres),id->Optional.of(usuario(id)));}
    AprovisionarCarpetasUsuariosCasoDeUso provision(boolean habilitado){return new AprovisionarCarpetasUsuariosCasoDeUso(habilitado,1,
        intenciones,h.carpetas,h.drive,id->Optional.of(usuario(id)),h.reloj,h.politica);}
    void programar(UUID id){intenciones.programar(id,CarpetaUsuarioRepositorio.Origen.ALTA,false,h.ahora);}
    String estado(UUID id){return intenciones.estado(id).orElseThrow().estado();}
    @Test void v24VaciaYActualizacionDesde22ConservanTodosLosDatosYNoProgramanUsuarios(){
        var anterior=new DatosB1(PG,"22");anterior.poblar(3,"a".repeat(64),"historico/archivo");
        var tablas=List.of("usuario","usuario_pais","pedido","adjunto","entrada_etapa","evento_pedido","evento_adjunto","carpeta_drive");
        Map<String,List<Map<String,Object>>> antes=new LinkedHashMap<>();
        for(String t:tablas)antes.put(t,anterior.sql.queryForList("select * from "+t));
        var fly=Flyway.configure().dataSource(anterior.ds).schemas(anterior.esquema).locations("classpath:db/migration").load();
        assertThat(fly.migrate().migrationsExecuted).isEqualTo(2);fly.validate();
        assertThat(fly.info().current().getVersion().toString()).isEqualTo("24");
        assertThat(Arrays.stream(fly.info().applied()).filter(m->m.getVersion()!=null)).hasSize(24);
        assertThat(fly.migrate().migrationsExecuted).isZero();
        for(String t:tablas)assertThat(anterior.sql.queryForList("select * from "+t)).as(t).isEqualTo(antes.get(t));
        assertThat(anterior.sql.queryForObject("select count(*) from carpeta_usuario_drive",Long.class)).isZero();
        assertThat(h.d.sql.queryForObject("select count(*) from flyway_schema_history where success and version is not null",Long.class)).isEqualTo(24);
        assertThat(h.d.sql.queryForObject("select count(*) from carpeta_usuario_drive",Long.class)).isZero();
    }
    @Test void apagadoNoReclamaYProgramacionEsIdempotente(){
        programar(h.d.usuario);programar(h.d.usuario);
        assertThat(provision(false).ejecutar()).isZero();
        assertThat(estado(h.d.usuario)).isEqualTo("PENDIENTE");
        assertThat(h.d.sql.queryForObject("select intentos from carpeta_usuario_drive where usuario_id=?",Integer.class,h.d.usuario)).isZero();
        assertThat(h.drive.reservas.get()).isZero();assertThat(h.drive.creaciones).isEmpty();
    }
    @Test void provisionCreaSoloTresNivelesConfirmadosSinPaisesNiMesesVacios(){
        programar(h.d.usuario);assertThat(provision(true).ejecutar()).isEqualTo(1);
        assertThat(estado(h.d.usuario)).isEqualTo("CREADA");
        assertThat(h.drive.creaciones).extracting(AlmacenArchivos.Remoto::nombre)
            .containsExactly("DECOKASA","Usuarios",usuario(h.d.usuario)+" · "+h.d.usuario.toString().substring(28));
        assertThat(h.d.sql.queryForObject("select drive_id from carpeta_usuario_drive where usuario_id=?",String.class,h.d.usuario))
            .isEqualTo(h.carpetas.buscar(clave(h.d.usuario)).orElseThrow().driveId());
        assertThat(h.drive.creaciones).allSatisfy(c->assertThat(h.carpetas.buscar(c.claveCarpeta()).orElseThrow().confirmada()).isTrue());
        assertThat(provision(true).ejecutar()).isZero();assertThat(h.drive.creaciones).hasSize(3);
    }
    @Test void inicialYTresReintentosPersistenEsperasYSinQuintoTrasReinicio(){
        programar(h.d.usuario);h.drive.fallarAntesDeCrear=true;
        long[] esperas={60,300,900};
        for(int intento=1;intento<=4;intento++){
            assertThat(provision(true).ejecutar()).isEqualTo(1);
            var fila=h.d.sql.queryForMap("select intentos,estado,proximo_intento_en from carpeta_usuario_drive where usuario_id=?",h.d.usuario);
            assertThat(fila.get("intentos")).isEqualTo(intento);
            if(intento<4){
                assertThat(fila.get("estado")).isEqualTo("PENDIENTE");
                assertThat(((Timestamp)fila.get("proximo_intento_en")).toInstant()).isEqualTo(h.ahora.plusSeconds(esperas[intento-1]));
                assertThat(provision(true).ejecutar()).isZero();h.ahora=h.ahora.plusSeconds(esperas[intento-1]);
            }else assertThat(fila.get("estado")).isEqualTo("FALLIDA");
        }
        h.ahora=h.ahora.plus(Duration.ofDays(1));
        intenciones=h.tx(CarpetaUsuarioRepositorio.class,new CarpetaUsuarioSqlRepositorio(h.d.ds));
        assertThat(provision(true).ejecutar()).isZero();assertThat(h.drive.reservas.get()).isEqualTo(1);
        assertThat(h.drive.creaciones).isEmpty();
    }
    @Test void respuestaInciertaDespuesDeCrearSeConciliaSinOtraCarpeta(){
        programar(h.d.usuario);h.drive.falloCrear=Causa.RESULTADO_INCIERTO;
        assertThat(provision(true).ejecutar()).isEqualTo(1);assertThat(estado(h.d.usuario)).isEqualTo("CREADA");
        assertThat(h.drive.creaciones).hasSize(3);assertThat(h.drive.reservas.get()).isEqualTo(3);
        assertThat(provision(true).ejecutar()).isZero();
    }
    @Test void leaseFencingYReinicioNoPermitenConfirmarAlPropietarioAntiguo(){
        programar(h.d.usuario);UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),a)).singleElement().satisfies(p->assertThat(p.intentos()).isEqualTo(1));
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),b)).isEmpty();
        h.ahora=h.ahora.plusSeconds(11);
        intenciones=h.tx(CarpetaUsuarioRepositorio.class,new CarpetaUsuarioSqlRepositorio(h.d.ds));
        assertThat(intenciones.reclamar(1,h.ahora,h.ahora.plusSeconds(10),b)).singleElement().satisfies(p->assertThat(p.intentos()).isEqualTo(1));
        assertThat(intenciones.confirmar(h.d.usuario,a,"no-confirmar",h.ahora)).isFalse();
        assertThat(intenciones.registrarFallo(h.d.usuario,a,Causa.CONFIGURACION,null,true,h.ahora)).isFalse();
        assertThat(intenciones.renovar(h.d.usuario,a,h.ahora,h.ahora.plusSeconds(30))).isFalse();
        assertThat(intenciones.confirmar(h.d.usuario,b,"id-confirmado-ficticio",h.ahora)).isTrue();
        assertThat(estado(h.d.usuario)).isEqualTo("CREADA");
    }
    @Test void dosProcesosNoReclamanLaMismaIntencion()throws Exception{
        programar(h.d.usuario);var inicio=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->{inicio.await(5,TimeUnit.SECONDS);return provision(true).ejecutar();});
            var b=pool.submit(()->{inicio.await(5,TimeUnit.SECONDS);return provision(true).ejecutar();});
            assertThat(a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado(h.d.usuario)).isEqualTo("CREADA");assertThat(h.drive.creaciones).hasSize(3);
    }
    @Test void aprovisionamientoYCopiaConcurrentesCompartenLaMismaCarpetaSinDuplicados()throws Exception{
        programar(h.d.usuario);h.drive.carrera=true;
        try(var pool=Executors.newFixedThreadPool(2)){
            var a=pool.submit(()->provision(true).ejecutar());var b=pool.submit(()->copia().ejecutar());
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(1);assertThat(b.get(30,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado(h.d.usuario)).isEqualTo("CREADA");assertThat(h.estado(h.d.adjunto)).isEqualTo("COPIADO");
        assertThat(h.drive.creaciones).hasSize(7);assertThat(h.drive.completos).hasSize(1);
        assertThat(h.d.sql.queryForObject("select count(*) from carpeta_drive",Long.class)).isEqualTo(7);
        assertThat(h.drive.idsCrear.get("d")).containsOnly(h.carpetas.buscar("d").orElseThrow().driveId());
    }
    @ParameterizedTest @CsvSource({"EC,2027-01-01T04:59:59Z,2026-12-31","EC,2026-03-01T04:59:59Z,2026-02-28",
        "CO,2027-01-01T04:59:59Z,2026-12-31","CO,2026-03-01T05:00:00Z,2026-03-01"})
    void fechaYMesDelPedidoEnZonaDelPaisSinMesesVacios(String pais,String instante,String fecha){
        UUID p=h.d.pedido,id=h.d.adjunto;
        if(pais.equals("CO")){assertThat(copia().ejecutar()).isEqualTo(1);p=h.pedido("CO");id=h.archivo(p,h.d.usuario,"admin");}
        h.d.sql.update("update pedido set creado_en=? where id=?",Timestamp.from(Instant.parse(instante)),p);
        assertThat(copia().ejecutar()).isEqualTo(1);assertThat(h.estado(id)).isEqualTo("COPIADO");
        String numero=h.d.sql.queryForObject("select numero from pedido where id=?",String.class,p);
        assertThat(h.drive.remotos.get(h.destino(id)).nombre()).isEqualTo(fecha+"_"+numero);
        String base=clave(h.d.usuario)+"/"+pais;
        assertThat(h.drive.creaciones.stream().filter(c->c.claveCarpeta().startsWith(base)).map(AlmacenArchivos.Remoto::nombre).toList())
            .containsExactly(pais,fecha.substring(0,4),fecha.substring(5,7));
        assertThat(copia().ejecutar()).isZero();
    }
    @Test void cambioDePerfilNoMueveCarpetaNiCambiaAutorHistorico(){
        assertThat(copia().ejecutar()).isEqualTo(1);String destino=h.destino(h.d.adjunto);
        var anterior=h.carpetas.buscar(clave(h.d.usuario)).orElseThrow();
        h.d.sql.update("update usuario set nombre='Otro nombre ficticio',usuario='otro-login-ficticio' where id=?",h.d.usuario);
        h.nombre(h.d.usuario,"Otro perfil",Set.of());
        UUID otro=h.archivo(h.d.pedido,h.d.usuario,"admin");assertThat(copia().ejecutar()).isEqualTo(1);
        assertThat(h.destino(otro)).isEqualTo(destino);assertThat(h.carpetas.buscar(clave(h.d.usuario)).orElseThrow()).isEqualTo(anterior);
        assertThat(h.drive.creaciones).hasSize(7);assertThat(h.drive.completos).hasSize(2);
        assertThat(estado(h.d.usuario)).isEqualTo("CREADA");
    }
    @Test void uuidCompletoSeparaHomonimosConLosMismosOchoCaracteresFinales(){
        assertThat(copia().ejecutar()).isEqualTo(1);
        UUID a=UUID.fromString("11111111-1111-4111-8111-1111deadbeef"),b=UUID.fromString("22222222-2222-4222-8222-2222deadbeef");
        for(UUID id:List.of(a,b)){
            h.d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Homónimo',?,?,?,'hash-ficticio',now(),now(),0)",id,id+"@ejemplo.test",id.toString(),"+5939"+Math.abs(id.hashCode()));
            h.nombre(id,"Homónimo",Set.of(new CodigoRol("admin")));
        }
        UUID aa=h.archivo(h.d.pedido,a,"admin"),bb=h.archivo(h.d.pedido,b,"admin");
        var worker=new CopiarPendientesCasoDeUso(true,2,h.copias,h.carpetas,pedidos(),h.drive,h.local,new BloqueoCopiasEnDisco(disco),
            h.protector,h.reloj,h.politica,h.autores,ids->Map.copyOf(h.nombres),id->Optional.of("homonimo"));
        assertThat(worker.ejecutar()).isEqualTo(2);
        assertThat(h.carpetas.buscar(clave(a)).orElseThrow().nombre()).isEqualTo("homonimo · deadbeef");
        assertThat(h.carpetas.buscar(clave(b)).orElseThrow().nombre()).isEqualTo("homonimo · deadbeef");
        assertThat(h.carpetas.buscar(clave(a)).orElseThrow().driveId()).isNotEqualTo(h.carpetas.buscar(clave(b)).orElseThrow().driveId());
        assertThat(h.destino(aa)).isNotEqualTo(h.destino(bb));
    }
    @Test void copiasEmpezadasEnPaso34TerminanAlliYCopiadasConservanEnlace(){
        var anteriorWorker=new CopiarPendientesCasoDeUso(true,1,h.copias,h.carpetas,pedidos(),h.drive,h.local,
            new BloqueoCopiasEnDisco(disco),h.protector,h.reloj,h.politica);
        assertThat(anteriorWorker.ejecutar()).isEqualTo(1);
        String destino=h.destino(h.d.adjunto);
        var anterior=h.d.sql.queryForMap("select drive_id,drive_enlace,estado_copia from adjunto where id=?",h.d.adjunto);
        UUID otro=h.archivo(h.d.pedido,h.d.usuario,"admin");
        h.d.sql.update("update adjunto set drive_carpeta_id=? where id=?",destino,otro);
        assertThat(copia().ejecutar()).isEqualTo(1);assertThat(h.destino(otro)).isEqualTo(destino);
        assertThat(h.d.sql.queryForMap("select drive_id,drive_enlace,estado_copia from adjunto where id=?",h.d.adjunto)).isEqualTo(anterior);
        assertThat(h.drive.creaciones).hasSize(2);assertThat(h.carpetas.buscar("d")).isEmpty();
    }
    @Test void copiasEmpezadasEnAdr25TerminanAlliYCopiadasConservanEnlace(){
        h.copiar(h.d.adjunto);String destino=h.destino(h.d.adjunto);
        var anterior=h.d.sql.queryForMap("select drive_id,drive_enlace,estado_copia from adjunto where id=?",h.d.adjunto);
        UUID otro=h.archivo(h.d.pedido,h.d.usuario,"admin");
        h.d.sql.update("update adjunto set drive_carpeta_id=? where id=?",destino,otro);
        assertThat(copia().ejecutar()).isEqualTo(1);assertThat(h.destino(otro)).isEqualTo(destino);
        assertThat(h.d.sql.queryForMap("select drive_id,drive_enlace,estado_copia from adjunto where id=?",h.d.adjunto)).isEqualTo(anterior);
        assertThat(h.drive.creaciones).hasSize(5);assertThat(h.carpetas.buscar("d/u")).isEmpty();
    }
}
