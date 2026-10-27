package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import java.sql.SQLException;
import java.time.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Un esquema UUID por caso dentro de un contenedor exclusivo de la suite. Sin clean ni reuse. */
@Testcontainers
class MigracionesFase5IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    Flyway fly(DatosB1 d){return Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration").load();}
    @Test void vaciaTieneV21ConfiguracionPropuestaYEsquemaSinFeriados(){
        var d=new DatosB1(PG,null);var f=fly(d);
        assertThat(f.info().current().getVersion().toString()).isEqualTo("24");
        assertThat(Arrays.stream(f.info().applied()).filter(m->m.getVersion()!=null).map(m->m.getVersion().toString()).toList()).containsExactly("1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16","17","18","19","20","21","22","23","24");
        assertThat(f.validateWithResult().validationSuccessful).isTrue();assertThat(f.migrate().migrationsExecuted).isZero();
        assertThat(d.sql.queryForMap("select meta_dias,meta_origen,version from configuracion_plazos")).containsEntry("meta_dias",90).containsEntry("meta_origen","PROPUESTA").containsEntry("version",0L);
        assertThat(d.sql.queryForObject("select count(*) from information_schema.tables where table_schema=? and table_name='feriado'",Integer.class,d.esquema)).isZero();
        assertThat(d.sql.queryForObject("select count(*) from aviso_vencimiento",Integer.class)).isZero();
    }
    @ParameterizedTest @ValueSource(strings={"8","13","14"})
    void datosPreviosSeConservanSinInventarAvisos(String version){
        var d=new DatosB1(PG,version);d.poblar(3,"0".repeat(64),"ficticio-f5");var pedido=d.sql.queryForMap("select * from pedido where id=?",d.pedido);var archivo=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);var evento=d.sql.queryForMap("select * from evento_pedido where id=?",d.evento);
        d.actualizar();var p=d.sql.queryForMap("select * from pedido where id=?",d.pedido);pedido.forEach((k,v)->assertThat(p.get(k)).as(k).isEqualTo(v));
        assertThat(p.get("urgencia_activacion_id")).isNull();assertThat(d.sql.queryForMap("select * from adjunto where id=?",d.adjunto)).isEqualTo(archivo);
        var esperado=new LinkedHashMap<>(evento);esperado.putIfAbsent("secuencia",null);assertThat(d.sql.queryForMap("select * from evento_pedido where id=?",d.evento)).isEqualTo(esperado);
        assertThat(d.sql.queryForObject("select count(*) from aviso_vencimiento",Integer.class)).isZero();assertThat(d.sql.queryForObject("select count(*) from notificacion",Integer.class)).isZero();assertThat(fly(d).validateWithResult().validationSuccessful).isTrue();
    }
    @Test void urgenciaAnteriorConEntradaUnicaConservaSuIdentidadYHoras(){
        var d=new DatosB1(PG,"14");d.poblar(3,"0".repeat(64),"ficticio-f5");
        d.sql.update("update pedido set urgencia_nivel='URGENTE',urgencia_activada_en=(select entrada_en from entrada_etapa where id=?),urgencia_vence_en=(select entrada_en + interval '4 hours' from entrada_etapa where id=?) where id=?",d.entrada,d.entrada,d.pedido);
        var antes=d.sql.queryForMap("select * from pedido where id=?",d.pedido);d.actualizar();var despues=d.sql.queryForMap("select * from pedido where id=?",d.pedido);
        antes.forEach((k,v)->assertThat(despues.get(k)).as(k).isEqualTo(v));assertThat(despues.get("urgencia_activacion_id")).isEqualTo(d.entrada);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void urgenciaSinIdentidadInequivocaImpideMigrarAtomicamente(boolean ambigua){
        var d=new DatosB1(PG,"14");d.poblar(3,"0".repeat(64),"ficticio-f5");
        if(ambigua)d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) select ?,pedido_id,1,entrada_en from entrada_etapa where id=?",UUID.randomUUID(),d.entrada);
        d.sql.update("update pedido set urgencia_nivel='URGENTE',urgencia_activada_en=(select entrada_en from entrada_etapa where id=?)+ ? * interval '1 second',urgencia_vence_en=(select entrada_en+interval '4 hours' from entrada_etapa where id=?) where id=?",d.entrada,ambigua?0:1,d.entrada,d.pedido);
        var antes=d.sql.queryForMap("select * from pedido where id=?",d.pedido);
        assertThatThrownBy(d::actualizar).isInstanceOf(RuntimeException.class);
        assertThat(d.sql.queryForMap("select * from pedido where id=?",d.pedido)).isEqualTo(antes);
        assertThat(d.sql.queryForObject("select count(*) from information_schema.columns where table_schema=? and table_name='pedido' and column_name='urgencia_activacion_id'",Integer.class,d.esquema)).isZero();
    }
    void sqlstate(String estado,Runnable accion){assertThatThrownBy(accion::run).rootCause().isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo(estado);}
    @Test void avisosSonSoloInsercionYUnicosPorIdentidadYVencimiento(){
        var d=new DatosB1(PG,null);d.poblar(3,"0".repeat(64),"ficticio-f5");UUID aviso=UUID.randomUUID();
        d.sql.update("insert into aviso_vencimiento(id,pedido_id,clase,etapa,entrada_id,vence_en,evento_id,registrado_en) values(?,?,'ETAPA',1,?,now(),?,now())",aviso,d.pedido,d.entrada,d.evento);
        sqlstate("P0001",()->d.sql.update("update aviso_vencimiento set etapa=2 where id=?",aviso));sqlstate("P0001",()->d.sql.update("delete from aviso_vencimiento where id=?",aviso));sqlstate("P0001",()->d.sql.execute("truncate aviso_vencimiento"));
        sqlstate("23505",()->d.sql.update("insert into aviso_vencimiento select ?,pedido_id,clase,etapa,entrada_id,urgencia_activacion_id,vence_en,evento_id,registrado_en from aviso_vencimiento where id=?",UUID.randomUUID(),aviso));
        assertThat(d.sql.queryForObject("select count(*) from aviso_vencimiento",Integer.class)).isEqualTo(1);
        sqlstate("23514",()->d.sql.update("update configuracion_plazos set meta_dias=0"));
        sqlstate("23514",()->d.sql.update("insert into configuracion_plazos(id,meta_dias,meta_origen) values(2,90,'PROPUESTA')"));
        assertThat(d.sql.queryForObject("select count(*) from configuracion_plazos",Integer.class)).isEqualTo(1);
    }
}
