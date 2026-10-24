package com.decokasa.seguimiento;

import static org.assertj.core.api.Assertions.*;
import java.sql.*;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** SQL real, exclusivamente en el contenedor de esta clase. Cada caso se revierte, incluso TRUNCATE. */
@Testcontainers
class V2RestriccionesIT {
    @Container static final PostgreSQLContainer POSTGRES=new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
    private static final UUID FLUJO=UUID.fromString("01a13cdb-cc00-7000-8000-000000000001");
    private Connection db;
    private UUID usuario;
    private UUID pedido;

    @BeforeAll static void migrarBaseVacia() {
        var resultado=Flyway.configure().dataSource(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword())
                .locations("classpath:db/migration").load().migrate();
        assertThat(resultado.success).isTrue(); assertThat(resultado.migrationsExecuted).isEqualTo(21);
        assertThat(resultado.targetSchemaVersion).isEqualTo("21");
    }
    @BeforeEach void preparar() throws SQLException {
        db=DriverManager.getConnection(POSTGRES.getJdbcUrl(),POSTGRES.getUsername(),POSTGRES.getPassword());
        db.setAutoCommit(false); usuario=UUID.randomUUID(); pedido=UUID.randomUUID();
        sql("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                +"values (?,'Persona ficticia','persona@ejemplo.test','persona','+593990000001','hash-ficticio',now(),now(),0)",usuario);
        insertarPedido(pedido,"DK-EC-2026-0001",usuario,FLUJO,"EC","A");
        UUID entrada=UUID.randomUUID();
        sql("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,4,now())",entrada,pedido);
        sql("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id,reentradas) values (?, ?,4,'ACTUAL',now(),?,0)",UUID.randomUUID(),pedido,entrada);
        sql("insert into etapa_pedido(id,pedido_id,numero,estado,reentradas) values (?, ?,2,'COMPLETADA',0)",UUID.randomUUID(),pedido);
        sql("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en) "
                +"values (?, ?,4,'ETAPA_APROBADA',?,'admin',now())",UUID.randomUUID(),pedido,usuario);
        sql("insert into secuencia_pedido values ('EC',2026,7,0)");
    }
    @AfterEach void revertir() throws SQLException { if(db!=null) { db.rollback(); db.close(); } }
    private void sql(String texto,Object... args) throws SQLException {
        try(var q=db.prepareStatement(texto)) { for(int i=0;i<args.length;i++) q.setObject(i+1,args[i]); q.executeUpdate(); }
    }
    private List<String> textos(String texto) throws SQLException {
        try(var q=db.createStatement();var rs=q.executeQuery(texto)) {
            var lista=new ArrayList<String>(); while(rs.next()) lista.add(rs.getString(1)); return lista;
        }
    }
    private void insertarPedido(UUID id,String numero,UUID autor,UUID flujo,String pais,String tipo) throws SQLException {
        sql("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) "
                +"values (?,?,?,?,?,'Pedido ficticio',?,now(),4,now(),0)",id,numero,pais,tipo,flujo,autor);
    }
    private void rechaza(String estado,String texto,Object... args) throws SQLException {
        Savepoint punto=db.setSavepoint();
        try { assertThatThrownBy(() -> sql(texto,args)).isInstanceOfSatisfying(SQLException.class,
                error -> assertThat(error.getSQLState()).isEqualTo(estado)); }
        finally { db.rollback(punto); }
    }
    @Test void siembraExactaDeOnceEtapasSinInventarPlazos() throws SQLException {
        assertThat(textos("select pais_codigo||':'||tipo_flujo||':'||version||':'||vigente from definicion_flujo where tipo_flujo='A' and pais_codigo='EC'"))
                .containsExactly("EC:A:1:true");
        assertThat(textos("select numero||':'||coalesce(rol_codigo,'-')||':'||plazo_estado||':'||coalesce(plazo_cantidad::text,'-')"
                +"||':'||coalesce(plazo_tipo,'-')||':'||automatica from definicion_etapa where flujo_id='01a13cdb-cc00-7000-8000-000000000001' order by numero"))
                .containsExactly("1:admin:SIN_PLAZO:-:-:false","2:marketing:DEFINIDO:24:HORAS_HABILES:false",
                        "3:asistente_compras:DEFINIDO:24:HORAS_HABILES:false","4:compras_china:DEFINIDO:3:DIAS_HABILES:false",
                        "5:compras_china:PENDIENTE:-:-:false","6:compras_china:DEFINIDO:10:DIAS_HABILES:false",
                        "7:compras_china:PENDIENTE:-:-:false","8:logistica:DEFINIDO:24:HORAS_HABILES:false",
                        "9:logistica:DEFINIDO:24:HORAS_HABILES:false","10:logistica:PENDIENTE:-:-:false","11:-:SIN_PLAZO:-:-:true");
    }
    @Test void catorceTransicionesConRequisitosAprobadosYPendientes() throws SQLException {
        assertThat(textos("select etapa_origen||':'||accion||':'||coalesce(etapa_destino::text,'-')||':'||requiere_observacion"
                +"||':'||requiere_urgencia||':'||requiere_bodega||':'||requisitos_documentales "
                +"from transicion_permitida where flujo_id='01a13cdb-cc00-7000-8000-000000000001' order by etapa_origen,accion"))
                .containsExactly("1:ENVIAR:2:false:false:false:DEFINIDOS","2:ENVIAR:3:false:false:false:DEFINIDOS",
                        "3:APROBAR:4:false:false:false:DEFINIDOS","3:RECHAZAR:2:true:false:false:DEFINIDOS",
                        "4:APROBAR:5:false:false:false:DEFINIDOS","4:DEVOLVER_POR_ERROR:2:true:false:false:DEFINIDOS",
                        "4:DEVOLVER_POR_PRODUCTO_NUEVO:2:true:true:false:DEFINIDOS","5:APROBAR:6:true:false:false:DEFINIDOS",
                        "6:APROBAR:7:false:false:false:DEFINIDOS","7:APROBAR:8:false:false:false:DEFINIDOS",
                        "7:REPORTAR_DEMORA:-:true:false:false:DEFINIDOS","8:APROBAR:9:false:false:false:DEFINIDOS",
                        "9:APROBAR:10:false:false:true:DEFINIDOS","10:APROBAR:11:false:false:false:DEFINIDOS");
        assertThat(textos("select t.etapa_origen||':'||d.tipo_documento from documento_requerido d "
                +"join transicion_permitida t on t.id=d.transicion_id where t.flujo_id='01a13cdb-cc00-7000-8000-000000000001' order by t.etapa_origen"))
                .containsExactly("1:Solicitud","2:Documentos codificados","6:Packing List");
    }
    @ParameterizedTest @ValueSource(strings={"update evento_pedido set observacion='Cambio ficticio'", "delete from evento_pedido", "truncate table evento_pedido cascade"})
    void historialRechazaUpdateDeleteYTruncate(String sentencia) throws SQLException { rechaza("P0001",sentencia); }
    @ParameterizedTest @ValueSource(strings={"numero='otro'", "pais_codigo='CO'", "tipo_flujo='B'", "flujo_id='00000000-0000-0000-0000-000000000001'"})
    void identidadDelPedidoEsInmutable(String cambio) throws SQLException { rechaza("P0001","update pedido set "+cambio); }
    @Test void pedidoNoSeBorra() throws SQLException { rechaza("P0001","delete from pedido where id=?",pedido); }
    @ParameterizedTest @ValueSource(strings={"update secuencia_pedido set ultimo_numero=6", "delete from secuencia_pedido",
            "update secuencia_pedido set anio=2027", "update secuencia_pedido set pais_codigo='CO'"})
    void contadorNoRetrocedeNiSeBorraNiCambiaIdentidad(String sentencia) throws SQLException { rechaza("P0001",sentencia); }
    @Test void contadorTampocoSePuedeVaciarPorTruncate() throws SQLException {
        rechaza("P0001","truncate table secuencia_pedido"); // Un vaciado permitiría reutilizar numeración confirmada.
    }
    @Test void indiceUnicoRechazaDosActualesYPermiteCambioA4A2EnOrden() throws SQLException {
        UUID entrada=UUID.randomUUID();
        sql("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,2,now())",entrada,pedido);
        rechaza("23505","update etapa_pedido set estado='ACTUAL',entrada_en=now(),entrada_id=? where pedido_id=? and numero=2",entrada,pedido);
        sql("update etapa_pedido set estado='DEVUELTA',salida_en=now() where pedido_id=? and numero=4",pedido);
        sql("update etapa_pedido set estado='ACTUAL',entrada_en=now(),entrada_id=?,reentradas=1 where pedido_id=? and numero=2",entrada,pedido);
        assertThat(textos("select numero from etapa_pedido where estado='ACTUAL'")).containsExactly("2");
        assertThat(textos("select indexdef from pg_indexes where indexname='etapa_pedido_una_actual'"))
                .singleElement().asString().contains("UNIQUE","WHERE (estado = 'ACTUAL'::text)");
    }
    @Test void numeroUnicoYReferenciasDePedido() throws SQLException {
        rechaza("23505","insert into pedido select gen_random_uuid(),numero,pais_codigo,tipo_flujo,flujo_id,nombre,descripcion,"
                +"incluye_productos_nuevos,creado_por,creado_en,etapa_actual,finalizado,finalizado_en,urgencia_nivel,urgencia_activada_en,urgencia_vence_en,actualizado_en,version from pedido");
        Savepoint punto=db.setSavepoint();
        assertThatThrownBy(() -> insertarPedido(UUID.randomUUID(),"otro",UUID.randomUUID(),FLUJO,"EC","A"))
                .isInstanceOfSatisfying(SQLException.class,e -> assertThat(e.getSQLState()).isEqualTo("23503")); db.rollback(punto);
        punto=db.setSavepoint();
        assertThatThrownBy(() -> insertarPedido(UUID.randomUUID(),"otro",usuario,FLUJO,"CO","A"))
                .isInstanceOfSatisfying(SQLException.class,e -> assertThat(e.getSQLState()).isEqualTo("23503")); db.rollback(punto);
    }
    @ParameterizedTest @CsvSource({"estado='INEXISTENTE',23514", "reentradas=-1,23514", "numero=31,23514", "asignado_a='00000000-0000-0000-0000-000000000001',23503"})
    void restriccionesDeEtapas(String cambio,String estado) throws SQLException { rechaza(estado,"update etapa_pedido set "+cambio); }
    @Test void clavesForaneasDeTransicionesDocumentosYEventos() throws SQLException {
        rechaza("23503","update transicion_permitida set etapa_destino=30 where etapa_origen=1");
        rechaza("23503","insert into documento_requerido values (?, 'Documento ficticio')",UUID.randomUUID());
        rechaza("23503","insert into etapa_pedido(id,pedido_id,numero,estado,reentradas) values (?, ?,1,'PENDIENTE',0)",UUID.randomUUID(),UUID.randomUUID());
        rechaza("23503","insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en) "
                +"values (?, ?,1,'PEDIDO_CREADO',?,'admin',now())",UUID.randomUUID(),UUID.randomUUID(),usuario);
        rechaza("23503","delete from usuario where id=?",usuario);
    }
    @Test void checksDePlazoResponsableYUrgencia() throws SQLException {
        rechaza("23514","update definicion_etapa set plazo_cantidad=0 where numero=2");
        rechaza("23514","update definicion_etapa set plazo_estado='SIN_PLAZO' where numero=2");
        rechaza("23514","update definicion_etapa set rol_codigo=null where numero=2");
        rechaza("23514","update pedido set urgencia_nivel='URGENTE' where id=?",pedido);
        rechaza("23514","update pedido set urgencia_nivel='URGENTE',urgencia_activada_en=now(),urgencia_vence_en=now()-interval '1 hour' where id=?",pedido);
    }

    @Test void entradaExigeFechaEIdentificadorJuntosYLaFkCompuestaEsDiferida() throws SQLException {
        rechaza("23514","update etapa_pedido set entrada_id=null where numero=4");
        rechaza("23514","update etapa_pedido set entrada_en=null where numero=4");
        rechaza("23503","update etapa_pedido set entrada_id=? where numero=4; set constraints all immediate",UUID.randomUUID());
        UUID ajena=UUID.randomUUID();
        sql("insert into entrada_etapa values (?,?,2,now())",ajena,pedido);
        rechaza("23503","update etapa_pedido set entrada_id=? where numero=4; set constraints all immediate",ajena);
        UUID nueva=UUID.randomUUID();
        sql("update etapa_pedido set entrada_id=? where numero=4",nueva);
        sql("insert into entrada_etapa values (?,?,4,now())",nueva,pedido);
        sql("set constraints all immediate");
    }

    private UUID adjuntoValido() throws SQLException {
        UUID id=UUID.randomUUID();
        sql("insert into adjunto(id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,clave_local,estado_copia,subido_por,subido_en) "
                +"select ?,pedido_id,numero,entrada_id,'Solicitud','ficticio.pdf',8,repeat('a',64),'clave-ficticia','PENDIENTE',?,now() from etapa_pedido where numero=4",id,usuario);
        return id;
    }

    @ParameterizedTest @ValueSource(strings={"tamano_bytes=0","sha256='incorrecto'","nombre=''","tipo_documento=''",
            "clave_local=null","estado_copia='OTRO'","estado_copia='COPIADO'","intentos=-1"})
    void checksDeAdjunto(String cambio) throws SQLException {
        adjuntoValido(); rechaza("23514","update adjunto set "+cambio);
    }

    @Test void adjuntoExigeLaMismaEntradaPedidoYEtapa() throws SQLException {
        adjuntoValido();
        rechaza("23503","update adjunto set etapa=2");
        rechaza("23503","update adjunto set entrada_id=?",UUID.randomUUID());
        UUID otro=UUID.randomUUID(); insertarPedido(otro,"DK-EC-2026-0002",usuario,FLUJO,"EC","A");
        rechaza("23503","update adjunto set pedido_id=?",otro);
        rechaza("23503","update adjunto set subido_por=?",UUID.randomUUID());
    }

    @ParameterizedTest @ValueSource(strings={"update entrada_etapa set entrada_en=now()","delete from entrada_etapa",
            "truncate entrada_etapa cascade","delete from adjunto","truncate adjunto cascade",
            "update evento_adjunto set relacion='PRESENTADO'","delete from evento_adjunto","truncate evento_adjunto"})
    void nuevosRegistrosConservanElHistorial(String sentencia) throws SQLException {
        UUID id=adjuntoValido();
        sql("insert into evento_adjunto select id,?,'SUBIDO' from evento_pedido",id);
        // Vacía los eventos diferidos de la FK antes de comprobar el trigger de TRUNCATE.
        sql("set constraints all immediate");
        rechaza("P0001",sentencia);
    }

    @Test void relacionDeRevisionYRelacionDeEvento() throws SQLException {
        assertThat(textos("select numero||':'||revisa_etapa from definicion_etapa where revisa_etapa is not null and flujo_id='01a13cdb-cc00-7000-8000-000000000001'")).containsExactly("3:2");
        rechaza("23514","update definicion_etapa set revisa_etapa=numero where numero=3 and flujo_id='01a13cdb-cc00-7000-8000-000000000001'");
        rechaza("23503","update definicion_etapa set revisa_etapa=30 where numero=3 and flujo_id='01a13cdb-cc00-7000-8000-000000000001'");
        UUID id=adjuntoValido();
        rechaza("23514","insert into evento_adjunto select id,?,'OTRA' from evento_pedido",id);
        rechaza("23503","insert into evento_adjunto select id,?,'SUBIDO' from evento_pedido",UUID.randomUUID());
        sql("insert into evento_adjunto select id,?,'SUBIDO' from evento_pedido",id);
        rechaza("23505","insert into evento_adjunto select id,?,'SUBIDO' from evento_pedido",id);
    }
}
