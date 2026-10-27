package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.archivos.adapter.out.drive.GoogleDriveAlmacen;
import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

/** Protocolo HTTP real, exclusivamente loopback. El transporte redirige en el doble; producto conserva URL Google fija. */
@SuppressWarnings({"unchecked","rawtypes"})
class GoogleDescargaHttpLocalTest {
    HttpServer servidor;GoogleDriveAlmacen drive;AtomicInteger llamadas=new AtomicInteger();int estado=200;
    @BeforeEach void preparar()throws Exception {
        servidor=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        servidor.createContext("/",e->{
            llamadas.incrementAndGet();assertThat(e.getRequestMethod()).isEqualTo("GET");
            assertThat(e.getRequestURI().getPath()).isEqualTo("/drive/v3/files/remoto-ficticio");
            assertThat(e.getRequestURI().getQuery()).isEqualTo("alt=media");
            byte[] b=(estado==200?"Contenido ficticio":"{\"error\":{\"message\":\"detalle privado ficticio\"}}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
            e.sendResponseHeaders(estado,b.length);try(var f=e.getResponseBody()){f.write(b);}
        });servidor.start();
        HttpClient real=HttpClient.newHttpClient(),doble=mock(HttpClient.class);
        var builder=mock(HttpClient.Builder.class,RETURNS_SELF);when(builder.build()).thenReturn(doble);
        doAnswer(i->{
            HttpRequest original=i.getArgument(0);
            assertThat(original.uri().getHost()).isEqualTo("www.googleapis.com");
            assertThat(original.headers().firstValue("Authorization")).contains("Bearer token-ficticio");
            var local=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+servidor.getAddress().getPort()+original.uri().getRawPath()+"?"+original.uri().getRawQuery())).GET().build();
            return real.send(local,(HttpResponse.BodyHandler)i.getArgument(1));
        }).when(doble).send(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
        try(var fabrica=mockStatic(HttpClient.class)) {
            fabrica.when(HttpClient::newBuilder).thenReturn(builder);
            drive=new GoogleDriveAlmacen(true,()->new AccesoCuentaGoogle.Credencial("ficticio@ejemplo.test","token-ficticio"),JsonMapper.builder().build(),Instant::now);
        }
    }
    @AfterEach void detener(){servidor.stop(0);}
    @Test void contenidoAltMediaSeTransmiteSinInvocarOtrosEndpoints()throws Exception {
        try(var f=drive.descargar("remoto-ficticio")){assertThat(new String(f.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("Contenido ficticio");}
        assertThat(llamadas.get()).isEqualTo(1);
    }
    @ParameterizedTest @CsvSource({"401,AUTORIZACION_REQUERIDA","403,PERMANENTE","404,METADATOS_DIVERGENTES","429,CUOTA","500,TRANSITORIA"})
    void erroresHttpSeClasificanSinDetallesPrivadosNiReintentos(int codigo,String causa) {
        estado=codigo;
        assertThatThrownBy(()->drive.descargar("remoto-ficticio")).isInstanceOfSatisfying(FalloIntegracionGoogle.class,e->{
            assertThat(e.causa().name()).isEqualTo(causa);assertThat(e.getMessage()).doesNotContain("detalle privado","token-ficticio");
        });assertThat(llamadas.get()).isEqualTo(1);
    }
}
