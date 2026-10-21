package com.decokasa.seguimiento.pedidos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.plazos.application.port.in.*;
import com.decokasa.seguimiento.plazos.application.port.out.PedidosConEtapaActiva;
import com.decokasa.seguimiento.plazos.adapter.in.tareas.TareaVencimientos;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Fixtures de estado aislados: no constituyen recorrido A1-Fin ni aprobación documental. */
@SpringBootTest(properties={"spring.config.import=","decokasa.semilla.habilitada=false","decokasa.plazos.vencimientos.automatico=false","decokasa.google.oauth.habilitada=false"})
@AutoConfigureMockMvc @Import({TestcontainersConfiguration.class,Fase5IT.Tiempo.class})
class Fase5IT {
    static final Instant AHORA=Instant.parse("2026-10-20T15:00:00Z"),ENTRADA=Instant.parse("2026-10-19T15:00:00Z");
    static final UUID FLUJO=UUID.fromString("01a13cdb-cc00-7000-8000-000000000001");
    static final AtomicReference<Instant> RELOJ=new AtomicReference<>(AHORA);
    static final AtomicInteger NUMERO=new AtomicInteger(500000);
    static final Path DISCO=disco();
    static Path disco(){try{return Files.createTempDirectory("decokasa-f5-it-");}catch(Exception e){throw new IllegalStateException(e);}}
    @TestConfiguration(proxyBeanMethods=false) static class Tiempo {@Bean @Primary Reloj relojF5(){return RELOJ::get;}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",DISCO::toString);}
    @Autowired MockMvc mvc; @Autowired JdbcTemplate sql; @Autowired PlatformTransactionManager gestor;
    @Autowired RegistrarVencimientosDePedido registrar; @Autowired PedidosConEtapaActiva candidatos;
    @Autowired DetectarVencimientos detector;
    @Autowired ApplicationContext contexto;
    UUID admin;
    @BeforeEach void preparar(){RELOJ.set(AHORA);admin=persona("admin");sql.update("update configuracion_plazos set meta_dias=90,meta_origen='PROPUESTA',actualizado_en=null,actualizado_por=null,version=0");}
    UUID persona(String rol){UUID id=UUID.randomUUID();String s=id.toString().replace("-","");
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Persona ficticia F5',?,?,?,'hash-ficticio',?,?,0)",id,s+"@ejemplo.test",s,"+"+s.replaceAll("[a-f]","1").substring(0,14),Timestamp.from(AHORA),Timestamp.from(AHORA));
        rol(id,rol);return id;}
    void rol(UUID id,String rol){sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo=?",id,rol);}
    UUID pedido(int etapa,Instant vence){UUID p=UUID.randomUUID(),entrada=UUID.randomUUID();new TransactionTemplate(gestor).executeWithoutResult(tx->{
        sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values(?,?,'EC','A',?,'Fixture F5',?,'2026-10-16T22:00:00Z',?,?,0)",p,"DK-EC-2026-"+NUMERO.incrementAndGet(),FLUJO,admin,etapa,Timestamp.from(ENTRADA));
        sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,?,?)",entrada,p,etapa,Timestamp.from(ENTRADA));
        for(int n=1;n<=11;n++)sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id,vence_en) values(?,?,?,?,?,?,?)",UUID.randomUUID(),p,n,n==etapa?"ACTUAL":"PENDIENTE",n==etapa?Timestamp.from(ENTRADA):null,n==etapa?entrada:null,n==etapa&&vence!=null?Timestamp.from(vence):null);
    });return p;}
    UUID entrada(UUID p){return sql.queryForObject("select e.entrada_id from etapa_pedido e join pedido p on p.id=e.pedido_id and p.etapa_actual=e.numero where p.id=?",UUID.class,p);}
    void urgencia(UUID p,UUID activacion,Instant vence){sql.update("update pedido set urgencia_nivel='URGENTE',urgencia_activada_en=?,urgencia_vence_en=?,urgencia_activacion_id=? where id=?",Timestamp.from(ENTRADA),Timestamp.from(vence),activacion,p);}
    String base(UUID p){return "/api/v1/pedidos/"+p;}
    String http(MockHttpServletRequestBuilder r,UUID u,String cuerpo,int status)throws Exception{return mvc.perform(r.with(jwt().jwt(j->j.subject(u.toString()).claim("roles",List.of("admin")))).contentType("application/json").content(cuerpo)).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();}
    int total(String tabla,UUID p){return sql.queryForObject("select count(*) from "+tabla+" where pedido_id=?",Integer.class,p);}
    String motivo(UUID p,UUID u,UUID entrada,String texto,int status)throws Exception{return http(put(base(p)+"/etapas/8/motivo-retraso"),u,"{\"entradaEsperadaId\":\""+entrada+"\",\"motivo\":\""+texto+"\"}",status);}
    @Test void tareaNoExisteSinActivacionExpresa(){assertThat(contexto.getBeansOfType(TareaVencimientos.class)).isEmpty();}
    @Test void cronogramaHttpDistinguePendienteMetaPropuestaYAnclaReal()throws Exception{
        UUID p=pedido(7,null);String r=http(get(base(p)+"/cronograma"),admin,"",200);
        assertThat((Boolean)JsonPath.read(r,"$.calculable")).isFalse();Object totalDias=JsonPath.read(r,"$.totalDias");assertThat(totalDias).isNull();
        assertThat((String)JsonPath.read(r,"$.meta.origen")).isEqualTo("PROPUESTA");assertThat((Boolean)JsonPath.read(r,"$.meta.pendienteDelCliente")).isTrue();
        sql.update("update pedido set demora_reportada_por=?,demora_reportada_en=?,demora_motivo='Demora ficticia',demora_nueva_fecha_llegada='2026-10-22' where id=?",admin,Timestamp.from(AHORA),p);
        r=http(get(base(p)+"/cronograma"),admin,"",200);assertThat((String)JsonPath.read(r,"$.llegada.procedencia")).isEqualTo("LLEGADA_ESTIMADA");
        sql.update("update pedido set llegada_aduana_en='2026-10-24T04:00:00Z' where id=?",p);
        r=http(get(base(p)+"/cronograma"),admin,"",200);assertThat((String)JsonPath.read(r,"$.llegada.fecha")).isEqualTo("2026-10-23");assertThat((String)JsonPath.read(r,"$.llegada.procedencia")).isEqualTo("LLEGADA_REAL");
        assertThat((Integer)JsonPath.read(r,"$.totalDias")).isEqualTo(7);
    }
    @Test void metaAdminVersionAuditoriaYPropuestaPendiente()throws Exception{
        String antes=http(get("/api/v1/configuracion/plazos"),admin,"",200);int version=JsonPath.read(antes,"$.version");
        String r=http(put("/api/v1/configuracion/plazos"),admin,"{\"metaDias\":80,\"version\":"+version+",\"feriados\":[]}",200);
        assertThat((String)JsonPath.read(r,"$.origenMeta")).isEqualTo("ADMIN");assertThat((Integer)JsonPath.read(r,"$.version")).isEqualTo(version+1);assertThat((Boolean)JsonPath.read(r,"$.metaPendienteDelCliente")).isTrue();
        assertThat(sql.queryForObject("select actualizado_por from configuracion_plazos",UUID.class)).isEqualTo(admin);
        assertThat(sql.queryForObject("select actualizado_en from configuracion_plazos",Timestamp.class).toInstant()).isEqualTo(AHORA);
        http(put("/api/v1/configuracion/plazos"),admin,"{\"metaDias\":70,\"version\":"+version+"}",409);
        assertThat(sql.queryForObject("select meta_dias from configuracion_plazos",Integer.class)).isEqualTo(80);
    }
    @ParameterizedTest @ValueSource(strings={"0","-1","1.5","\"90\"","true","[]","{}","null","2147483648"})
    void metaRechazaCoercionSinCambiarFila(String meta)throws Exception{
        var antes=sql.queryForMap("select * from configuracion_plazos");String r=http(put("/api/v1/configuracion/plazos"),admin,"{\"metaDias\":"+meta+",\"version\":0}",400);
        assertThat(r).contains("codigo");assertThat(sql.queryForMap("select * from configuracion_plazos")).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"consulta","logistica","compras_china","marketing"})
    void jwtAdminNoReemplazaRolVigenteParaMeta(String rol)throws Exception{
        UUID u=persona(rol);var antes=sql.queryForMap("select * from configuracion_plazos");http(put("/api/v1/configuracion/plazos"),u,"{\"metaDias\":100,\"version\":0}",403);assertThat(sql.queryForMap("select * from configuracion_plazos")).isEqualTo(antes);
    }
    @Test void feriadosNoAprobadosYCuentaDesactivadaSeRechazan()throws Exception{
        http(put("/api/v1/configuracion/plazos"),admin,"{\"metaDias\":100,\"version\":0,\"feriados\":[\"2026-12-25\"]}",400);
        sql.update("update usuario set activo=false where id=?",admin);http(put("/api/v1/configuracion/plazos"),admin,"{\"metaDias\":100,\"version\":0}",403);
    }
    @Test void limiteExactoNoVenceYDosClasesVencidasSonDosAvisosUnicos()throws Exception{
        UUID p=pedido(8,AHORA);urgencia(p,entrada(p),AHORA);
        assertThat(registrar.registrar(p,AHORA)).isZero();assertThat(candidatos.candidatos(AHORA,1000)).doesNotContain(p);
        Instant despues=AHORA.plusNanos(1);assertThat(registrar.registrar(p,despues)).isEqualTo(2);assertThat(registrar.registrar(p,despues.plusSeconds(1))).isZero();
        for(String tabla:List.of("aviso_vencimiento","evento_pedido","notificacion"))assertThat(total(tabla,p)).as(tabla).isEqualTo(2);
        assertThat(sql.queryForList("select clase from aviso_vencimiento where pedido_id=? order by clase",String.class,p)).containsExactly("ETAPA","URGENCIA");
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and usuario_id is null and tipo='PLAZO_VENCIDO'",Integer.class,p)).isEqualTo(2);
        assertThat(sql.queryForObject("select count(*) from notificacion where pedido_id=? and destinatario is null and estado='PENDIENTE'",Integer.class,p)).isEqualTo(2);
        String registro=http(get("/api/v1/notificaciones"),admin,"",200);assertThat(registro).contains("DESTINATARIO_PENDIENTE");
    }
    @Test void plazoPendienteSinUrgenciaYFinalizadoNoSonCandidatos(){
        UUID pendiente=pedido(10,null),fin=pedido(11,AHORA.minusSeconds(1));sql.update("update pedido set finalizado=true,finalizado_en=? where id=?",Timestamp.from(AHORA),fin);
        sql.update("update etapa_pedido set estado='COMPLETADA',salida_en=? where pedido_id=? and numero=11",Timestamp.from(AHORA),fin);
        assertThat(registrar.registrar(pendiente,AHORA)).isZero();assertThat(registrar.registrar(fin,AHORA)).isZero();assertThat(registrar.registrar(UUID.randomUUID(),AHORA)).isZero();
        assertThat(candidatos.candidatos(AHORA,1000)).doesNotContain(pendiente,fin);
    }
    @Test void activacionEstableNoSeDuplicaAlCambiarEntradaPeroOtraActivacionSiAvisa(){
        UUID p=pedido(8,null),primera=entrada(p);urgencia(p,primera,AHORA.minusSeconds(1));assertThat(registrar.registrar(p,AHORA)).isEqualTo(1);
        UUID otra=UUID.randomUUID();sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,8,?)",otra,p,Timestamp.from(AHORA));
        sql.update("update etapa_pedido set entrada_id=? where pedido_id=? and numero=8",otra,p);
        assertThat(registrar.registrar(p,AHORA)).isZero();
        urgencia(p,otra,AHORA.minusSeconds(1));assertThat(registrar.registrar(p,AHORA)).isEqualTo(1);assertThat(total("aviso_vencimiento",p)).isEqualTo(2);
    }
    @Test void reentradaDeEtapaIdentificaNuevoAvisoSinBorrarAnterior(){
        UUID p=pedido(8,AHORA.minusSeconds(1));assertThat(registrar.registrar(p,AHORA)).isEqualTo(1);UUID anterior=entrada(p),nueva=UUID.randomUUID();
        sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,8,?)",nueva,p,Timestamp.from(ENTRADA.plusSeconds(1)));
        sql.update("update etapa_pedido set entrada_id=?,entrada_en=?,reentradas=1 where pedido_id=? and numero=8",nueva,Timestamp.from(ENTRADA.plusSeconds(1)),p);
        assertThat(registrar.registrar(p,AHORA)).isEqualTo(1);assertThat(sql.queryForList("select entrada_id from aviso_vencimiento where pedido_id=?",UUID.class,p)).containsExactlyInAnyOrder(anterior,nueva);
    }
    @Test void dosWorkersConfirmanUnSoloEventoYOutbox()throws Exception{
        UUID p=pedido(8,AHORA.minusSeconds(1));var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try{Callable<Integer> tarea=()->{inicio.await();return registrar.registrar(p,AHORA);};var a=pool.submit(tarea);var b=pool.submit(tarea);inicio.countDown();assertThat(a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS)).isEqualTo(1);}finally{pool.shutdownNow();}
        for(String tabla:List.of("aviso_vencimiento","evento_pedido","notificacion"))assertThat(total(tabla,p)).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"aviso_vencimiento","evento_pedido","notificacion"})
    void falloEnCualquierEscrituraRevierteAvisoHistorialYOutbox(String tabla){
        UUID p=pedido(8,AHORA.minusSeconds(1));String nombre="f5_"+p.toString().replace("-","");
        sql.execute("create function "+nombre+"() returns trigger language plpgsql as $$ begin if new.pedido_id='"+p+"'::uuid then raise exception 'Fallo ficticio F5'; end if; return new; end $$");
        sql.execute("create trigger "+nombre+" before insert on "+tabla+" for each row execute function "+nombre+"()");
        try{assertThatThrownBy(()->registrar.registrar(p,AHORA)).isInstanceOf(RuntimeException.class);for(String t:List.of("aviso_vencimiento","evento_pedido","notificacion"))assertThat(total(t,p)).as(t).isZero();}
        finally{sql.execute("drop trigger "+nombre+" on "+tabla);sql.execute("drop function "+nombre+"()");}
        assertThat(registrar.registrar(p,AHORA)).isEqualTo(1);
    }
    @Test void motivoSeRegistraCorrigeYNoGeneraCorreo()throws Exception{
        UUID p=pedido(8,AHORA.minusSeconds(1)),e=entrada(p),u=persona("logistica");rol(u,"consulta");
        motivo(p,u,e,"  Motivo ficticio  ",200);motivo(p,u,e,"Correccion ficticia",200);
        assertThat(sql.queryForObject("select motivo_retraso from etapa_pedido where pedido_id=? and numero=8",String.class,p)).isEqualTo("Correccion ficticia");
        assertThat(total("evento_pedido",p)).isEqualTo(2);assertThat(total("notificacion",p)).isZero();
        assertThat(http(get(base(p)+"/historial"),admin,"",200)).contains("MOTIVO_RETRASO_REGISTRADO","Motivo ficticio","Correccion ficticia");
    }
    @ParameterizedTest @ValueSource(strings={"consulta","asistente_china","compras_china"})
    void motivoReleePermisosSinEfectos(String rol)throws Exception{
        UUID p=pedido(8,AHORA.minusSeconds(1));motivo(p,persona(rol),entrada(p),"No autorizado",403);
        assertThat(total("evento_pedido",p)).isZero();assertThat(sql.queryForObject("select motivo_retraso from etapa_pedido where pedido_id=? and numero=8",String.class,p)).isNull();
    }
    @Test void motivoNecesitaVencimientoYEntradaVigentes()throws Exception{
        UUID p=pedido(8,AHORA);motivo(p,admin,entrada(p),"No vencido",409);motivo(p,admin,UUID.randomUUID(),"Entrada antigua",409);
        assertThat(total("evento_pedido",p)).isZero();
    }
    @Test void dosAdminsConLaMismaVersionNoPierdenActualizaciones()throws Exception{
        long v=sql.queryForObject("select version from configuracion_plazos",Long.class);UUID otro=persona("admin");var pool=Executors.newFixedThreadPool(2);var salida=new CountDownLatch(1);
        try{
            Callable<Integer> a=()->{salida.await();return mvc.perform(put("/api/v1/configuracion/plazos").with(jwt().jwt(j->j.subject(admin.toString()))).contentType("application/json").content("{\"metaDias\":70,\"version\":"+v+"}")).andReturn().getResponse().getStatus();};
            Callable<Integer> b=()->{salida.await();return mvc.perform(put("/api/v1/configuracion/plazos").with(jwt().jwt(j->j.subject(otro.toString()))).contentType("application/json").content("{\"metaDias\":80,\"version\":"+v+"}")).andReturn().getResponse().getStatus();};
            var ra=pool.submit(a);var rb=pool.submit(b);salida.countDown();assertThat(List.of(ra.get(20,TimeUnit.SECONDS),rb.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }finally{pool.shutdownNow();}
        assertThat(sql.queryForObject("select version from configuracion_plazos",Long.class)).isEqualTo(v+1);
        assertThat(sql.queryForObject("select meta_dias from configuracion_plazos",Integer.class)).isIn(70,80);
    }
    @Test void devolucionRealPersisteActivacionEstableYRespetaLimiteDeUrgencia()throws Exception{
        UUID p=pedido(4,null);
        http(post(base(p)+"/acciones"),admin,"{\"accion\":\"DEVOLVER_POR_PRODUCTO_NUEVO\",\"etapaEsperada\":4,\"observacion\":\"Producto nuevo ficticio\",\"nivelUrgencia\":\"URGENTE\",\"adjuntoIds\":[]}",200);
        UUID activacion=sql.queryForObject("select urgencia_activacion_id from pedido where id=?",UUID.class,p);
        assertThat(activacion).isEqualTo(entrada(p));assertThat(activacion).isNotNull();
        assertThat(sql.queryForObject("select entrada_en from entrada_etapa where id=?",Timestamp.class,activacion).toInstant()).isEqualTo(AHORA);
        Instant vence=AHORA.plusSeconds(4*3600);assertThat(sql.queryForObject("select urgencia_vence_en from pedido where id=?",Timestamp.class,p).toInstant()).isEqualTo(vence);
        assertThat(registrar.registrar(p,vence)).isZero();assertThat(registrar.registrar(p,vence.plusNanos(1))).isEqualTo(1);
        http(get(base(p)),admin,"",200);assertThat(sql.queryForObject("select urgencia_activacion_id from pedido where id=?",UUID.class,p)).isEqualTo(activacion);
        assertThat(sql.queryForObject("select count(*) from aviso_vencimiento where pedido_id=? and clase='URGENCIA' and urgencia_activacion_id=?",Integer.class,p,activacion)).isEqualTo(1);
    }
    @Test void bloqueoPostgresRealProduceDiagnosticoSanitizado()throws Exception{
        UUID p=pedido(8,AHORA.minusSeconds(1));var log=(ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(com.decokasa.seguimiento.plazos.adapter.out.diagnostico.DiagnosticoVencimientosLog.class);
        var appender=new ch.qos.logback.core.read.ListAppender<ch.qos.logback.classic.spi.ILoggingEvent>();appender.start();log.addAppender(appender);
        try(var bloqueo=Objects.requireNonNull(sql.getDataSource()).getConnection();var espera=sql.getDataSource().getConnection()){
            bloqueo.setAutoCommit(false);
            try(var s=bloqueo.prepareStatement("select id from pedido where id=? for update")){s.setObject(1,p);s.executeQuery().close();}
            try{
                try(var s=espera.createStatement()){s.execute("set lock_timeout='100ms'");}
                java.sql.SQLException error=catchThrowableOfType(java.sql.SQLException.class,()->{try(var s=espera.prepareStatement("select id from pedido where id=? for update")){s.setObject(1,p);s.executeQuery().close();}});
                assertThat((Throwable)error).isNotNull();assertThat(error.getSQLState()).isEqualTo("55P03");
                new com.decokasa.seguimiento.plazos.adapter.out.diagnostico.DiagnosticoVencimientosLog().falloDePedido(UUID.randomUUID(),p,AHORA,new RuntimeException("Dato ficticio privado",error));
                var e=appender.list.getLast();assertThat(e.getFormattedMessage()).contains("categoria=BLOQUEO","codigo=55P03").doesNotContain("Dato ficticio privado",error.getMessage());assertThat(e.getThrowableProxy()).isNull();
            }finally{bloqueo.rollback();}
        }finally{log.detachAppender(appender);appender.stop();}
        assertThat(total("aviso_vencimiento",p)).isZero();
    }
    @Test void motivoYEventoSeReviertenJuntosSiFallaHistorial()throws Exception{
        UUID p=pedido(8,AHORA.minusSeconds(1));var antes=sql.queryForMap("select * from etapa_pedido where pedido_id=? and numero=8",p);
        String nombre="f5_m_"+p.toString().replace("-","");
        sql.execute("create function "+nombre+"() returns trigger language plpgsql as $$ begin if new.pedido_id='"+p+"'::uuid then raise exception 'Fallo ficticio'; end if; return new; end $$");
        sql.execute("create trigger "+nombre+" before insert on evento_pedido for each row execute function "+nombre+"()");
        try{motivo(p,admin,entrada(p),"Motivo ficticio no confirmado",500);assertThat(sql.queryForMap("select * from etapa_pedido where pedido_id=? and numero=8",p)).isEqualTo(antes);assertThat(total("evento_pedido",p)).isZero();assertThat(total("notificacion",p)).isZero();}
        finally{sql.execute("drop trigger "+nombre+" on evento_pedido");sql.execute("drop function "+nombre+"()");}
    }
    @Test void detectorIntegradoUsaRelojControladoYRecuperaSinRepetirAvisos(){
        UUID p=pedido(8,AHORA);detector.detectar();assertThat(total("aviso_vencimiento",p)).isZero();
        RELOJ.set(AHORA.plusNanos(1000));detector.detectar();assertThat(total("aviso_vencimiento",p)).isEqualTo(1);
        detector.detectar();assertThat(total("aviso_vencimiento",p)).isEqualTo(1);assertThat(total("evento_pedido",p)).isEqualTo(1);assertThat(total("notificacion",p)).isEqualTo(1);
        assertThat(candidatos.candidatos(RELOJ.get(),1000)).doesNotContain(p);
    }
}
