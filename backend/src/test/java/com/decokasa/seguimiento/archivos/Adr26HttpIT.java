package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.jayway.jsonpath.JsonPath;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Rutas normales + PostgreSQL propio. Ni base manual, ni red Google, ni arnés de integración real. */
@SpringBootTest(properties={"spring.config.import=","decokasa.jwt.secreto=clave-ficticia-exclusiva-adr26-de-mas-de-32-bytes",
    "decokasa.google.oauth.habilitada=false","decokasa.google.drive.habilitada=false",
    "decokasa.google.gmail.habilitada=false","decokasa.google.drive.worker.automatico=false",
    "decokasa.google.gmail.worker.automatico=false","decokasa.ia.habilitada=false","decokasa.ia.borrado.automatico=false"})
@AutoConfigureMockMvc @Import(TestcontainersConfiguration.class)
class Adr26HttpIT {
    static final Path DISCO=disco();
    static final AtomicInteger TELEFONOS=new AtomicInteger(20000000);
    static Path disco(){try{return Files.createTempDirectory("decokasa-adr26-http-");}catch(IOException e){throw new UncheckedIOException(e);}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",()->DISCO.toString());}
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate sql;
    @Autowired PlatformTransactionManager gestor;
    @MockitoBean AlmacenArchivos drive;
    UUID admin,pedido,entrada;
    RequestPostProcessor token(UUID id){return jwt().jwt(j->j.subject(id.toString()).claim("roles",List.of("admin")))
        .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROL_admin"));}
    UUID persona(String rol){
        UUID id=UUID.randomUUID();String suf=id.toString().replace("-","");
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Persona ficticia ADR26',?,?,?,'hash-ficticio',now(),now(),0)",id,suf+"@ejemplo.test",suf,"+5939"+TELEFONOS.incrementAndGet());
        sql.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo=?",id,rol);
        sql.update("insert into usuario_pais(usuario_id,pais_codigo) values (?,'EC')",id);return id;
    }
    @BeforeEach void preparar() throws Exception {
        admin=persona("admin");
        String json=mvc.perform(post("/api/v1/pedidos").with(token(admin)).contentType("application/json")
            .content("{\"nombre\":\"Pedido ficticio ADR26\",\"tipoFlujo\":\"A\",\"pais\":\"EC\"}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        pedido=UUID.fromString(JsonPath.read(json,"$.id"));
        entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=1",UUID.class,pedido);
        clearInvocations(drive);
    }
    byte[] fixture(String nombre)throws IOException{
        try(var in=getClass().getResourceAsStream("/formatos-adr26/"+nombre)){return Objects.requireNonNull(in,nombre).readAllBytes();}
    }
    String ruta(){return "/api/v1/pedidos/"+pedido+"/etapas/1/archivos";}
    String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    ResultActions simple(UUID actor,String recurso,String nombre,String tipo)throws Exception{
        return mvc.perform(multipart(ruta()).file(new MockMultipartFile("archivo",nombre,tipo,fixture(recurso)))
            .param("tipoDocumento","Ficha").param("entradaEsperadaId",entrada.toString()).with(token(actor)));
    }
    UUID iniciar(String nombre,long bytes,int status)throws Exception{
        String json="{\"nombre\":\""+nombre+"\",\"tipoDocumento\":\"Ficha\",\"entradaEsperadaId\":\""+entrada+"\",\"claveIdempotencia\":\""+UUID.randomUUID()+"\",\"tamanoBytes\":"+bytes+"}";
        var result=mvc.perform(post(ruta()+"/cargas").with(token(admin)).contentType("application/json").content(json))
            .andExpect(status().is(status)).andReturn();
        return status==201?UUID.fromString(JsonPath.read(result.getResponse().getContentAsString(),"$.id")):null;
    }
    UUID recibir(String recurso,String nombre)throws Exception{
        byte[] bytes=fixture(recurso);UUID id=iniciar(nombre,bytes.length,201);
        mvc.perform(put("/api/v1/archivos/cargas/"+id+"/fragmentos").with(token(admin))
            .contentType("application/octet-stream").header("Content-Range","bytes 0-"+(bytes.length-1)+"/"+bytes.length)
            .header("X-Fragmento-SHA256",sha(bytes)).content(bytes)).andExpect(status().isOk());return id;
    }
    static Stream<Arguments> validos(){return FormatosAdr26Test.validos().filter(a->!a.get()[0].equals("pdf-con-prefijo.pdf"));}
    void comprobarAdjunto(String json,String mime,byte[] bytes)throws Exception{
        UUID id=UUID.fromString(JsonPath.read(json,"$.id"));
        var fila=sql.queryForMap("select tipo_detectado,clave_local,sha256 from adjunto where id=?",id);
        assertThat(fila.get("tipo_detectado")).isEqualTo(mime);
        assertThat(fila.get("sha256")).isEqualTo(sha(bytes));
        assertThat(Files.readAllBytes(DISCO.resolve((String)fila.get("clave_local")))).containsExactly(bytes);
        assertThat(sql.queryForObject("select count(*) from evento_adjunto where adjunto_id=? and relacion='SUBIDO'",Long.class,id)).isEqualTo(1);
        verifyNoInteractions(drive);
    }
    @ParameterizedTest @MethodSource("validos")
    void subidaSimpleAceptaContenidoValidoYGuardaMimeCanonico(String recurso,String nombre,String mime)throws Exception{
        String json=simple(admin,recurso,nombre,mime).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        comprobarAdjunto(json,mime,fixture(recurso));
    }
    @ParameterizedTest @MethodSource("validos")
    void subidaReanudableValidaSoloAlFinalYFinalizacionEsIdempotente(String recurso,String nombre,String mime)throws Exception{
        UUID id=recibir(recurso,nombre);
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        String url="/api/v1/archivos/cargas/"+id+"/finalizacion";
        String json=mvc.perform(post(url).with(token(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        comprobarAdjunto(json,mime,fixture(recurso));
        mvc.perform(post(url).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value((String)JsonPath.read(json,"$.id")));
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isEqualTo(1);
    }
    static Stream<Arguments> rechazos(){return Stream.of(Arguments.of("valido.pdf","falso.csv"),
        Arguments.of("zip-sin-libro.zip","falso.xlsx"),Arguments.of("macros.xlsx","falso.xlsx"));}
    static Stream<Arguments> renombrados(){return Stream.of(Arguments.of("no-excel.ole","falso.xls"),
        Arguments.of("no-excel.ole","falso.xlsx"),Arguments.of("valido.xls","falso.xlsx"),
        Arguments.of("pdf-con-prefijo.pdf","falso.csv"),Arguments.of("marca-sin-libro.xlsx","falso.xlsx"),
        Arguments.of("xml-malformado.xlsx","falso.xlsx"),Arguments.of("entidad-externa.xlsx","falso.xlsx"),
        // Revalidación 2026-10-26.
        Arguments.of("word-real.doc","falso.xls"),Arguments.of("word-real.doc","falso.xlsx"),
        Arguments.of("almacen-workbook.xls","falso.xls"),Arguments.of("vba-sin-declarar.xlsx","falso.xlsx"),
        Arguments.of("excel-real.xlsm","falso.xlsx"),Arguments.of("excel-cifrado.xls","falso.xlsx"));}
    @ParameterizedTest @MethodSource("renombrados")
    // Cuando se corrija el detector, cada rechazo retendrá su reserva. Otro contenedor/contexto
    // para la siguiente invocación evita agotar la cuota global con residuos de otros casos.
    @DirtiesContext(methodMode=DirtiesContext.MethodMode.AFTER_METHOD)
    void formatoRenombradoNoDebeConfirmarsePorLaApi(String recurso,String nombre)throws Exception{
        var resultado=simple(admin,recurso,nombre,"application/octet-stream").andReturn().getResponse();
        long adjuntos=sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido);
        System.out.printf("ADR26 formato=%s nombre=%s HTTP=%d adjuntos_confirmados=%d%n",recurso,nombre,resultado.getStatus(),adjuntos);
        org.junit.jupiter.api.Assertions.assertAll(
            ()->assertThat(resultado.getStatus()).isEqualTo(415),
            ()->assertThat(adjuntos).isZero());
        verifyNoInteractions(drive);
    }
    @ParameterizedTest @MethodSource("renombrados")
    @DirtiesContext(methodMode=DirtiesContext.MethodMode.AFTER_METHOD)
    void formatoRenombradoNoDebeConfirmarsePorFinalizacionReanudable(String recurso,String nombre)throws Exception{
        UUID id=recibir(recurso,nombre);
        var resultado=mvc.perform(post("/api/v1/archivos/cargas/"+id+"/finalizacion").with(token(admin))).andReturn().getResponse();
        long adjuntos=sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido);
        System.out.printf("ADR26 reanudable formato=%s HTTP=%d adjuntos_confirmados=%d%n",recurso,resultado.getStatus(),adjuntos);
        org.junit.jupiter.api.Assertions.assertAll(
            ()->assertThat(resultado.getStatus()).isEqualTo(415),
            ()->assertThat(adjuntos).isZero());
        verifyNoInteractions(drive);
    }
    @ParameterizedTest @MethodSource("rechazos")
    void rechazoSimpleNoConfirmaAdjuntoNiAbandonaTemporalPeroRetieneReserva(String recurso,String nombre)throws Exception{
        String json=simple(admin,recurso,nombre,"application/octet-stream").andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.codigo").value("TIPO_ARCHIVO_NO_ACEPTADO")).andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("Exception",DISCO.toString(),"java.");
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where propietario_id=? and estado='RESERVADA'",Long.class,admin)).isEqualTo(1);
        try(var archivos=Files.walk(DISCO.resolve(".temporales"))){assertThat(archivos.filter(Files::isRegularFile).count()).isZero();}
        verifyNoInteractions(drive);
    }
    @ParameterizedTest @MethodSource("rechazos")
    void rechazoReanudableConservaParcialYReservaAsociadosSinAdjunto(String recurso,String nombre)throws Exception{
        UUID id=recibir(recurso,nombre);
        mvc.perform(post("/api/v1/archivos/cargas/"+id+"/finalizacion").with(token(admin)))
            .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.codigo").value("TIPO_ARCHIVO_NO_ACEPTADO"));
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
        assertThat(sql.queryForObject("select estado from carga_archivo where id=?",String.class,id)).isEqualTo("RECIBIENDO");
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where propietario_id=? and estado='RESERVADA'",Long.class,admin)).isEqualTo(1);
        try(var archivos=Files.walk(DISCO.resolve(".temporales"))){assertThat(archivos.filter(Files::isRegularFile).count()).isZero();}
        verifyNoInteractions(drive);
    }
    @Test void extensionYTipoDeclaradoSeRechazanAntesDeReservar()throws Exception{
        simple(admin,"valido.pdf","prohibido.doc","application/pdf").andExpect(status().isUnsupportedMediaType());
        simple(admin,"valido.pdf","valido.pdf","image/png").andExpect(status().isUnsupportedMediaType());
        iniciar("prohibido.txt",1,415);
        assertThat(sql.queryForObject("select count(*) from archivo_cuota where propietario_id=?",Long.class,admin)).isZero();
    }
    // Revalidación 2026-10-26: tipos declarados habituales (Windows declara CSV como Excel) y extensiones en mayúsculas.
    static Stream<Arguments> mimeHabituales(){return Stream.of(Arguments.of("utf8.csv","DATOS.CSV","application/vnd.ms-excel","text/csv"),
        Arguments.of("cp1252.csv","datos.csv","text/plain; charset=windows-1252","text/csv"),
        Arguments.of("excel-real.xlsx","LIBRO.XLSX","application/x-zip-compressed","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        Arguments.of("excel-real.xls","Libro.Xls","application/msexcel","application/vnd.ms-excel"),
        Arguments.of("valido.jpg","FOTO.JPEG","image/pjpeg","image/jpeg"),Arguments.of("valido.png","FOTO.PNG","image/x-png","image/png"),
        Arguments.of("valido.pdf","ACTA.PDF","application/x-pdf","application/pdf"),Arguments.of("valido.pdf","acta.pdf","","application/pdf"));}
    @ParameterizedTest @MethodSource("mimeHabituales")
    void tiposDeclaradosHabitualesYMayusculasSeAceptanConMimeCanonico(String recurso,String nombre,String declarado,String mime)throws Exception{
        String json=simple(admin,recurso,nombre,declarado).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        comprobarAdjunto(json,mime,fixture(recurso));
    }
    @Test void subidaSimpleDeExactamente100MiBSeAceptaYUnByteMasSeRechaza()throws Exception{
        byte[] bytes=new byte[104857600];Arrays.fill(bytes,(byte)'x');
        for(int i=79;i<bytes.length;i+=80)bytes[i]='\n';
        String json=mvc.perform(multipart(ruta()).file(new MockMultipartFile("archivo","limite.csv","text/csv",bytes))
            .param("tipoDocumento","Ficha").param("entradaEsperadaId",entrada.toString()).with(token(admin)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.tamanoBytes").value(104857600)).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(JsonPath.read(json,"$.id"));
        assertThat(Files.size(DISCO.resolve(sql.queryForObject("select clave_local from adjunto where id=?",String.class,id)))).isEqualTo(104857600);
        byte[] mas=Arrays.copyOf(bytes,104857601);mas[104857600]='\n';bytes=null;
        mvc.perform(multipart(ruta()).file(new MockMultipartFile("archivo","excede.csv","text/csv",mas))
            .param("tipoDocumento","Ficha").param("entradaEsperadaId",entrada.toString()).with(token(admin)))
            .andExpect(status().isContentTooLarge()).andExpect(jsonPath("$.codigo").value("ARCHIVO_DEMASIADO_GRANDE"));
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isEqualTo(1);
        try(var archivos=Files.walk(DISCO.resolve(".temporales"))){assertThat(archivos.filter(Files::isRegularFile).count()).isZero();}
        verifyNoInteractions(drive);
    }
    @Test void excesoDeTamanoSeRechazaSinCrearSesion()throws Exception{
        iniciar("grande.csv",104857601,413);
        assertThat(sql.queryForObject("select count(*) from carga_archivo where pedido_id=?",Long.class,pedido)).isZero();
    }
    @Test void permisosVigentesSeAplicanASimpleBloqueYFinalizacion()throws Exception{
        UUID consulta=persona("consulta");simple(consulta,"valido.pdf","valido.pdf","application/pdf").andExpect(status().isForbidden());
        UUID id=recibir("utf8.csv","valido.csv");
        sql.update("delete from usuario_pais where usuario_id=?",admin);
        mvc.perform(post("/api/v1/archivos/cargas/"+id+"/finalizacion").with(token(admin))).andExpect(status().isNotFound());
        mvc.perform(put("/api/v1/archivos/cargas/"+id+"/fragmentos").with(token(admin)).contentType("application/octet-stream")
            .header("Content-Range","bytes 0-0/1").header("X-Fragmento-SHA256","a".repeat(64)).content(new byte[]{97}))
            .andExpect(status().isNotFound());
        assertThat(sql.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,pedido)).isZero();
    }
    String alta(String correo)throws Exception{
        String suf=UUID.randomUUID().toString().replace("-","");
        return "{\"nombre\":\"Usuario ficticio ADR26\",\"correo\":\""+correo+"\",\"usuario\":\"u"+suf+"\",\"telefono\":\"+5939"+TELEFONOS.incrementAndGet()+"\",\"roles\":[\"logistica\"],\"paises\":[\"EC\"],\"contrasenaInicial\":\"Ficticia-ADR26-123\"}";
    }
    @Test void altaPedidoInsertaIntencionPendienteEnLaMismaTransaccionSinProveedor()throws Exception{
        assertThat(sql.queryForMap("select estado,origen,intentos,drive_id from carpeta_pedido_drive where pedido_id=?",pedido))
            .containsEntry("estado","PENDIENTE").containsEntry("origen","ALTA").containsEntry("intentos",0).containsEntry("drive_id",null);
        String json=mvc.perform(post("/api/v1/usuarios").with(token(admin)).contentType("application/json")
            .content(alta(UUID.randomUUID()+"@ejemplo.test"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        UUID id=UUID.fromString(JsonPath.read(json,"$.id"));
        assertThat(sql.queryForObject("select count(*) from carpeta_usuario_drive where usuario_id=?",Long.class,id)).isZero();
        verifyNoInteractions(drive);
    }
    String nuevoPedido(String nombre){return "{\"nombre\":\""+nombre+"\",\"tipoFlujo\":\"A\",\"pais\":\"EC\"}";}
    @Test void falloAlInsertarIntencionRevierteAltaCompleta()throws Exception{
        String nombre="Rollback ficticio "+UUID.randomUUID();
        sql.execute("create function fallo_adr27() returns trigger language plpgsql as $$ begin raise exception 'detalle-interno-ficticio'; end; $$");
        sql.execute("create trigger fallo_adr27 before insert on carpeta_pedido_drive for each row execute function fallo_adr27()");
        try{
            String json=mvc.perform(post("/api/v1/pedidos").with(token(admin)).contentType("application/json").content(nuevoPedido(nombre)))
                .andExpect(status().isInternalServerError()).andReturn().getResponse().getContentAsString();
            assertThat(json).doesNotContain("detalle-interno-ficticio","insert into","SQLException");
            assertThat(sql.queryForObject("select count(*) from pedido where nombre=?",Long.class,nombre)).isZero();
            verifyNoInteractions(drive);
        }finally{sql.execute("drop trigger fallo_adr27 on carpeta_pedido_drive");sql.execute("drop function fallo_adr27()");}
    }
    @Test void rollbackExteriorReviertePedidoEIntencion()throws Exception{
        String nombre="Rollback exterior ficticio "+UUID.randomUUID();
        new TransactionTemplate(gestor).executeWithoutResult(tx->{try{
            mvc.perform(post("/api/v1/pedidos").with(token(admin)).contentType("application/json").content(nuevoPedido(nombre))).andExpect(status().isCreated());
            assertThat(sql.queryForObject("select count(*) from carpeta_pedido_drive c join pedido p on p.id=c.pedido_id where p.nombre=?",Long.class,nombre)).isEqualTo(1);
            tx.setRollbackOnly();
        }catch(Exception e){throw new RuntimeException(e);}});
        assertThat(sql.queryForObject("select count(*) from pedido where nombre=?",Long.class,nombre)).isZero();verifyNoInteractions(drive);
    }
    @Test void estadoProgramacionIdempotenteYRevocacionDeAdminSinIdsPrivados()throws Exception{
        String url="/api/v1/pedidos/"+pedido+"/carpeta-drive";
        mvc.perform(get(url).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        for(int i=0;i<2;i++)mvc.perform(post(url).with(token(admin))).andExpect(status().isAccepted()).andExpect(jsonPath("$.estado").value("PENDIENTE"));
        assertThat(sql.queryForObject("select count(*) from carpeta_pedido_drive where pedido_id=?",Long.class,pedido)).isEqualTo(1);
        sql.update("update carpeta_pedido_drive set estado='FALLIDA',intentos=4,ultimo_error='CUOTA' where pedido_id=?",pedido);
        mvc.perform(get(url).with(token(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("FALLIDA")).andExpect(jsonPath("$.intentos").value(4));
        mvc.perform(post(url).with(token(admin))).andExpect(status().isAccepted()).andExpect(jsonPath("$.estado").value("PENDIENTE"))
            .andExpect(jsonPath("$.intentos").value(0)).andExpect(jsonPath("$.ultimoError").doesNotExist());
        assertThat(sql.queryForObject("select count(*) from carpeta_pedido_drive where pedido_id=?",Long.class,pedido)).isEqualTo(1);
        String json=mvc.perform(get(url).with(token(admin))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(json).doesNotContain("driveId","drive_id","https://","reclamado","clave");
        sql.update("delete from usuario_rol where usuario_id=?",admin);
        for(var request:List.of(get(url),post(url))){
            String respuesta=mvc.perform(request.with(token(admin))).andExpect(status().isForbidden()).andReturn().getResponse().getContentAsString();
            assertThat(respuesta).doesNotContain("driveId","https://");
        }
        verifyNoInteractions(drive);
    }
}
