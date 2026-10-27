package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Solo SQL real. No carga clases compiladas de main ni simula una API que no compila. */
@Testcontainers
class MigracionesFase7IT {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17").withReuse(false);
    Flyway fly(DatosB1 d,String target){var b=Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration");if(target!=null)b.target(target);return b.load();}
    UUID flujo(DatosB1 d,String pais,String tipo){return d.sql.queryForObject("select id from definicion_flujo where pais_codigo=? and tipo_flujo=? and vigente",UUID.class,pais,tipo);}
    void equivalentes(DatosB1 d){
        for(String tipo:List.of("A","B")){
            UUID ec=flujo(d,"EC",tipo),co=flujo(d,"CO",tipo);
            var etapas="select numero,replace(nombre,'Aduana Ecuador','Aduana Colombia') nombre,rol_codigo,plazo_estado,plazo_cantidad,plazo_tipo,automatica,revisa_etapa,registra_llegada from definicion_etapa where flujo_id=? order by numero";
            assertThat(d.sql.queryForList(etapas,co)).isEqualTo(d.sql.queryForList(etapas,ec));
            var trans="select etapa_origen,accion,etapa_destino,requiere_observacion,requiere_urgencia,requiere_bodega,requisitos_documentales,exige_algun_documento from transicion_permitida where flujo_id=? order by etapa_origen,accion";
            assertThat(d.sql.queryForList(trans,co)).isEqualTo(d.sql.queryForList(trans,ec));
            var docs="select t.etapa_origen,t.accion,d.tipo_documento from documento_requerido d join transicion_permitida t on t.id=d.transicion_id where t.flujo_id=? order by 1,2,3";
            assertThat(d.sql.queryForList(docs,co)).isEqualTo(d.sql.queryForList(docs,ec));
            assertThat(d.sql.queryForObject("select count(*) from definicion_etapa where flujo_id=?",Integer.class,co)).isEqualTo(tipo.equals("A")?11:12);
        }
    }
    @Test void vaciaAplicaTodasValidaYConservaIaDesactivada(){
        var d=new DatosB1(PG,null);var f=fly(d,null);
        assertThat(Arrays.stream(f.info().applied()).filter(m->m.getVersion()!=null).map(m->m.getVersion().toString()).toList()).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1,24).mapToObj(Integer::toString).toList());
        assertThat(f.validateWithResult().validationSuccessful).isTrue();assertThat(f.migrate().migrationsExecuted).isZero();equivalentes(d);
        assertThat(d.sql.queryForObject("select habilitada from configuracion_ia where id=1",Boolean.class)).isFalse();
        assertThat(d.sql.queryForObject("select count(*) from usuario_pais",Integer.class)).isZero();
        assertThat(d.sql.queryForObject("select count(*) from pedido",Integer.class)).isZero();
        assertThat(d.sql.queryForObject("select ciudad from bodega where pais_codigo='CO'",String.class)).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"18","19","20","21","22","23"})
    void datosPreviosSeConservanHasta21(String version){
        var d=new DatosB1(PG,version);d.poblar(4,"0".repeat(64),"ficticio-f7");
        d.sql.update("insert into usuario_pais(usuario_id,pais_codigo) values(?,'EC')",d.usuario);
        var tablas=List.of("pedido","adjunto","evento_pedido","entrada_etapa","usuario_pais");
        var antes=new LinkedHashMap<String,List<Map<String,Object>>>();for(var t:tablas)antes.put(t,d.sql.queryForList("select * from "+t));
        var ec=d.sql.queryForList("select e.* from definicion_etapa e join definicion_flujo f on f.id=e.flujo_id where f.pais_codigo='EC' order by e.id");
        var bodega=d.sql.queryForMap("select * from bodega where id='01a13cdb-cc00-7000-8000-000000000401'");
        assertThat(fly(d,null).migrate().migrationsExecuted).isEqualTo(24-Integer.parseInt(version));
        for(var t:tablas)assertThat(d.sql.queryForList("select * from "+t)).as(t).isEqualTo(antes.get(t));
        assertThat(d.sql.queryForList("select e.* from definicion_etapa e join definicion_flujo f on f.id=e.flujo_id where f.pais_codigo='EC' order by e.id")).isEqualTo(ec);
        var despues=d.sql.queryForMap("select * from bodega where id='01a13cdb-cc00-7000-8000-000000000401'");bodega.forEach((k,v)->assertThat(despues.get(k)).as(k).isEqualTo(v));
        equivalentes(d);assertThat(fly(d,null).validateWithResult().validationSuccessful).isTrue();
        assertThatThrownBy(()->d.sql.update("update evento_pedido set datos='{}' where id=?",d.evento)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(d.sql.queryForObject("select count(*) from usuario_pais where pais_codigo='CO'",Integer.class)).isZero();
    }
    @Test void bodegaDeOtroPaisNoSeAsignaYNombreEsUnicoPorPais(){
        var d=new DatosB1(PG,null);d.poblar(4,"0".repeat(64),"ficticio-f7-bodega");
        assertThatThrownBy(()->d.sql.update("update pedido set bodega_id='01a160e8-5000-7000-8000-000000000401' where id=?",d.pedido)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(d.sql.queryForObject("select bodega_id from pedido where id=?",UUID.class,d.pedido)).isNull();
        d.sql.update("insert into bodega(id,nombre,ciudad,activa,creada_en,actualizada_en,version,pais_codigo) values(?,'Bodega 1','Ciudad ficticia',true,now(),now(),0,'EC')",UUID.randomUUID());
        assertThatThrownBy(()->d.sql.update("insert into bodega(id,nombre,ciudad,activa,creada_en,actualizada_en,version,pais_codigo) values(?,'BODEGA 1','Ciudad ficticia',true,now(),now(),0,'EC')",UUID.randomUUID())).hasRootCauseInstanceOf(java.sql.SQLException.class);
    }
    @Test void colombiaDebeQuedarHabilitadaSinAsignarlaAutomaticamente(){
        var d=new DatosB1(PG,null);
        assertThat(d.sql.queryForObject("select activo from pais where codigo='CO'",Boolean.class)).isTrue();
        assertThat(d.sql.queryForObject("select count(*) from usuario_pais",Integer.class)).isZero();
    }
    @Test void contadorPersistenteSeparaPaisYAnioYRevierteUnaReserva(){
        var d=new DatosB1(PG,null);
        d.sql.update("insert into secuencia_pedido(pais_codigo,anio,ultimo_numero,version) values('EC',2026,7,0),('CO',2026,0,0),('CO',2027,0,0)");
        assertThat(d.sql.queryForObject("update secuencia_pedido set ultimo_numero=ultimo_numero+1,version=version+1 where pais_codigo='CO' and anio=2026 returning ultimo_numero",Integer.class)).isEqualTo(1);
        assertThat(d.sql.queryForObject("select ultimo_numero from secuencia_pedido where pais_codigo='EC' and anio=2026",Integer.class)).isEqualTo(7);
        assertThat(d.sql.queryForObject("select ultimo_numero from secuencia_pedido where pais_codigo='CO' and anio=2027",Integer.class)).isZero();
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(d.ds));
        tx.executeWithoutResult(status->{d.sql.update("update secuencia_pedido set ultimo_numero=ultimo_numero+1,version=version+1 where pais_codigo='CO' and anio=2026");status.setRollbackOnly();});
        assertThat(d.sql.queryForObject("select ultimo_numero from secuencia_pedido where pais_codigo='CO' and anio=2026",Integer.class)).isEqualTo(1);
    }
}
