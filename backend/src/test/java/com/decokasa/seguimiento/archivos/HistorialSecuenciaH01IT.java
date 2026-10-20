package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Contexto distinto indispensable: datos anteriores insertados en V12, V13 real y JPA validate.
 * Sin desactivar CHECK/triggers, sin reconstruir una causalidad antigua desconocida. */
@SpringBootTest(properties={"spring.config.import=","decokasa.semilla.habilitada=false"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class,HistorialSecuenciaH01IT.Legado.class})
class HistorialSecuenciaH01IT {
    static final Instant HORA=Instant.parse("2026-10-16T22:00:00Z");
    static final UUID ACTOR=UUID.fromString("00000000-0000-4000-8000-000000007001");
    static UUID pedido(int n){return UUID.fromString("00000000-0000-4000-8000-"+String.format("%012d",8000+n));}
    static UUID alto(int n){return UUID.fromString("ffffffff-ffff-4fff-8fff-"+String.format("%012d",9000+n));}
    static UUID bajo(int n){return UUID.fromString("00000000-0000-4000-8000-"+String.format("%012d",9000+n));}
    static final Path DISCO=disco();
    static Path disco(){try{return Files.createTempDirectory("decokasa-h01-legado-");}catch(Exception e){throw new IllegalStateException(e);}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",()->DISCO.toString());}
    @TestConfiguration(proxyBeanMethods=false)
    static class Legado {
        @Bean FlywayMigrationStrategy datosAntesDeV13(){return f->{
            Flyway.configure().configuration(f.getConfiguration()).target("12").load().migrate();
            var sql=new JdbcTemplate(f.getConfiguration().getDataSource());
            sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Actor ficticio H01','h01@ejemplo.test','h01','+593990007001','hash-ficticio',?,?,0)",ACTOR,Timestamp.from(HORA),Timestamp.from(HORA));
            sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,? from rol where codigo='admin'",ACTOR,Timestamp.from(HORA));
            for(int n=1;n<=3;n++){
                UUID p=pedido(n),entrada=UUID.randomUUID();
                sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values(?,?,'EC','A','01a13cdb-cc00-7000-8000-000000000001','Fixture legado H01',?,?,9,?,0)",p,"DK-EC-2026-"+(8000+n),ACTOR,Timestamp.from(HORA),Timestamp.from(HORA));
                sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,9,?)",entrada,p,Timestamp.from(HORA));
                for(int e=1;e<=11;e++)sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id) values(?,?,?,?,?,?)",UUID.randomUUID(),p,e,e==9?"ACTUAL":"PENDIENTE",e==9?Timestamp.from(HORA):null,e==9?entrada:null);
                for(UUID id:List.of(alto(n),bajo(n)))insertar(sql,p,id,HORA,id.equals(alto(n))?"Legado UUID alto":"Legado UUID bajo");
            }
            f.migrate();
        };}
    }
    static void insertar(JdbcTemplate sql,UUID pedido,UUID id,Instant hora,String observacion){
        sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,observacion,datos) values(?,?,9,'ETAPA_APROBADA',?,'admin',?,?,'{\"etapaDestino\":10}')",id,pedido,ACTOR,Timestamp.from(hora),observacion);
    }
    @Autowired JdbcTemplate sql;
    @Autowired MockMvc mvc;
    String consultar(UUID p,String sufijo)throws Exception{return mvc.perform(get("/api/v1/pedidos/"+p+sufijo).with(jwt().jwt(j->j.subject(ACTOR.toString())))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();}
    List<String> ids(UUID p)throws Exception{return JsonPath.read(consultar(p,"/historial"),"$[*].id");}
    @Test void antiguosConservanHoraSecuenciaNulaYDesempateId()throws Exception {
        UUID p=pedido(1);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and secuencia is null and ocurrido_en=?",Integer.class,p,Timestamp.from(HORA))).isEqualTo(2);
        assertThat(ids(p)).containsExactly(alto(1).toString(),bajo(1).toString());
        assertThat(ids(p)).containsExactly(alto(1).toString(),bajo(1).toString());
        assertThat((String)JsonPath.read(consultar(p,""),"$.lineaDeTiempo[8].resumen")).contains("Legado UUID alto").doesNotContain("Legado UUID bajo");
    }
    @Test void mezclaMismaHoraDocumentaNullsYNoInventaOrdenCausalAntiguo()throws Exception {
        UUID p=pedido(2),primero=UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"),segundo=UUID.fromString("00000000-0000-4000-8000-000000000001");
        insertar(sql,p,primero,HORA,"Nuevo primero");insertar(sql,p,segundo,HORA,"Nuevo segundo");
        // PostgreSQL ASC deja NULL al final; la API invierte: antiguos por id DESC, luego nuevos por secuencia DESC.
        List<String> esperado=List.of(alto(2).toString(),bajo(2).toString(),segundo.toString(),primero.toString());
        assertThat(ids(p)).containsExactlyElementsOf(esperado);assertThat(ids(p)).containsExactlyElementsOf(esperado);
        assertThat(sql.queryForList("select id::text from evento_pedido where pedido_id=? order by ocurrido_en desc,secuencia desc,id desc",String.class,p)).containsExactlyElementsOf(esperado);
        assertThat((String)JsonPath.read(consultar(p,""),"$.lineaDeTiempo[8].resumen")).contains("Legado UUID alto").doesNotContain("Nuevo segundo");
        assertThat(sql.queryForList("select secuencia from evento_pedido where pedido_id=? and secuencia is not null order by secuencia",Long.class,p)).hasSize(2).doesNotHaveDuplicates().isSorted();
    }
    @Test void horaPrevaleceSobreNullYResumenCoincideConHistorial()throws Exception {
        UUID p=pedido(3),futuro=UUID.randomUUID();insertar(sql,p,futuro,HORA.plusSeconds(1),"Cierre posterior por hora");
        assertThat(ids(p)).containsExactly(futuro.toString(),alto(3).toString(),bajo(3).toString());
        assertThat((String)JsonPath.read(consultar(p,""),"$.lineaDeTiempo[8].resumen")).contains("Cierre posterior por hora").doesNotContain("Legado UUID alto");
    }
}
