
package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.archivos.adapter.out.drive.GoogleDriveAlmacen;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import tools.jackson.databind.json.JsonMapper;

/** Dobla el constructor HTTP público antes de crear el adaptador: nunca construye un cliente de red real.
 * Complementa el doble del puerto del worker con las respuestas del protocolo reanudable. */
@SuppressWarnings({"unchecked","rawtypes"})
class GoogleDriveProtocoloB1Test {
    final HttpClient http=mock(HttpClient.class);
    final HttpResponse<byte[]> respuesta=mock(HttpResponse.class);
    GoogleDriveAlmacen drive; HttpRequest peticion;
    int estado=200;String cuerpo="{}";Map<String,List<String>> cabeceras=Map.of();
    final String sesion="https://www.googleapis.com/upload/drive/v3/files?upload_id=ficticio";
    @BeforeEach void preparar() {
        var builder=mock(HttpClient.Builder.class,RETURNS_SELF);when(builder.build()).thenReturn(http);
        try(var fabrica=mockStatic(HttpClient.class)) {
            fabrica.when(HttpClient::newBuilder).thenReturn(builder);
            drive=new GoogleDriveAlmacen(true,()->new AccesoCuentaGoogle.Credencial("ficticio@ejemplo.test","token-ficticio"),
                JsonMapper.builder().build(),()->Instant.parse("2026-10-17T12:00:00Z"));
        }
        when(respuesta.statusCode()).thenAnswer(i->estado);
        when(respuesta.body()).thenAnswer(i->cuerpo.getBytes(StandardCharsets.UTF_8));
        when(respuesta.headers()).thenAnswer(i->HttpHeaders.of(cabeceras,(a,b)->true));
        doAnswer(i->{peticion=i.getArgument(0);return CompletableFuture.completedFuture(respuesta);})
            .when(http).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    AlmacenArchivos.Copia copia() {return new AlmacenArchivos.Copia("id-ficticio",UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"ficticio.bin",null,3,"carpeta-ficticia");}
    void causa(Causa causa,org.assertj.core.api.ThrowableAssert.ThrowingCallable accion) {
        assertThatThrownBy(accion).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->assertThat(e.causa()).isEqualTo(causa));
    }
    @Test void reservaIdEIniciaSesionYReiniciaConPatchDelMismoId() {
        cuerpo="{\"ids\":[\"id-ficticio\"]}";assertThat(drive.reservarId()).isEqualTo("id-ficticio");
        assertThat(peticion.method()).isEqualTo("GET");
        cabeceras=Map.of("Location",List.of(sesion));drive.iniciar(copia());
        assertThat(peticion.method()).isEqualTo("POST");assertThat(peticion.headers().firstValue("X-Upload-Content-Length")).contains("3");
        drive.reiniciar(copia());assertThat(peticion.method()).isEqualTo("PATCH");
        assertThat(peticion.uri().getPath()).endsWith("/files/id-ficticio");
    }
    @Test void respuesta308ConservaProgresoYFormaContentRangeReal() {
        estado=308;cabeceras=Map.of("Range",List.of("bytes=0-262143"));
        var p=drive.enviarFragmento(new AlmacenArchivos.Sesion(sesion),0,524288,new byte[262144]);
        assertThat(p.siguienteByte()).isEqualTo(262144);assertThat(p.completo()).isEmpty();
        assertThat(peticion.headers().firstValue("Content-Range")).contains("bytes 0-262143/524288");
        cabeceras=Map.of();assertThat(drive.consultar(new AlmacenArchivos.Sesion(sesion),524288).siguienteByte()).isZero();
        assertThat(peticion.headers().firstValue("Content-Range")).contains("bytes */524288");
    }
    @ParameterizedTest @ValueSource(strings={"bytes=1-2","bytes=0-999","bytes=0-x"})
    void rangoRemotoInvalidoNoEsProgresoConfirmado(String rango) {
        estado=308;cabeceras=Map.of("Range",List.of(rango));causa(Causa.RESPUESTA_INVALIDA,()->drive.consultar(new AlmacenArchivos.Sesion(sesion),3));
    }
    @ParameterizedTest @ValueSource(ints={404,410})
    void sesionCaducadaSeDistingueDeArchivoAusente(int codigo) {
        estado=codigo;causa(Causa.SESION_CADUCADA,()->drive.consultar(new AlmacenArchivos.Sesion(sesion),3));
        if(codigo==404)assertThat(drive.metadatos("id-ficticio")).isEmpty();
    }
    @ParameterizedTest @CsvSource({"401,AUTORIZACION_REQUERIDA","429,CUOTA","500,TRANSITORIA","408,TRANSITORIA","409,CONFLICTO_REMOTO","400,PERMANENTE"})
    void erroresNoSonExitosNiReintentosOcultos(int codigo,String error) {
        estado=codigo;causa(Causa.valueOf(error),()->drive.metadatos("id-ficticio"));
        verify(http,times(1)).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test void cuotaConRetryAfterUsaRelojControlado() {
        estado=429;cabeceras=Map.of("Retry-After",List.of("1200"));
        assertThatThrownBy(()->drive.metadatos("id-ficticio")).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->assertThat(e.esperaMinima()).isEqualTo(Duration.ofMinutes(20)));
        cabeceras=Map.of("Retry-After",List.of("Sat, 17 Oct 2026 12:05:00 GMT"));
        assertThatThrownBy(()->drive.metadatos("id-ficticio")).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->assertThat(e.esperaMinima()).isEqualTo(Duration.ofMinutes(5)));
    }
    @Test void metadatosFinalesDebenCoincidirConTamanoYConservarIdentidad() {
        cuerpo="{\"id\":\"id-ficticio\",\"size\":\"3\",\"webViewLink\":\"https://drive.google.com/file/d/ficticio\",\"parents\":[\"carpeta-ficticia\"],\"mimeType\":\"application/octet-stream\",\"ownedByMe\":true,\"appProperties\":{\"adjuntoId\":\"adjunto-ficticio\"}}";
        var p=drive.enviarFragmento(new AlmacenArchivos.Sesion(sesion),0,3,new byte[]{1,2,3});
        assertThat(p.completo().orElseThrow().adjuntoId()).isEqualTo("adjunto-ficticio");assertThat(p.siguienteByte()).isEqualTo(3);
        causa(Causa.RESPUESTA_INVALIDA,()->drive.consultar(new AlmacenArchivos.Sesion(sesion),4));
    }
    @Test void jsonIncompletoYFalloDeTransporteDejanErrorClasificado() {
        cuerpo="no-json";causa(Causa.RESPUESTA_INVALIDA,()->drive.metadatos("id-ficticio"));
        doReturn(CompletableFuture.failedFuture(new java.io.IOException("fallo ficticio"))).when(http).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        causa(Causa.RESULTADO_INCIERTO,()->drive.metadatos("id-ficticio"));
    }

    java.util.concurrent.Flow.Subscription suscripcion;
    void transmitirConSuscriptor(byte[] bytes,boolean error) {
        suscripcion=mock(java.util.concurrent.Flow.Subscription.class);
        doAnswer(i->{
            peticion=i.getArgument(0);HttpResponse.BodyHandler<byte[]> handler=i.getArgument(1);
            var info=mock(HttpResponse.ResponseInfo.class);when(info.statusCode()).thenReturn(estado);
            var headers=HttpHeaders.of(cabeceras,(a,b)->true);when(info.headers()).thenReturn(headers);
            var receptor=handler.apply(info);var cuerpoRecibido=receptor.getBody().toCompletableFuture();
            receptor.onSubscribe(suscripcion);
            if(error)receptor.onError(new java.io.IOException("Fallo ficticio del flujo"));
            else {receptor.onNext(List.of(java.nio.ByteBuffer.wrap(bytes)));if(!cuerpoRecibido.isCompletedExceptionally())receptor.onComplete();}
            return cuerpoRecibido.thenApply(b->{var r=(HttpResponse<byte[]>)mock(HttpResponse.class);when(r.statusCode()).thenReturn(estado);
                when(r.headers()).thenReturn(headers);when(r.body()).thenReturn(b);return r;});
        }).when(http).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test void recibeMetadataConSuscriptorRealSinClienteDeRed() {
        transmitirConSuscriptor("{\"ids\":[\"id-ficticio\"]}".getBytes(StandardCharsets.UTF_8),false);
        assertThat(drive.reservarId()).isEqualTo("id-ficticio");verify(suscripcion).request(Long.MAX_VALUE);
    }
    @Test void respuestaMayorAlLimiteCancelaElFlujoYNoSeConfirma() {
        transmitirConSuscriptor(new byte[65537],false);causa(Causa.RESULTADO_INCIERTO,()->drive.reservarId());verify(suscripcion).cancel();
    }
    @Test void errorDelFlujoSeClasificaSinExponerSuCausaInterna() {
        transmitirConSuscriptor(new byte[0],true);
        assertThatThrownBy(()->drive.reservarId()).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->{
            assertThat(e.causa()).isEqualTo(Causa.RESULTADO_INCIERTO);assertThat(e.getCause()).isNull();
            assertThat(e.getMessage()).doesNotContain("Fallo ficticio");});
    }
}
