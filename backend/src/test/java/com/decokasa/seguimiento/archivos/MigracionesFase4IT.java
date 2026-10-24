package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Reutiliza el fixture aislado B1. Esquema UUID por caso, datos ficticios, sin clean ni bases compartidas. */
@Testcontainers
class MigracionesFase4IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    Flyway fly(DatosB1 d,String version){var f=Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration");if(version!=null)f.target(version);return f.load();}
    @Test void vaciaAplicaExactamenteV1AV21ValidaYNoReaplica(){
        var d=new DatosB1(PG,null);var f=fly(d,null);
        assertThat(Arrays.stream(f.info().applied()).filter(m->m.getVersion()!=null).map(m->m.getVersion().toString()).toList()).containsExactly("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21");
        assertThat(f.validateWithResult().validationSuccessful).isTrue();assertThat(f.migrate().migrationsExecuted).isZero();
        assertThat(d.sql.queryForList("select nombre from bodega where pais_codigo='EC' order by nombre",String.class)).containsExactly("Guayaquil","Ibarra","Quito");
        assertThat(d.sql.queryForObject("select count(*) from definicion_etapa where registra_llegada and numero=7 and flujo_id in (select id from definicion_flujo where pais_codigo='EC')",Integer.class)).isEqualTo(1);
        assertThat(d.sql.queryForObject("select count(*) from definicion_etapa where registra_llegada and flujo_id='01a13cdb-cc00-7000-8000-000000000001'",Integer.class)).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"3","8"})
    void datosPreviosSobrevivenCadaPasoV9V10V11V12V13(String anterior){
        var d=new DatosB1(PG,anterior);d.poblar(3,"0".repeat(64),"ficticio-f4");
        var pedido=d.sql.queryForMap("select * from pedido where id=?",d.pedido);var adjunto=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);
        var evento=d.sql.queryForMap("select * from evento_pedido where id=?",d.evento);
        for(String v:List.of("9","10","11","12","13")){
            assertThat(fly(d,v).migrate().success).isTrue();
            var p=d.sql.queryForMap("select * from pedido where id=?",d.pedido);pedido.forEach((k,val)->assertThat(p.get(k)).as("V%s pedido.%s",v,k).isEqualTo(val));
            var a=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);adjunto.forEach((k,val)->assertThat(a.get(k)).as("V%s adjunto.%s",v,k).isEqualTo(val));
            var eventoEsperado=new LinkedHashMap<>(evento);if(v.equals("13"))eventoEsperado.put("secuencia",null);
            assertThat(d.sql.queryForMap("select * from evento_pedido where id=?",d.evento)).isEqualTo(eventoEsperado);
            assertThat(d.sql.queryForObject("select evento_id from evento_adjunto where adjunto_id=?",UUID.class,d.adjunto)).isEqualTo(d.evento);
            assertThat(p.get("bodega_id")).isNull();assertThat(p.get("llegada_aduana_en")).isNull();
        }
        assertThat(fly(d,"13").validateWithResult().validationSuccessful).isTrue();
    }
    void estadoSql(String codigo,Runnable accion){assertThatThrownBy(accion::run).rootCause().isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo(codigo);}
    @Test void v12DistingueBodegaInactivaUB001YAsignacionInmutableUB002(){
        var d=new DatosB1(PG,null);d.poblar(3,"0".repeat(64),"ficticio");
        UUID activa=d.sql.queryForObject("select id from bodega where nombre='Quito'",UUID.class);
        UUID inactiva=d.sql.queryForObject("select id from bodega where nombre='Ibarra'",UUID.class);
        d.sql.update("update bodega set activa=false where id=?",inactiva);
        estadoSql("UB001",()->d.sql.update("update pedido set bodega_id=? where id=?",inactiva,d.pedido));
        estadoSql("UB001",()->d.sql.update("update pedido set bodega_id=? where id=?",UUID.randomUUID(),d.pedido));
        d.sql.update("update pedido set bodega_id=? where id=?",activa,d.pedido);
        estadoSql("UB002",()->d.sql.update("update pedido set bodega_id=null where id=?",d.pedido));
        estadoSql("UB002",()->d.sql.update("update pedido set bodega_id=? where id=?",inactiva,d.pedido));
        d.sql.update("update bodega set activa=false where id=?",activa);
        d.sql.update("update pedido set bodega_id=bodega_id where id=?",d.pedido);
        assertThat(d.sql.queryForObject("select bodega_id from pedido where id=?",UUID.class,d.pedido)).isEqualTo(activa);
    }
    @Test void v9ImpideBorradoYDuplicadosNormalizadosDelCatalogo(){
        var d=new DatosB1(PG,null);
        estadoSql("23505",()->d.sql.update("insert into bodega(id,nombre,ciudad,creada_en,actualizada_en,pais_codigo) values(?,'QUITO','Otra',now(),now(),'EC')",UUID.randomUUID()));
        estadoSql("23514",()->d.sql.update("update bodega set nombre=' Quito ' where nombre='Quito'"));
        estadoSql("P0001",()->d.sql.update("delete from bodega where nombre='Quito'"));
    }
    @Test void v10ImpideDemoraParcialFinDeSemanaYFechasAnteriores(){
        var d=new DatosB1(PG,null);d.poblar(3,"0".repeat(64),"ficticio");
        estadoSql("23514",()->d.sql.update("update pedido set demora_motivo='Incompleta' where id=?",d.pedido));
        var hoy=LocalDate.now(ZoneId.of("America/Guayaquil"));var sabado=hoy.with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.SATURDAY));
        estadoSql("23514",()->d.sql.update("update pedido set demora_motivo='Ficticio',demora_nueva_fecha_llegada=?,demora_reportada_por=?,demora_reportada_en=now() where id=?",sabado,d.usuario,d.pedido));
        estadoSql("23514",()->d.sql.update("update pedido set llegada_aduana_en=creado_en-interval '1 second' where id=?",d.pedido));
    }
    @Test void v11ImpideDosLlegadasPorVersionYUnaAutomatica(){
        var d=new DatosB1(PG,null);
        estadoSql("23505",()->d.sql.update("update definicion_etapa set registra_llegada=true where numero=8"));
        estadoSql("23514",()->d.sql.update("update definicion_etapa set registra_llegada=true where automatica"));
    }

    @Test void v13ConservaFilasHorasYTriggersDeEventosAnteriores() {
        var d=new DatosB1(PG,"12");d.poblar(3,"0".repeat(64),"ficticio-h01");
        var antes=d.sql.queryForMap("select *,ctid::text as ubicacion,xmin::text as version_fila from evento_pedido where id=?",d.evento);
        var triggers=d.sql.queryForList("select tgname,pg_get_triggerdef(oid) as definicion from pg_trigger where tgrelid='evento_pedido'::regclass and not tgisinternal order by tgname");
        assertThat(fly(d,"13").migrate().migrationsExecuted).isEqualTo(1);
        var esperado=new LinkedHashMap<>(antes);esperado.put("secuencia",null);
        assertThat(d.sql.queryForMap("select *,ctid::text as ubicacion,xmin::text as version_fila from evento_pedido where id=?",d.evento)).isEqualTo(esperado);
        assertThat(d.sql.queryForList("select tgname,pg_get_triggerdef(oid) as definicion from pg_trigger where tgrelid='evento_pedido'::regclass and not tgisinternal order by tgname")).isEqualTo(triggers);
        assertThat(d.sql.queryForObject("select convalidated from pg_constraint where conrelid='evento_pedido'::regclass and conname='evento_pedido_secuencia_nueva'",Boolean.class)).isFalse();
        assertThat(fly(d,"13").validateWithResult().validationSuccessful).isTrue();
        for(String accion:List.of("update evento_pedido set observacion='No permitido' where id='"+d.evento+"'","delete from evento_pedido where id='"+d.evento+"'","truncate table evento_pedido cascade"))estadoSql("P0001",()->d.sql.execute(accion));
        assertThat(d.sql.queryForMap("select *,ctid::text as ubicacion,xmin::text as version_fila from evento_pedido where id=?",d.evento)).isEqualTo(esperado);
    }
    @Test void v13ExigeSecuenciaNuevaUnicaSinRellenarLasAnteriores() {
        var d=new DatosB1(PG,"12");d.poblar(3,"0".repeat(64),"ficticio-h01");
        UUID otroAntiguo=UUID.randomUUID();
        d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) select ?,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos from evento_pedido where id=?",otroAntiguo,d.evento);
        fly(d,"13").migrate();
        assertThat(d.sql.queryForObject("select count(*) from evento_pedido where secuencia is null",Integer.class)).isEqualTo(2);
        UUID nuevo=UUID.randomUUID();
        d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) select ?,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos from evento_pedido where id=?",nuevo,d.evento);
        Long secuencia=d.sql.queryForObject("select secuencia from evento_pedido where id=?",Long.class,nuevo);assertThat(secuencia).isPositive();
        estadoSql("23514",()->d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,secuencia) values(?,?,1,'OBSERVACION_AGREGADA',?,'admin',now(),null)",UUID.randomUUID(),d.pedido,d.usuario));
        estadoSql("23505",()->d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,secuencia) values(?,?,1,'OBSERVACION_AGREGADA',?,'admin',now(),?)",UUID.randomUUID(),d.pedido,d.usuario,secuencia));
        for(String accion:List.of("update evento_pedido set observacion='No permitido' where id='"+nuevo+"'","delete from evento_pedido where id='"+nuevo+"'","truncate table evento_pedido cascade"))estadoSql("P0001",()->d.sql.execute(accion));
        assertThat(d.sql.queryForObject("select count(*) from evento_pedido",Integer.class)).isEqualTo(3);
        assertThat(d.sql.queryForObject("select count(*) from evento_pedido where secuencia is null",Integer.class)).isEqualTo(2);
    }

}
