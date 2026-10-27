package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.flywaydb.core.Flyway;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class MigracionesArchivosB1IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    @TempDir Path disco;
    @Test void baseVaciaAplicaHastaV8SinBinariosYReaplicarNoCambiaNada() {
        var d=new DatosB1(PG,"8");
        var f=Flyway.configure().dataSource(d.ds).locations("classpath:db/migration").schemas(d.esquema).target("8").load();
        assertThat(f.info().current().getVersion().toString()).isEqualTo("8");
        assertThat(f.validateWithResult().validationSuccessful).isTrue();
        assertThat(f.migrate().migrationsExecuted).isZero();
        assertThat(d.sql.queryForObject("select count(*) from adjunto",Long.class)).isZero();
        assertThat(d.sql.queryForObject("select count(*) from information_schema.columns where table_schema=? and table_name='adjunto' and data_type='bytea'",Long.class,d.esquema)).isEqualTo(1); // sesión cifrada, no archivo
    }
    @Test void v3PobladaConservaUuidRelacionHuellaBytesEHistorialAlActualizar() throws Exception {
        var d=new DatosB1(PG,"3"); byte[] bytes={7,8,9}; var ruta=disco.resolve("archivo"); Files.write(ruta,bytes);
        var sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        d.poblar(3,sha,"archivo");
        var antes=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);
        var evento=d.sql.queryForMap("select * from evento_pedido where id=?",d.evento);
        d.actualizar();
        var despues=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);
        antes.forEach((k,v)->assertThat(despues.get(k)).as(k).isEqualTo(v));
        var eventoEsperado=new LinkedHashMap<>(evento);eventoEsperado.put("secuencia",null);
        assertThat(d.sql.queryForMap("select * from evento_pedido where id=?",d.evento)).isEqualTo(eventoEsperado);
        assertThat(d.sql.queryForObject("select evento_id from evento_adjunto where adjunto_id=?",UUID.class,d.adjunto)).isEqualTo(d.evento);
        assertThat(despues).containsEntry("drive_offset",0L).containsEntry("copia_en_intento",false);
        assertThat(d.sql.queryForObject("select count(*) from notificacion",Long.class)).isZero();
        assertThat(Files.readAllBytes(ruta)).isEqualTo(bytes);
    }
    @Test void restriccionesRechazanCopiaSinIdYOffsetsMayoresAlArchivo() {
        var d=new DatosB1(PG,null);d.poblar(3,"0".repeat(64),"archivo");
        assertThatThrownBy(()->d.sql.update("update adjunto set estado_copia='COPIADO' where id=?",d.adjunto)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThatThrownBy(()->d.sql.update("update adjunto set drive_offset=4 where id=?",d.adjunto)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(d.sql.queryForObject("select estado_copia from adjunto where id=?",String.class,d.adjunto)).isEqualTo("PENDIENTE");
    }

    @Test void v6ConFinalYParcialRecuperaReservasSinDuplicarNiCambiarPropietario() {
        var d=new DatosB1(PG,"6");d.poblar(3,"0".repeat(64),"ficticio");
        UUID parcial=UUID.randomUUID();
        d.sql.update("""
            insert into carga_archivo(id,usuario_id,clave_idempotencia,pedido_id,etapa,entrada_id,nombre,tipo_documento,
              tamano_bytes,offset_confirmado,estado,adjunto_reservado_id,adjunto_id,evento_id,creada_en,actualizada_en)
            values(?,?,?,?,1,?,'final.bin','Ficha',3,3,'COMPLETADA',?,?,?,now(),now())
            """,UUID.randomUUID(),d.usuario,UUID.randomUUID(),d.pedido,d.entrada,d.adjunto,d.adjunto,d.evento);
        d.sql.update("""
            insert into carga_archivo(id,usuario_id,clave_idempotencia,pedido_id,etapa,entrada_id,nombre,tipo_documento,
              tamano_bytes,adjunto_reservado_id,creada_en,actualizada_en)
            values(?,?,?,?,1,?,'parcial.bin','Ficha',7,?,now(),now())
            """,UUID.randomUUID(),d.usuario,UUID.randomUUID(),d.pedido,d.entrada,parcial);
        var antes=d.sql.queryForList("select * from carga_archivo order by id");d.actualizar();
        assertThat(d.sql.queryForList("select * from carga_archivo order by id")).isEqualTo(antes);
        assertThat(d.sql.queryForObject("select sum(bytes_comprometidos) from archivo_cuota",Long.class)).isEqualTo(10);
        assertThat(d.sql.queryForObject("select count(*) from archivo_cuota",Long.class)).isEqualTo(2);
        assertThat(d.sql.queryForMap("select propietario_id,bytes_comprometidos,estado from archivo_cuota where adjunto_id=?",d.adjunto))
            .containsEntry("propietario_id",d.usuario).containsEntry("bytes_comprometidos",3L).containsEntry("estado","CONFIRMADA");
        assertThat(d.sql.queryForMap("select propietario_id,bytes_comprometidos,estado from archivo_cuota where adjunto_id=?",parcial))
            .containsEntry("propietario_id",d.usuario).containsEntry("bytes_comprometidos",7L).containsEntry("estado","RESERVADA");
    }
    @Test void excesoHistoricoSeConservaYBloqueaNuevaAdmision() {
        var d=new DatosB1(PG,"3");d.poblar(2147483649L,"0".repeat(64),"historico");d.actualizar();
        var cuota=new com.decokasa.seguimiento.archivos.adapter.out.persistence.CuotaSqlRepositorio(d.ds,10737418240L,2147483648L,10,2);
        var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(d.ds));
        assertThatThrownBy(()->tx.executeWithoutResult(e->cuota.reservar(UUID.randomUUID(),d.usuario,1)))
            .isInstanceOf(com.decokasa.seguimiento.archivos.domain.CuotaAlmacenamientoExcedida.class);
        assertThat(d.sql.queryForObject("select bytes_comprometidos from archivo_cuota where adjunto_id=?",Long.class,d.adjunto)).isEqualTo(2147483649L);
        assertThat(d.sql.queryForObject("select subido_por from adjunto where id=?",UUID.class,d.adjunto)).isEqualTo(d.usuario);
    }
}
