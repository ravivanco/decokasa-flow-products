package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarga;
import com.decokasa.seguimiento.compartido.domain.*;
import com.jayway.jsonpath.JsonPath;
import java.nio.file.*;
import java.util.*;
import java.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties={"spring.config.import=","decokasa.semilla.habilitada=false",
    "decokasa.google.oauth.habilitada=false","decokasa.google.drive.habilitada=false",
    "decokasa.google.gmail.habilitada=false","decokasa.google.drive.worker.automatico=false",
    "decokasa.google.gmail.worker.automatico=false"})
@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CargasB1IT {
    static Path DISCO=disco();
    static Path disco() {try{return Files.createTempDirectory("decokasa-b1-cargas-");}catch(Exception e){throw new IllegalStateException(e);}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        DISCO=disco();
        r.add("decokasa.archivos.directorio",()->DISCO.toString());
        r.add("decokasa.archivos.tamano-maximo",()->"100MB");
    }
    @Autowired MockMvc mvc; @Autowired JdbcTemplate sql; @Autowired GestionarCarga cargas; @Autowired org.springframework.context.ApplicationContext contexto;
    UUID admin,pedido,entrada;
    RequestPostProcessor token(UUID id) {return jwt().jwt(j->j.subject(id.toString()).claim("roles",List.of("admin")));}
    @BeforeEach void datos() throws Exception {
        admin=UUID.randomUUID();String suf=admin.toString().replace("-","");
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Admin B1',?,?,?,'hash-ficticio',now(),now(),0)",admin,suf+"@ejemplo.test",suf,"+"+suf.replaceAll("[a-f]","1").substring(0,14));
        sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo='admin'",admin);
        String cuerpo=mvc.perform(post("/api/v1/pedidos").with(token(admin)).contentType("application/json")
            .content("{\"tipoFlujo\":\"A\",\"pais\":\"EC\",\"nombre\":\"Pedido B1\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        pedido=UUID.fromString(JsonPath.read(cuerpo,"$.id"));
        entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=1",UUID.class,pedido);
    }
    UUID iniciar(long tamano,UUID idem,int estado) throws Exception {
        String cuerpo="{\"entradaEsperadaId\":\""+entrada+"\",\"claveIdempotencia\":\""+idem+"\",\"nombre\":\"ficticio.desconocido\",\"tipoDocumento\":\"Ficha\",\"tamanoBytes\":"+tamano+"}";
        var r=mvc.perform(post("/api/v1/pedidos/"+pedido+"/etapas/1/archivos/cargas").with(token(admin)).contentType("application/json").content(cuerpo)).andExpect(status().is(estado)).andReturn();
        return estado==201?UUID.fromString(JsonPath.read(r.getResponse().getContentAsString(),"$.id")):null;
    }
    String base(UUID id) {return "/api/v1/archivos/cargas/"+id;}
    String sha(byte[] b) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    ResultActions bloque(UUID id,long inicio,long total,byte[] bytes,String hash) throws Exception {
        return mvc.perform(put(base(id)+"/fragmentos").with(token(admin)).contentType("application/octet-stream")
            .header("Content-Range","bytes "+inicio+"-"+(inicio+bytes.length-1)+"/"+total).header("X-Fragmento-SHA256",hash).content(bytes));
    }
    @Test void exactamente100MiBEn25BloquesConBytesRealesYAsociacionCompleta() throws Exception {
        UUID id=iniciar(104857600,UUID.randomUUID(),201);byte[] bytes=new byte[4194304];Arrays.fill(bytes,(byte)17);String hash=sha(bytes);
        var huellaCompleta=MessageDigest.getInstance("SHA-256");
        for(int n=0;n<25;n++) { huellaCompleta.update(bytes); bloque(id,(long)n*bytes.length,104857600,bytes,hash).andExpect(status().isOk())
            .andExpect(jsonPath("$.offsetConfirmado").value((long)(n+1)*bytes.length)); }
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        var r=mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.tamanoBytes").value(104857600)).andExpect(jsonPath("$.estadoCopia").value("PENDIENTE"))
            .andReturn().getResponse().getContentAsString();
        UUID adj=UUID.fromString(JsonPath.read(r,"$.id")),evento=UUID.fromString(JsonPath.read(r,"$.eventoId"));
        var a=sql.queryForMap("select * from adjunto where id=?",adj);
        assertThat(a).containsEntry("pedido_id",pedido).containsEntry("etapa",1).containsEntry("entrada_id",entrada);
        assertThat(Files.size(DISCO.resolve((String)a.get("clave_local")))).isEqualTo(104857600);
        assertThat(sql.queryForMap("select estado,bytes_comprometidos,propietario_id from archivo_cuota where adjunto_id=?",adj))
            .containsEntry("estado","CONFIRMADA").containsEntry("bytes_comprometidos",104857600L).containsEntry("propietario_id",admin);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota",Long.class)).isEqualTo(1);
        assertThat(a.get("sha256")).isEqualTo(HexFormat.of().formatHex(huellaCompleta.digest()));
        assertThat(sql.queryForObject("select evento_id from evento_adjunto where adjunto_id=? and relacion='SUBIDO'",UUID.class,adj)).isEqualTo(evento);
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(adj.toString()));
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isEqualTo(1);
    }
    @Test void mayorA100MiBSeRechazaAntesDeCrearSesion() throws Exception {
        iniciar(104857601,UUID.randomUUID(),413);
        assertThat(sql.queryForObject("select count(*) from carga_archivo where pedido_id=?",Long.class,pedido)).isZero();
    }
    @Test void pausaReanudaConIdempotenciaYParcialNoCuentaComoAdjunto() throws Exception {
        UUID idem=UUID.randomUUID(),id=iniciar(3,idem,201);byte[] b={1,2,3};
        assertThat(iniciar(3,idem,201)).isEqualTo(id);
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isConflict());
        bloque(id,0,3,b,sha(b)).andExpect(status().isOk());
        mvc.perform(get(base(id)).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.offsetConfirmado").value(3))
            .andExpect(jsonPath("$.adjuntoId").isEmpty()).andExpect(jsonPath("$.eventoId").isEmpty());
        bloque(id,0,3,b,sha(b)).andExpect(status().isOk());
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(sql.queryForObject("select sum(bytes_comprometidos) from archivo_cuota where propietario_id=?",Long.class,admin)).isEqualTo(3);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where estado='RESERVADA'",Long.class)).isEqualTo(1);
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isOk());
        assertThat(sql.queryForObject("select sum(bytes_comprometidos) from archivo_cuota where propietario_id=?",Long.class,admin)).isEqualTo(3);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where estado='RESERVADA'",Long.class)).isZero();
    }
    @Test void rechazaHashIncorrectoHuecoTotalDistintoYReplayAlterado() throws Exception {
        UUID id=iniciar(3,UUID.randomUUID(),201);byte[] b={1,2,3};
        bloque(id,0,3,b,"0".repeat(64)).andExpect(status().isConflict());
        bloque(id,1,3,new byte[]{1,2},sha(new byte[]{1,2})).andExpect(status().isConflict());
        bloque(id,0,4,b,sha(b)).andExpect(status().isConflict());
        bloque(id,0,3,b,sha(b)).andExpect(status().isOk());
        bloque(id,0,3,new byte[]{3,2,1},sha(new byte[]{3,2,1})).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"bytes 0-2/*","bytes 2-0/3","bytes 0-9/3","bytes -1-1/3","bytes 0-2/9999999999"})
    void rechazaRangosMalformados(String rango) throws Exception {
        UUID id=iniciar(3,UUID.randomUUID(),201);
        mvc.perform(put(base(id)+"/fragmentos").with(token(admin)).contentType("application/octet-stream")
            .header("Content-Range",rango).header("X-Fragmento-SHA256",sha(new byte[]{1,2,3})).content(new byte[]{1,2,3})).andExpect(status().isConflict());
    }
    @Test void permisoSeReleeAlCompletarAunqueElJwtAunDigaAdmin() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);bloque(id,0,1,new byte[]{1},sha(new byte[]{1})).andExpect(status().isOk());
        sql.update("update usuario set activo=false where id=?",admin);
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isForbidden());
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(DISCO.resolve(".cargas/"+id+".parte")).exists();
        iniciar(1,UUID.randomUUID(),403);
    }
    @Test void otraCuentaNoConsultaNiCompletaUnaCargaAjena() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);
        mvc.perform(get(base(id)).with(token(UUID.randomUUID()))).andExpect(status().isNotFound());
        mvc.perform(post(base(id)+"/finalizacion").with(token(UUID.randomUUID()))).andExpect(status().isNotFound());
    }
    @Test void entradaObsoletaNoPermiteIniciar() {
        var s=new Solicitante(admin,Set.of(new CodigoRol("admin")));
        assertThatThrownBy(()->cargas.iniciar(s,new GestionarCarga.Inicio(pedido,1,UUID.randomUUID(),UUID.randomUUID(),"f","Ficha",1))).isInstanceOf(ErrorDeDominio.class);
    }

    @Test void corrupcionDelParcialImpideFinalizarYConservaBytes() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);bloque(id,0,1,new byte[]{1},sha(new byte[]{1})).andExpect(status().isOk());
        Path parcial=DISCO.resolve(".cargas/"+id+".parte");Files.write(parcial,new byte[]{9});
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isServiceUnavailable());
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(Files.readAllBytes(parcial)).containsExactly(9);
    }
    @Test void cambioDeEntradaEntreRecepcionYFinalizacionNoAsociaDocumentoALaNueva() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);bloque(id,0,1,new byte[]{1},sha(new byte[]{1})).andExpect(status().isOk());
        UUID nueva=UUID.randomUUID();sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,1,now())",nueva,pedido);
        sql.update("update etapa_pedido set entrada_id=? where pedido_id=? and numero=1",nueva,pedido);
        mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isConflict());
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(DISCO.resolve(".cargas/"+id+".parte")).exists();
    }
    @Test void fragmentoMayorA4MiBNoAdelantaOffset() throws Exception {
        UUID id=iniciar(4194305,UUID.randomUUID(),201);byte[] b=new byte[4194305];
        mvc.perform(put(base(id)+"/fragmentos").with(token(admin)).contentType("application/octet-stream")
            .header("Content-Range","bytes 0-4194303/4194305").header("X-Fragmento-SHA256",sha(b)).content(b)).andExpect(status().is(413));
        mvc.perform(get(base(id)).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.offsetConfirmado").value(0));
    }

    @Test void nuevoCoordinadorRecuperaOffsetSqlYTruncaTailNoConfirmadoSinPerderPrefijo() throws Exception {
        UUID id=iniciar(4194305,UUID.randomUUID(),201);byte[] prefijo=new byte[4194304];Arrays.fill(prefijo,(byte)5);
        bloque(id,0,4194305,prefijo,sha(prefijo)).andExpect(status().isOk());
        Path parcial=DISCO.resolve(".cargas/"+id+".parte");Files.write(parcial,new byte[]{99,99},StandardOpenOption.APPEND);
        try(var reinicio=new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
        reinicio.setParent(contexto);
        reinicio.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource(
            "disco-b1",Map.of("decokasa.archivos.directorio",DISCO.toString())));
        reinicio.registerBean("almacenLocal",com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.class,
            ()->new com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco(DISCO));
        reinicio.register(com.decokasa.seguimiento.archivos.config.CargasConfig.class);
        reinicio.refresh();
        var segundo=reinicio.getBean(GestionarCarga.class);
        var s=new Solicitante(admin,Set.of(new CodigoRol("admin")));
        assertThat(segundo.consultar(s,id).carga().offsetConfirmado()).isEqualTo(4194304);
        segundo.fragmento(s,id,new GestionarCarga.Bloque(4194304,4194305,sha(new byte[]{7}),new byte[]{7}));
        var a=segundo.completar(s,id);
        assertThat(a.adjunto().tamanoBytes()).isEqualTo(4194305);assertThat(a.eventoId()).isNotNull();
        byte[] real=Files.readAllBytes(DISCO.resolve(a.adjunto().claveLocal()));
        assertThat(Arrays.copyOf(real,4194304)).isEqualTo(prefijo);assertThat(real[4194304]).isEqualTo((byte)7);
        assertThat(Files.size(parcial)).isEqualTo(4194305);
        }
    }

    @Test void mismaClaveConComandoDistintoNoSobrescribeLaSesion() throws Exception {
        UUID clave=UUID.randomUUID(),id=iniciar(3,clave,201);iniciar(4,clave,409);
        mvc.perform(get(base(id)).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.tamanoBytes").value(3))
            .andExpect(jsonPath("$.offsetConfirmado").value(0));
    }
    @Test void revocarPermisoAntesDeUnBloqueNoEscribeBytes() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);sql.update("update usuario set activo=false where id=?",admin);
        bloque(id,0,1,new byte[]{1},sha(new byte[]{1})).andExpect(status().isForbidden());
        assertThat(sql.queryForObject("select offset_confirmado from carga_archivo where id=?",Long.class,id)).isZero();
        assertThat(sql.queryForObject("select count(*) from carga_fragmento where carga_id=?",Long.class,id)).isZero();
    }
    @Test void falloAlRegistrarFinalRevierteMetadatosYPermiteRecuperarElArchivoPreparado() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);bloque(id,0,1,new byte[]{7},sha(new byte[]{7})).andExpect(status().isOk());
        var original=contexto.getBean(com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio.class);
        var unaVez=new java.util.concurrent.atomic.AtomicBoolean(true);
        var doble=(com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio)java.lang.reflect.Proxy.newProxyInstance(
            original.getClass().getClassLoader(),new Class<?>[]{com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio.class},
            (proxy,metodo,args)->{
                if(metodo.getName().equals("completar") && unaVez.getAndSet(false)) throw new IllegalStateException("Fallo ficticio de persistencia B1");
                try{return metodo.invoke(original,args);}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
            });
        try(var reinicio=new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
            reinicio.setParent(contexto);
            reinicio.getEnvironment().getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("disco-b1",Map.of("decokasa.archivos.directorio",DISCO.toString())));
            reinicio.registerBean("cargaSqlRepositorio",com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio.class,()->doble);
            reinicio.register(com.decokasa.seguimiento.archivos.config.CargasConfig.class);reinicio.refresh();
            var recuperar=reinicio.getBean(GestionarCarga.class);var s=new Solicitante(admin,Set.of(new CodigoRol("admin")));
            assertThatThrownBy(()->recuperar.completar(s,id)).isInstanceOf(IllegalStateException.class).hasMessage("Fallo ficticio de persistencia B1");
            assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
            assertThat(sql.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ARCHIVO_SUBIDO'",Long.class,pedido)).isZero();
            assertThat(sql.queryForObject("select estado from carga_archivo where id=?",String.class,id)).isEqualTo("RECIBIENDO");
            assertThat(DISCO.resolve(".cargas/"+id+".parte")).exists();
            UUID reservado=sql.queryForObject("select adjunto_reservado_id from carga_archivo where id=?",UUID.class,id);
            Path preparado=DISCO.resolve(pedido+"/1/"+reservado);assertThat(Files.readAllBytes(preparado)).containsExactly(7);
            assertThat(sql.queryForMap("select estado,bytes_comprometidos from archivo_cuota where adjunto_id=?",reservado))
                .containsEntry("estado","RESERVADA").containsEntry("bytes_comprometidos",1L);
            var a=recuperar.completar(s,id);assertThat(a.adjunto().id()).isEqualTo(reservado);
            assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isEqualTo(1);
        }
    }

    UUID adjuntoPequeno() throws Exception {
        UUID id=iniciar(1,UUID.randomUUID(),201);bloque(id,0,1,new byte[]{7},sha(new byte[]{7})).andExpect(status().isOk());
        String r=mvc.perform(post(base(id)+"/finalizacion").with(token(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(r,"$.id"));
    }
    void confirmarCopiaFicticia(UUID id) {
        sql.update("update adjunto set estado_copia='COPIADO',drive_id='id-ficticio',drive_enlace='https://drive.google.com/file/d/ficticio',copiado_en=now() where id=?",id);
    }
    @Test void consultaNoFabricaEnlaceParaIdReservadoYSoloLoEntregaTrasConfirmacion() throws Exception {
        UUID id=adjuntoPequeno();sql.update("update adjunto set drive_id_reservado='id-ficticio' where id=?",id);
        mvc.perform(get("/api/v1/archivos/"+id+"/enlace").with(token(admin))).andExpect(status().isConflict()).andExpect(jsonPath("$.url").doesNotExist());
        confirmarCopiaFicticia(id);
        mvc.perform(get("/api/v1/archivos/"+id+"/enlace").with(token(admin))).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.url").value("https://drive.google.com/file/d/ficticio"));
        mvc.perform(get("/api/v1/archivos/"+id+"/contenido").with(token(admin))).andExpect(status().isOk()).andExpect(content().bytes(new byte[]{7}));
    }
    @Test void consultaDeEnlaceCompruebaPermisoVigenteAunqueJwtConserveAdmin() throws Exception {
        UUID id=adjuntoPequeno();confirmarCopiaFicticia(id);sql.update("update usuario set activo=false where id=?",admin);
        mvc.perform(get("/api/v1/archivos/"+id+"/enlace").with(token(admin))).andExpect(status().isForbidden()).andExpect(jsonPath("$.url").doesNotExist());
    }
    @Test void registroDeCopiasFallidasConservaAsociacionSinClavesPrivadasYExigeAdminVigente() throws Exception {
        UUID id=adjuntoPequeno();sql.update("update adjunto set estado_copia='FALLIDO',ultimo_error='PERMANENTE' where id=?",id);
        String cuerpo=mvc.perform(get("/api/v1/archivos/copias-fallidas").with(token(admin))).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        List<Map<String,Object>> lista=JsonPath.read(cuerpo,"$");
        assertThat(lista).anySatisfy(f->{assertThat(f).containsEntry("id",id.toString()).containsEntry("pedidoId",pedido.toString()).containsEntry("entradaId",entrada.toString()).containsEntry("estadoCopia","FALLIDO");
            assertThat(f).doesNotContainKeys("claveLocal","driveSesionCifrada","sha256");});
        mvc.perform(get("/api/v1/archivos/copias-fallidas?tamano=101").with(token(admin))).andExpect(status().isBadRequest());
        sql.update("delete from usuario_rol where usuario_id=?",admin);
        mvc.perform(get("/api/v1/archivos/copias-fallidas").with(token(admin))).andExpect(status().isForbidden());
    }

    @Test void cupoHttpEsAtomicoYFinalizarPermiteReintentarInicioRechazado() throws Exception {
        UUID a=iniciar(1,UUID.randomUUID(),201),segundo=iniciar(1,UUID.randomUUID(),201),clave=UUID.randomUUID();
        String cuerpo="{\"entradaEsperadaId\":\""+entrada+"\",\"claveIdempotencia\":\""+clave+"\",\"nombre\":\"ficticio.bin\",\"tipoDocumento\":\"Ficha\",\"tamanoBytes\":1}";
        mvc.perform(post("/api/v1/pedidos/"+pedido+"/etapas/1/archivos/cargas").with(token(admin)).contentType("application/json").content(cuerpo))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CUOTA_ALMACENAMIENTO"));
        assertThat(sql.queryForObject("select count(*) from carga_archivo where pedido_id=?",Long.class,pedido)).isEqualTo(2);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where estado='RESERVADA'",Long.class)).isEqualTo(2);
        bloque(a,0,1,new byte[]{7},sha(new byte[]{7})).andExpect(status().isOk());
        mvc.perform(post(base(a)+"/finalizacion").with(token(admin))).andExpect(status().isOk());
        UUID nuevo=iniciar(1,clave,201);assertThat(nuevo).isNotEqualTo(segundo);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where estado='RESERVADA'",Long.class)).isEqualTo(2);
        assertThat(sql.queryForObject("select sum(bytes_comprometidos) from archivo_cuota",Long.class)).isEqualTo(3);
    }
}
