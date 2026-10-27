package com.decokasa.seguimiento.notificaciones.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

/** Recorridos HTTP TCP completos: login, creación, subida, acciones y asignación reales.
 * Solo se siembran las identidades iniciales de la base exclusiva. Ningún pedido, etapa,
 * adjunto, aviso ni evento se inserta como fixture. Google permanece sustituido por dobles.
 * La superclase aporta infraestructura; el comando selecciona únicamente real*.
 */
@TestPropertySource(properties={"decokasa.cors.origenes=http://127.0.0.1:5187", "decokasa.bcrypt.fuerza=4"})
class AvisosRecorridosHttpIT extends AvisosEscenariosIT {
    static final String CLAVE="Avisos-Ficticios-2026";
    static final String HASH=new BCryptPasswordEncoder(4).encode(CLAVE);
    final Map<UUID,String> sesiones=new ConcurrentHashMap<>();
    Instant inicio;
    @Override UUID persona(String nombre,String rol,String pais,boolean activo){
        UUID id=super.persona(nombre,rol,pais,activo);
        sql.update("update usuario set contrasena_hash=? where id=?",HASH,id);return id;
    }
    @BeforeEach void tiempoDeSesion(){ahora=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);inicio=ahora;sesiones.clear();}
    @Override String token(UUID id){return sesiones.computeIfAbsent(id,u->{
        try{
            var q=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+puerto+"/api/v1/auth/login"))
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(
                    json.writeValueAsString(Map.of("identificador",u.toString(),"contrasena",CLAVE)))).build();
            var r=http.send(q,HttpResponse.BodyHandlers.ofString());
            assertThat(r.statusCode()).as("Login real: "+r.body()).isEqualTo(200);
            return json.readTree(r.body()).get("tokenAcceso").asString();
        }catch(Exception e){throw new IllegalStateException(e);}
    });}
    JsonNode llamada(UUID actor,String metodo,String ruta,Object datos,int esperado)throws Exception{
        var r=peticion(actor,metodo,ruta,datos==null?null:json.writeValueAsString(datos));
        assertThat(r.statusCode()).as(metodo+" "+ruta+" -> "+r.body()).isEqualTo(esperado);
        return r.body().isBlank()?json.createObjectNode():json.readTree(r.body());
    }
    UUID crearReal(String flujo)throws Exception{
        ahora=ahora.plusSeconds(1);
        var r=llamada(flujo.equals("A")?admin:compras,"POST","/pedidos",Map.of("pais","EC","tipoFlujo",flujo,"nombre","Recorrido HTTP "+UUID.randomUUID()),201);
        UUID p=UUID.fromString(r.get("id").asString());
        System.out.printf("REAL pedido creado HTTP=201 flujo=%s pedido=%s%n",flujo,p);return p;
    }
    UUID ultimo(UUID p){return sql.queryForObject("select id from evento_pedido where pedido_id=? and tipo in ('PEDIDO_CREADO','ETAPA_ENVIADA','ETAPA_APROBADA','ETAPA_RECHAZADA','PEDIDO_DEVUELTO','ETAPA_REASIGNADA') order by ocurrido_en desc,secuencia desc limit 1",UUID.class,p);}
    void verificar(String caso,UUID p,UUID evento,UUID...esperados)throws Exception{
        var ids=Set.of(esperados);
        var obtenidos=sql.queryForList("select usuario_id from aviso_etapa where evento_id=?",UUID.class,evento);
        var emails=sql.queryForList("select destinatario from notificacion where evento_id=?",String.class,evento);
        System.out.printf("REAL escenario=%s pedido=%s evento=%s esperados=%s obtenidos=%s avisos=%d correos=%d%n",caso,p,evento,ids.stream().map(nombres::get).sorted().toList(),obtenidos.stream().map(nombres::get).sorted().toList(),obtenidos.size(),emails.size());
        assertThat(obtenidos).containsExactlyInAnyOrderElementsOf(ids);
        assertThat(emails).containsExactlyInAnyOrderElementsOf(ids.stream().map(u->u+"@ejemplo.test").toList());
        for(UUID u:ids)assertThat(get(u).get("avisos").toString()).contains(evento.toString());
        verifyNoInteractions(gmail,drive);
    }
    UUID subirReal(UUID p,int etapa,String tipo,UUID actor)throws Exception{
        UUID entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=?",UUID.class,p,etapa);
        String b="decokasa-"+UUID.randomUUID();
        String cuerpo="--"+b+"\r\nContent-Disposition: form-data; name=\"tipoDocumento\"\r\n\r\n"+tipo
            +"\r\n--"+b+"\r\nContent-Disposition: form-data; name=\"entradaEsperadaId\"\r\n\r\n"+entrada
            +"\r\n--"+b+"\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"documento-prueba.csv\"\r\nContent-Type: text/csv\r\n\r\ncodigo,producto\n1,Prueba de integración\n\r\n--"+b+"--\r\n";
        var q=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+puerto+"/api/v1/pedidos/"+p+"/etapas/"+etapa+"/archivos"))
            .header("Authorization","Bearer "+token(actor)).header("Content-Type","multipart/form-data; boundary="+b)
            .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo.getBytes(StandardCharsets.UTF_8))).build();
        var r=http.send(q,HttpResponse.BodyHandlers.ofString());assertThat(r.statusCode()).as("Subida real: "+r.body()).isEqualTo(201);
        return UUID.fromString(json.readTree(r.body()).get("id").asString());
    }
    UUID accionReal(UUID p,int etapa,String accion,UUID actor,Map<String,Object> extra)throws Exception{
        Map<String,Object> cuerpo=new LinkedHashMap<>(extra);cuerpo.put("accion",accion);cuerpo.put("etapaEsperada",etapa);
        ahora=ahora.plusSeconds(1);llamada(actor,"POST","/pedidos/"+p+"/acciones",cuerpo,200);return ultimo(p);
    }
    UUID bodegaReal()throws Exception{return UUID.fromString(llamada(admin,"POST","/bodegas",Map.of("nombre","Bodega HTTP "+UUID.randomUUID(),"ciudad","Ciudad de prueba"),201).get("id").asString());}
    UUID asignarReal(UUID p,int etapa,UUID asignado)throws Exception{
        ahora=ahora.plusSeconds(1);llamada(admin,"PUT","/pedidos/"+p+"/etapas/"+etapa+"/asignacion",Map.of("usuarioId",asignado,"motivo","Reasignación de prueba por HTTP"),200);return ultimo(p);
    }
    UUID pasoReal(UUID p,String flujo,int etapa)throws Exception{
        UUID actor;String accion="APROBAR";Map<String,Object> extra=new LinkedHashMap<>();
        if(flujo.equals("A")){
            actor=etapa==1?admin:etapa==2?marketing:etapa==3?revision:etapa<=7?compras:log1;
            if(etapa==1||etapa==2){accion="ENVIAR";extra.put("adjuntoIds",List.of(subirReal(p,etapa,etapa==1?"Solicitud":"Documentos codificados",actor)));}
            if(etapa==5)extra.put("observacion","Fabricación terminada por HTTP");
            if(etapa==6)extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Packing List",actor)));
            if(etapa==9){extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Documento de bodega",actor)));extra.put("bodegaId",bodegaReal());}
            if(etapa==10)extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Incidencias",actor)));
        }else{
            actor=etapa==1?compras:etapa==2?revision:etapa==3?marketing:etapa==4?revision:etapa<=8?compras:log1;
            if(etapa==1||etapa==3){accion="ENVIAR";extra.put("adjuntoIds",List.of(subirReal(p,etapa,etapa==1?"Solicitud":"Documentos codificados",actor)));}
            if(etapa==6)extra.put("observacion","Fabricación B terminada por HTTP");
            if(etapa==7)extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Packing List",actor)));
            if(etapa==10){extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Documento de bodega",actor)));extra.put("bodegaId",bodegaReal());}
            if(etapa==11)extra.put("adjuntoIds",List.of(subirReal(p,etapa,"Incidencias",actor)));
        }
        return accionReal(p,etapa,accion,actor,extra);
    }
    UUID hastaReal(String flujo,int etapa)throws Exception{UUID p=crearReal(flujo);for(int n=1;n<etapa;n++)pasoReal(p,flujo,n);return p;}
    void editarReal(UUID u,Map<String,Object> cambios)throws Exception{
        var actual=llamada(admin,"GET","/usuarios/"+u,null,200);var c=new LinkedHashMap<>(cambios);c.put("version",actual.get("version").asLong());llamada(admin,"PATCH","/usuarios/"+u,c,200);
    }
    @Test void real01CrearNoAnticipaEtapa()throws Exception{UUID p=crearReal("A");verificar("01",p,ultimo(p),admin);assertThat(get(marketing).get("avisos").isEmpty()).isTrue();}
    @ParameterizedTest @CsvSource({"02,1","03,2","04,3","06,7","07,8"})
    void realAvancesAConResponsablesVigentes(String caso,int origen)throws Exception{
        UUID p=hastaReal("A",origen),evento=pasoReal(p,"A",origen);
        UUID[] esperados=switch(origen){case 1->new UUID[]{admin,marketing};case 2->new UUID[]{admin,revision};case 3->new UUID[]{admin,compras};case 7->new UUID[]{admin,log1,log2};default->new UUID[]{admin,log2};};
        verificar(caso,p,evento,esperados);
        if(origen==1)assertThat(sql.queryForObject("select count(distinct evento_id) from aviso_etapa where pedido_id=?",Integer.class,p)).isEqualTo(2);
    }
    @Test void real05MismaPersonaEnTramoChinaYSoloAsignacionActual()throws Exception{
        UUID p=hastaReal("A",4);asignarReal(p,4,compras);
        llamada(admin,"PUT","/pedidos/"+p+"/etapas/5/asignacion",Map.of("usuarioId",compras),409);
        verificar("05-misma-persona",p,pasoReal(p,"A",4),admin);
        assertThat(sql.queryForObject("select asignado_a from etapa_pedido where pedido_id=? and numero=5",UUID.class,p)).isNull();
        System.out.println("REAL escenario=05 variante destino individual BLOQUEADA: API solo asigna etapa actual; A5 inactiva devuelve 409");
    }
    @Test void real08AsignacionNoSeHeredaAA10()throws Exception{
        UUID p=hastaReal("A",9);asignarReal(p,9,log1);
        llamada(admin,"PUT","/pedidos/"+p+"/etapas/10/asignacion",Map.of("usuarioId",log1),409);
        verificar("08-A10-sin-asignacion",p,pasoReal(p,"A",9),admin,log2);
        assertThat(sql.queryForObject("select asignado_a from etapa_pedido where pedido_id=? and numero=10",UUID.class,p)).isNull();
        verificar("08-asignar-A10-activa",p,asignarReal(p,10,log2),admin,log2);
        System.out.println("REAL escenario=08 llegada con destino ya asignado BLOQUEADA por contrato de asignación actual; no se fabrica estado futuro");
    }
    @Test void real09RechazoRespetaAsignadoIndividual()throws Exception{
        UUID m2=persona("Marketing2","marketing","EC",true),p=hastaReal("A",2);asignarReal(p,2,marketing);pasoReal(p,"A",2);
        UUID e=accionReal(p,3,"RECHAZAR",revision,Map.of("observacion","Corregir código por HTTP"));verificar("09",p,e,admin,marketing);
        assertThat(get(m2).get("avisos").toString()).doesNotContain(e.toString());
        assertThat(get(marketing).get("avisos").toString()).contains("Corregir código por HTTP",p.toString());llamada(marketing,"GET","/pedidos/"+p,null,200);
        List<Notificador.Correo> entregas=new ArrayList<>();
        when(gmail.enviar(any())).thenAnswer(i->{entregas.add(i.getArgument(0));return new Notificador.Entrega("doble-enlace");});
        worker(inicio.toString(),directorio()).procesar();
        var ids=sql.queryForList("select id from notificacion where evento_id=?",UUID.class,e);
        var devoluciones=entregas.stream().filter(c->ids.contains(c.notificacionId())).toList();
        assertThat(devoluciones).hasSize(2).allSatisfy(c->assertThat(c.texto()).contains("Corregir código por HTTP","/pedidos/"+p));
    }
    @Test void real10DevolucionProductoNuevoConUrgencia()throws Exception{
        UUID p=hastaReal("A",4),e=accionReal(p,4,"DEVOLVER_POR_PRODUCTO_NUEVO",compras,Map.of("observacion","Nuevo producto por HTTP","nivelUrgencia","URGENTE"));
        verificar("10",p,e,admin,marketing);
        assertThat(sql.queryForObject("select urgencia_nivel from pedido where id=?",String.class,p)).isEqualTo("URGENTE");
        assertThat(sql.queryForList("select datos->>'nivelUrgencia' from notificacion where evento_id=?",String.class,e)).containsOnly("URGENTE");
    }
    @Test void real11FinAutomaticoSinDuplicar()throws Exception{
        UUID p=hastaReal("A",10),e=pasoReal(p,"A",10);verificar("11",p,e,admin);
        assertThat(sql.queryForObject("select finalizado from pedido where id=?",Boolean.class,p)).isTrue();
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='PEDIDO_FINALIZADO'",Integer.class,p)).isEqualTo(1);
        assertThat(sql.queryForObject("select count(*) from aviso_etapa where pedido_id=? and tipo='CIERRE'",Integer.class,p)).isEqualTo(1);
    }
    @ParameterizedTest @CsvSource({"1,2","2,1","4,3","8,9"})
    void real12FlujoBAvancesYRechazos(int origen,int destino)throws Exception{
        UUID p=hastaReal("B",origen),e=destino<origen?accionReal(p,origen,"RECHAZAR",revision,Map.of("observacion","Rechazo B por HTTP")):pasoReal(p,"B",origen);
        UUID[] ids=destino==2?new UUID[]{admin,revision}:destino==1?new UUID[]{admin,compras}:destino==3?new UUID[]{admin,marketing}:new UUID[]{admin,log1,log2};
        verificar("12-B"+origen+"-B"+destino,p,e,ids);
    }
    @Test void real13ReasignarMismoRolPorEndpoint()throws Exception{
        UUID p=hastaReal("A",9);asignarReal(p,9,log1);UUID e=asignarReal(p,9,log2);verificar("13",p,e,admin,log2);
        int antes=sql.queryForObject("select count(*) from aviso_etapa where pedido_id=?",Integer.class,p);asignarReal(p,9,log2);
        assertThat(sql.queryForObject("select count(*) from aviso_etapa where pedido_id=?",Integer.class,p)).isEqualTo(antes);
    }
    @Test void real14InactivosPaisPropiedadYLecturas()throws Exception{
        persona("Inactivo","marketing","EC",false);persona("SinPais","marketing",null,true);persona("OtroPais","marketing","CO",true);persona("OtroRol","consulta","EC",true);
        UUID p=hastaReal("A",1),e=pasoReal(p,"A",1);verificar("14-filtros",p,e,admin,marketing);
        assertThat(llamada(log1,"GET","/avisos?usuarioId="+marketing,null,200).toString()).doesNotContain(e.toString());
        llamada(log1,"POST","/avisos/leidos?usuarioId="+marketing,null,200);assertThat(get(marketing).get("noLeidos").asInt()).isEqualTo(1);
        int adminAntes=get(admin).get("noLeidos").asInt();llamada(marketing,"POST","/avisos/leidos",null,200);
        assertThat(get(marketing).get("noLeidos").asInt()).isZero();assertThat(get(admin).get("noLeidos").asInt()).isEqualTo(adminAntes);
        editarReal(marketing,Map.of("paises",List.of("CO")));assertThat(get(marketing).get("avisos").isEmpty()).isTrue();
        editarReal(marketing,Map.of("activo",false));llamada(marketing,"GET","/avisos",null,403);llamada(marketing,"POST","/avisos/leidos",null,403);
    }
    @Test void real14RevocarRolPorPatchDebeRetirarAcceso()throws Exception{
        UUID p=hastaReal("A",1),e=pasoReal(p,"A",1);get(marketing);editarReal(marketing,Map.of("roles",List.of("consulta")));
        assertThat(get(marketing).get("avisos").toString()).as("Rol revocado mediante PATCH real").doesNotContain(e.toString());
    }
    @Test void real14AsignadoPierdeRolAntesDeReentrada()throws Exception{
        UUID p=hastaReal("A",2);asignarReal(p,2,marketing);pasoReal(p,"A",2);editarReal(marketing,Map.of("roles",List.of("consulta")));
        UUID e=accionReal(p,3,"RECHAZAR",revision,Map.of("observacion","Reentrada a asignado sin rol vigente"));verificar("14-asignado-sin-rol",p,e,admin);
    }
    @Test void real15MultirrolYDosTransicionesConcurrentes()throws Exception{
        editarReal(admin,Map.of("roles",List.of("admin","logistica")));UUID tramo=hastaReal("A",7);verificar("15-admin-multirrol",tramo,pasoReal(tramo,"A",7),admin,log1,log2);
        UUID p=crearReal("A"),doc=subirReal(p,1,"Solicitud",admin);String cuerpo=json.writeValueAsString(Map.of("accion","ENVIAR","etapaEsperada",1,"adjuntoIds",List.of(doc)));
        var barrera=new CyclicBarrier(2);List<Integer> respuestas;
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Integer> pedir=()->{barrera.await();return peticion(admin,"POST","/pedidos/"+p+"/acciones",cuerpo).statusCode();};
            var a=pool.submit(pedir);var b=pool.submit(pedir);respuestas=List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS));
        }
        assertThat(respuestas).containsExactlyInAnyOrder(200,409);llamada(admin,"POST","/pedidos/"+p+"/acciones",json.readTree(cuerpo),409);
        UUID e=ultimo(p);verificar("15-HTTP-concurrente",p,e,admin,marketing);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ETAPA_ENVIADA'",Integer.class,p)).isEqualTo(1);
    }
    @Test void real15RollbackDelEndpointNoDejaAvisoCorreoNiAvance()throws Exception{
        UUID p=crearReal("A"),doc=subirReal(p,1,"Solicitud",admin);String funcion="fallo_http_"+p.toString().replace("-","");
        int eventosAntes=sql.queryForObject("select count(*) from evento_pedido where pedido_id=?",Integer.class,p),avisosAntes=sql.queryForObject("select count(*) from aviso_etapa where pedido_id=?",Integer.class,p),correosAntes=sql.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p);
        sql.execute("create function "+funcion+"() returns trigger language plpgsql as $$ begin if new.pedido_id='"+p+"'::uuid then raise exception 'Fallo de prueba en outbox'; end if; return new; end $$");
        sql.execute("create trigger "+funcion+" before insert on notificacion for each row execute function "+funcion+"()");
        var cuerpo=Map.of("accion","ENVIAR","etapaEsperada",1,"adjuntoIds",List.of(doc));
        try{llamada(admin,"POST","/pedidos/"+p+"/acciones",cuerpo,500);}finally{sql.execute("drop trigger "+funcion+" on notificacion");sql.execute("drop function "+funcion+"()");}
        assertThat(sql.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p)).isEqualTo(1);
        assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=?",Integer.class,p)).isEqualTo(eventosAntes);
        assertThat(sql.queryForObject("select count(*) from aviso_etapa where pedido_id=?",Integer.class,p)).isEqualTo(avisosAntes);
        assertThat(sql.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p)).isEqualTo(correosAntes);
        llamada(admin,"POST","/pedidos/"+p+"/acciones",cuerpo,200);verificar("15-tras-rollback",p,ultimo(p),admin,marketing);
    }
    @Test void real16CorteConPedidosCreadosPorHttp()throws Exception{
        UUID viejo=crearReal("A");Instant corte=ahora.plusSeconds(1);ahora=ahora.plusSeconds(2);UUID nuevo=crearReal("A");
        assertThat(worker("",directorio()).procesar()).isZero();verifyNoInteractions(gmail);
        when(gmail.enviar(any())).thenReturn(new Notificador.Entrega("doble-http"));assertThat(worker(corte.toString(),directorio()).procesar()).isEqualTo(1);
        assertThat(sql.queryForObject("select estado from notificacion where pedido_id=?",String.class,viejo)).isEqualTo("PENDIENTE");
        assertThat(sql.queryForObject("select estado from notificacion where pedido_id=?",String.class,nuevo)).isEqualTo("ENVIADA");verify(gmail,times(1)).enviar(any());
    }
    @Test void real16RevocarRolPorHttpAntesDeEnviar()throws Exception{
        UUID p=hastaReal("A",1);pasoReal(p,"A",1);editarReal(marketing,Map.of("roles",List.of("consulta")));
        when(gmail.enviar(any())).thenReturn(new Notificador.Entrega("doble-http"));worker(inicio.toString(),directorio()).procesar();
        verify(gmail,never()).enviar(argThat(c->c.destinatario().equals(marketing+"@ejemplo.test")));
    }
    @Test void real16RevocarPaisYCuentaPorHttpAntesDeEnviar()throws Exception{
        UUID p=hastaReal("A",1);pasoReal(p,"A",1);
        editarReal(marketing,Map.of("activo",false));editarReal(admin,Map.of("paises",List.of("CO")));
        assertThat(worker(inicio.toString(),directorio()).procesar()).isEqualTo(3);verifyNoInteractions(gmail);
        assertThat(sql.queryForList("select ultimo_error from notificacion where pedido_id=?",String.class,p)).containsOnly("CONFIGURACION");
    }
    @Test void real16IncertidumbreSinSegundoEnvio()throws Exception{
        UUID p=crearReal("A");when(gmail.enviar(any())).thenThrow(new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.RESULTADO_INCIERTO));
        assertThat(worker(inicio.toString(),directorio()).procesar()).isEqualTo(1);ahora=ahora.plusSeconds(86400);
        assertThat(worker(inicio.toString(),directorio()).procesar()).isZero();verify(gmail,times(1)).enviar(any());
        assertThat(sql.queryForObject("select resultado_incierto from notificacion where pedido_id=?",Boolean.class,p)).isTrue();
    }
    @Test void real16FiltroPruebaNoCambiaDestinatariosInternos()throws Exception{
        UUID p=hastaReal("A",1),e=pasoReal(p,"A",1);worker(inicio.toString(),NotificacionesConfig.desde(NotificacionesConfig.DESTINATARIO_PRUEBA_AUTORIZADO)).procesar();verificar("16-invariante-interna",p,e,admin,marketing);
    }
    @Test @Tag("navegador-avisos") void real99ServirParaChrome()throws Exception{
        UUID p=hastaReal("A",2);hastaReal("B",2);Path fin=Path.of("fin-navegador");Files.deleteIfExists(fin);
        Files.writeString(Path.of("navegador.json"),json.writeValueAsString(Map.of("puerto",puerto,"admin",admin.toString(),"marketing",marketing.toString(),"clave",CLAVE,"pedido",p.toString())));
        System.out.printf("REAL NAVEGADOR listo puerto=%d; pedidos creados y documentos subidos por HTTP%n",puerto);
        Instant limite=Instant.now().plusSeconds(600);while(!Files.exists(fin)&&Instant.now().isBefore(limite))Thread.sleep(250);
        assertThat(Files.exists(fin)).isTrue();verifyNoInteractions(gmail,drive);
    }
}
