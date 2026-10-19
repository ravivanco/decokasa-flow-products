package com.decokasa.seguimiento.archivos;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Fixtures ficticios: cada caso usa un esquema exclusivo dentro de SU contenedor, sin reuse. */
final class DatosB1 {
    final DriverManagerDataSource ds;
    final JdbcTemplate sql;
    final String esquema="b1_"+UUID.randomUUID().toString().replace("-","");
    final UUID usuario=UUID.randomUUID(),pedido=UUID.randomUUID(),entrada=UUID.randomUUID(),adjunto=UUID.randomUUID(),evento=UUID.randomUUID();
    DatosB1(PostgreSQLContainer pg,String version) {
        String url=pg.getJdbcUrl()+(pg.getJdbcUrl().contains("?")?"&":"?")+"currentSchema="+esquema;
        ds=new DriverManagerDataSource(url,pg.getUsername(),pg.getPassword());sql=new JdbcTemplate(ds);
        var fly=Flyway.configure().dataSource(ds).locations("classpath:db/migration").schemas(esquema);
        if(version!=null)fly.target(version);
        fly.load().migrate();
    }
    void poblar(long tamano,String sha,String clave) {
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Persona B1',?,?,?,'hash-ficticio',now(),now(),0)",
            usuario,usuario+"@ejemplo.test",usuario.toString(),"+593990000001");
        sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values (?,'DK-EC-2026-0001','EC','A','01a13cdb-cc00-7000-8000-000000000001','Pedido B1',?,now(),1,now(),0)",pedido,usuario);
        sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,1,now())",entrada,pedido);
        sql.update("insert into adjunto(id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,clave_local,estado_copia,subido_por,subido_en) values (?,?,1,?,'Ficha','ficticio.bin',?,?,?,'PENDIENTE',?,now())",adjunto,pedido,entrada,tamano,sha,clave,usuario);
        sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values (?,?,1,'ARCHIVO_SUBIDO',?,'admin',now(),'{}')",evento,pedido,usuario);
        sql.update("insert into evento_adjunto(evento_id,adjunto_id,relacion) values (?,?,'SUBIDO')",evento,adjunto);
    }
    void actualizar() { Flyway.configure().dataSource(ds).locations("classpath:db/migration").schemas(esquema).load().migrate(); }
}
