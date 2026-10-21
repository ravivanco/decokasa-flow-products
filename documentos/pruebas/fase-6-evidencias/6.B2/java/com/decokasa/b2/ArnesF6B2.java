package com.decokasa.b2;

import com.decokasa.seguimiento.SeguimientoApplication;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

/** Infraestructura opt-in: API del producto sin cambiar reglas, base privada y control solo en loopback. */
public final class ArnesF6B2 {
 static final AtomicInteger GOOGLE=new AtomicInteger();
 static UUID id(String s){return UUID.nameUUIDFromBytes(("f6b2-"+s).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @TestConfiguration(proxyBeanMethods=false) public static class Dobles {
  @Bean @Primary Notificador correoDoble(){return correo->{GOOGLE.incrementAndGet();throw new IllegalStateException("Correo doble no debe ejecutarse en 6.B2");};}
  @Bean @Primary AlmacenArchivos driveDoble(){return (AlmacenArchivos)java.lang.reflect.Proxy.newProxyInstance(AlmacenArchivos.class.getClassLoader(),new Class<?>[]{AlmacenArchivos.class},(proxy,m,args)->{
   if(m.getDeclaringClass()==Object.class)return switch(m.getName()){case "toString"->"DriveDobleF6";case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];default->null;};
   GOOGLE.incrementAndGet();throw new IllegalStateException("Drive doble no debe ejecutarse en 6.B2");});}
 }
 public static void main(String[] args)throws Exception{
  Path base=Path.of(args[0]),tmp=base.resolve(".tmp");Files.createDirectories(tmp);
  var pg=new PostgreSQLContainer("postgres:17").withDatabaseName("f6b2").withUsername("f6b2").withPassword(UUID.randomUUID().toString()).withLabel("decokasa.ensayo","f6b2").withTmpFs(Map.of("/var/lib/postgresql/data","rw"));
  ConfigurableApplicationContext ctx=null;HttpServer control=null;
  try{
   pg.start();var app=new SpringApplication(SeguimientoApplication.class,Dobles.class);Map<String,Object> p=new HashMap<>();
   p.put("server.address","127.0.0.1");p.put("server.port",44285);p.put("spring.config.import","");
   p.put("spring.datasource.url",pg.getJdbcUrl());p.put("spring.datasource.username",pg.getUsername());p.put("spring.datasource.password",pg.getPassword());
   p.put("decokasa.jwt.secreto",UUID.randomUUID().toString()+UUID.randomUUID());p.put("decokasa.archivos.directorio",tmp.resolve("almacen").toAbsolutePath().toString());
   p.put("decokasa.cors.origenes","http://127.0.0.1:44284");p.put("decokasa.semilla.habilitada",false);
   for(String key:List.of("decokasa.google.oauth.habilitada","decokasa.google.drive.habilitada","decokasa.google.gmail.habilitada","decokasa.google.drive.worker.automatico","decokasa.google.gmail.worker.automatico","decokasa.plazos.vencimientos.automatico"))p.put(key,false);
   app.addInitializers(c->c.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("f6b2-aislado",p)));
   ctx=app.run("--spring.config.import=");var sql=ctx.getBean(JdbcTemplate.class);var json=new ObjectMapper();String password=UUID.randomUUID()+"aA1!",hash=new BCryptPasswordEncoder(12).encode(password);
   int n=0;for(String role:List.of("admin","compras_china","logistica","consulta","marketing","asistente_compras","revocable","mixto","doble")){
    UUID u=id(role);n++;sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,?,?,?,?,?,now(),now(),0)",u,"Ficticio F6 "+role,role+"@ejemplo.test","f6-"+role,"+59399000600"+n,hash);
    for(String r:roles(role))sql.update("insert into usuario_rol select ?,id,now() from rol where codigo=?",u,r);
    sql.update("insert into usuario_pais values(?,'EC')",u);
   }
   sql.execute("create function f6_limite_pedidos() returns trigger language plpgsql as $$ begin perform pg_advisory_xact_lock(600602); if (select count(*) from pedido)>=20 then raise exception 'Limite aislado de 20 pedidos'; end if; return new; end $$");
   sql.execute("create trigger f6_limite_pedidos before insert on pedido for each row execute function f6_limite_pedidos()");
   final var pgFinal=pg;
   control=HttpServer.create(new InetSocketAddress("127.0.0.1",44286),0);
   control.createContext("/",ex->{try{String path=ex.getRequestURI().getPath();Object result;
    if(path.equals("/estado"))result=Map.of("pedidos",sql.queryForObject("select count(*) from pedido",Integer.class),"maximo",20,"contenedor",pgFinal.getContainerId(),"invocacionesDoblesGoogle",GOOGLE.get(),"googleReal",false,"automaticos",false);
    else if(path.equals("/evidencia")){
     result=Map.of("pedidos",sql.queryForList("select row_to_json(p)::text from pedido p order by numero",String.class),"eventos",sql.queryForList("select row_to_json(e)::text from evento_pedido e order by pedido_id,secuencia",String.class),"outbox",sql.queryForList("select row_to_json(n)::text from notificacion n order by pedido_id,id",String.class),"adjuntos",sql.queryForList("select id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,estado_copia from adjunto order by pedido_id,etapa"));
    }else if(path.startsWith("/usuario/")){
     String[] x=path.split("/");String u=x[2],accion=x[3];if(!Set.of("revocable","mixto").contains(u))throw new IllegalArgumentException();
     switch(accion){case "revocar"->sql.update("delete from usuario_rol where usuario_id=?",id(u));case "desactivar"->sql.update("update usuario set activo=false where id=?",id(u));case "restaurar"->{sql.update("update usuario set activo=true where id=?",id(u));for(String r:roles(u))sql.update("insert into usuario_rol select ?,id,now() from rol where codigo=? on conflict do nothing",id(u),r);}default->throw new IllegalArgumentException();}result=Map.of("ok",true);
    }else{ex.sendResponseHeaders(404,-1);ex.close();return;}
    byte[] b=json.writeValueAsBytes(result);ex.getResponseHeaders().set("Content-Type","application/json");ex.sendResponseHeaders(200,b.length);ex.getResponseBody().write(b);ex.close();
   }catch(Exception e){ex.sendResponseHeaders(500,-1);ex.close();}});control.start();
   Files.writeString(tmp.resolve("acceso.json"),json.writeValueAsString(Map.of("password",password)));
   Files.writeString(base.resolve("entorno.json"),json.writeValueAsString(Map.of("pedidosIniciales",0,"maximo",20,"postgres",pg.getDockerImageName(),"contenedor",pg.getContainerId(),"api","http://127.0.0.1:44285","googleReal",false,"dobles",true,"automaticos",false)));
   System.out.println("F6_B2_LISTO_0_PEDIDOS");while(!Files.exists(tmp.resolve("detener")))Thread.sleep(500);
  }finally{if(control!=null)control.stop(0);if(ctx!=null)ctx.close();if(pg.getContainerId()!=null)DockerClientFactory.instance().client().stopContainerCmd(pg.getContainerId()).exec();Files.deleteIfExists(tmp.resolve("acceso.json"));Files.writeString(base.resolve("parada.txt"),"Contexto y contenedor propio detenidos; sin borrar volumenes. "+Instant.now());}
 }
 static List<String> roles(String u){return switch(u){case "mixto"->List.of("marketing","asistente_compras");case "doble"->List.of("admin","compras_china");case "revocable"->List.of("asistente_compras");default->List.of(u);};}
}
