package com.decokasa.seguimiento.pedidos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** API y PostgreSQL reales; datos ficticios, reloj controlado y almacenamiento exclusivo. No es E2E de pantallas. */
@SpringBootTest(properties={"spring.config.import=","decokasa.semilla.habilitada=false",
    "decokasa.google.oauth.habilitada=false","decokasa.google.drive.habilitada=false",
    "decokasa.google.gmail.habilitada=false","decokasa.google.drive.worker.automatico=false",
    "decokasa.google.gmail.worker.automatico=false","decokasa.plazos.vencimientos.automatico=false"})
@AutoConfigureMockMvc @Import({TestcontainersConfiguration.class,Fase6IT.Tiempo.class})
class Fase6IT {
    static final Instant AHORA=Instant.parse("2026-10-21T15:00:00Z");
    static final AtomicReference<Instant> RELOJ=new AtomicReference<>(AHORA);
    static final Path DISCO=disco();
    static Path disco(){try{return Files.createTempDirectory("decokasa-f6-b1-");}catch(Exception e){throw new IllegalStateException(e);}}
    @TestConfiguration(proxyBeanMethods=false) static class Tiempo {@Bean @Primary Reloj relojF6(){return RELOJ::get;}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",DISCO::toString);}
    @Autowired MockMvc mvc; @Autowired JdbcTemplate sql;
    UUID admin,china,marketing,revision,logistica;
    @BeforeEach void preparar(){RELOJ.set(AHORA);admin=persona("admin");china=persona("compras_china");marketing=persona("marketing");revision=persona("asistente_compras");logistica=persona("logistica");}
    UUID persona(String rol){UUID id=UUID.randomUUID();String s=id.toString().replace("-","");
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Persona ficticia F6',?,?,?,'hash-ficticio',?,?,0)",id,s+"@ejemplo.test",s,"+"+s.replaceAll("[a-f]","1").substring(0,14),Timestamp.from(AHORA),Timestamp.from(AHORA));rol(id,rol);sql.update("insert into usuario_pais(usuario_id,pais_codigo) values (?,'EC')",id);return id;}
    void rol(UUID id,String rol){sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo=?",id,rol);}
    String http(MockHttpServletRequestBuilder r,UUID u,String cuerpo,int status)throws Exception{
        return mvc.perform(r.with(jwt().jwt(j->j.subject(u.toString()).claim("roles",List.of("admin","compras_china")))).contentType("application/json").content(cuerpo)).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();}
    String base(UUID p){return "/api/v1/pedidos/"+p;}
    UUID crear(String tipo,UUID u,int status)throws Exception{String r=http(post("/api/v1/pedidos"),u,"{\"pais\":\"EC\",\"tipoFlujo\":\""+tipo+"\",\"nombre\":\"Pedido ficticio F6\"}",status);return status==201?UUID.fromString(JsonPath.read(r,"$.id")):null;}
    UUID entrada(UUID p,int n){return sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=?",UUID.class,p,n);}
    UUID archivo(UUID p,int n,String tipo)throws Exception{return archivo(p,n,tipo,admin,201);}
    UUID archivo(UUID p,int n,String tipo,UUID u,int status)throws Exception{
        String r=mvc.perform(multipart(base(p)+"/etapas/"+n+"/archivos").file(new MockMultipartFile("archivo","ficticio-f6.txt","text/plain",new byte[]{1,2,3,4}))
            .param("tipoDocumento",tipo).param("entradaEsperadaId",entrada(p,n).toString()).with(jwt().jwt(j->j.subject(u.toString()))))
            .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();return status==201?UUID.fromString(JsonPath.read(r,"$.id")):null;}
    String adj(UUID a){return ",\"adjuntoIds\":[\""+a+"\"]";}
    String accion(UUID p,int n,String tipo,String extras,int status)throws Exception{return accion(p,n,tipo,extras,admin,status);}
    String accion(UUID p,int n,String tipo,String extras,UUID u,int status)throws Exception{return http(post(base(p)+"/acciones"),u,"{\"accion\":\""+tipo+"\",\"etapaEsperada\":"+n+extras+"}",status);}
    List<Object> estado(UUID p){return List.of(sql.queryForList("select * from pedido where id=?",p),sql.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p),sql.queryForList("select * from entrada_etapa where pedido_id=? order by id",p),sql.queryForList("select * from evento_pedido where pedido_id=? order by id",p),sql.queryForList("select * from notificacion where pedido_id=? order by id",p));}
    List<Map<String,Object>> numeracion(){return sql.queryForList("select * from secuencia_pedido order by pais_codigo,anio");}
    int actual(UUID p){return sql.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p);}
    void enviar(UUID p,int n,String tipo)throws Exception{accion(p,n,"ENVIAR",adj(archivo(p,n,tipo)),200);}
    UUID hasta(int n)throws Exception{UUID p=crear("B",china,201);for(int i=1;i<n;i++){
        if(i==1)enviar(p,i,"Solicitud");else if(i==3)enviar(p,i,"Documentos codificados");
        else if(i==6)accion(p,i,"APROBAR",",\"observacion\":\"Fabricacion ficticia\"",200);
        else if(i==7)accion(p,i,"APROBAR",adj(archivo(p,i,"Packing List")),200);
        else accion(p,i,"APROBAR","",200);
    }return p;}
    UUID bodega(boolean activa)throws Exception{return UUID.fromString(JsonPath.read(http(post("/api/v1/bodegas"),admin,"{\"nombre\":\"F6 "+UUID.randomUUID()+"\",\"ciudad\":\"Ciudad ficticia\",\"activa\":"+activa+"}",201),"$.id"));}
    void sinCambio(UUID p,int n,String tipo,String extras,int status)throws Exception{var antes=estado(p);accion(p,n,tipo,extras,status);assertThat(estado(p)).isEqualTo(antes);}
    @Test void definicionPublicaDoceEtapasYContratosSinCambiarA()throws Exception{
        String a=http(get("/api/v1/flujos/EC/A"),admin,"",200),b=http(get("/api/v1/flujos/EC/B"),admin,"",200);
        assertThat((List<?>)JsonPath.read(a,"$.etapas")).hasSize(11);assertThat((List<?>)JsonPath.read(b,"$.etapas")).hasSize(12);
        assertThat(JsonPath.<List<Integer>>read(b,"$.etapas[?(@.estadoPlazo == 'PENDIENTE')].numero")).containsExactly(2,6,8,11);
        assertThat(JsonPath.<List<Integer>>read(b,"$.etapas[?(@.automatica == true)].numero")).containsExactly(12);
        assertThat(JsonPath.<List<Integer>>read(b,"$.etapas[?(@.registraLlegada == true)].numero")).containsExactly(8);
        assertThat(b).contains("Packing List","Documentos codificados","Solicitud","exigeAlgunDocumento");
        assertThat(http(get("/api/v1/flujos/EC/A"),admin,"",200)).isEqualTo(a);
    }
    @ParameterizedTest @ValueSource(strings={"admin","marketing","logistica","consulta","asistente_compras","asistente_china"})
    void crearBSinChinaVigenteNoConsumeNumeracion(String rol)throws Exception{UUID u=persona(rol);var antes=numeracion();int pedidos=sql.queryForObject("select count(*) from pedido",Integer.class);crear("B",u,403);assertThat(numeracion()).isEqualTo(antes);assertThat(sql.queryForObject("select count(*) from pedido",Integer.class)).isEqualTo(pedidos);}
    @ParameterizedTest @ValueSource(booleans={true,false})
    void crearBReleeCuentaYRolAunqueJwtConserveChina(boolean desactivar)throws Exception{if(desactivar)sql.update("update usuario set activo=false where id=?",china);else sql.update("delete from usuario_rol where usuario_id=?",china);var antes=numeracion();crear("B",china,403);assertThat(numeracion()).isEqualTo(antes);}
    @Test void rolesMultiplesCreanPorResponsableYNumeracionCompartidaSinCruzarPedidos()throws Exception{
        rol(admin,"compras_china");UUID a=crear("A",admin,201),b=crear("B",admin,201);var antesA=estado(a);
        assertThat(sql.queryForObject("select count(*) from etapa_pedido where pedido_id=?",Integer.class,b)).isEqualTo(12);
        assertThat(sql.queryForObject("select count(*) from etapa_pedido where pedido_id=?",Integer.class,a)).isEqualTo(11);
        String numeroA=sql.queryForObject("select numero from pedido where id=?",String.class,a),numeroB=sql.queryForObject("select numero from pedido where id=?",String.class,b);
        assertThat(Integer.parseInt(numeroB.substring(numeroB.lastIndexOf('-')+1))).isEqualTo(Integer.parseInt(numeroA.substring(numeroA.lastIndexOf('-')+1))+1);
        assertThat(sql.queryForObject("select rol_codigo from evento_pedido where pedido_id=? and tipo='PEDIDO_CREADO'",String.class,b)).isEqualTo("compras_china");
        UUID ab=archivo(b,1,"Solicitud"),aa=archivo(a,1,"Solicitud");antesA=estado(a);sinCambio(b,1,"ENVIAR",adj(aa),400);
        accion(b,1,"ENVIAR",adj(ab),200);assertThat(estado(a)).isEqualTo(antesA);
    }
    @ParameterizedTest @CsvSource({"2,1,RECHAZAR","4,3,RECHAZAR","5,3,DEVOLVER_POR_ERROR"})
    void devolucionesCreanEntradaCorrectaYNoB2(int origen,int destino,String tipo)throws Exception{
        UUID p=hasta(origen),anterior=entrada(p,destino);sinCambio(p,origen,tipo,"",422);
        accion(p,origen,tipo,",\"observacion\":\"Correccion ficticia\"",200);assertThat(actual(p)).isEqualTo(destino);assertThat(entrada(p,destino)).isNotEqualTo(anterior);
        assertThat(sql.queryForObject("select reentradas from etapa_pedido where pedido_id=? and numero=?",Integer.class,p,destino)).isEqualTo(1);
        if(origen==5)assertThat(actual(p)).isNotEqualTo(2);
        sinCambio(p,destino,"ENVIAR","",422);
        UUID archivoAnterior=sql.queryForObject("select id from adjunto where pedido_id=? and etapa=? and entrada_id=? limit 1",UUID.class,p,destino,anterior);
        sinCambio(p,destino,"ENVIAR",adj(archivoAnterior),400);
        if(origen==5)assertThat(sql.queryForObject("select estado from etapa_pedido where pedido_id=? and numero=4",String.class,p)).isEqualTo("DEVUELTA");
    }
    @ParameterizedTest @CsvSource({"URGENTE,4","PRIORITARIO,12","NORMAL,24"})
    void urgenciaConIdentidadEstableHastaB5YP10Provisional(String nivel,int horas)throws Exception{
        UUID p=hasta(5);accion(p,5,"DEVOLVER_POR_PRODUCTO_NUEVO",",\"observacion\":\"Producto ficticio\",\"nivelUrgencia\":\""+nivel+"\"",200);
        UUID activacion=sql.queryForObject("select urgencia_activacion_id from pedido where id=?",UUID.class,p);assertThat(activacion).isEqualTo(entrada(p,3));
        var vence=sql.queryForObject("select urgencia_vence_en from pedido where id=?",Timestamp.class,p);assertThat(vence.toInstant()).isEqualTo(AHORA.plusSeconds(horas*3600L));
        enviar(p,3,"Documentos codificados");accion(p,4,"RECHAZAR",",\"observacion\":\"Rechazo ficticio\"",200);
        assertThat(sql.queryForObject("select urgencia_activacion_id from pedido where id=?",UUID.class,p)).isEqualTo(activacion);assertThat(sql.queryForObject("select urgencia_vence_en from pedido where id=?",Timestamp.class,p)).isEqualTo(vence);
        enviar(p,3,"Documentos codificados");accion(p,4,"APROBAR","",200);assertThat(actual(p)).isEqualTo(5);assertThat(sql.queryForObject("select urgencia_nivel from pedido where id=?",String.class,p)).isNull();
        RELOJ.set(AHORA.plusSeconds(60));accion(p,5,"DEVOLVER_POR_PRODUCTO_NUEVO",",\"observacion\":\"Otro producto ficticio\",\"nivelUrgencia\":\"URGENTE\"",200);
        assertThat(sql.queryForObject("select urgencia_activacion_id from pedido where id=?",UUID.class,p)).isNotEqualTo(activacion);
        assertThat(http(get(base(p)+"/historial"),admin,"",200)).contains("PRODUCTO_NUEVO","urgenciaTerminada");
    }
    @ParameterizedTest @CsvSource({"1,2,Solicitud","3,4,Documentos codificados"})
    void descargaRevisoraRespetaAsignacionRolesMultiplesYEstadoVigente(int origen,int revisora,String tipo)throws Exception{
        UUID p=hasta(origen),a=archivo(p,origen,tipo),otro=persona("asistente_compras"),multiple=persona("consulta");rol(multiple,"asistente_compras");
        String ruta="/api/v1/archivos/"+a+"/contenido";http(get(ruta),revision,"",200);http(get(ruta),multiple,"",200);http(get(ruta),admin,"",200);
        accion(p,origen,"ENVIAR",adj(a),200);http(put(base(p)+"/etapas/"+revisora+"/asignacion"),admin,"{\"usuarioId\":\""+revision+"\"}",200);
        http(get(ruta),otro,"",403);http(get(ruta),multiple,"",403);http(get(ruta),revision,"",200);http(get(ruta),admin,"",200);
        sql.update("delete from usuario_rol where usuario_id=?",revision);http(get(ruta),revision,"",403);rol(revision,"asistente_compras");sql.update("update usuario set activo=false where id=?",revision);http(get(ruta),revision,"",403);
        if(origen==3){accion(p,4,"APROBAR","",200);assertThat(actual(p)).isEqualTo(5);http(get(ruta),china,"",403);} // B5 no revisa B3.
    }
    @Test void desdeB6HastaFinConDocumentosDemoraLlegadaYBodega()throws Exception{
        UUID p=hasta(6);sinCambio(p,6,"APROBAR","",422);accion(p,6,"APROBAR",",\"observacion\":\"Fabricacion ficticia finalizada\"",china,200);
        assertThat(sql.queryForObject("select vence_en from etapa_pedido where pedido_id=? and numero=7",Timestamp.class,p).toInstant()).isEqualTo(Instant.parse("2026-11-04T15:00:00Z"));
        sinCambio(p,7,"APROBAR","",422);UUID bl=archivo(p,7,"BL");sinCambio(p,7,"APROBAR",adj(bl),422);UUID pl=archivo(p,7,"Packing List");accion(p,7,"APROBAR",adj(pl),china,200);
        assertThat(sql.queryForObject("select vence_en from etapa_pedido where pedido_id=? and numero=8",Timestamp.class,p)).isNull();
        accion(p,8,"REPORTAR_DEMORA",",\"observacion\":\"Demora ficticia\",\"nuevaFechaEstimadaLlegada\":\"2026-12-05\"",china,200);assertThat(actual(p)).isEqualTo(8);
        assertThat(sql.queryForObject("select demora_nueva_fecha_llegada from pedido where id=?",java.sql.Date.class,p).toLocalDate()).isEqualTo(LocalDate.parse("2026-12-07"));
        RELOJ.set(AHORA.plusSeconds(60));accion(p,8,"APROBAR","",china,200);assertThat(sql.queryForObject("select llegada_aduana_en from pedido where id=?",Timestamp.class,p).toInstant()).isEqualTo(RELOJ.get());
        accion(p,9,"APROBAR","",logistica,200);sinCambio(p,10,"APROBAR","",422);UUID doc=archivo(p,10,"Documento libre"),inactiva=bodega(false),activa=bodega(true);
        sinCambio(p,10,"APROBAR",adj(doc)+",\"bodegaId\":\""+inactiva+"\"",400);sinCambio(p,10,"APROBAR",",\"bodegaId\":\""+activa+"\"",422);
        accion(p,10,"APROBAR",adj(doc)+",\"bodegaId\":\""+activa+"\"",logistica,200);assertThat(actual(p)).isEqualTo(11);
        String detalle=http(get(base(p)),logistica,"",200);assertThat((List<?>)JsonPath.read(detalle,"$.accionesDisponibles")).isNotEmpty();
        http(post(base(p)+"/observaciones"),logistica,"{\"texto\":\"Observacion ficticia\",\"etapaEsperada\":11,\"entradaEsperadaId\":\""+entrada(p,11)+"\"}",201);
        sinCambio(p,11,"APROBAR","",422);UUID finalDoc=archivo(p,11,"Incidencia ficticia",logistica,201);accion(p,11,"APROBAR",adj(finalDoc),logistica,200);
        assertThat(actual(p)).isEqualTo(12);assertThat(sql.queryForObject("select finalizado from pedido where id=?",Boolean.class,p)).isTrue();
        assertThat(sql.queryForObject("select count(*) from etapa_pedido where pedido_id=? and numero in(11,12) and estado='COMPLETADA'",Integer.class,p)).isEqualTo(2);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='PEDIDO_FINALIZADO' and usuario_id is null and ocurrido_en=?",Integer.class,p,Timestamp.from(RELOJ.get()))).isEqualTo(1);
        detalle=http(get(base(p)),admin,"",200);assertThat((List<?>)JsonPath.read(detalle,"$.accionesDisponibles")).isEmpty();
        for(int n:List.of(11,12))sinCambio(p,n,"APROBAR","",409);
        archivo(p,12,"No admitido",admin,409);
        http(post(base(p)+"/observaciones"),admin,"{\"texto\":\"No admitida\",\"etapaEsperada\":12,\"entradaEsperadaId\":\""+entrada(p,12)+"\"}",409);
        http(put(base(p)+"/etapas/12/motivo-retraso"),admin,"{\"motivo\":\"No admitido\",\"entradaEsperadaId\":\""+entrada(p,12)+"\"}",409);
        String hist=http(get(base(p)+"/historial"),admin,"",200);assertThat(hist).contains("DEMORA_REPORTADA","BODEGA_ASIGNADA","PEDIDO_FINALIZADO");
    }
    @Test void concurrentesConfirmarB11SoloUnaVezYNoTocarA()throws Exception{
        UUID a=crear("A",admin,201),p=hasta(10);UUID b=bodega(true),doc=archivo(p,10,"Libre");accion(p,10,"APROBAR",adj(doc)+",\"bodegaId\":\""+b+"\"",200);UUID fin=archivo(p,11,"Libre");var antesA=estado(a);
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try{Callable<Integer> tarea=()->{inicio.await();return mvc.perform(post(base(p)+"/acciones").with(jwt().jwt(j->j.subject(logistica.toString()))).contentType("application/json").content("{\"accion\":\"APROBAR\",\"etapaEsperada\":11,\"adjuntoIds\":[\""+fin+"\"]}")).andReturn().getResponse().getStatus();};var x=pool.submit(tarea);var y=pool.submit(tarea);inicio.countDown();assertThat(List.of(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);}finally{pool.shutdownNow();}
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='PEDIDO_FINALIZADO'",Integer.class,p)).isEqualTo(1);assertThat(estado(a)).isEqualTo(antesA);
    }
    @ParameterizedTest @ValueSource(strings={"evento_pedido","notificacion"})
    void falloDeHistorialOOutboxRevierteCierreB11Completo(String tabla)throws Exception{
        UUID p=hasta(10),b=bodega(true),doc=archivo(p,10,"Libre");accion(p,10,"APROBAR",adj(doc)+",\"bodegaId\":\""+b+"\"",200);UUID fin=archivo(p,11,"Libre");var antes=estado(p);String trigger="f6_"+p.toString().replace("-","");
        sql.execute("create function "+trigger+"() returns trigger language plpgsql as $$ begin if new.pedido_id='"+p+"'::uuid then raise exception 'Fallo ficticio F6'; end if; return new; end $$");sql.execute("create trigger "+trigger+" before insert on "+tabla+" for each row execute function "+trigger+"()");
        try{try{accion(p,11,"APROBAR",adj(fin),500);}catch(jakarta.servlet.ServletException e){assertThat(e.getCause()).isInstanceOf(RuntimeException.class);}assertThat(estado(p)).isEqualTo(antes);}finally{sql.execute("drop trigger "+trigger+" on "+tabla);sql.execute("drop function "+trigger+"()");}
        accion(p,11,"APROBAR",adj(fin),200);
    }
    @Test void regresionCompletaDeAYFinA11NoSeHabilitaPorExistirB11()throws Exception{
        UUID b=crear("B",china,201),a=crear("A",admin,201);var antesB=estado(b);
        enviar(a,1,"Solicitud");enviar(a,2,"Documentos codificados");accion(a,3,"APROBAR","",revision,200);accion(a,4,"APROBAR","",china,200);
        accion(a,5,"APROBAR",",\"observacion\":\"Fabricacion A ficticia\"",china,200);accion(a,6,"APROBAR",adj(archivo(a,6,"Packing List")),china,200);
        accion(a,7,"APROBAR","",china,200);accion(a,8,"APROBAR","",logistica,200);UUID almacen=bodega(true);
        accion(a,9,"APROBAR",adj(archivo(a,9,"Documento A"))+",\"bodegaId\":\""+almacen+"\"",logistica,200);
        accion(a,10,"APROBAR",adj(archivo(a,10,"Incidencia A")),logistica,200);
        assertThat(actual(a)).isEqualTo(11);assertThat(sql.queryForObject("select finalizado from pedido where id=?",Boolean.class,a)).isTrue();assertThat(estado(b)).isEqualTo(antesB);
        var antes=estado(a);for(String accion:List.of("ENVIAR","APROBAR","RECHAZAR","DEVOLVER_POR_ERROR","DEVOLVER_POR_PRODUCTO_NUEVO","REPORTAR_DEMORA"))accion(a,11,accion,"",409);
        assertThat((List<?>)JsonPath.read(http(get(base(a)),admin,"",200),"$.accionesDisponibles")).isEmpty();
        archivo(a,11,"No admitido",admin,409);http(post(base(a)+"/observaciones"),admin,"{\"texto\":\"No admitida\",\"etapaEsperada\":11,\"entradaEsperadaId\":\""+entrada(a,11)+"\"}",409);
        http(put(base(a)+"/etapas/11/motivo-retraso"),admin,"{\"motivo\":\"No admitido\",\"entradaEsperadaId\":\""+entrada(a,11)+"\"}",409);assertThat(estado(a)).isEqualTo(antes);
    }
}
