package com.decokasa.seguimiento.notificaciones.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.eventos.*;
import com.decokasa.seguimiento.notificaciones.adapter.out.persistence.NotificacionSqlRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.out.*;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** PostgreSQL Testcontainers exclusivo; eventos simulados, oyente/SQL/API HTTP reales; Google siempre doble.
 * No afirma recorrer el motor de transiciones para los eventos sembrados. Creación sí usa POST real.
 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.config.import=", "decokasa.jwt.secreto=clave-ficticia-avisos-20261027-exclusiva-32-bytes",
    "decokasa.google.oauth.habilitada=false", "decokasa.google.drive.habilitada=false",
    "decokasa.google.gmail.habilitada=false", "decokasa.google.drive.worker.automatico=false",
    "decokasa.google.gmail.worker.automatico=false", "decokasa.plazos.vencimientos.automatico=false",
    "decokasa.ia.habilitada=false", "decokasa.ia.borrado.automatico=false", "decokasa.semilla.habilitada=false"})
@Import(TestcontainersConfiguration.class)
class AvisosEscenariosIT {
    static final String SECRETO="clave-ficticia-avisos-20261027-exclusiva-32-bytes";
    static final Instant T=Instant.parse("2026-10-27T15:00:00Z");
    static final Path DISCO=disco();
    static Path disco(){try{return Files.createTempDirectory("decokasa-avisos-ficticios-");}catch(Exception e){throw new RuntimeException(e);}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",()->DISCO.toString());}
    @LocalServerPort int puerto;
    @Autowired JdbcTemplate sql;
    @Autowired PlatformTransactionManager gestor;
    @Autowired ApplicationEventPublisher eventos;
    @Autowired EntityManager em;
    @Autowired NotificacionSqlRepositorio correos;
    @Autowired ConsultarContactos contactos;
    @MockitoBean Reloj reloj;
    @MockitoBean AlmacenArchivos drive;
    @MockitoBean Notificador gmail;
    final JsonMapper json=JsonMapper.builder().build();
    final HttpClient http=HttpClient.newHttpClient();
    final Map<UUID,String> nombres=new HashMap<>();
    TransactionTemplate tx;
    UUID admin,marketing,revision,compras,log1,log2;
    Instant ahora;
    @BeforeEach void preparar(){
        tx=new TransactionTemplate(gestor); ahora=T; when(reloj.ahora()).thenAnswer(i->ahora);
        when(reloj.zona()).thenReturn(Reloj.ZONA_ECUADOR);
        when(reloj.ahoraEnEcuador()).thenAnswer(i->ahora.atZone(ZoneId.of("America/Guayaquil")));
        // Esta base pertenece solo a esta clase; desactiva cohortes ficticias de casos anteriores.
        sql.update("update usuario set activo=false");
        sql.update("update notificacion set estado='FALLIDA' where estado='PENDIENTE'");
        admin=persona("Admin","admin","EC",true);marketing=persona("Marketing","marketing","EC",true);
        revision=persona("Revision","asistente_compras","EC",true);compras=persona("China","compras_china","EC",true);
        log1=persona("Log1","logistica","EC",true);log2=persona("Log2","logistica","EC",true);
    }
    UUID persona(String nombre,String rol,String pais,boolean activa){
        UUID id=UUID.randomUUID();nombres.put(id,nombre);
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version,activo) values (?,?,?,?,?,'ficticio',?,?,0,?)",
            id,nombre,id+"@ejemplo.test",id.toString(),"+"+id.toString().replaceAll("[^0-9]", "").substring(0,10),Timestamp.from(T.minusSeconds(86400)),Timestamp.from(T),activa);
        rol(id,rol);if(pais!=null)sql.update("insert into usuario_pais(usuario_id,pais_codigo) values (?,?)",id,pais);return id;
    }
    void rol(UUID id,String rol){sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,? from rol where codigo=? on conflict do nothing",id,Timestamp.from(T),rol);}
    String token(UUID id){
        var encoder=new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8),"HmacSHA256")));
        var claims=JwtClaimsSet.builder().issuer("decokasa").subject(id.toString()).issuedAt(Instant.now().minusSeconds(1))
            .expiresAt(Instant.now().plusSeconds(3600)).claim("roles",List.of("admin","marketing","logistica","compras_china","asistente_compras")).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
    HttpResponse<String> peticion(UUID id,String metodo,String ruta,String cuerpo)throws Exception{
        var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+puerto+"/api/v1"+ruta))
            .header("Authorization","Bearer "+token(id)).header("Content-Type","application/json")
            .timeout(Duration.ofSeconds(20));
        return http.send(b.method(metodo,cuerpo==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(cuerpo)).build(),HttpResponse.BodyHandlers.ofString());
    }
    JsonNode get(UUID id)throws Exception{
        var r=peticion(id,"GET","/avisos",null);assertThat(r.statusCode()).as(r.body()).isEqualTo(200);
        assertThat(r.headers().firstValue("Cache-Control")).contains("no-store");return json.readTree(r.body());
    }
    DatosDelPedido pedido(String flujo,int etapa,UUID asignado){
        UUID p=UUID.randomUUID();String numero="SIM-AVISO-"+p.toString().substring(0,24);
        UUID f=sql.queryForObject("select id from definicion_flujo where tipo_flujo=? and pais_codigo='EC' and vigente",UUID.class,flujo);
        sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values (?,?,'EC',?,?,'Aviso ficticio',?,?,?, ?,0)",p,numero,flujo,f,admin,Timestamp.from(T),etapa,Timestamp.from(T));
        UUID entrada=UUID.randomUUID();
        sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,?,?)",entrada,p,etapa,Timestamp.from(T));
        sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id,reentradas,asignado_a) values (?,?,?,'ACTUAL',?,?,0,?)",UUID.randomUUID(),p,etapa,Timestamp.from(T),entrada,asignado);
        return new DatosDelPedido(p,numero,CodigoPais.EC,TipoFlujo.valueOf(flujo));
    }
    Interviniente actor(UUID id){return new Interviniente(id,new CodigoRol(id.equals(admin)?"admin":id.equals(marketing)?"marketing":id.equals(revision)?"asistente_compras":id.equals(compras)?"compras_china":"logistica"));}
    EventoPedidoRegistrado registrar(EventoDePedido e){
        var r=new EventoPedidoRegistrado(UUID.randomUUID(),e);
        tx.executeWithoutResult(s->{historial(r);eventos.publishEvent(r);});return r;
    }
    void historial(EventoPedidoRegistrado r){var e=r.evento();
        String tipo=switch(e){case PedidoFinalizado f->"PEDIDO_FINALIZADO";case EtapaAprobada a->"ETAPA_APROBADA";case EtapaRechazada a->"ETAPA_RECHAZADA";case PedidoDevuelto d->"PEDIDO_DEVUELTO";case EtapaReasignada a->"ETAPA_REASIGNADA";default->"ETAPA_ENVIADA";};
        sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values (?,?,?,?,?,?,?,'{}')",r.eventoId(),e.pedidoId(),e.etapa(),tipo,e.actor()==null?null:e.actor().usuarioId(),e.actor()==null?null:e.actor().rol().valor(),Timestamp.from(e.ocurridoEn()));
    }
    List<String> obtenidos(UUID evento){return sql.queryForList("select usuario_id from aviso_etapa where evento_id=?",UUID.class,evento).stream().map(nombres::get).sorted().toList();}
    void comprobar(String caso,EventoPedidoRegistrado r,UUID...esperados)throws Exception{
        var ids=Set.of(esperados);var avisos=sql.queryForList("select usuario_id from aviso_etapa where evento_id=?",UUID.class,r.eventoId());
        var emails=sql.queryForList("select destinatario from notificacion where evento_id=?",String.class,r.eventoId());
        System.out.printf("ESCENARIO %s evento=%s esperados=%s obtenidos=%s avisos=%d correos=%d%n",caso,r.eventoId(),ids.stream().map(nombres::get).sorted().toList(),obtenidos(r.eventoId()),avisos.size(),emails.size());
        assertThat(avisos).containsExactlyInAnyOrderElementsOf(ids);
        assertThat(emails).containsExactlyInAnyOrderElementsOf(ids.stream().map(i->i+"@ejemplo.test").toList());
        assertThat(sql.queryForList("select plantilla from notificacion where evento_id=?",String.class,r.eventoId())).allMatch("AVISO_ETAPA"::equals);
        for(UUID id:ids){var respuesta=get(id);assertThat(respuesta.get("avisos").toString()).contains(r.eventoId().toString());}
        verifyNoInteractions(gmail,drive);
    }
    EventoPedidoRegistrado avance(String flujo,int origen,int destino,UUID actor,UUID asignado){
        var p=pedido(flujo,destino,asignado);return registrar(new EtapaEnviada(p,origen,actor(actor),ahora,destino,"Avance ficticio",false));
    }
    @Test void e01CreacionRealNoAnticipaMarketing()throws Exception{
        var r=peticion(admin,"POST","/pedidos","{\"nombre\":\"Simulado creación\",\"tipoFlujo\":\"A\",\"pais\":\"EC\"}");
        assertThat(r.statusCode()).as(r.body()).isEqualTo(201);UUID p=UUID.fromString(json.readTree(r.body()).get("id").asText());
        var id=sql.queryForObject("select evento_id from aviso_etapa where pedido_id=?",UUID.class,p);
        assertThat(obtenidos(id)).containsExactly("Admin");assertThat(get(marketing).get("avisos").isEmpty()).isTrue();
        assertThat(sql.queryForObject("select count(*) from notificacion where evento_id=?",Integer.class,id)).isEqualTo(1);
        System.out.printf("ESCENARIO 01 creación HTTP=201 avisos=1 correos=1 destinatarios=Admin pedido=%s%n",p);
        verifyNoInteractions(gmail,drive);
    }
    @Test void e02A1A2()throws Exception{comprobar("02",avance("A",1,2,admin,null),admin,marketing);}
    @Test void e02HttpCreacionYEnvioSonOperacionesDistintas()throws Exception{
        var creado=peticion(admin,"POST","/pedidos","{\"nombre\":\"HTTP ficticio creación y envío\",\"tipoFlujo\":\"A\",\"pais\":\"EC\"}");
        assertThat(creado.statusCode()).as(creado.body()).isEqualTo(201);
        UUID p=UUID.fromString(json.readTree(creado.body()).get("id").asText());
        UUID entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=1",UUID.class,p),adjunto=UUID.randomUUID();
        // Documento de prueba en disco exclusivo; se siembra su metadato, no se ejercita aquí el detector de formatos.
        byte[] contenido="Documento ficticio exclusivo de avisos".getBytes(StandardCharsets.UTF_8);
        Files.write(DISCO.resolve(adjunto.toString()),contenido);
        String sha=HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(contenido));
        sql.update("insert into adjunto(id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,clave_local,estado_copia,subido_por,subido_en) values (?,?,1,?,'Solicitud','solicitud-ficticia.csv',?,?,?,'PENDIENTE',?,?)",adjunto,p,entrada,contenido.length,sha,adjunto.toString(),admin,Timestamp.from(T));
        ahora=T.plusSeconds(1);
        var enviado=peticion(admin,"POST","/pedidos/"+p+"/acciones","{\"accion\":\"ENVIAR\",\"etapaEsperada\":1,\"adjuntoIds\":[\""+adjunto+"\"]}");
        assertThat(enviado.statusCode()).as(enviado.body()).isEqualTo(200);
        assertThat(json.readTree(enviado.body()).get("etapaActual").asInt()).isEqualTo(2);
        var tipos=sql.queryForList("select tipo from aviso_etapa where pedido_id=? and usuario_id=? order by ocurrido_en",String.class,p,admin);
        assertThat(tipos).containsExactly("CREACION","LLEGADA");
        assertThat(sql.queryForList("select tipo from aviso_etapa where pedido_id=? and usuario_id=?",String.class,p,marketing)).containsExactly("LLEGADA");
        assertThat(sql.queryForObject("select count(distinct evento_id) from aviso_etapa where pedido_id=?",Integer.class,p)).isEqualTo(2);
        assertThat(sql.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p)).isEqualTo(3);
        verifyNoInteractions(gmail,drive);
        System.out.printf("ESCENARIO 02-HTTP POST creación=201 ENVIAR=200 eventos=2 avisos=3 correos=3 pedido=%s%n",p);
    }
    @Test void e03A2A3()throws Exception{comprobar("03",avance("A",2,3,marketing,null),admin,revision);}
    @Test void e04A3A4()throws Exception{comprobar("04",avance("A",3,4,revision,null),admin,compras);}
    @Test void e05MismoAsignado()throws Exception{comprobar("05",avance("A",4,5,compras,compras),admin);}
    @Test void e06A7A8()throws Exception{comprobar("06",avance("A",7,8,compras,null),admin,log1,log2);}
    @Test void e07MismoRolOtraPersona()throws Exception{comprobar("07",avance("A",8,9,log1,null),admin,log2);}
    @Test void e08AsignacionYOtrosExcluidos()throws Exception{
        comprobar("08-nuevo",avance("A",9,10,log1,log2),admin,log2);
        comprobar("08-mismo",avance("A",9,10,log1,log1),admin);
    }
    @Test void e09RechazoConMotivo()throws Exception{
        var r=registrar(new EtapaRechazada(pedido("A",2,null),3,actor(revision),T,2,"Corregir código ficticio",false));
        comprobar("09",r,admin,marketing);
        assertThat(get(marketing).get("avisos").get(0).get("motivo").asText()).isEqualTo("Corregir código ficticio");
    }
    @Test void e10ProductoNuevoDebeConservarUrgencia()throws Exception{
        var r=registrar(new PedidoDevuelto(pedido("A",2,null),4,actor(compras),T,2,MotivoDevolucion.PRODUCTO_NUEVO,"Nuevo producto ficticio",NivelUrgencia.URGENTE));
        comprobar("10",r,admin,marketing);
        assertAll(()->assertThat(get(marketing).get("avisos").get(0).get("motivo").asText()).isEqualTo("Nuevo producto ficticio"),
            ()->assertThat(tx.execute(s->correos.listar(null,0,100)).stream().filter(n->n.eventoId().equals(r.eventoId())).toList())
                .allSatisfy(n->assertThat(n.datos()).containsEntry("nivelUrgencia","URGENTE")));
    }
    @Test void e11FinAutomaticoUnico()throws Exception{
        var p=pedido("A",11,null);var r=registrar(new EtapaAprobada(p,10,actor(log1),T,11,null,false));
        registrar(new PedidoFinalizado(p,11,T));comprobar("11",r,admin);
        assertThat(sql.queryForObject("select count(*) from aviso_etapa where pedido_id=?",Integer.class,p.pedidoId())).isEqualTo(1);
        assertThat(sql.queryForObject("select tipo from aviso_etapa where evento_id=?",String.class,r.eventoId())).isEqualTo("CIERRE");
    }
    @ParameterizedTest @CsvSource({"1,2,China,Revision","2,1,Revision,China","4,3,Revision,Marketing","8,9,China,Log"})
    void e12FlujoB(int origen,int destino,String quien,String recibe)throws Exception{
        UUID a=quien.equals("China")?compras:revision;var p=pedido("B",destino,null);
        EventoDePedido e=destino<origen?new EtapaRechazada(p,origen,actor(a),T,destino,"Rechazo B ficticio",false):new EtapaEnviada(p,origen,actor(a),T,destino,null,false);
        var r=registrar(e);
        UUID[] ids=recibe.equals("Log")?new UUID[]{admin,log1,log2}:new UUID[]{admin,recibe.equals("Revision")?revision:recibe.equals("Marketing")?marketing:compras};
        comprobar("12-B"+origen+"-B"+destino,r,ids);
    }
    @Test void e13ReasignacionMismoRol()throws Exception{
        var p=pedido("A",9,log2);comprobar("13",registrar(new EtapaReasignada(p,9,actor(admin),T,log1,log2,"Sustitución ficticia")),admin,log2);
    }
    @Test void e14InactivosRolYPaisNoReciben()throws Exception{
        persona("Inactivo","marketing","EC",false);persona("OtroPais","marketing","CO",true);persona("SinPais","marketing",null,true);persona("OtroRol","consulta","EC",true);
        comprobar("14-filtros",avance("A",1,2,admin,null),admin,marketing);
    }
    @Test void e14HttpLecturaIndependienteNoSuplantaUsuario()throws Exception{
        var r=avance("A",1,2,admin,null);comprobar("14-propiedad",r,admin,marketing);
        assertThat(get(log1).get("avisos").isEmpty()).isTrue();
        var ajeno=peticion(log1,"POST","/avisos/leidos?usuarioId="+marketing,null);assertThat(ajeno.statusCode()).isEqualTo(200);
        assertThat(get(marketing).get("noLeidos").asInt()).isEqualTo(1);
        assertThat(peticion(marketing,"POST","/avisos/leidos",null).statusCode()).isEqualTo(200);
        assertThat(get(marketing).get("noLeidos").asInt()).isZero();assertThat(get(admin).get("noLeidos").asInt()).isEqualTo(1);
        ahora=T.minusSeconds(1);peticion(marketing,"POST","/avisos/leidos",null);
        assertThat(get(marketing).get("leidasHasta").asText()).isEqualTo(T.toString());
    }
    @Test void e14RevocarPaisInactivarYSinRolesImpidenAcceso()throws Exception{
        avance("A",1,2,admin,null);sql.update("delete from usuario_pais where usuario_id=?",marketing);
        assertThat(get(marketing).get("avisos").isEmpty()).isTrue();
        sql.update("update usuario set activo=false where id=?",marketing);
        assertThat(peticion(marketing,"GET","/avisos",null).statusCode()).isEqualTo(403);
        assertThat(peticion(marketing,"POST","/avisos/leidos",null).statusCode()).isEqualTo(403);
        sql.update("delete from usuario_rol where usuario_id=?",log1);
        assertThat(peticion(log1,"GET","/avisos",null).statusCode()).isEqualTo(403);
    }
    @Test void e14RevocarRolDestinoConOtroRolDebeOcultarAvisos()throws Exception{
        avance("A",1,2,admin,null);rol(marketing,"consulta");
        sql.update("delete from usuario_rol where usuario_id=? and rol_id=(select id from rol where codigo='marketing')",marketing);
        assertThat(get(marketing).get("avisos").isEmpty()).as("Permiso del rol destino revocado, queda consulta").isTrue();
    }
    @Test void e14AsignadoSinRolDestinoNoDebeRecibir()throws Exception{
        UUID invalido=persona("AsignadoSinRol","marketing","EC",true);rol(invalido,"consulta");
        sql.update("delete from usuario_rol where usuario_id=? and rol_id=(select id from rol where codigo='marketing')",invalido);
        comprobar("14-asignado-invalido",avance("A",1,2,admin,invalido),admin);
    }
    @Test void e15AdminMultirrolConcurrenteYRepeticion()throws Exception{
        rol(admin,"marketing");var r=avance("A",1,2,compras,null);
        var barrera=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)){
            Callable<Void> repetir=()->{barrera.await(10,TimeUnit.SECONDS);tx.executeWithoutResult(s->eventos.publishEvent(r));return null;};
            var a=pool.submit(repetir);var b=pool.submit(repetir);a.get(20,TimeUnit.SECONDS);b.get(20,TimeUnit.SECONDS);
        }
        tx.executeWithoutResult(s->eventos.publishEvent(r));comprobar("15-idempotencia",r,admin,marketing);
        assertThat(sql.queryForObject("select como_admin and como_responsable from aviso_etapa where evento_id=? and usuario_id=?",Boolean.class,r.eventoId(),admin)).isTrue();
    }
    @Test void e15RollbackNoDejaHistorialAvisoNiCorreo(){
        var p=pedido("A",2,null);var r=new EventoPedidoRegistrado(UUID.randomUUID(),new EtapaEnviada(p,1,actor(admin),T,2,null,false));
        tx.executeWithoutResult(s->{historial(r);eventos.publishEvent(r);assertThat(obtenidos(r.eventoId())).hasSize(2);s.setRollbackOnly();});
        for(String tabla:List.of("evento_pedido","aviso_etapa","notificacion"))assertThat(sql.queryForObject("select count(*) from "+tabla+" where "+(tabla.equals("evento_pedido")?"id":"evento_id")+"=?",Integer.class,r.eventoId())).isZero();
    }
    com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes worker(String corte,DirectorioDestinatarios directorio){
        return new CorreoConfig().enviarNotificacionesPendientes(gestor,correos,gmail,directorio,reloj,true,true,25,Duration.ofMinutes(3),corte);
    }
    DirectorioDestinatarios directorio(){return NotificacionesConfig.registrados(contactos);}
    @Test void e16VacioCorteYFronteraNoTocanHistoricos(){
        ahora=T.minusSeconds(1);var viejo=avance("A",1,2,admin,null);ahora=T;var frontera=avance("A",1,2,admin,null);
        ahora=T.plusSeconds(1);var nuevo=avance("A",1,2,admin,null);
        when(gmail.enviar(any())).thenAnswer(i->new Notificador.Entrega("doble-"+UUID.randomUUID()));
        assertThat(worker("",directorio()).procesar()).isZero();verifyNoInteractions(gmail);
        assertThat(worker(T.toString(),directorio()).procesar()).isEqualTo(4);
        assertThat(sql.queryForList("select estado from notificacion where evento_id=?",String.class,viejo.eventoId())).containsOnly("PENDIENTE");
        for(var r:List.of(frontera,nuevo))assertThat(sql.queryForList("select estado from notificacion where evento_id=?",String.class,r.eventoId())).containsOnly("ENVIADA");
        verify(gmail,times(4)).enviar(any());
    }
    @Test void e16RevalidaPaisYCuentaAntesDeEnviar(){
        var r=avance("A",1,2,admin,null);sql.update("update usuario set activo=false where id=?",marketing);
        sql.update("delete from usuario_pais where usuario_id=?",admin);
        assertThat(worker(T.toString(),directorio()).procesar()).isEqualTo(2);verifyNoInteractions(gmail);
        assertThat(sql.queryForList("select ultimo_error from notificacion where evento_id=?",String.class,r.eventoId())).containsOnly("CONFIGURACION");
    }
    @Test void e16RevocarRolAntesDeEnvioDebeExcluirCorreo(){
        var r=avance("A",1,2,admin,null);rol(marketing,"consulta");
        sql.update("delete from usuario_rol where usuario_id=? and rol_id=(select id from rol where codigo='marketing')",marketing);
        when(gmail.enviar(any())).thenReturn(new Notificador.Entrega("ficticio"));worker(T.toString(),directorio()).procesar();
        verify(gmail,never()).enviar(argThat(c->c.destinatario().equals(marketing+"@ejemplo.test")));
    }
    @Test void e16InciertoNoReenviaTrasReinicio(){
        var r=avance("A",1,2,admin,null);when(gmail.enviar(any())).thenThrow(new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.RESULTADO_INCIERTO));
        assertThat(worker(T.toString(),directorio()).procesar()).isEqualTo(2);ahora=T.plusSeconds(86400);
        assertThat(worker(T.toString(),directorio()).procesar()).isZero();verify(gmail,times(2)).enviar(any());
        assertThat(sql.queryForList("select resultado_incierto from notificacion where evento_id=?",Boolean.class,r.eventoId())).containsOnly(true);
    }
    @Test void e16DesvioNoCambiaAvisoInterno()throws Exception{
        var r=avance("A",1,2,admin,null);var prueba=NotificacionesConfig.desde(NotificacionesConfig.DESTINATARIO_PRUEBA_AUTORIZADO);
        assertThat(worker(T.toString(),prueba).procesar()).isEqualTo(2);verifyNoInteractions(gmail);
        comprobar("16-desvio",r,admin,marketing);
        // El directorio de prueba hoy filtra, no redirige: registrar explícitamente si se exige desvío efectivo.
    }
}
