package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.TestcontainersConfiguration;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.json.JsonMapper;

/** HTTP real/Tomcat de la ruta normal. Corrupción remota con doble, jamás Google real. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.config.import=",
    "server.address=127.0.0.1","decokasa.jwt.secreto=ficticio-exclusivo-adr28-http-12345678901234567890",
    "decokasa.ia.habilitada=false","decokasa.ia.borrado.automatico=false","decokasa.google.drive.worker.automatico=false"})
@Import(TestcontainersConfiguration.class)
class Adr28HttpStreamingIT {
    static final String SECRETO="ficticio-exclusivo-adr28-http-12345678901234567890";
    static final Path DISCO=disco();static final JsonMapper JSON=JsonMapper.builder().build();
    static final java.util.concurrent.atomic.AtomicInteger TELEFONOS=new java.util.concurrent.atomic.AtomicInteger(70000000);
    static Path disco(){try{return Files.createTempDirectory("decokasa-adr28-http-");}catch(IOException e){throw new UncheckedIOException(e);}}
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r){r.add("decokasa.archivos.directorio",DISCO::toString);}
    @LocalServerPort int puerto;@Autowired JdbcTemplate sql;@MockitoBean AlmacenArchivos drive;
    HttpClient http;String token;UUID pedido,adjunto,entrada,admin;byte[] bytes;
    String base(){return "http://127.0.0.1:"+puerto+"/api/v1";}
    @BeforeEach void preparar()throws Exception {
        http=HttpClient.newHttpClient();admin=UUID.randomUUID();String s=admin.toString();
        sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'HTTP ficticio',?,?,?,'ficticio',now(),now(),0)",admin,s+"@ejemplo.test",s,"+5939"+TELEFONOS.incrementAndGet());
        sql.update("insert into usuario_rol select ?,id,now() from rol where codigo='admin'",admin);sql.update("insert into usuario_pais values(?,'EC')",admin);
        String cab=b64("{\"alg\":\"HS256\"}".getBytes(StandardCharsets.UTF_8)),claims=b64(JSON.writeValueAsBytes(Map.of("iss","decokasa","sub",s,"exp",Instant.now().plusSeconds(600).getEpochSecond(),"roles",List.of("admin"))));
        var mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8),"HmacSHA256"));
        token=cab+"."+claims+"."+b64(mac.doFinal((cab+"."+claims).getBytes(StandardCharsets.US_ASCII)));
        var creado=http.send(peticion("/pedidos").header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString("{\"pais\":\"EC\",\"tipoFlujo\":\"A\",\"nombre\":\"HTTP ficticio\"}")).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(creado.statusCode()).isEqualTo(201);pedido=UUID.fromString(JSON.readTree(creado.body()).path("id").asString());
        entrada=sql.queryForObject("select entrada_id from etapa_pedido where pedido_id=? and numero=1",UUID.class,pedido);
        bytes=new byte[2*1024*1024];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)(i%4==3?'\n':i%4==1?',':'a');
        String limite="ficticio-"+UUID.randomUUID();
        String cabeza="--"+limite+"\r\nContent-Disposition: form-data; name=\"tipoDocumento\"\r\n\r\nSolicitud\r\n--"+limite+"\r\nContent-Disposition: form-data; name=\"entradaEsperadaId\"\r\n\r\n"+entrada+"\r\n--"+limite+"\r\nContent-Disposition: form-data; name=\"archivo\"; filename=\"ficticio.csv\"\r\nContent-Type: text/csv\r\n\r\n";
        var cuerpo=HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofString(cabeza),HttpRequest.BodyPublishers.ofByteArray(bytes),HttpRequest.BodyPublishers.ofString("\r\n--"+limite+"--\r\n"));
        var subida=http.send(peticion("/pedidos/"+pedido+"/etapas/1/archivos").header("Content-Type","multipart/form-data; boundary="+limite).POST(cuerpo).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(subida.statusCode()).isEqualTo(201);adjunto=UUID.fromString(JSON.readTree(subida.body()).path("id").asString());
        String clave=sql.queryForObject("select clave_local from adjunto where id=?",String.class,adjunto);
        Files.move(DISCO.resolve(clave),DISCO.resolve("apartado-"+adjunto));
        sql.update("update adjunto set estado_copia='COPIADO',drive_id='remoto-ficticio',drive_enlace='https://drive.google.com/file/d/remoto-ficticio' where id=?",adjunto);
        when(drive.metadatos("remoto-ficticio")).thenReturn(Optional.of(new AlmacenArchivos.Remoto("remoto-ficticio",bytes.length,"",adjunto.toString(),pedido.toString(),entrada.toString(),List.of("carpeta"),"text/csv","","",false,true,"","","ficticio.csv")));
    }
    static String b64(byte[] bytes){return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    HttpRequest.Builder peticion(String ruta){return HttpRequest.newBuilder(URI.create(base()+ruta)).timeout(java.time.Duration.ofSeconds(20)).header("Authorization","Bearer "+token);}
    @AfterEach void detener(){http.close();}
    @ParameterizedTest @ValueSource(strings={"huella","truncado","exceso"})
    void httpNoEntregaUnaRespuestaCompletaConContenidoAlterado(String caso)throws Exception {
        byte[] malo=Arrays.copyOf(bytes,bytes.length+(caso.equals("exceso")?1:caso.equals("truncado")?-1:0));malo[0]='b';
        when(drive.descargar("remoto-ficticio")).thenReturn(new ByteArrayInputStream(malo));
        var respuesta=http.send(peticion("/archivos/"+adjunto+"/contenido").GET().build(),HttpResponse.BodyHandlers.ofInputStream());
        long recibidos=0;boolean cortada=false;byte[] bloque=new byte[8192];
        try(var f=respuesta.body()){int n;while((n=f.read(bloque))!=-1)recibidos+=n;}catch(IOException esperada){cortada=true;}
        assertThat(respuesta.statusCode()).isEqualTo(200);assertThat(cortada).isTrue();assertThat(recibidos).isLessThan(bytes.length);
        assertThat(sql.queryForObject("select estado_copia from adjunto where id=?",String.class,adjunto)).isEqualTo("COPIADO");
    }
    @Test void httpIntegroConservaBytesLongitudYCabecerasDeDescarga()throws Exception {
        when(drive.descargar("remoto-ficticio")).thenReturn(new ByteArrayInputStream(bytes));
        var r=http.send(peticion("/archivos/"+adjunto+"/contenido").GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertThat(r.statusCode()).isEqualTo(200);assertThat(r.body()).containsExactly(bytes);
        assertThat(r.headers().firstValue("Content-Type")).contains("application/octet-stream");
        assertThat(r.headers().firstValue("Content-Disposition").orElseThrow()).startsWith("attachment");
        assertThat(r.headers().firstValue("X-Content-Type-Options")).contains("nosniff");
    }
}
