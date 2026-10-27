package com.decokasa.seguimiento;

import static org.assertj.core.api.Assertions.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Actualización V2 poblada -> V3, en un contenedor propio sin reuse ni base compartida.
 * Target 3 explícito: conserva el reproductor del backfill y rollback de esa migración.
 * MigracionesArchivosB1IT cubre por separado la actualización poblada V3 -> V6. */
@Testcontainers
class V3MigracionIT {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    @Test void backfillConservaFechasYDaEntradasSoloAEtapasVisitadas() throws Exception {
        Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword())
                .locations("classpath:db/migration").target("2").load().migrate();
        UUID u=UUID.randomUUID(),p=UUID.randomUUID();
        try(Connection db=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
                Statement s=db.createStatement()) {
            s.executeUpdate("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                    +"values ('"+u+"','Persona ficticia','v3@ejemplo.test','personav3','+593990000001','hash-ficticio',now(),now(),0)");
            s.executeUpdate("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) "
                    +"values ('"+p+"','DK-EC-2026-0001','EC','A','01a13cdb-cc00-7000-8000-000000000001','Pedido ficticio','"+u+"',now(),2,now(),0)");
            for(int n=1;n<=3;n++) s.executeUpdate("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,reentradas) values ('"
                    +UUID.randomUUID()+"','"+p+"',"+n+",'"+(n==1?"COMPLETADA":n==2?"ACTUAL":"PENDIENTE")+"',"
                    +(n<3?"'2026-10-15T12:00:00Z'":"null")+",0)");
        }
        Flyway flyway=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword())
                .locations("classpath:db/migration").target("3").load();
        org.flywaydb.core.api.output.MigrateResult r;
        try { r=flyway.migrate(); }
        catch(RuntimeException fallo) {
            // El reproductor sigue rojo, pero también comprueba que Flyway no dejó una V3 parcial.
            assertThat(flyway.info().current().getVersion().toString()).isEqualTo("2");
            try(Connection db=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
                    Statement s=db.createStatement(); ResultSet rs=s.executeQuery("select "
                            +"(select count(*) from information_schema.tables where table_schema='public' and table_name in ('entrada_etapa','adjunto','evento_adjunto')) as tablas,"
                            +"(select count(*) from information_schema.columns where table_schema='public' and table_name='etapa_pedido' and column_name='entrada_id') as columnas,"
                            +"(select count(*) from etapa_pedido) as etapas")) {
                assertThat(rs.next()).isTrue(); assertThat(rs.getInt("tablas")).isZero();
                assertThat(rs.getInt("columnas")).isZero(); assertThat(rs.getInt("etapas")).isEqualTo(3);
            }
            throw fallo;
        }
        assertThat(r.success).isTrue(); assertThat(r.migrationsExecuted).isEqualTo(1);
        assertThat(r.targetSchemaVersion).isEqualTo("3");
        try(Connection db=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
                Statement s=db.createStatement(); ResultSet rs=s.executeQuery("select e.numero,e.entrada_id,n.pedido_id,n.etapa,n.entrada_en=e.entrada_en as misma_fecha "
                        +"from etapa_pedido e left join entrada_etapa n on n.id=e.entrada_id order by e.numero")) {
            for(int n=1;n<=3;n++) {
                assertThat(rs.next()).isTrue(); assertThat(rs.getInt("numero")).isEqualTo(n);
                if(n<3) {
                    assertThat(rs.getObject("entrada_id")).isNotNull();
                    assertThat(rs.getObject("pedido_id",UUID.class)).isEqualTo(p);
                    assertThat(rs.getInt("etapa")).isEqualTo(n); assertThat(rs.getBoolean("misma_fecha")).isTrue();
                } else assertThat(rs.getObject("entrada_id")).isNull();
            }
            assertThat(rs.next()).isFalse();
        }
        assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }
}
