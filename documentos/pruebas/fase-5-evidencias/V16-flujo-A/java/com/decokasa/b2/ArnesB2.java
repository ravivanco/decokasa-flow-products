package com.decokasa.b2;

import com.decokasa.seguimiento.SeguimientoApplication;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/** Infraestructura opt-in de verificación, fuera de src/main y de las suites normales. */
public final class ArnesB2 {
 // Decisión expresa de Richard: escenario principal limitado a veinte, no objetivo del manual.
 static final int MAXIMO_PEDIDOS = 20;
 public static class Inspector implements StatementInspector {
  static final List<String> SQL = new CopyOnWriteArrayList<>();
  public String inspect(String sql) { if(SQL.size()<20000)SQL.add(sql); return sql; }
 }
 static UUID id(String value){return UUID.nameUUIDFromBytes(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 public static void main(String[] args) throws Exception {
  Path base=Path.of(args[0]); Path tmp=base.resolve(".tmp");Files.createDirectories(tmp);
  String password=UUID.randomUUID()+"aA1!";
  var pg=new PostgreSQLContainer("postgres:17").withDatabaseName("f5b2").withUsername("f5b2")
    .withPassword(UUID.randomUUID().toString()).withLabel("decokasa.ensayo","f5b2")
    .withTmpFs(Map.of("/var/lib/postgresql/data","rw"));
  ConfigurableApplicationContext context=null; HttpServer control=null;
  try {
   pg.start();
   var app=new SpringApplication(SeguimientoApplication.class);
   Map<String,Object> p=new HashMap<>();
   p.put("spring.config.import", "");p.put("server.address","127.0.0.1");p.put("server.port","43185");
   p.put("spring.datasource.url",pg.getJdbcUrl());p.put("spring.datasource.username",pg.getUsername());p.put("spring.datasource.password",pg.getPassword());
   p.put("decokasa.jwt.secreto",UUID.randomUUID().toString()+UUID.randomUUID());
   p.put("decokasa.archivos.directorio",tmp.resolve("almacen").toAbsolutePath().toString());
   p.put("decokasa.cors.origenes","http://127.0.0.1:43184");p.put("decokasa.semilla.habilitada","false");
   for(String key:List.of("decokasa.google.oauth.habilitada","decokasa.google.drive.habilitada","decokasa.google.gmail.habilitada","decokasa.google.drive.worker.automatico","decokasa.google.gmail.worker.automatico","decokasa.plazos.vencimientos.automatico"))p.put(key,"false");
   p.put("spring.jpa.properties.hibernate.session_factory.statement_inspector",Inspector.class.getName());
   app.addInitializers(c->c.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("soloB2",p)));
   context=app.run("--spring.config.import="); var sql=context.getBean(JdbcTemplate.class);var json=new ObjectMapper();
   String hash=new BCryptPasswordEncoder(12).encode(password);
   int n=0;for(String role:List.of("admin","compras_china","logistica","consulta","marketing","asistente_compras")){
    UUID u=id(role);n++;
    sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,?,?,?,?,?,now(),now(),0)",u,"Ficticio B2 "+role,role+"@ejemplo.test","b2-"+role,"+59399000900"+n,hash);
    sql.update("insert into usuario_rol select ?,id,now() from rol where codigo=?",u,role);
    sql.update("insert into usuario_pais values(?,'EC')",u);
   }
   // Sin pedidos sembrados: el recorrido completo se crea exclusivamente por la API real.
   sql.execute("analyze"); Inspector.SQL.clear();
   final var pgFinal=pg; final var ctx=context;
   control=HttpServer.create(new InetSocketAddress("127.0.0.1",43186),0);
   control.createContext("/",ex->{try {
    String path=ex.getRequestURI().getPath();Object result;
    if(path.startsWith("/archivo/")){
     String[] partes=path.split("/");UUID archivo=UUID.fromString(partes[2]);String modo=partes[3];
     String clave=sql.queryForObject("select clave_local from adjunto where id=?",String.class,archivo);
     Path raiz=tmp.resolve("almacen").toAbsolutePath().normalize();Path bytes=raiz.resolve(clave).normalize();Path copia=bytes.resolveSibling(bytes.getFileName()+".v16-original");
     if(!bytes.startsWith(raiz)||!copia.startsWith(raiz))throw new IllegalArgumentException("Fuera de disco aislado");
     if(modo.equals("restaurar")){if(Files.exists(copia))Files.copy(copia,bytes,StandardCopyOption.REPLACE_EXISTING);}
     else {if(!Files.exists(copia))Files.copy(bytes,copia);if(modo.equals("ausente")){Path ausente=bytes.resolveSibling(bytes.getFileName()+".v16-ausente");if(!ausente.startsWith(raiz))throw new IllegalArgumentException();Files.move(bytes,ausente);}else if(modo.equals("corrupto")){byte[] b=Files.readAllBytes(bytes);b[0]^=1;Files.write(bytes,b);}else throw new IllegalArgumentException();}
     result=Map.of("ok",true,"archivo",archivo);
    }
    else if(path.equals("/reset")){Inspector.SQL.clear();result=Map.of("ok",true);}
    else if(path.equals("/stats"))result=Map.of("count",Inspector.SQL.size(),"sql",List.copyOf(Inspector.SQL));
    else if(path.equals("/quitar-permisos")){sql.update("delete from usuario_rol where usuario_id=?",id("logistica"));result=Map.of("ok",true);}
    else if(path.equals("/restaurar-permisos")){sql.update("insert into usuario_rol select ?,id,now() from rol where codigo='logistica' on conflict do nothing",id("logistica"));result=Map.of("ok",true);}
    else if(path.equals("/reentrada")){
     UUID pedido=id("pedido8"),entrada=UUID.randomUUID();
     new org.springframework.transaction.support.TransactionTemplate(ctx.getBean(org.springframework.transaction.PlatformTransactionManager.class)).executeWithoutResult(t->{
      sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,8,now())",entrada,pedido);
      sql.update("update etapa_pedido set entrada_id=?,entrada_en=now(),reentradas=reentradas+1 where pedido_id=? and numero=8",entrada,pedido);
      sql.update("update pedido set version=version+1 where id=?",pedido);
     });result=Map.of("entradaId",entrada);
    }else if(path.equals("/explain")){
     Map<String,Object> plans=new LinkedHashMap<>();
     for(String q:List.of("select id,numero from pedido order by creado_en desc,id desc limit 20","select id from pedido where numero='DK-EC-2026-0008'","select id from pedido where nombre ilike '%ficticio%' order by creado_en desc,id desc limit 20","select count(*) from pedido","select e.* from etapa_pedido e join pedido p on p.id=e.pedido_id and p.etapa_actual=e.numero where e.estado='ACTUAL' limit 20"))plans.put(q,sql.queryForList("explain (analyze,buffers,format json) "+q));
     result=plans;
    }else if(path.equals("/estado"))result=Map.of("pedidos",sql.queryForObject("select count(*) from pedido",Integer.class),"contenedor",pgFinal.getContainerId(),"google",false,"automaticos",false);
    else {ex.sendResponseHeaders(404,-1);ex.close();return;}
    byte[] bytes=json.writeValueAsBytes(result);ex.getResponseHeaders().set("Content-Type","application/json");ex.sendResponseHeaders(200,bytes.length);ex.getResponseBody().write(bytes);ex.close();
   }catch(Exception e){byte[] b=e.getClass().getSimpleName().getBytes();ex.sendResponseHeaders(500,b.length);ex.getResponseBody().write(b);ex.close();}});
   control.start();
   Files.writeString(tmp.resolve("acceso.json"),json.writeValueAsString(Map.of("password",password,"pedido8",id("pedido8"),"pedido7",id("pedido7"),"pedido6",id("pedido6"),"pedido10",id("pedido10"))));
   Files.writeString(base.resolve("entorno.json"),json.writeValueAsString(Map.of("pedidos",0,"maximo",MAXIMO_PEDIDOS,"postgres",pg.getDockerImageName(),"contenedor",pg.getContainerId(),"api","http://127.0.0.1:43185","google",false,"automaticos",false)));
   System.out.println("V16_LISTO_0_PEDIDOS");
   while(!Files.exists(tmp.resolve("detener")))Thread.sleep(500);
  }finally{
   if(control!=null)control.stop(0);if(context!=null)context.close();
   if(pg.getContainerId()!=null)DockerClientFactory.instance().client().stopContainerCmd(pg.getContainerId()).exec();
   Files.deleteIfExists(tmp.resolve("acceso.json"));
   Files.writeString(base.resolve("parada.txt"),"Contexto y control detenidos; contenedor propio parado, sin borrar volúmenes. "+Instant.now());
  }
 }
}
