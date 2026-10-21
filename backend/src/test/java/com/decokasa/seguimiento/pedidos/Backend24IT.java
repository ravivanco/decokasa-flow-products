package com.decokasa.seguimiento.pedidos;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.compartido.domain.*;
import com.jayway.jsonpath.JsonPath;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;

/** Backend real (sin navegador): SQL y disco aislados; cada caso crea sus propios datos ficticios.
 * JWT de prueba con roles deliberadamente obsoletos: la autorización debe releer la base. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties={"spring.config.import=", "decokasa.semilla.habilitada=false"})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class Backend24IT {
    static final Path DISCO=crearDisco();
    static Path crearDisco() {
        try { return Files.createTempDirectory("decokasa-backend24-"); }
        catch(IOException e) { throw new UncheckedIOException(e); }
    }
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("decokasa.archivos.directorio", () -> DISCO.toString());
        r.add("decokasa.archivos.tamano-maximo", () -> "1KB");
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SubirArchivo subir;
    @Autowired javax.crypto.SecretKey claveJwt;
    @org.springframework.boot.test.web.server.LocalServerPort int puerto;
    UUID admin;
    @BeforeEach void preparar() { admin=persona("admin"); }
    UUID persona(String rol) {
        UUID id=UUID.randomUUID(); String s=id.toString().replace("-", "");
        jdbc.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) "
                +"values (?,'Persona ficticia',?,?,?,'hash-ficticio',now(),now(),0)",id,s+"@ejemplo.test",s,"+"+s.replaceAll("[a-f]","1").substring(0,14));
        jdbc.update("insert into usuario_rol(usuario_id,rol_id,asignado_en) select ?,id,now() from rol where codigo=?",id,rol);
        return id;
    }
    RequestPostProcessor token(UUID id) {
        return jwt().jwt(j -> j.subject(id.toString()).claim("roles",List.of("admin")));
    }
    String respuesta(RequestBuilder r,int estado) throws Exception {
        return mvc.perform(r).andExpect(status().is(estado)).andReturn().getResponse().getContentAsString();
    }
    String json(MockHttpServletRequestBuilder r,UUID actor,String cuerpo,int estado) throws Exception {
        return respuesta(r.with(token(actor)).contentType("application/json").content(cuerpo),estado);
    }
    UUID crear() throws Exception {
        var r=mvc.perform(post("/api/v1/pedidos").with(token(admin)).contentType("application/json")
                .content("{\"tipoFlujo\":\"A\",\"pais\":\"EC\",\"nombre\":\"Pedido ficticio\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        UUID id=UUID.fromString(JsonPath.read(r.getResponse().getContentAsString(),"$.id"));
        assertThat(r.getResponse().getHeader("Location")).isEqualTo("/api/v1/pedidos/"+id);
        return id;
    }
    String base(UUID p) { return "/api/v1/pedidos/"+p; }
    UUID entrada(UUID p,int n) {
        return jdbc.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=?",UUID.class,p,n);
    }
    UUID archivo(UUID actor,UUID p,int n,String tipo,UUID entrada,byte[] bytes,int estado) throws Exception {
        String s=respuesta(multipart(base(p)+"/etapas/"+n+"/archivos")
                .file(new MockMultipartFile("archivo","../../ficticio.pdf","text/html",bytes))
                .param("tipoDocumento",tipo).param("entradaEsperadaId",entrada.toString()).with(token(actor)),estado);
        return estado==201 ? UUID.fromString(JsonPath.read(s,"$.id")) : null;
    }
    UUID archivo(UUID p,int n) throws Exception {
        return archivo(admin,p,n,n==1?"Solicitud":"Documentos codificados",entrada(p,n),"%PDF-ficticio".getBytes(java.nio.charset.StandardCharsets.UTF_8),201);
    }
    String accion(UUID actor,UUID p,int n,String accion,String extras,int estado) throws Exception {
        return json(post(base(p)+"/acciones"),actor,"{\"accion\":\""+accion+"\",\"etapaEsperada\":"+n+extras+"}",estado);
    }
    void avanzar(UUID p,int n) throws Exception {
        UUID a=archivo(p,n);
        accion(admin,p,n,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",200);
    }
    UUID enEtapa(int n) throws Exception {
        UUID p=crear(); for(int i=1;i<n;i++) {
            if(i<3) avanzar(p,i); else accion(admin,p,i,"APROBAR","",200);
        } return p;
    }
    List<Object> estado(UUID p) {
        return List.of(jdbc.queryForList("select * from pedido where id=?",p),
                jdbc.queryForList("select * from etapa_pedido where pedido_id=? order by numero",p),
                jdbc.queryForList("select * from entrada_etapa where pedido_id=? order by id",p),
                jdbc.queryForList("select * from evento_pedido where pedido_id=? order by id",p),
                jdbc.queryForList("select * from adjunto where pedido_id=? order by id",p),
                jdbc.queryForList("select ea.* from evento_adjunto ea join evento_pedido e on e.id=ea.evento_id where e.pedido_id=? order by ea.evento_id,ea.adjunto_id",p));
    }
    void problema(String s,String codigo) {
        Map<String,Object> cuerpo=JsonPath.read(s,"$");
        assertThat(cuerpo).containsEntry("codigo",codigo);
    }
    @Test void consultasContratoYConfiguracion() throws Exception {
        UUID p=crear();
        mvc.perform(get("/api/v1/flujos/EC/A").with(token(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.etapas.length()").value(11));
        respuesta(get("/api/v1/flujos/EC/A?version=1").with(token(admin)),200);
        respuesta(get("/api/v1/flujos/EC/A?version=999").with(token(admin)),404);
        String detalle=respuesta(get(base(p)).with(token(admin)),200);
        assertThat((Integer)JsonPath.read(detalle,"$.etapaActual")).isEqualTo(1);
        assertThat((String)JsonPath.read(detalle,"$.lineaDeTiempo[0].estadoPlazo")).isEqualTo("SIN_PLAZO");
        assertThat((String)JsonPath.read(detalle,"$.lineaDeTiempo[4].estadoPlazo")).isEqualTo("PENDIENTE");
        assertThat((Boolean)JsonPath.read(detalle,"$.accionesDisponibles[0].puedeCompletarse")).isFalse();
        String numero=JsonPath.read(detalle,"$.numero");
        respuesta(get("/api/v1/pedidos/buscar").param("numero",numero).with(token(admin)),200);
        mvc.perform(get("/api/v1/pedidos").param("texto",numero).with(token(admin))).andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));
        respuesta(get(base(p)+"/historial").with(token(admin)),200);
        respuesta(get(base(p)+"/etapas/1/archivos").with(token(admin)),200);
        respuesta(get("/api/v1/pedidos?pagina=-1").with(token(admin)),400);
        respuesta(get("/api/v1/pedidos?tamano=101").with(token(admin)),400);
        respuesta(get("/api/v1/pedidos/"+UUID.randomUUID()).with(token(admin)),404);
        respuesta(get(base(p)),401);
    }
    @Test void bandejaRespetaContratoSinErrorInterno() throws Exception {
        UUID p=enEtapa(2); var antes=estado(p);
        respuesta(get("/api/v1/pedidos/bandeja").with(token(admin)),200);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void rechazosDevolucionPorErrorYA4A5MantienenContrato() throws Exception {
        UUID p=enEtapa(3); var antes=estado(p);
        accion(admin,p,3,"RECHAZAR","",422);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"   \"",422);
        assertThat(estado(p)).isEqualTo(antes);
        accion(admin,p,3,"APROBAR","",200); antes=estado(p);
        accion(admin,p,4,"DEVOLVER_POR_ERROR","",422);
        accion(admin,p,4,"DEVOLVER_POR_PRODUCTO_NUEVO",",\"observacion\":\"Nuevo\"",422);
        assertThat(estado(p)).isEqualTo(antes);
        accion(admin,p,4,"DEVOLVER_POR_ERROR",",\"observacion\":\"Error ficticio\"",200);
        avanzar(p,2); accion(admin,p,3,"APROBAR","",200);
        accion(admin,p,4,"APROBAR","",200); antes=estado(p);
        accion(admin,p,5,"APROBAR",",\"observacion\":\"Fabricacion ficticia terminada\"",200);
        assertThat(jdbc.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p)).isEqualTo(6);
        antes=estado(p);
        accion(admin,p,6,"APROBAR","",409); // Bloqueo documental vigente, no bloqueo de fase en A5.
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void fase4IncidenciasHabilitadaConservaBloqueoDocumental() throws Exception {
        // Fixture SQL explícito en A10: no es un recorrido desde A1 ni omite A6/A9 en producción.
        UUID p=crear(),e=UUID.randomUUID();
        new org.springframework.transaction.support.TransactionTemplate(transaccionesB2).executeWithoutResult(tx->{
            jdbc.update("update etapa_pedido set estado='COMPLETADA',salida_en=now() where pedido_id=? and numero=1",p);
            jdbc.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,10,now())",e,p);
            jdbc.update("update etapa_pedido set estado='ACTUAL',entrada_en=now(),entrada_id=? where pedido_id=? and numero=10",e,p);
            jdbc.update("update pedido set etapa_actual=10 where id=?",p);
        });
        var antes=estado(p);
        assertThat(accion(admin,p,10,"APROBAR","",409)).contains("documentos");
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void resumenDeEtapaIncluyeLoOcurrido() throws Exception {
        UUID p=enEtapa(3);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Corregir códigos ficticios\"",200);
        String s=respuesta(get(base(p)).with(token(admin)),200);
        assertThat((String)JsonPath.read(s,"$.lineaDeTiempo[2].resumen")).isNotBlank();
    }
    @Test void observacionEHistorialConservanElMismoJsonPublicoTrasSepararAdaptadores() throws Exception {
        UUID p=enEtapa(2),a=archivo(p,2);
        String cuerpo="{\"texto\":\"Observación de compatibilidad ficticia\",\"etapaEsperada\":2,\"entradaEsperadaId\":\""
                +entrada(p,2)+"\",\"adjuntoIds\":[\""+a+"\"]}";
        Map<String,Object> creado=JsonPath.read(json(post(base(p)+"/observaciones"),admin,cuerpo,201),"$");
        assertThat(creado.keySet()).containsExactlyInAnyOrder("id","pedidoId","numeroPedido","etapa","tipo",
                "usuario","rolIntervencion","detalle","fecha","observacion","nivelUrgencia","adjuntoIds");
        assertThat(creado).containsEntry("pedidoId",p.toString()).containsEntry("etapa",2)
                .containsEntry("tipo","OBSERVACION_AGREGADA").containsEntry("rolIntervencion","admin")
                .containsEntry("observacion","Observación de compatibilidad ficticia").containsEntry("nivelUrgencia",null)
                .containsEntry("adjuntoIds",List.of(a.toString()));
        assertThat(UUID.fromString((String)creado.get("id"))).isNotNull();
        assertThat(java.time.OffsetDateTime.parse((String)creado.get("fecha")).getOffset())
                .isEqualTo(java.time.ZoneOffset.ofHours(-5));
        assertThat(creado.get("numeroPedido")).isEqualTo(jdbc.queryForObject("select numero from pedido where id=?",String.class,p));
        Map<String,Object> persona=JsonPath.read(creado,"$.usuario");
        assertThat(persona.keySet()).containsExactlyInAnyOrder("id","nombre","roles");
        assertThat(persona).containsEntry("id",admin.toString()).containsEntry("nombre","Persona ficticia")
                .containsEntry("roles",List.of("admin"));
        Map<String,Object> detalle=JsonPath.read(creado,"$.detalle");
        assertThat(detalle.keySet()).containsExactlyInAnyOrder("etapaDestino","motivoDevolucion","urgenciaTerminada","asignadoAnterior","asignadoA","fechaEstimadaAnterior","fechaEstimadaSolicitada","nuevaFechaEstimadaLlegada","bodegaId","claseVencimiento","venceEn");
        assertThat(detalle.values()).containsOnlyNulls();
        var antes=estado(p);
        List<Map<String,Object>> eventos=JsonPath.read(respuesta(get(base(p)+"/historial").with(token(admin)),200),"$");
        assertThat(eventos.stream().filter(e -> creado.get("id").equals(e.get("id")))).containsExactly(creado);
        assertThat(estado(p)).isEqualTo(antes);
    }
    String resumen(UUID p,int numero) throws Exception {
        return JsonPath.read(respuesta(get(base(p)).with(token(admin)),200),
                "$.lineaDeTiempo["+(numero-1)+"].resumen");
    }
    @Test void resumenP20ReentradaConservaUltimoCierreYLoReemplazaAlCerrarDeNuevo() throws Exception {
        UUID p=enEtapa(3);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Primera corrección\"",200);
        String anterior=resumen(p,2);
        assertThat(anterior).startsWith("Entrada anterior: Enviada").contains("pasa a A3","1 archivo presentado.");
        UUID a=archivo(p,2);
        accion(admin,p,2,"ENVIAR",",\"observacion\":\"Segundo envío corregido\",\"adjuntoIds\":[\""+a+"\"]",200);
        String segundo=resumen(p,2);
        assertThat(segundo).startsWith("Enviada").doesNotContain("Entrada anterior:").contains("Segundo envío corregido");
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Segunda corrección\"",200);
        var antes=estado(p);
        assertThat(resumen(p,2)).isEqualTo("Entrada anterior: "+segundo);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void resumenP20EtapaIntermediaConservaSuCierreTrasDevolucionDesdeA4() throws Exception {
        UUID p=enEtapa(4); String revision=resumen(p,3);
        assertThat(revision).startsWith("Aprobada").contains("pasa a A4");
        accion(admin,p,4,"DEVOLVER_POR_ERROR",",\"observacion\":\"Devolución exclusiva de Análisis\"",200);
        var antes=estado(p);
        assertThat(jdbc.queryForObject("select estado from etapa_pedido where pedido_id=? and numero=3",String.class,p)).isEqualTo("DEVUELTA");
        assertThat(resumen(p,3)).isEqualTo(revision).doesNotContain("Devuelta","Devolución exclusiva");
        assertThat(resumen(p,4)).startsWith("Devuelta por error").contains("vuelve a A2","Devolución exclusiva de Análisis");
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void resumenP20RecortaUnicodeSinCambiarObservacionCompletaDelHistorial() throws Exception {
        UUID p=enEtapa(3);
        String primeros="\uD83D\uDE00".repeat(200),observacion=primeros+"FINAL_NO_VISIBLE";
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\""+observacion+"\"",200);
        var antes=estado(p);
        assertThat(resumen(p,3)).contains("Observación: «"+primeros+"…».").doesNotContain("FINAL_NO_VISIBLE","\uFFFD");
        assertThat(jdbc.queryForObject("select observacion from evento_pedido where pedido_id=? and tipo='ETAPA_RECHAZADA'",String.class,p)).isEqualTo(observacion);
        List<Map<String,Object>> historia=JsonPath.read(respuesta(get(base(p)+"/historial").with(token(admin)),200),"$");
        assertThat(historia.stream().filter(e -> "ETAPA_RECHAZADA".equals(e.get("tipo"))).map(e -> e.get("observacion")))
                .containsExactly(observacion);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void resumenP20SinCierreEsNuloAunqueHayaObservacionYArchivo() throws Exception {
        UUID p=crear(); UUID a=archivo(p,1);
        json(post(base(p)+"/observaciones"),admin,"{\"texto\":\"Todavía no se envía\",\"etapaEsperada\":1,\"entradaEsperadaId\":\""+entrada(p,1)+"\",\"adjuntoIds\":[\""+a+"\"]}",201);
        var antes=estado(p);
        assertThat(resumen(p,1)).isNull();
        assertThat(resumen(p,2)).isNull();
        String numero=jdbc.queryForObject("select numero from pedido where id=?",String.class,p);
        String buscado=respuesta(get("/api/v1/pedidos/buscar").param("numero",numero).with(token(admin)),200);
        assertThat((String)JsonPath.read(buscado,"$.lineaDeTiempo[0].resumen")).isNull();
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void resumenP20UsaNombreVigentePeroConservaRolHistoricoYFechaEcuador() throws Exception {
        UUID p=enEtapa(3);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Corregir\"",200);
        jdbc.update("update usuario set nombre='Nombre actualizado ficticio' where id=?",admin);
        var antes=estado(p);
        String texto=resumen(p,3);
        String nombreRol=jdbc.queryForObject("select nombre from rol where codigo='admin'",String.class);
        var fecha=jdbc.queryForObject("select ocurrido_en from evento_pedido where pedido_id=? and tipo='ETAPA_RECHAZADA'",java.sql.Timestamp.class,p).toInstant();
        String esperada=java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(java.time.ZoneId.of("America/Guayaquil")).format(fecha);
        assertThat(texto).contains("por Nombre actualizado ficticio ("+nombreRol+") el "+esperada,"vuelve a A2");
        assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"consulta","asistente_china","marketing","asistente_compras","compras_china","logistica"})
    void rolesVigentesLeenPeroNoCreanConJwtAdmin(String rol) throws Exception {
        UUID u=persona(rol),p=crear();
        respuesta(get(base(p)).with(token(u)),200);
        respuesta(get(base(p)+"/historial").with(token(u)),200);
        json(post("/api/v1/pedidos"),u,"{\"tipoFlujo\":\"A\",\"nombre\":\"No autorizado\"}",403);
        var antes=estado(p);
        problema(accion(u,p,1,"ENVIAR","",403),"SIN_PERMISO");
        assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(booleans={true,false})
    void cuentaDesactivadaOSinRolesNoLeeNiEscribe(boolean desactivar) throws Exception {
        UUID p=crear();
        if(desactivar) jdbc.update("update usuario set activo=false where id=?",admin);
        else jdbc.update("delete from usuario_rol where usuario_id=?",admin);
        var antes=estado(p);
        respuesta(get(base(p)).with(token(admin)),403);
        respuesta(get(base(p)+"/historial").with(token(admin)),403);
        accion(admin,p,1,"ENVIAR","",403);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void requisitosErroresYEtapaDesactualizadaSonAtomicos() throws Exception {
        UUID p=crear(); var antes=estado(p);
        problema(accion(admin,p,1,"ENVIAR","",422),"REQUISITO_FALTANTE");
        assertThat(estado(p)).isEqualTo(antes);
        problema(accion(admin,p,2,"ENVIAR","",409),"ETAPA_CAMBIO");
        assertThat(estado(p)).isEqualTo(antes);
        accion(admin,p,1,"ENVIAR",",\"adjuntoIds\":[\""+UUID.randomUUID()+"\"]",400);
        assertThat(estado(p)).isEqualTo(antes);
        json(post(base(p)+"/acciones"),admin,"{\"accion\":\"INVENTADA\",\"etapaEsperada\":1}",400);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void reasignacionExcluyeOtrosYReleeRolDeAsignado() throws Exception {
        UUID p=enEtapa(2),m=persona("marketing"),otro=persona("marketing"),consulta=persona("consulta");
        var antes=estado(p);
        json(put(base(p)+"/etapas/2/asignacion"),otro,"{\"usuarioId\":\""+m+"\"}",403);
        json(put(base(p)+"/etapas/2/asignacion"),admin,"{\"usuarioId\":\""+consulta+"\"}",400);
        assertThat(estado(p)).isEqualTo(antes);
        json(put(base(p)+"/etapas/2/asignacion"),admin,"{\"usuarioId\":\""+m+"\",\"motivo\":\"Reparto ficticio\"}",200);
        antes=estado(p);
        archivo(otro,p,2,"Documentos codificados",entrada(p,2),new byte[]{1},403);
        assertThat(estado(p)).isEqualTo(antes);
        UUID a=archivo(m,p,2,"Documentos codificados",entrada(p,2),new byte[]{1},201);
        jdbc.update("delete from usuario_rol where usuario_id=?",m);
        antes=estado(p);
        accion(m,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",403);
        accion(otro,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",403);
        assertThat(estado(p)).isEqualTo(antes);
        accion(admin,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",200);
        assertThat(jdbc.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ETAPA_REASIGNADA'",Long.class,p)).isEqualTo(1);
    }
    @Test void observacionesExigenEntradaVigenteYRegistranActorFechaSinAvance() throws Exception {
        UUID p=enEtapa(2),m=persona("marketing"); var antes=estado(p);
        String cuerpo="{\"texto\":\"Observación ficticia\",\"etapaEsperada\":2,\"entradaEsperadaId\":\""+entrada(p,2)+"\"}";
        json(post(base(p)+"/observaciones"),persona("consulta"),cuerpo,403);
        json(post(base(p)+"/observaciones"),m,cuerpo.replace(entrada(p,2).toString(),UUID.randomUUID().toString()),409);
        assertThat(estado(p)).isEqualTo(antes);
        String s=json(post(base(p)+"/observaciones"),m,cuerpo,201);
        assertThat((String)JsonPath.read(s,"$.usuario.id")).isEqualTo(m.toString());
        assertThat((String)JsonPath.read(s,"$.rolIntervencion")).isEqualTo("marketing");
        assertThat((String)JsonPath.read(s,"$.fecha")).isNotBlank();
        assertThat(jdbc.queryForObject("select etapa_actual from pedido where id=?",Integer.class,p)).isEqualTo(2);
        jdbc.update("delete from usuario_rol where usuario_id=?",m);
        String h=respuesta(get(base(p)+"/historial").with(token(admin)),200);
        assertThat(h).contains("OBSERVACION_AGREGADA","marketing","Observación ficticia");
    }
    @Test void reentradaNoReutilizaDocumentosNiEntradaAnterior() throws Exception {
        UUID p=enEtapa(2),vieja=entrada(p,2),a=archivo(p,2);
        accion(admin,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",200);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Corregir\"",200);
        assertThat(entrada(p,2)).isNotEqualTo(vieja); var antes=estado(p);
        archivo(admin,p,2,"Documentos codificados",vieja,new byte[]{1},409);
        accion(admin,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",400);
        accion(admin,p,2,"ENVIAR","",422);
        assertThat(estado(p)).isEqualTo(antes);
        avanzar(p,2);
    }
    @ParameterizedTest @ValueSource(strings={"URGENTE","PRIORITARIO","NORMAL"})
    void devolucionConUrgenciaConservaHistorialHastaVolverA4(String nivel) throws Exception {
        UUID p=enEtapa(4);
        accion(admin,p,4,"DEVOLVER_POR_PRODUCTO_NUEVO",",\"observacion\":\"Producto ficticio nuevo\",\"nivelUrgencia\":\""+nivel+"\"",200);
        Object vence=jdbc.queryForObject("select urgencia_vence_en from pedido where id=?",Object.class,p);
        avanzar(p,2);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Corregir de nuevo\"",200);
        assertThat(jdbc.queryForObject("select urgencia_vence_en from pedido where id=?",Object.class,p)).isEqualTo(vence);
        avanzar(p,2); accion(admin,p,3,"APROBAR","",200);
        assertThat(jdbc.queryForObject("select urgencia_nivel from pedido where id=?",String.class,p)).isNull();
        assertThat(respuesta(get(base(p)+"/historial").with(token(admin)),200)).contains("PRODUCTO_NUEVO","urgenciaTerminada");
    }
    @Test void dosAvancesSimultaneosConfirmanUnoSolo() throws Exception {
        UUID p=crear(),a=archivo(p,1);
        var inicio=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        Callable<Integer> trabajo=() -> { inicio.await(10,TimeUnit.SECONDS); return mvc.perform(post(base(p)+"/acciones").with(token(admin))
                .contentType("application/json").content("{\"accion\":\"ENVIAR\",\"etapaEsperada\":1,\"adjuntoIds\":[\""+a+"\"]}"))
                .andReturn().getResponse().getStatus(); };
        try {
            var x=pool.submit(trabajo); var y=pool.submit(trabajo); inicio.countDown();
            assertThat(List.of(x.get(20,TimeUnit.SECONDS),y.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        } finally { pool.shutdownNow(); }
        assertThat(jdbc.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ETAPA_ENVIADA'",Long.class,p)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from etapa_pedido where pedido_id=? and estado='ACTUAL'",Long.class,p)).isEqualTo(1);
    }
    @Test void archivosRealesMetadatosYPermisosDeDescarga() throws Exception {
        UUID p=enEtapa(2),a=archivo(p,2),consulta=persona("consulta"),revisor=persona("asistente_compras"),china=persona("compras_china");
        mvc.perform(get("/api/v1/archivos/"+a+"/contenido").with(token(admin))).andExpect(status().isOk())
                .andExpect(content().bytes("%PDF-ficticio".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .andExpect(header().string("X-Content-Type-Options","nosniff"))
                .andExpect(header().string("Content-Type","application/octet-stream"));
        respuesta(get("/api/v1/archivos/"+a+"/contenido").with(token(consulta)),403);
        respuesta(get("/api/v1/archivos/"+a+"/contenido").with(token(china)),403);
        respuesta(get("/api/v1/archivos/"+a+"/contenido").with(token(revisor)),200);
        var fila=jdbc.queryForMap("select * from adjunto where id=?",a);
        assertThat(fila.get("tamano_bytes")).isEqualTo(13L);
        assertThat(fila.get("tipo_detectado")).isEqualTo("application/pdf");
        assertThat(fila.get("clave_local").toString()).doesNotContain("ficticio.pdf","..");
        assertThat(fila.get("sha256").toString()).hasSize(64);
        assertThat(jdbc.queryForObject("select count(*) from evento_adjunto where adjunto_id=? and relacion='SUBIDO'",Long.class,a)).isEqualTo(1);
    }
    Path ruta(UUID a) { return DISCO.resolve(jdbc.queryForObject("select clave_local from adjunto where id=?",String.class,a)); }
    @ParameterizedTest @ValueSource(strings={"ausente","truncado","corrupto"})
    void descargaRechazaArchivoAusenteTruncadoOCorrupto(String modo) throws Exception {
        UUID p=crear(),a=archivo(p,1); var antes=estado(p);
        if(modo.equals("ausente")) Files.delete(ruta(a));
        else Files.write(ruta(a),new byte[modo.equals("corrupto")?13:2]);
        problema(respuesta(get("/api/v1/archivos/"+a+"/contenido").with(token(admin)),409),"ARCHIVO_NO_DISPONIBLE");
        assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(strings={"ausente","truncado","corrupto"})
    void accionRechazaArchivoAusenteTruncadoOCorruptoSinAvance(String modo) throws Exception {
        UUID p=crear(),a=archivo(p,1); var antes=estado(p);
        if(modo.equals("ausente")) Files.delete(ruta(a));
        else Files.write(ruta(a),new byte[modo.equals("corrupto")?13:2]);
        var r=mvc.perform(post(base(p)+"/acciones").with(token(admin)).contentType("application/json")
                .content("{\"accion\":\"ENVIAR\",\"etapaEsperada\":1,\"adjuntoIds\":[\""+a+"\"]}"))
                .andReturn().getResponse();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> assertThat(r.getStatus()).as("Estado HTTP con archivo %s",modo).isEqualTo(400),
                () -> assertThat(estado(p)).as("Sin avance ni evento al presentar archivo %s",modo).isEqualTo(antes));
    }
    @Test void multipartExigeEntradaYPermisoSinCrearNada() throws Exception {
        UUID p=crear(); var antes=estado(p); long f=numeroArchivos();
        respuesta(multipart(base(p)+"/etapas/1/archivos")
                .file(new MockMultipartFile("archivo","ficticio.bin","application/octet-stream",new byte[]{1}))
                .param("tipoDocumento","Solicitud").with(token(admin)),400);
        archivo(persona("consulta"),p,1,"Solicitud",entrada(p,1),new byte[]{1},403);
        archivo(admin,p,1,"Solicitud",UUID.randomUUID(),new byte[]{1},409);
        assertThat(estado(p)).isEqualTo(antes); assertThat(numeroArchivos()).isEqualTo(f);
    }
    @Test void observacionInvalidaOAdjuntoAjenoNoDejanEvento() throws Exception {
        UUID p=crear(),otro=crear(),a=archivo(otro,1); var antes=estado(p);
        for(String texto:List.of("", " ","x".repeat(4001))) {
            json(post(base(p)+"/observaciones"),admin,"{\"texto\":\""+texto+"\",\"etapaEsperada\":1,\"entradaEsperadaId\":\""+entrada(p,1)+"\"}",400);
            assertThat(estado(p)).isEqualTo(antes);
        }
        json(post(base(p)+"/observaciones"),admin,"{\"texto\":\"Texto ficticio\",\"etapaEsperada\":1,\"entradaEsperadaId\":\""+entrada(p,1)+"\",\"adjuntoIds\":[\""+a+"\"]}",400);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @Test void adjuntosDeOtroPedidoDuplicadosNulosYDeOtraEtapaNoSeIgnoran() throws Exception {
        UUID p=crear(),otro=crear(),ajeno=archivo(otro,1),propio=archivo(p,1); var antes=estado(p);
        for(String ids:List.of("[\""+ajeno+"\"]","[\""+propio+"\",\""+propio+"\"]","[null]","[\"no-uuid\"]")) {
            accion(admin,p,1,"ENVIAR",",\"adjuntoIds\":"+ids,400); assertThat(estado(p)).isEqualTo(antes);
        }
        accion(admin,p,1,"ENVIAR",",\"adjuntoIds\":[\""+propio+"\"]",200); antes=estado(p);
        accion(admin,p,2,"ENVIAR",",\"adjuntoIds\":[\""+propio+"\"]",400);
        assertThat(estado(p)).isEqualTo(antes);
    }
    @ParameterizedTest @ValueSource(ints={0,1024,1025})
    void vacioLimiteExactoYExcesoSeAislanPorPropietarioSinLiberarReservasFallidas(int tamano) throws Exception {
        UUID p=crear();var antes=estado(p);long f=numeroArchivos();
        UUID a=archivo(admin,p,1,"Solicitud",entrada(p,1),new byte[tamano],tamano==0?400:tamano==1024?201:413);
        if(tamano==1024) {
            assertThat(Files.size(ruta(a))).isEqualTo(1024);
            assertThat(jdbc.queryForObject("select bytes_comprometidos from archivo_cuota where adjunto_id=?",Long.class,a)).isEqualTo(1024);
        } else {
            assertThat(estado(p)).isEqualTo(antes);assertThat(numeroArchivos()).isEqualTo(f);
            assertThat(jdbc.queryForObject("select count(*) from archivo_cuota where propietario_id=? and estado='RESERVADA'",Long.class,admin)).isEqualTo(1);
        }
    }
    @Test void multipartRealAplicaLimiteDelServletConJwtFirmado() throws Exception {
        UUID p=crear();
        var encoder=new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(claveJwt));
        var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("decokasa").subject(admin.toString())
                .issuedAt(java.time.Instant.now()).expiresAt(java.time.Instant.now().plusSeconds(60)).claim("roles",List.of("admin")).build();
        String firmado=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
        var client=java.net.http.HttpClient.newHttpClient();
        for(int tamano:List.of(1024,1025)) {
            var antes=estado(p); long f=numeroArchivos();
            String boundary="frontera-ficticia-b24";
            String inicio="--"+boundary+"\r\nContent-Disposition: form-data; name=\"tipoDocumento\"\r\n\r\nSolicitud\r\n"
                    +"--"+boundary+"\r\nContent-Disposition: form-data; name=\"entradaEsperadaId\"\r\n\r\n"+entrada(p,1)+"\r\n"
                    +"--"+boundary+"\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"ficticio.bin\"\r\nContent-Type: application/octet-stream\r\n\r\n";
            var bytes=new ByteArrayOutputStream(); bytes.write(inicio.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            bytes.write(new byte[tamano]); bytes.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:"+puerto+base(p)+"/etapas/1/archivos"))
                    .timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+firmado)
                    .header("Content-Type","multipart/form-data; boundary="+boundary)
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofByteArray(bytes.toByteArray())).build();
            var r=client.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());
            assertThat(r.statusCode()).as("multipart real de %s bytes: %s",tamano,r.body()).isEqualTo(tamano==1024?201:413);
            if(tamano>1024) {
                org.junit.jupiter.api.Assertions.assertAll(
                        () -> problema(r.body(),"ARCHIVO_DEMASIADO_GRANDE"),
                        () -> assertThat(estado(p)).isEqualTo(antes),
                        () -> assertThat(numeroArchivos()).isEqualTo(f));
            }
        }
    }
    long numeroArchivos() throws IOException { try(var s=Files.walk(DISCO)) { return s.filter(Files::isRegularFile).count(); } }
    @Test void lecturaInterrumpidaNoDejaTemporalAdjuntoNiEvento() throws Exception {
        UUID p=crear(); var antes=estado(p); long f=numeroArchivos();
        InputStream cortado=new InputStream() { int n; public int read() throws IOException { if(++n>7) throw new IOException("Corte ficticio"); return 1; } };
        assertThatThrownBy(() -> subir.subir(new Solicitante(admin,Set.of(new CodigoRol("admin"))),
                new SubirArchivo.Comando(p,1,entrada(p,1),"Solicitud","ficticio.pdf",cortado))).isInstanceOf(UncheckedIOException.class);
        assertThat(estado(p)).isEqualTo(antes); assertThat(numeroArchivos()).isEqualTo(f);
    }
    @Test void subidaRevalidaEntradaDespuesDeRecibirYRevierteSiHuboReentrada() throws Exception {
        UUID p=enEtapa(2),vieja=entrada(p,2),a=archivo(p,2);
        InputStream contenido=new ByteArrayInputStream(new byte[]{1,2,3}) {
            boolean cambio;
            @Override public synchronized int read(byte[] b,int off,int len) {
                if(!cambio) {
                    cambio=true;
                    try {
                        accion(admin,p,2,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",200);
                        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Corregir\"",200);
                    } catch(Exception e) { throw new AssertionError(e); }
                }
                return super.read(b,off,len);
            }
        };
        long f=numeroArchivos();
        long adjuntos=jdbc.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,p);
        assertThatThrownBy(() -> subir.subir(new Solicitante(admin,Set.of(new CodigoRol("admin"))),
                new SubirArchivo.Comando(p,2,vieja,"Documentos codificados","ficticio.pdf",contenido)))
                .isInstanceOf(EtapaCambio.class);
        assertThat(numeroArchivos()).isEqualTo(f+1); // Retained preparation, not a valid attachment.
        assertThat(jdbc.queryForObject("select count(*) from archivo_cuota where propietario_id=? and estado='RESERVADA'",Long.class,admin)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from adjunto where pedido_id=?",Long.class,p)).isEqualTo(adjuntos);
        assertThat(jdbc.queryForObject("select count(*) from evento_pedido where pedido_id=? and tipo='ARCHIVO_SUBIDO'",Long.class,p)).isEqualTo(2);
        assertThat(entrada(p,2)).isNotEqualTo(vieja);
    }
    @Test void errorAlRegistrarRevierteMetadatosYConservaBytesYReserva() throws Exception {
        UUID p=crear(); var antes=estado(p); long f=numeroArchivos();
        // Fallo SQL real al insertar el evento, después de guardar el archivo y el adjunto.
        jdbc.execute("create function fallo_subida_b24() returns trigger language plpgsql as $$ begin raise exception 'Fallo ficticio B24'; end; $$");
        jdbc.execute("create trigger fallo_subida_b24 before insert on evento_pedido for each row when (new.pedido_id='"+p+"'::uuid and new.tipo='ARCHIVO_SUBIDO') execute function fallo_subida_b24()");
        try {
            assertThatThrownBy(() -> subir.subir(new Solicitante(admin,Set.of(new CodigoRol("admin"))),
                    new SubirArchivo.Comando(p,1,entrada(p,1),"Solicitud","ficticio.pdf",new ByteArrayInputStream(new byte[]{1}))))
                    .isInstanceOf(RuntimeException.class).hasStackTraceContaining("Fallo ficticio B24");
        } finally {
            jdbc.execute("drop trigger fallo_subida_b24 on evento_pedido");
            jdbc.execute("drop function fallo_subida_b24()");
        }
        assertThat(estado(p)).isEqualTo(antes); assertThat(numeroArchivos()).isEqualTo(f+1);
        assertThat(jdbc.queryForObject("select count(*) from archivo_cuota where propietario_id=? and estado='RESERVADA'",Long.class,admin)).isEqualTo(1);
    }

    @Test void b2TransicionesRealesEncolanUnaIntencionPorEventoSinInventarDestinatario() throws Exception {
        UUID p=enEtapa(3);
        accion(admin,p,3,"RECHAZAR",",\"observacion\":\"Correccion ficticia B2\"",200);
        avanzar(p,2);accion(admin,p,3,"APROBAR","",200);
        accion(admin,p,4,"DEVOLVER_POR_ERROR",",\"observacion\":\"Devolucion ficticia B2\"",200);
        var filas=jdbc.queryForList("select n.*,e.tipo as tipo_historial from notificacion n join evento_pedido e on e.id=n.evento_id where n.pedido_id=?",p);
        assertThat(filas).hasSize(6);
        assertThat(filas.stream().map(f->f.get("tipo_historial")).toList()).contains("ETAPA_ENVIADA","ETAPA_RECHAZADA","ETAPA_APROBADA","PEDIDO_DEVUELTO");
        for(var fila:filas) {assertThat(fila.get("destinatario")).isNull();assertThat(fila.get("estado")).isEqualTo("PENDIENTE");assertThat(fila.get("intentos")).isEqualTo(0);}
        assertThat(jdbc.queryForObject("select count(distinct evento_id) from notificacion where pedido_id=?",Integer.class,p)).isEqualTo(6);
    }
    @Autowired org.springframework.transaction.PlatformTransactionManager transaccionesB2;
    @Test void b2RollbackRealDeTransicionNoDejaAvisoNiHistorialNiAvance() throws Exception {
        UUID p=crear();UUID a=archivo(p,1);var antes=estado(p);
        var tx=new org.springframework.transaction.support.TransactionTemplate(transaccionesB2);
        tx.executeWithoutResult(s->{try {
            accion(admin,p,1,"ENVIAR",",\"adjuntoIds\":[\""+a+"\"]",200);
            assertThat(jdbc.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p)).isEqualTo(1);
            s.setRollbackOnly();
        } catch(Exception e){throw new IllegalStateException(e);}});
        assertThat(estado(p)).isEqualTo(antes);
        assertThat(jdbc.queryForObject("select count(*) from notificacion where pedido_id=?",Integer.class,p)).isZero();
    }
    @Test void b2RegistroHttpExigeAdminVigenteInclusoConTokenAntiguo() throws Exception {
        mvc.perform(get("/api/v1/notificaciones").with(token(admin))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
        UUID otro=persona("marketing");
        mvc.perform(get("/api/v1/notificaciones").with(token(otro))).andExpect(status().isForbidden());
        jdbc.update("delete from usuario_rol where usuario_id=?",admin);
        mvc.perform(get("/api/v1/notificaciones").with(token(admin))).andExpect(status().isForbidden());
    }

    @Test void b2RegistroSanitizaErroresYPaginaSinExponerDatosPrivados() throws Exception {
        UUID p=enEtapa(2);
        jdbc.update("update notificacion set ultimo_error='dato-ficticio-no-publicable',estado='FALLIDA' where pedido_id=?",p);
        String cuerpo=respuesta(get("/api/v1/notificaciones?estado=FALLIDA&pagina=0&tamano=100").with(token(admin)),200);
        assertThat(cuerpo).contains("ERROR_NO_CLASIFICADO").doesNotContain("dato-ficticio-no-publicable","messageId","proveedorId","datos\":");
        mvc.perform(get("/api/v1/notificaciones?pagina=-1").with(token(admin))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/notificaciones?tamano=101").with(token(admin))).andExpect(status().isBadRequest());
    }
}
