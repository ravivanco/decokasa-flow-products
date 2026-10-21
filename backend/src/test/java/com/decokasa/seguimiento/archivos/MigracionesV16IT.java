package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import java.sql.SQLException;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** V16 sobre PostgreSQL propio, esquema UUID por caso, sin clean ni bases compartidas. */
@Testcontainers
class MigracionesV16IT {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17");
    static final UUID FLUJO=UUID.fromString("01a13cdb-cc00-7000-8000-000000000001");
    Flyway fly(DatosB1 d){return Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration").load();}
    void reglas(DatosB1 d){
        assertThat(d.sql.queryForList("select etapa_origen from transicion_permitida where exige_algun_documento order by etapa_origen",Integer.class)).containsExactly(9,10);
        assertThat(d.sql.queryForObject("select count(*) from transicion_permitida where requisitos_documentales='PENDIENTES' and flujo_id=?",Integer.class,FLUJO)).isZero();
        assertThat(d.sql.queryForList("select d.tipo_documento from documento_requerido d join transicion_permitida t on t.id=d.transicion_id where t.etapa_origen=6 and t.flujo_id=?",String.class,FLUJO)).containsExactly("Packing List");
        assertThat(d.sql.queryForObject("select exige_algun_documento from transicion_permitida where etapa_origen=6 and flujo_id=?",Boolean.class,FLUJO)).isFalse();
        assertThat(d.sql.queryForList("select numero from definicion_etapa where flujo_id=? and plazo_estado='PENDIENTE' order by numero",Integer.class,FLUJO)).containsExactly(5,7,10);
    }
    @Test void vaciaConservaEsquemaMetaPendienteYValidaV16(){
        var d=new DatosB1(PG,null);var f=fly(d);assertThat(f.info().current().getVersion().toString()).isEqualTo("16");
        assertThat(f.validateWithResult().validationSuccessful).isTrue();assertThat(f.migrate().migrationsExecuted).isZero();reglas(d);
        assertThat(d.sql.queryForMap("select meta_dias,meta_origen from configuracion_plazos")).containsEntry("meta_dias",90).containsEntry("meta_origen","PROPUESTA");
        assertThat(d.sql.queryForObject("select count(*) from information_schema.columns where table_schema=? and table_name='transicion_permitida' and column_name='exige_algun_documento' and is_nullable='NO'",Integer.class,d.esquema)).isEqualTo(1);
        assertThatThrownBy(()->d.sql.update("update transicion_permitida set requisitos_documentales='PENDIENTES',exige_algun_documento=true where flujo_id=? and etapa_origen=3 and accion='APROBAR'",FLUJO))
                .rootCause().isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("23514");
    }
    @Test void v15ConPedidoEnA6ConservaDatosHistorialArchivosYVersion(){
        var d=new DatosB1(PG,"15");d.poblar(3,"0".repeat(64),"clave-ficticia-v16");UUID entrada6=UUID.randomUUID();
        d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,6,now())",entrada6,d.pedido);
        d.sql.update("update pedido set etapa_actual=6 where id=?",d.pedido);
        for(int n=1;n<=11;n++)d.sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id) values(?,?,?,?,case when ? then now() else null end,?)",UUID.randomUUID(),d.pedido,n,n==6?"ACTUAL":"PENDIENTE",n==6,n==6?entrada6:null);
        var pedido=d.sql.queryForMap("select * from pedido where id=?",d.pedido);var archivo=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);var evento=d.sql.queryForMap("select * from evento_pedido where id=?",d.evento);var etapas=d.sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",d.pedido);var entradas=d.sql.queryForList("select * from entrada_etapa where pedido_id=? order by id",d.pedido);
        assertThat(fly(d).migrate().migrationsExecuted).isEqualTo(1);reglas(d);
        assertThat(d.sql.queryForMap("select * from pedido where id=?",d.pedido)).isEqualTo(pedido);
        assertThat(d.sql.queryForMap("select * from adjunto where id=?",d.adjunto)).isEqualTo(archivo);
        assertThat(d.sql.queryForMap("select * from evento_pedido where id=?",d.evento)).isEqualTo(evento);
        assertThat(d.sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",d.pedido)).isEqualTo(etapas);
        assertThat(d.sql.queryForList("select * from entrada_etapa where pedido_id=? order by id",d.pedido)).isEqualTo(entradas);
        assertThat(d.sql.queryForObject("select count(*) from evento_adjunto where adjunto_id=? and evento_id=?",Integer.class,d.adjunto,d.evento)).isEqualTo(1);
        assertThatThrownBy(()->d.sql.update("update evento_pedido set datos=datos where id=?",d.evento)).rootCause().isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("P0001");
        assertThat(fly(d).validateWithResult().validationSuccessful).isTrue();assertThat(fly(d).migrate().migrationsExecuted).isZero();
    }
}
