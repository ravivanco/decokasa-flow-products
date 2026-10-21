package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import java.sql.SQLException;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** V17/V18 reales, esquema UUID por caso en PostgreSQL exclusivo; ningún clean ni volumen compartido. */
@Testcontainers
class MigracionesFase6IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    static final UUID A=UUID.fromString("01a13cdb-cc00-7000-8000-000000000001"),B=UUID.fromString("01a15bc1-f400-7000-8000-000000000001");
    Flyway fly(DatosB1 d,String version){var f=Flyway.configure().dataSource(d.ds).schemas(d.esquema).locations("classpath:db/migration");if(version!=null)f.target(version);return f.load();}
    List<Object> definicionA(DatosB1 d){return List.of(d.sql.queryForList("select * from definicion_flujo where id=?",A),d.sql.queryForList("select * from definicion_etapa where flujo_id=? order by numero",A),d.sql.queryForList("select * from transicion_permitida where flujo_id=? order by id",A),d.sql.queryForList("select d.* from documento_requerido d join transicion_permitida t on t.id=d.transicion_id where t.flujo_id=? order by d.transicion_id,d.tipo_documento",A));}
    void reglas(DatosB1 d){
        assertThat(d.sql.queryForList("select numero||':'||coalesce(rol_codigo,'-')||':'||plazo_estado||':'||coalesce(plazo_cantidad::text,'-')||':'||coalesce(plazo_tipo,'-')||':'||automatica from definicion_etapa where flujo_id=? order by numero",String.class,B)).containsExactly(
            "1:compras_china:SIN_PLAZO:-:-:false","2:asistente_compras:PENDIENTE:-:-:false","3:marketing:DEFINIDO:24:HORAS_HABILES:false","4:asistente_compras:DEFINIDO:24:HORAS_HABILES:false","5:compras_china:DEFINIDO:3:DIAS_HABILES:false","6:compras_china:PENDIENTE:-:-:false","7:compras_china:DEFINIDO:10:DIAS_HABILES:false","8:compras_china:PENDIENTE:-:-:false","9:logistica:DEFINIDO:24:HORAS_HABILES:false","10:logistica:DEFINIDO:24:HORAS_HABILES:false","11:logistica:PENDIENTE:-:-:false","12:-:SIN_PLAZO:-:-:true");
        assertThat(d.sql.queryForList("select etapa_origen||':'||accion||':'||coalesce(etapa_destino::text,'-')||':'||requiere_observacion||':'||requiere_urgencia||':'||requiere_bodega||':'||exige_algun_documento from transicion_permitida where flujo_id=? order by etapa_origen,accion",String.class,B)).containsExactly(
            "1:ENVIAR:2:false:false:false:false","2:APROBAR:3:false:false:false:false","2:RECHAZAR:1:true:false:false:false","3:ENVIAR:4:false:false:false:false","4:APROBAR:5:false:false:false:false","4:RECHAZAR:3:true:false:false:false","5:APROBAR:6:false:false:false:false","5:DEVOLVER_POR_ERROR:3:true:false:false:false","5:DEVOLVER_POR_PRODUCTO_NUEVO:3:true:true:false:false","6:APROBAR:7:true:false:false:false","7:APROBAR:8:false:false:false:false","8:APROBAR:9:false:false:false:false","8:REPORTAR_DEMORA:-:true:false:false:false","9:APROBAR:10:false:false:false:false","10:APROBAR:11:false:false:true:true","11:APROBAR:12:false:false:false:true");
        assertThat(d.sql.queryForObject("select count(*) from transicion_permitida where flujo_id=? and requisitos_documentales<>'DEFINIDOS'",Integer.class,B)).isZero();
        assertThat(d.sql.queryForList("select t.etapa_origen||':'||d.tipo_documento from documento_requerido d join transicion_permitida t on t.id=d.transicion_id where t.flujo_id=? order by t.etapa_origen",String.class,B)).containsExactly("1:Solicitud","3:Documentos codificados","7:Packing List");
        assertThat(d.sql.queryForList("select numero||':'||revisa_etapa from definicion_etapa where flujo_id=? and revisa_etapa is not null order by numero",String.class,B)).containsExactly("2:1","4:3");
        assertThat(d.sql.queryForList("select numero from definicion_etapa where flujo_id=? and registra_llegada",Integer.class,B)).containsExactly(8);
    }
    @Test void vaciaAplicaV1AV18ConEsquemaYReglasAprobadas(){var d=new DatosB1(PG,null);var f=fly(d,null);
        assertThat(Arrays.stream(f.info().applied()).filter(m->m.getVersion()!=null).map(m->m.getVersion().toString()).toList()).containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1,18).mapToObj(Integer::toString).toList());
        assertThat(f.validateWithResult().validationSuccessful).isTrue();assertThat(f.migrate().migrationsExecuted).isZero();reglas(d);
        assertThat(d.sql.queryForObject("select count(*) from definicion_etapa where flujo_id=?",Integer.class,A)).isEqualTo(11);
        assertThat(d.sql.queryForObject("select count(*) from transicion_permitida where flujo_id=?",Integer.class,A)).isEqualTo(14);
        assertThat(d.sql.queryForObject("select meta_origen from configuracion_plazos",String.class)).isEqualTo("PROPUESTA");
    }
    @Test void v16ConPedidosAEnCursoPreservaCadaFilaAlAplicar17Y18(){var d=new DatosB1(PG,"16");d.poblar(4,"0".repeat(64),"ficticio-f6-migracion");
        UUID entrada6=UUID.randomUUID();d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,6,'2026-10-20T15:00:00Z')",entrada6,d.pedido);
        d.sql.update("update pedido set etapa_actual=6 where id=?",d.pedido);
        for(int n=1;n<=11;n++)d.sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id) values(?,?,?,?,case when ? then '2026-10-20T15:00:00Z'::timestamptz else null end,?)",UUID.randomUUID(),d.pedido,n,n==6?"ACTUAL":"PENDIENTE",n==6,n==6?entrada6:null);
        var a=definicionA(d);var datos=new LinkedHashMap<String,List<Map<String,Object>>>();
        for(String t:List.of("pedido","etapa_pedido","entrada_etapa","adjunto","evento_pedido"))datos.put(t,d.sql.queryForList("select * from "+t+" order by id"));
        for(String version:List.of("17","18")){assertThat(fly(d,version).migrate().migrationsExecuted).isEqualTo(1);assertThat(definicionA(d)).isEqualTo(a);
            for(var item:datos.entrySet())assertThat(d.sql.queryForList("select * from "+item.getKey()+" order by id")).as(item.getKey()+" tras V"+version).isEqualTo(item.getValue());
            if(version.equals("17"))assertThat(d.sql.queryForObject("select count(*) from definicion_etapa where flujo_id=? and revisa_etapa is not null",Integer.class,B)).isZero();
        }
        reglas(d);assertThat(d.sql.queryForObject("select count(*) from evento_adjunto where adjunto_id=? and evento_id=?",Integer.class,d.adjunto,d.evento)).isEqualTo(1);
        assertThatThrownBy(()->d.sql.update("update evento_pedido set datos=datos where id=?",d.evento)).rootCause().isInstanceOf(SQLException.class).extracting(e->((SQLException)e).getSQLState()).isEqualTo("P0001");
        assertThat(fly(d,null).validateWithResult().validationSuccessful).isTrue();assertThat(fly(d,null).migrate().migrationsExecuted).isZero();
    }
}
