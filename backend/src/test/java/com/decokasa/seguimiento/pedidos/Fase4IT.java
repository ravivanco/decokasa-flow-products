package com.decokasa.seguimiento.pedidos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL y disco exclusivos. Los pedidos posteriores a A6 son fixtures de estado,
 * NO recorridos completos. La versión ficticia resuelve exclusivamente documentos A9;
 * nunca modifica la definición vigente ni representa una decisión documental del cliente. */
@SpringBootTest(properties={"spring.config.import=","decokasa.semilla.habilitada=false"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class,Fase4IT.Tiempo.class})
class Fase4IT {
    static final Instant AHORA=Instant.parse("2026-10-16T22:00:00Z");
    static final UUID FLUJO=UUID.fromString("01a13cdb-cc00-7000-8000-000000000001");
    static final AtomicInteger NUMERO=new AtomicInteger(100000);
    static final Path DISCO=disco();
    static Path disco() {try{return Files.createTempDirectory("decokasa-fase4-");}catch(Exception e){throw new IllegalStateException(e);}}
    @TestConfiguration(proxyBeanMethods=false)
    static class Tiempo {@Bean @Primary Reloj relojF4(){return ()->AHORA;}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",()->DISCO.toString());}
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate sql;
    @Autowired DataSource ds;
    @Autowired PlatformTransactionManager gestor;
    UUID admin;
    @BeforeEach void preparar(){admin=persona("admin");}
    UUID persona(String rol){
        UUID id=UUID.randomUUID();String s=id.toString().replace("-","");
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Ficticio F4',?,?,?,'hash-ficticio',?, ?,0)",id,s+"@ejemplo.test",s,"+"+s.replaceAll("[a-f]","1").substring(0,14),Timestamp.from(AHORA),Timestamp.from(AHORA));
        rol(id,rol);return id;
    }
    void rol(UUID id,String rol){sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo=?",id,rol);}
    String http(MockHttpServletRequestBuilder r,UUID actor,String cuerpo,int codigo)throws Exception{
        return mvc.perform(r.with(jwt().jwt(j->j.subject(actor.toString()).claim("roles",List.of("admin"))))
                .contentType("application/json").content(cuerpo)).andExpect(status().is(codigo)).andReturn().getResponse().getContentAsString();
    }
    String base(UUID p){return "/api/v1/pedidos/"+p;}
    String detalle(UUID p)throws Exception{return http(get(base(p)),admin,"",200);}
    List<Object> estado(UUID p){return List.of(sql.queryForList("select * from pedido where id=?",p),
            sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p),
            sql.queryForList("select * from entrada_etapa where pedido_id=? order by id",p),
            sql.queryForList("select * from evento_pedido where pedido_id=? order by id",p),
            sql.queryForList("select * from notificacion where pedido_id=? order by id",p));}
    UUID versionFicticia(int llegada){
        UUID f=UUID.randomUUID();
        sql.update("insert into definicion_flujo(id,pais_codigo,tipo_flujo,version,vigente,creada_en) values(?,'EC','A',?,false,now())",f,NUMERO.incrementAndGet());
        sql.update("insert into definicion_etapa(id,flujo_id,numero,nombre,rol_codigo,plazo_estado,plazo_cantidad,plazo_tipo,automatica,registra_llegada) select gen_random_uuid(),?,numero,nombre,rol_codigo,plazo_estado,plazo_cantidad,plazo_tipo,automatica,numero=? from definicion_etapa where flujo_id=?",f,llegada,FLUJO);
        sql.update("update definicion_etapa n set revisa_etapa=o.revisa_etapa from definicion_etapa o where n.flujo_id=? and o.flujo_id=? and n.numero=o.numero",f,FLUJO);
        sql.update("insert into transicion_permitida(id,flujo_id,etapa_origen,accion,etapa_destino,requiere_observacion,requiere_urgencia,requiere_bodega,requisitos_documentales,exige_algun_documento) select gen_random_uuid(),?,etapa_origen,accion,etapa_destino,requiere_observacion,requiere_urgencia,requiere_bodega,case when etapa_origen=9 then 'DEFINIDOS' else requisitos_documentales end,exige_algun_documento from transicion_permitida where flujo_id=?",f,FLUJO);
        sql.update("insert into documento_requerido(transicion_id,tipo_documento) select n.id,d.tipo_documento from transicion_permitida n join transicion_permitida o on o.etapa_origen=n.etapa_origen and o.accion=n.accion join documento_requerido d on d.transicion_id=o.id where n.flujo_id=? and o.flujo_id=?",f,FLUJO);
        sql.update("insert into documento_requerido(transicion_id,tipo_documento) select id,'Documento ficticio F4' from transicion_permitida where flujo_id=? and etapa_origen=9",f);
        return f;
    }
    UUID fixture(int etapa,UUID flujo)throws Exception{
        UUID p=UUID.randomUUID(),entrada=UUID.randomUUID();
        new TransactionTemplate(gestor).executeWithoutResult(tx->{
            sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values(?,?,'EC','A',?,'Fixture aislado F4',?,?,?, ?,0)",p,"DK-EC-2026-"+NUMERO.incrementAndGet(),flujo,admin,Timestamp.from(AHORA),etapa,Timestamp.from(AHORA));
            sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,?,?)",entrada,p,etapa,Timestamp.from(AHORA));
            for(int i=1;i<=11;i++)sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id) values(?,?,?,?,?,?)",UUID.randomUUID(),p,i,i==etapa?"ACTUAL":"PENDIENTE",i==etapa?Timestamp.from(AHORA):null,i==etapa?entrada:null);
        });return p;
    }
    UUID conDocumento(UUID p)throws Exception{
        UUID entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=9",UUID.class,p);
        String r=mvc.perform(multipart(base(p)+"/etapas/9/archivos").file(new MockMultipartFile("archivo","ficticio.txt","text/plain",new byte[]{1,2,3}))
                .param("tipoDocumento","Documento ficticio F4").param("entradaEsperadaId",entrada.toString())
                .with(jwt().jwt(j->j.subject(admin.toString())))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(r,"$.id"));
    }
    String accion(UUID actor,UUID p,int etapa,String accion,String extras,int codigo)throws Exception{
        return http(post(base(p)+"/acciones"),actor,"{\"accion\":\""+accion+"\",\"etapaEsperada\":"+etapa+extras+"}",codigo);
    }
    UUID bodega(boolean activa)throws Exception{
        return UUID.fromString(JsonPath.read(http(post("/api/v1/bodegas"),admin,"{\"nombre\":\"Bodega "+UUID.randomUUID()+"\",\"ciudad\":\"Ciudad ficticia\",\"activa\":"+activa+"}",201),"$.id"));
    }
    String datos(UUID b,UUID a){return ",\"bodegaId\":\""+b+"\",\"adjuntoIds\":[\""+a+"\"]";}
    @ParameterizedTest @ValueSource(strings={"admin","logistica"})
    void catalogoPermiteRolesVigentesNormalizaYControlaVersion(String rol)throws Exception{
        UUID actor=persona(rol);String nombre="F4 "+UUID.randomUUID();
        String r=http(post("/api/v1/bodegas"),actor,"{\"nombre\":\"  "+nombre+"  \",\"ciudad\":\"  Quito  \"}",201);
        UUID b=UUID.fromString(JsonPath.read(r,"$.id"));assertThat((String)JsonPath.read(r,"$.nombre")).isEqualTo(nombre);
        assertThat((Boolean)JsonPath.read(r,"$.activa")).isTrue();
        http(post("/api/v1/bodegas"),actor,"{\"nombre\":\""+nombre.toUpperCase(Locale.ROOT)+"\",\"ciudad\":\"Otra\"}",409);
        String editado=http(patch("/api/v1/bodegas/"+b),actor,"{\"ciudad\":\"Ibarra\",\"activa\":false,\"version\":0}",200);
        assertThat((Integer)JsonPath.read(editado,"$.version")).isEqualTo(1);
        http(patch("/api/v1/bodegas/"+b),actor,"{\"activa\":true,\"version\":0}",409);
        assertThat(sql.queryForObject("select activa from bodega where id=?",Boolean.class,b)).isFalse();
        assertThat(http(get("/api/v1/bodegas?activa=false"),actor,"",200)).contains(b.toString());
    }
    @ParameterizedTest @ValueSource(strings={"consulta","asistente_china","compras_china","marketing","asistente_compras"})
    void catalogoRechazaMantenimientoConJwtAdminObsoleto(String rol)throws Exception{
        UUID u=persona(rol),b=bodega(true);var antes=sql.queryForMap("select * from bodega where id=?",b);
        http(post("/api/v1/bodegas"),u,"{\"nombre\":\"Ficticia\",\"ciudad\":\"Quito\"}",403);
        http(patch("/api/v1/bodegas/"+b),u,"{\"activa\":false,\"version\":0}",403);
        assertThat(sql.queryForMap("select * from bodega where id=?",b)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void catalogoReleeCuentaYRoles(boolean desactivar)throws Exception{
        UUID b=bodega(true);if(desactivar)sql.update("update usuario set activo=false where id=?",admin);else sql.update("delete from usuario_rol where usuario_id=?",admin);
        http(patch("/api/v1/bodegas/"+b),admin,"{\"activa\":false,\"version\":0}",403);
    }
    @ParameterizedTest @ValueSource(strings={"null","\"0\"","1.5","true","-1","9223372036854775808"})
    void versionDeBodegaEsEnteroEstricto(String version)throws Exception{
        UUID b=bodega(true);http(patch("/api/v1/bodegas/"+b),admin,"{\"activa\":false,\"version\":"+version+"}",400);
        assertThat(sql.queryForObject("select version from bodega where id=?",Long.class,b)).isZero();
    }
    @Test void catalogoConservaValidacionesYAutenticacion()throws Exception{
        mvc.perform(get("/api/v1/bodegas")).andExpect(status().isUnauthorized());
        http(get("/api/v1/bodegas"),persona("consulta"),"",200);
        UUID b=bodega(true);var antes=sql.queryForMap("select * from bodega where id=?",b);
        for(String cuerpo:List.of("{\"version\":0}","{\"activa\":false}","{\"nombre\":\"   \",\"version\":0}","{\"ciudad\":\""+"x".repeat(101)+"\",\"version\":0}"))http(patch("/api/v1/bodegas/"+b),admin,cuerpo,400);
        http(patch("/api/v1/bodegas/"+UUID.randomUUID()),admin,"{\"activa\":false,\"version\":0}",404);
        assertThat(sql.queryForMap("select * from bodega where id=?",b)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void altasOEdicionesSimultaneasNoDuplicanNombre(boolean alta)throws Exception{
        String nombre="Concurrente "+UUID.randomUUID();UUID b1=bodega(true),b2=bodega(true);var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            java.util.function.Function<UUID,Callable<Integer>> tarea=b->()->{inicio.await();
                return mvc.perform((alta?post("/api/v1/bodegas"):patch("/api/v1/bodegas/"+b)).with(jwt().jwt(j->j.subject(admin.toString())))
                    .contentType("application/json").content("{\"nombre\":\""+nombre+"\",\"ciudad\":\"Ficticia\",\"version\":0}")).andReturn().getResponse().getStatus();};
            var uno=pool.submit(tarea.apply(b1));var dos=pool.submit(tarea.apply(b2));inicio.countDown();
            assertThat(List.of(uno.get(20,TimeUnit.SECONDS),dos.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(alta?201:200,409);
        }assertThat(sql.queryForObject("select count(*) from bodega where nombre=?",Integer.class,nombre)).isEqualTo(1);
    }
    @Test void dosEdicionesConLaMismaVersionSoloConfirmanUna()throws Exception{
        UUID b=bodega(true);var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> tarea=()->{inicio.await();return mvc.perform(patch("/api/v1/bodegas/"+b).with(jwt().jwt(j->j.subject(admin.toString())))
                .contentType("application/json").content("{\"activa\":false,\"version\":0}")).andReturn().getResponse().getStatus();};
            var uno=pool.submit(tarea);var dos=pool.submit(tarea);inicio.countDown();
            assertThat(List.of(uno.get(20,TimeUnit.SECONDS),dos.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }assertThat(sql.queryForObject("select version from bodega where id=?",Long.class,b)).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(ints={5,7,9})
    void accionesPosterioresReleenRolCuentaYAsignado(int etapa)throws Exception{
        for(String tipo:List.of("otro_rol","otro_asignado","desactivado","sin_roles")){
            String rol=etapa==9?"logistica":"compras_china";UUID p=fixture(etapa,FLUJO),u=persona(tipo.equals("otro_rol")?"consulta":rol);
            if(tipo.equals("otro_asignado"))sql.update("update etapa_pedido set asignado_a=? where pedido_id=? and numero=?",persona(rol),p,etapa);
            if(tipo.equals("desactivado"))sql.update("update usuario set activo=false where id=?",u);
            if(tipo.equals("sin_roles"))sql.update("delete from usuario_rol where usuario_id=?",u);
            var antes=estado(p);
            accion(u,p,etapa,etapa==7?"REPORTAR_DEMORA":"APROBAR",",\"observacion\":\"Ficticia\",\"nuevaFechaEstimadaLlegada\":\"2026-10-19\"",403);
            assertThat(estado(p)).as(tipo+" A"+etapa).isEqualTo(antes);
        }
    }
    @Test void a5ExigeObservacionYEmbarqueTieneDiezDiasHabiles()throws Exception{
        UUID p=fixture(5,FLUJO);var antes=estado(p);
        assertThat((String)JsonPath.read(detalle(p),"$.lineaDeTiempo[4].estadoPlazo")).isEqualTo("PENDIENTE");
        accion(admin,p,5,"APROBAR","",422);accion(admin,p,5,"APROBAR",",\"observacion\":\"   \"",422);assertThat(estado(p)).isEqualTo(antes);
        accion(persona("compras_china"),p,5,"APROBAR",",\"observacion\":\"Fabricacion ficticia concluida\"",200);
        assertThat(sql.queryForObject("select vence_en from etapa_pedido where pedido_id=? and numero=6",Timestamp.class,p).toInstant()).isEqualTo(Instant.parse("2026-10-30T22:00:00Z"));
        assertThat(sql.queryForObject("select observacion from evento_pedido where pedido_id=? and tipo='ETAPA_APROBADA'",String.class,p)).isEqualTo("Fabricacion ficticia concluida");
    }
    @ParameterizedTest @ValueSource(ints={6,9})
    void requisitosV16BloqueanSinDocumentoSinMutarNiEncolar(int etapa)throws Exception{
        UUID p=fixture(etapa,FLUJO),b=bodega(true);var antes=estado(p);
        String r=accion(admin,p,etapa,"APROBAR",etapa==9?",\"bodegaId\":\""+b+"\"":"",422);
        assertThat(r).contains(etapa==6 ? "Packing List" : "al menos un documento");assertThat(estado(p)).isEqualTo(antes);
        assertThat((String)JsonPath.read(detalle(p),"$.accionesDisponibles[0].requisitosDocumentales")).isEqualTo("DEFINIDOS");
        assertThat((Boolean)JsonPath.read(detalle(p),"$.accionesDisponibles[0].puedeCompletarse")).isFalse();
        assertThat(sql.queryForObject("select requisitos_documentales from transicion_permitida where flujo_id=? and etapa_origen=? and accion='APROBAR'",String.class,FLUJO,etapa)).isEqualTo("DEFINIDOS");
        assertThat((Boolean)JsonPath.read(detalle(p),"$.accionesDisponibles[0].exigeAlgunDocumento")).isEqualTo(etapa==9);
    }
    @ParameterizedTest @ValueSource(strings={
        "\"2026-02-30\"","\"2026-10-17T00:00:00Z\"","123","[2026,10,17]","\"17/10/2026\"","\"2026-1-2\"",
        "[]","[\"2026-10-19\"]","[[2026,10,17]]","123.5","{}","{\"year\":2026,\"month\":10,\"day\":17}",
        "true","false","null","\"\"","\" 2026-10-19\"","\"2026-10-19 \"","\"2026-02-29\"",
        "\"2026-13-01\"","\"2026-10-00\"","\"2026-10-19T12:30:00\"","\"2026-10-19Z\""})
    void demoraRechazaFechasNoIsoEstrictas(String fecha)throws Exception{
        UUID p=fixture(7,FLUJO);var antes=estado(p);
        MvcResult resultado=mvc.perform(post(base(p)+"/acciones").with(jwt().jwt(j->j.subject(admin.toString())))
                .contentType("application/json").content("{\"accion\":\"REPORTAR_DEMORA\",\"etapaEsperada\":7,\"observacion\":\"Demora\",\"nuevaFechaEstimadaLlegada\":"+fecha+"}"))
                .andReturn();
        // Problema exige title/status/codigo; type es opcional (OpenAPI, RFC 9457).
        // La ausencia de cambios se comprueba incluso si falla una aserción de respuesta.
        org.junit.jupiter.api.Assertions.assertAll(
                ()->status().isBadRequest().match(resultado),
                ()->content().contentTypeCompatibleWith(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON).match(resultado),
                ()->jsonPath("$.status").value(400).match(resultado),
                ()->jsonPath("$.codigo").value("VALIDACION").match(resultado),
                ()->jsonPath("$.title").isNotEmpty().match(resultado),
                ()->jsonPath("$.detail").isNotEmpty().match(resultado),
                ()->assertThat(estado(p)).isEqualTo(antes));
    }
    @ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"2026-10-19,2026-10-19","2026-10-17,2026-10-19","2026-10-18,2026-10-19","2028-02-29,2028-02-29"})
    void demoraAceptaCadenaValidaIncluidoBisiestoYConservaReglas(String solicitada,String efectiva)throws Exception{
        UUID p=fixture(7,FLUJO);var etapas=sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p);
        accion(admin,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Fecha valida ficticia\",\"nuevaFechaEstimadaLlegada\":\""+solicitada+"\"",200);
        assertThat(sql.queryForObject("select demora_nueva_fecha_llegada from pedido where id=?",LocalDate.class,p)).isEqualTo(LocalDate.parse(efectiva));
        assertThat(sql.queryForObject("select demora_reportada_por from pedido where id=?",UUID.class,p)).isEqualTo(admin);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='DEMORA_REPORTADA'",Integer.class,p)).isEqualTo(1);
        assertThat(sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p)).isEqualTo(etapas);
        String h=http(get(base(p)+"/historial"),admin,"",200);
        assertThat((String)JsonPath.read(h,"$[0].detalle.nuevaFechaEstimadaLlegada")).isEqualTo(efectiva);
        assertThat((String)JsonPath.read(h,"$[0].detalle.fechaEstimadaSolicitada")).isEqualTo(solicitada.equals(efectiva)?null:solicitada);
    }
    @Test void fechaAusenteConservaRequisitoDeDemoraYSigueOpcionalEnOtrasAcciones()throws Exception{
        UUID demora=fixture(7,FLUJO);var antes=estado(demora);
        accion(admin,demora,7,"REPORTAR_DEMORA",",\"observacion\":\"Falta fecha\"",422);
        assertThat(estado(demora)).isEqualTo(antes);
        UUID fabricacion=fixture(5,FLUJO);antes=estado(fabricacion);
        accion(admin,fabricacion,5,"APROBAR",",\"observacion\":\"No admite fecha\",\"nuevaFechaEstimadaLlegada\":\"2026-10-19\"",400);
        assertThat(estado(fabricacion)).isEqualTo(antes);
        accion(admin,fabricacion,5,"APROBAR",",\"observacion\":\"Sin fecha opcional\"",200);
        assertThat(sql.queryForObject("select etapa_actual from pedido where id=?",Integer.class,fabricacion)).isEqualTo(6);
    }
    @Test void demoraAjustaFinDeSemanaConservaHistorialYNoTocaPlazoNiLlegada()throws Exception{
        UUID p=fixture(7,FLUJO);UUID actor=persona("compras_china");var etapas=sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p);
        accion(actor,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Primera demora\",\"nuevaFechaEstimadaLlegada\":\"2026-10-17\"",200);
        accion(actor,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Segunda demora\",\"nuevaFechaEstimadaLlegada\":\"2026-10-24\"",200);
        assertThat(sql.queryForObject("select demora_nueva_fecha_llegada from pedido where id=?",LocalDate.class,p)).isEqualTo(LocalDate.of(2026,10,26));
        assertThat(sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p)).isEqualTo(etapas);
        assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p)).isNull();
        String h=http(get(base(p)+"/historial"),admin,"",200);
        assertThat(h).contains("Primera demora","Segunda demora","2026-10-17","2026-10-19","2026-10-24","2026-10-26",actor.toString());
        assertThat(sql.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p)).isZero();
        var antes=estado(p);
        for(String fecha:List.of("2026-10-25","2026-10-26","2026-10-15"))accion(actor,p,7,"REPORTAR_DEMORA",",\"observacion\":\"No posterior\",\"nuevaFechaEstimadaLlegada\":\""+fecha+"\"",400);
        accion(actor,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Falta fecha\"",422);
        accion(actor,p,7,"REPORTAR_DEMORA",",\"nuevaFechaEstimadaLlegada\":\"2026-10-27\"",422);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"admin","logistica","consulta+logistica"})
    void salidaAvanzaConPermisoVigenteYVeinticuatroHorasHabiles(String rol)throws Exception{
        UUID p=fixture(8,FLUJO),u=persona(rol.contains("+")?"consulta":rol);if(rol.contains("+"))rol(u,"logistica");
        accion(u,p,8,"APROBAR","",200);
        assertThat(sql.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p)).isEqualTo(9);
        assertThat(sql.queryForObject("select vence_en from etapa_pedido where pedido_id=? and numero=9",Timestamp.class,p).toInstant()).isEqualTo(Instant.parse("2026-10-19T22:00:00Z"));
        assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p)).isNull();
    }
    @ParameterizedTest @ValueSource(strings={"consulta","asistente_china","compras_china","marketing","otro_asignado","desactivado","sin_roles"})
    void salidaRechazaPermisosObsoletosSinCambios(String tipo)throws Exception{
        UUID p=fixture(8,FLUJO),u=persona(Set.of("otro_asignado","desactivado","sin_roles").contains(tipo)?"logistica":tipo);
        if(tipo.equals("otro_asignado"))sql.update("update etapa_pedido set asignado_a=? where pedido_id=? and numero=8",persona("logistica"),p);
        if(tipo.equals("desactivado"))sql.update("update usuario set activo=false where id=?",u);
        if(tipo.equals("sin_roles"))sql.update("delete from usuario_rol where usuario_id=?",u);
        var antes=estado(p);accion(u,p,8,"APROBAR","",403);assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(ints={7,8})
    void llegadaSigueLaMarcaVersionadaInclusoFueraDeA7(int marcada)throws Exception{
        UUID f=versionFicticia(marcada),p=fixture(7,f);
        accion(admin,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Demora\",\"nuevaFechaEstimadaLlegada\":\"2026-10-19\"",200);
        assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p)).isNull();
        accion(admin,p,7,"APROBAR","",200);
        assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p)).isEqualTo(marcada==7?Timestamp.from(AHORA):null);
        accion(admin,p,8,"APROBAR","",200);
        assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p)).isEqualTo(Timestamp.from(AHORA));
        assertThat(http(get("/api/v1/flujos/EC/A"),admin,"",200)).contains("registraLlegada");
    }
    @Test void bodegaEnFixtureConDocumentoExigeIdActivoYAsignaUnaVez()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),a=conDocumento(p);var antes=estado(p);
        accion(admin,p,9,"APROBAR",",\"adjuntoIds\":[\""+a+"\"]",422);
        accion(admin,p,9,"APROBAR",datos(UUID.randomUUID(),a),400);
        accion(admin,p,9,"APROBAR",datos(bodega(false),a),400);
        assertThat(estado(p)).isEqualTo(antes);UUID b=bodega(true);
        accion(admin,p,9,"APROBAR",datos(b,a),200);
        assertThat((String)JsonPath.read(detalle(p),"$.bodega.id")).isEqualTo(b.toString());
        comprobarOrdenEmitido(p,List.of("BODEGA_ASIGNADA","ETAPA_APROBADA"));
        assertThat(sql.queryForList("select tipo from evento_pedido where pedido_id=? and tipo in ('BODEGA_ASIGNADA','ETAPA_APROBADA')",String.class,p)).containsExactlyInAnyOrder("BODEGA_ASIGNADA","ETAPA_APROBADA");
        assertThat(sql.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p)).isEqualTo(10);
        http(patch("/api/v1/bodegas/"+b),admin,"{\"activa\":false,\"version\":0}",200);
        assertThat((Boolean)JsonPath.read(detalle(p),"$.bodega.activa")).isFalse();
        antes=estado(p);accion(admin,p,9,"APROBAR",datos(b,a),409);
        accion(admin,p,10,"APROBAR","",422);assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void fixtureNoOmiteDocumentosNiPermisosDeBodega()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),b=bodega(true);var antes=estado(p);
        assertThat(accion(admin,p,9,"APROBAR",",\"bodegaId\":\""+b+"\"",422)).contains("Documento ficticio F4");
        accion(persona("compras_china"),p,9,"APROBAR",",\"bodegaId\":\""+b+"\"",403);
        accion(admin,p,9,"ELEGIR_BODEGA",",\"bodegaId\":\""+b+"\"",409);
        accion(admin,p,9,"APROBAR",",\"bodegaId\":\"incorrecto\"",400);assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void dobleAprobacionDeBodegaSoloConfirmaUna()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),a=conDocumento(p),b=bodega(true);var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> tarea=()->{inicio.await();return mvc.perform(post(base(p)+"/acciones").with(jwt().jwt(j->j.subject(admin.toString())))
                .contentType("application/json").content("{\"accion\":\"APROBAR\",\"etapaEsperada\":9"+datos(b,a)+"}")).andReturn().getResponse().getStatus();};
            var uno=pool.submit(tarea);var dos=pool.submit(tarea);inicio.countDown();assertThat(List.of(uno.get(20,TimeUnit.SECONDS),dos.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='BODEGA_ASIGNADA'",Integer.class,p)).isEqualTo(1);
    }
    void esperarBloqueo(int pid)throws Exception{
        long fin=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<fin){if(Boolean.TRUE.equals(sql.queryForObject("select exists(select 1 from pg_stat_activity where ?=any(pg_blocking_pids(pid)))",Boolean.class,pid)))return;Thread.yield();}
        throw new AssertionError("El segundo worker no espero el bloqueo PostgreSQL");
    }
    @Test void desactivacionGanadoraProduceUB001TraducidoYRollbackCompleto()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),a=conDocumento(p),b=bodega(true);var antes=estado(p);
        try(Connection c=ds.getConnection();var pool=Executors.newSingleThreadExecutor()){
            c.setAutoCommit(false);int pid;try(var s=c.createStatement();var r=s.executeQuery("select pg_backend_pid()")){r.next();pid=r.getInt(1);}
            try(var s=c.prepareStatement("update bodega set activa=false where id=?")){s.setObject(1,b);s.executeUpdate();}
            var tarea=pool.submit(()->accion(admin,p,9,"APROBAR",datos(b,a),400));
            try{esperarBloqueo(pid);}finally{c.commit();}
            assertThat(tarea.get(20,TimeUnit.SECONDS)).contains("bodegaId");
        }assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void asignacionGanadoraHaceEsperarDesactivacionYConservaBodega()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),a=conDocumento(p),b=bodega(true);
        try(var pool=Executors.newSingleThreadExecutor()){
            final Future<?>[] tarea=new Future<?>[1];
            new TransactionTemplate(gestor).executeWithoutResult(tx->{try{
                accion(admin,p,9,"APROBAR",datos(b,a),200);
                int pid=sql.queryForObject("select pg_backend_pid()",Integer.class);
                tarea[0]=pool.submit(()->sql.update("update bodega set activa=false where id=?",b));
                esperarBloqueo(pid);assertThat(tarea[0].isDone()).isFalse();
            }catch(Exception e){throw new IllegalStateException(e);}});
            tarea[0].get(20,TimeUnit.SECONDS);
        }
        assertThat(sql.queryForObject("select bodega_id from pedido where id=?",UUID.class,p)).isEqualTo(b);
        assertThat(sql.queryForObject("select activa from bodega where id=?",Boolean.class,b)).isFalse();
    }
    @ParameterizedTest @ValueSource(ints={7,9})
    void falloAlInsertarHistorialRevierteLlegadaOBodega(int etapa)throws Exception{
        UUID p=fixture(etapa,etapa==9?versionFicticia(7):FLUJO),b=bodega(true),a=etapa==9?conDocumento(p):null;var antes=estado(p);
        String fn="fallo_f4_"+UUID.randomUUID().toString().replace("-","");
        sql.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'Fallo ficticio F4'; end $$");
        sql.execute("create trigger "+fn+" before insert on evento_pedido for each row when(new.pedido_id='"+p+"'::uuid) execute function "+fn+"()");
        try{assertThat(accion(admin,p,etapa,"APROBAR",etapa==9?datos(b,a):"",500)).doesNotContain("Fallo ficticio F4");}
        finally{sql.execute("drop trigger "+fn+" on evento_pedido");sql.execute("drop function "+fn+"()");}
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void demoraRevierteSiHistorialFalla()throws Exception{
        UUID p=fixture(7,FLUJO);var antes=estado(p);String fn="demora_f4_"+UUID.randomUUID().toString().replace("-","");
        sql.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'Fallo demora F4'; end $$");
        sql.execute("create trigger "+fn+" before insert on evento_pedido for each row when(new.pedido_id='"+p+"'::uuid) execute function "+fn+"()");
        try{assertThat(accion(admin,p,7,"REPORTAR_DEMORA",",\"observacion\":\"No guardar\",\"nuevaFechaEstimadaLlegada\":\"2026-10-19\"",500)).doesNotContain("Fallo demora F4");}
        finally{sql.execute("drop trigger "+fn+" on evento_pedido");sql.execute("drop function "+fn+"()");}
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void bodegaYaAsignadaNoSeCambiaAunqueLaNuevaEsteActiva()throws Exception{
        UUID p=fixture(9,versionFicticia(7)),a=conDocumento(p),original=bodega(true),otra=bodega(true);
        sql.update("update pedido set bodega_id=? where id=?",original,p);var antes=estado(p);
        accion(admin,p,9,"APROBAR",datos(otra,a),409);assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(ints={5,7,8})
    void bodegaFueraDeSuAccionNoSeIgnora(int etapa)throws Exception{
        UUID p=fixture(etapa,FLUJO);var antes=estado(p);
        accion(admin,p,etapa,"APROBAR",",\"observacion\":\"Ficticia\",\"bodegaId\":\""+bodega(true)+"\"",400);
        if(etapa==7)accion(admin,p,7,"REPORTAR_DEMORA",",\"observacion\":\"Demora\",\"nuevaFechaEstimadaLlegada\":\"2026-10-19\",\"bodegaId\":\""+bodega(true)+"\"",400);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void salidaAsignadaAdmiteSoloAsignadoOAdmin()throws Exception{
        UUID p=fixture(8,FLUJO),u=persona("logistica");sql.update("update etapa_pedido set asignado_a=? where pedido_id=? and numero=8",u,p);
        accion(u,p,8,"APROBAR","",200);
        UUID otro=fixture(8,FLUJO);sql.update("update etapa_pedido set asignado_a=? where pedido_id=? and numero=8",u,otro);
        accion(admin,otro,8,"APROBAR","",200);
    }
    @Test void historialConservaEmisionAunqueUuidInviertaElOrden()throws Exception{
        UUID p=fixture(9,FLUJO);UUID primero=UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"),segundo=UUID.fromString("00000000-0000-4000-8000-000000000001");
        // Inserción causal explícita: asignación primero, aprobación después, a la misma hora.
        for(var e:List.of(Map.entry(primero,"BODEGA_ASIGNADA"),Map.entry(segundo,"ETAPA_APROBADA")))
            sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values(?,?,9,?,?,'admin',?,'{}')",e.getKey(),p,e.getValue(),admin,Timestamp.from(AHORA));
        List<String> orden=JsonPath.read(http(get(base(p)+"/historial"),admin,"",200),"$[*].tipo");
        // La API sigue descendente: V13 conserva la emision; el UUID adversarial ya no la invierte.
        assertThat(orden).containsExactly("ETAPA_APROBADA","BODEGA_ASIGNADA");
        comprobarOrdenEmitido(p,List.of("BODEGA_ASIGNADA","ETAPA_APROBADA"));
        assertThat(JsonPath.<List<String>>read(http(get(base(p)+"/historial"),admin,"",200),"$[*].tipo")).containsExactlyElementsOf(orden);
        assertThat(sql.queryForList("select tipo from evento_pedido where pedido_id=? order by ocurrido_en,id",String.class,p))
                .containsExactly("ETAPA_APROBADA","BODEGA_ASIGNADA");
    }

    // Fixture posterior a los bloqueos, NO un recorrido A1-Fin ni una decision F4-D8.
    UUID versionIncidenciasFicticia() {
        UUID f=versionFicticia(7);
        sql.update("update transicion_permitida set requisitos_documentales='DEFINIDOS' where flujo_id=? and etapa_origen=10",f);
        sql.update("insert into documento_requerido(transicion_id,tipo_documento) select id,'Incidencia ficticia F4' from transicion_permitida where flujo_id=? and etapa_origen=10",f);
        return f;
    }
    UUID documentoIncidencia(UUID p)throws Exception {
        UUID entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=10",UUID.class,p);
        String r=mvc.perform(multipart(base(p)+"/etapas/10/archivos")
                .file(new MockMultipartFile("archivo","incidencia-ficticia.txt","text/plain",new byte[]{4,5,6}))
                .param("tipoDocumento","Incidencia ficticia F4").param("entradaEsperadaId",entrada.toString())
                .with(jwt().jwt(j->j.subject(admin.toString())))).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(r,"$.id"));
    }
    String documento(UUID id){return ",\"adjuntoIds\":[\""+id+"\"]";}
    void comprobarFin(UUID p)throws Exception {
        String r=detalle(p);
        comprobarOrdenEmitido(p,List.of("ETAPA_APROBADA","PEDIDO_FINALIZADO"));
        assertThat((Boolean)JsonPath.read(r,"$.finalizado")).isTrue();
        assertThat((Integer)JsonPath.read(r,"$.etapaActual")).isEqualTo(11);
        assertThat((List<?>)JsonPath.read(r,"$.accionesDisponibles")).isEmpty();
        assertThat(sql.queryForObject("select finalizado_en from pedido where id=?",Timestamp.class,p)).isEqualTo(Timestamp.from(AHORA));
        assertThat(sql.queryForObject("select count(*) from etapa_pedido where pedido_id=? and estado='ACTUAL'",Integer.class,p)).isZero();
        assertThat(sql.queryForList("select estado from etapa_pedido where pedido_id=? and numero in (10,11)",String.class,p)).containsExactly("COMPLETADA","COMPLETADA");
        var fin=sql.queryForMap("select entrada_en,salida_en,vence_en from etapa_pedido where pedido_id=? and numero=11",p);
        assertThat(fin.get("entrada_en")).isEqualTo(Timestamp.from(AHORA));
        assertThat(fin.get("salida_en")).isEqualTo(Timestamp.from(AHORA));
        assertThat(fin.get("vence_en")).isNull();
        assertThat(sql.queryForObject("select count(*) from entrada_etapa where pedido_id=? and etapa=11",Integer.class,p)).isEqualTo(1);
        var sistema=sql.queryForMap("select usuario_id,rol_codigo,ocurrido_en from evento_pedido where pedido_id=? and tipo='PEDIDO_FINALIZADO'",p);
        assertThat(sistema.get("usuario_id")).isNull();assertThat(sistema.get("rol_codigo")).isNull();
        assertThat(sistema.get("ocurrido_en")).isEqualTo(Timestamp.from(AHORA));
        List<Map<String,Object>> historial=JsonPath.read(http(get(base(p)+"/historial"),admin,"",200),"$");
        assertThat(historial.stream().filter(e->e.get("tipo").equals("PEDIDO_FINALIZADO")).toList())
                .singleElement().satisfies(e->assertThat(e.get("usuario")).isNull());
    }
    @ParameterizedTest @ValueSource(strings={"admin","logistica"})
    void a10VigenteExigeDocumentoConPlazoPendienteYFinAutomatico(String rol)throws Exception {
        UUID p=fixture(10,FLUJO),u=persona(rol);var antes=estado(p);
        String r=http(get(base(p)),u,"",200);
        assertThat((String)JsonPath.read(r,"$.lineaDeTiempo[9].estadoPlazo")).isEqualTo("PENDIENTE");
        assertThat(JsonPath.<Object>read(r,"$.lineaDeTiempo[9].venceEn")).isNull();
        assertThat((String)JsonPath.read(r,"$.accionesDisponibles[0].requisitosDocumentales")).isEqualTo("DEFINIDOS");
        assertThat((Boolean)JsonPath.read(r,"$.accionesDisponibles[0].exigeAlgunDocumento")).isTrue();
        assertThat((Boolean)JsonPath.read(r,"$.accionesDisponibles[0].puedeCompletarse")).isFalse();
        assertThat(accion(u,p,10,"APROBAR","",422)).contains("al menos un documento");
        assertThat(estado(p)).isEqualTo(antes);
        UUID archivo=documentoIncidencia(p);var conArchivo=estado(p);
        assertThat(antes.get(0)).isEqualTo(conArchivo.get(0));
        accion(u,p,10,"APROBAR",documento(archivo),200);comprobarFin(p);
        var cerrado=estado(p);accion(u,p,10,"APROBAR",documento(archivo),409);assertThat(estado(p)).isEqualTo(cerrado);
    }
    @ParameterizedTest @ValueSource(strings={"admin","logistica"})
    void a10ConRequisitosDeFixtureFinalizaYRegistraSistema(String rol)throws Exception {
        UUID p=fixture(10,versionIncidenciasFicticia()),u=persona(rol);var antes=estado(p);
        accion(u,p,10,"APROBAR","",422);assertThat(estado(p)).isEqualTo(antes);
        UUID archivo=documentoIncidencia(p);
        String r=accion(u,p,10,"APROBAR",documento(archivo),200);
        assertThat((Boolean)JsonPath.read(r,"$.finalizado")).isTrue();comprobarFin(p);
        assertThat(sql.queryForObject("select usuario_id from evento_pedido where pedido_id=? and tipo='ETAPA_APROBADA'",UUID.class,p)).isEqualTo(u);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo in ('ETAPA_APROBADA','PEDIDO_FINALIZADO')",Integer.class,p)).isEqualTo(2);
        var cerrado=estado(p);accion(u,p,10,"APROBAR",documento(archivo),409);assertThat(estado(p)).isEqualTo(cerrado);
        assertThat(sql.queryForObject("select requisitos_documentales from transicion_permitida where flujo_id=? and etapa_origen=10",String.class,FLUJO)).isEqualTo("DEFINIDOS");
    }
    @Test void a10NoAdmiteDocumentoDeOtroPedido()throws Exception {
        UUID f=versionIncidenciasFicticia(),p=fixture(10,f),otro=fixture(10,f),a=documentoIncidencia(otro);var antes=estado(p);
        accion(admin,p,10,"APROBAR",documento(a),400);assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"otro_rol","otro_asignado","desactivado","sin_roles"})
    void a10ReleePermisosAntesDeCerrar(String tipo)throws Exception {
        UUID p=fixture(10,versionIncidenciasFicticia()),a=documentoIncidencia(p),u=persona(tipo.equals("otro_rol")?"consulta":"logistica");
        if(tipo.equals("otro_asignado"))sql.update("update etapa_pedido set asignado_a=? where pedido_id=? and numero=10",persona("logistica"),p);
        if(tipo.equals("desactivado"))sql.update("update usuario set activo=false where id=?",u);
        if(tipo.equals("sin_roles"))sql.update("delete from usuario_rol where usuario_id=?",u);
        var antes=estado(p);accion(u,p,10,"APROBAR",documento(a),403);assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"entrada","aprobacion","sistema"})
    void falloDeCierreRevierteAccionFinEHistorial(String punto)throws Exception {
        UUID p=fixture(10,versionIncidenciasFicticia()),a=documentoIncidencia(p);var antes=estado(p);
        String fn="cierre_f4_"+UUID.randomUUID().toString().replace("-","");
        String tabla=punto.equals("entrada")?"entrada_etapa":"evento_pedido";
        String condicion=punto.equals("entrada")?"new.etapa=11":"new.tipo='"+(punto.equals("sistema")?"PEDIDO_FINALIZADO":"ETAPA_APROBADA")+"'";
        sql.execute("create function "+fn+"() returns trigger language plpgsql as $$ begin raise exception 'Fallo cierre ficticio'; end $$");
        sql.execute("create trigger "+fn+" before insert on "+tabla+" for each row when(new.pedido_id='"+p+"'::uuid and "+condicion+") execute function "+fn+"()");
        try{assertThat(accion(admin,p,10,"APROBAR",documento(a),500)).doesNotContain("Fallo cierre ficticio");}
        finally{sql.execute("drop trigger "+fn+" on "+tabla);sql.execute("drop function "+fn+"()");}
        assertThat(estado(p)).isEqualTo(antes);
        accion(admin,p,10,"APROBAR",documento(a),200);comprobarFin(p);
    }
    @Test void a10DobleEnvioConcurrenteSoloCierraUnaVez()throws Exception {
        UUID p=fixture(10,versionIncidenciasFicticia()),a=documentoIncidencia(p);var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> tarea=()->{inicio.await();return mvc.perform(post(base(p)+"/acciones").with(jwt().jwt(j->j.subject(admin.toString())))
                    .contentType("application/json").content("{\"accion\":\"APROBAR\",\"etapaEsperada\":10"+documento(a)+"}")).andReturn().getResponse().getStatus();};
            var uno=pool.submit(tarea);var dos=pool.submit(tarea);inicio.countDown();
            assertThat(List.of(uno.get(20,TimeUnit.SECONDS),dos.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        comprobarFin(p);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ETAPA_APROBADA'",Integer.class,p)).isEqualTo(1);
    }
    @ParameterizedTest @ValueSource(strings={"admin","logistica"})
    void finRechazaAccionesObservacionesSubidasYReasignacion(String rol)throws Exception {
        UUID p=fixture(10,versionIncidenciasFicticia()),a=documentoIncidencia(p),u=persona(rol);
        accion(admin,p,10,"APROBAR",documento(a),200);var antes=estado(p);
        for(String nombre:List.of("APROBAR","ENVIAR","REPORTAR_DEMORA","ELEGIR_BODEGA"))accion(u,p,11,nombre,"",409);
        UUID entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=11",UUID.class,p);
        http(post(base(p)+"/observaciones"),u,"{\"etapaEsperada\":11,\"entradaEsperadaId\":\""+entrada+"\",\"texto\":\"No guardar\"}",409);
        mvc.perform(multipart(base(p)+"/etapas/11/archivos").file(new MockMultipartFile("archivo","no.txt","text/plain",new byte[]{1}))
                .param("tipoDocumento","Ficticio").param("entradaEsperadaId",entrada.toString()).with(jwt().jwt(j->j.subject(u.toString())))).andExpect(status().isConflict());
        http(put(base(p)+"/etapas/11/asignacion"),admin,"{\"usuarioId\":\""+persona("logistica")+"\",\"motivo\":\"No guardar\"}",409);
        assertThat(estado(p)).isEqualTo(antes);
    }


    void comprobarOrdenEmitido(UUID p,List<String> tipos)throws Exception {
        var filas=sql.queryForList("select tipo,secuencia,ocurrido_en from evento_pedido where pedido_id=? and tipo in ('BODEGA_ASIGNADA','ETAPA_APROBADA','PEDIDO_FINALIZADO') order by secuencia",p);
        assertThat(filas.stream().map(e->e.get("tipo")).toList()).containsExactlyElementsOf(tipos);
        assertThat(filas.stream().map(e->(Long)e.get("secuencia")).toList()).doesNotContainNull().doesNotHaveDuplicates().isSorted();
        assertThat(filas.stream().map(e->e.get("ocurrido_en")).toList()).allMatch(Timestamp.from(AHORA)::equals);
        var descendente=new ArrayList<>(tipos);Collections.reverse(descendente);
        List<String> recibido=JsonPath.read(http(get(base(p)+"/historial"),admin,"",200),"$[*].tipo");
        assertThat(recibido.stream().filter(tipos::contains).toList()).containsExactlyElementsOf(descendente);
        assertThat(http(get(base(p)+"/historial"),admin,"",200)).doesNotContain("\"secuencia\"");
    }
    @Test void resumenEligeUltimoCierrePorSecuenciaConLaMismaHora()throws Exception {
        UUID p=fixture(9,FLUJO);
        List<UUID> ids=List.of(UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffff01"),UUID.fromString("00000000-0000-4000-8000-000000000002"));
        for(int i=0;i<2;i++)sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,observacion,datos) values(?,?,9,'ETAPA_APROBADA',?,'admin',?,?,'{\"etapaDestino\":10}')",ids.get(i),p,admin,Timestamp.from(AHORA),i==0?"Cierre primero ficticio":"Cierre ultimo ficticio");
        List<String> observaciones=JsonPath.read(http(get(base(p)+"/historial"),admin,"",200),"$[*].observacion");
        assertThat(observaciones).containsExactly("Cierre ultimo ficticio","Cierre primero ficticio");
        assertThat((String)JsonPath.read(detalle(p),"$.lineaDeTiempo[8].resumen")).contains("Cierre ultimo ficticio").doesNotContain("Cierre primero ficticio");
    }
    @Test void accionesConcurrentesDePedidosDistintosRecibenSecuenciasUnicas()throws Exception {
        List<UUID> pedidos=new ArrayList<>();for(int i=0;i<4;i++)pedidos.add(fixture(8,FLUJO));
        var inicio=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(4)){
            List<Future<String>> respuestas=new ArrayList<>();
            for(UUID p:pedidos)respuestas.add(pool.submit(()->{inicio.await();return accion(admin,p,8,"APROBAR","",200);}));
            inicio.countDown();for(var r:respuestas)assertThat(r.get(30,TimeUnit.SECONDS)).contains("etapaActual");
        }
        var secuencias=new ArrayList<Long>();
        for(UUID p:pedidos){comprobarOrdenEmitido(p,List.of("ETAPA_APROBADA"));secuencias.add(sql.queryForObject("select secuencia from evento_pedido where pedido_id=? and tipo='ETAPA_APROBADA'",Long.class,p));}
        assertThat(secuencias).hasSize(4).doesNotContainNull().doesNotHaveDuplicates();
    }

}
