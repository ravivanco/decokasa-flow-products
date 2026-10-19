package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.notificaciones.adapter.out.gmail.GmailNotificador;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import tools.jackson.databind.json.JsonMapper;

/** HttpClient factory is mocked before construction: impossible to open a network connection. */
@SuppressWarnings({"unchecked","rawtypes"})
class GmailProtocoloLocalTest {
    HttpClient http=mock(HttpClient.class);HttpResponse<byte[]> respuesta=mock(HttpResponse.class);
    GmailNotificador gmail;int estado=403;String cuerpo="";Map<String,List<String>> cabeceras=Map.of();
    Instant ahora=Instant.parse("2026-10-19T15:00:00Z");
    GmailNotificador construir(AccesoCuentaGoogle acceso) {
        var builder=mock(HttpClient.Builder.class,RETURNS_SELF);when(builder.build()).thenReturn(http);
        try(var fabrica=mockStatic(HttpClient.class)) {
            fabrica.when(HttpClient::newBuilder).thenReturn(builder);
            return new GmailNotificador(true,acceso,JsonMapper.builder().build(),()->ahora);
        }
    }
    @BeforeEach void preparar() {
        gmail=construir(()->new AccesoCuentaGoogle.Credencial("ficticio@gmail.com","token-ficticio"));
        when(respuesta.statusCode()).thenAnswer(i->estado);when(respuesta.body()).thenAnswer(i->cuerpo.getBytes(StandardCharsets.UTF_8));
        when(respuesta.headers()).thenAnswer(i->HttpHeaders.of(cabeceras,(a,b)->true));
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(CompletableFuture.completedFuture(respuesta));
        rechazo();
    }
    void rechazo() {cuerpo="{\"error\":{\"code\":"+estado+",\"errors\":[{\"domain\":\"usageLimits\",\"reason\":\"rateLimitExceeded\"}]}}";}
    Notificador.Correo correo() {UUID id=UUID.randomUUID();return new Notificador.Correo(id,"admin@ejemplo.test","Aviso ficticio","Prueba", "<notificacion-"+id+"@decokasa.invalid>");}
    FalloIntegracionGoogle fallo() {
        Throwable error=catchThrowable(()->gmail.enviar(correo()));
        assertThat(error).isInstanceOf(FalloIntegracionGoogle.class);return (FalloIntegracionGoogle)error;
    }
    @ParameterizedTest @CsvSource({"0,0","30,30","1200,1200"})
    void retryAfterEnSegundosConservaRechazoConfirmado(String valor,long segundos) {
        cabeceras=Map.of("Retry-After",List.of(valor));var error=fallo();
        assertThat(error.rechazoConfirmado()).isTrue();assertThat(error.esperaMinima()).isEqualTo(Duration.ofSeconds(segundos));
        verify(http,times(1)).sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class));
    }
    @Test void fechaHttpFuturaYPasadaYAusenciaDeCabecera() {
        cabeceras=Map.of("Retry-After",List.of("Mon, 19 Oct 2026 15:10:00 GMT"));assertThat(fallo().esperaMinima()).isEqualTo(Duration.ofMinutes(10));
        cabeceras=Map.of("Retry-After",List.of("Mon, 19 Oct 2026 14:00:00 GMT"));assertThat(fallo().esperaMinima()).isZero();
        cabeceras=Map.of();assertThat(fallo().rechazoConfirmado()).isTrue();
    }
    @ParameterizedTest @ValueSource(strings={"invalida","99999999999999999999999999999999","-2"})
    void retryAfterInvalidoNoAutorizaReenvio(String valor) {
        cabeceras=Map.of("Retry-After",List.of(valor));assertThat(fallo().causa()).isEqualTo(Causa.CONFIGURACION);
        assertThat(fallo().rechazoConfirmado()).isFalse();
    }
    @ParameterizedTest @ValueSource(strings={"{}","{","{\"error\":{\"code\":403}}","{\"error\":{\"code\":429,\"errors\":[{\"domain\":\"global\",\"reason\":\"rateLimitExceeded\"}]}}","{\"id\":\"posible-envio\",\"error\":{\"code\":429}}"})
    void respuesta429NoDemostradaMantieneIncertidumbre(String body) {
        estado=429;cuerpo=body;var error=fallo();assertThat(error.rechazoConfirmado()).isFalse();assertThat(error.causa()).isEqualTo(Causa.RESULTADO_INCIERTO);
    }
    @ParameterizedTest @CsvSource({"401,AUTORIZACION_REQUERIDA","400,PERMANENTE","408,RESULTADO_INCIERTO","500,RESULTADO_INCIERTO","503,RESULTADO_INCIERTO"})
    void permanenteOInciertoNuncaEsRechazoTransitorioSeguro(int status,String causa) {
        estado=status;cuerpo="{}";var error=fallo();assertThat(error.causa()).isEqualTo(Causa.valueOf(causa));assertThat(error.rechazoConfirmado()).isFalse();
    }
    @Test void respuestaAceptadaRequiereIdRealYNoDemuestraRecepcionHumana() {
        estado=200;cuerpo="{\"id\":\"id-ficticio\"}";assertThat(gmail.enviar(correo()).proveedorId()).isEqualTo("id-ficticio");
        cuerpo="{}";assertThat(fallo().causa()).isEqualTo(Causa.RESULTADO_INCIERTO);
    }
    @Test void falloDeTransporteNoSeReclasificaComoRechazoSeguro() {
        when(http.sendAsync(any(HttpRequest.class),any(HttpResponse.BodyHandler.class))).thenReturn(CompletableFuture.failedFuture(new java.io.IOException("detalle-privado")));
        var error=fallo();assertThat(error.causa()).isEqualTo(Causa.RESULTADO_INCIERTO);assertThat(error.rechazoConfirmado()).isFalse();assertThat(error.getMessage()).doesNotContain("detalle-privado");
    }
    @Test void falloDeCredencialAntesDelPostPuedeDemostrarNoAceptacion() {
        gmail=construir(()->{throw new FalloIntegracionGoogle(Causa.TRANSITORIA,Duration.ofSeconds(90));});
        var error=fallo();assertThat(error.rechazoConfirmado()).isTrue();assertThat(error.esperaMinima()).isEqualTo(Duration.ofSeconds(90));verifyNoInteractions(http);
    }

    @ParameterizedTest @CsvSource({"403,rateLimitExceeded","403,userRateLimitExceeded","429,rateLimitExceeded","429,userRateLimitExceeded"})
    void rechazoCompletoDeLimiteDePeticionesAcreditaNoAceptacion(int status,String motivo) {
        estado=status;cuerpo="{\"error\":{\"code\":"+status+",\"errors\":[{\"domain\":\"usageLimits\",\"reason\":\""+motivo+"\"}]}}";
        var error=fallo();assertThat(error.rechazoConfirmado()).isTrue();assertThat(error.causa()).isEqualTo(Causa.CUOTA);
    }
    @Test void mezclaDeMotivosConUnoDesconocidoNoAcreditaRechazoSeguro() {
        estado=429;cuerpo="{\"error\":{\"code\":429,\"errors\":[{\"domain\":\"usageLimits\",\"reason\":\"rateLimitExceeded\"},{\"domain\":\"global\",\"reason\":\"desconocido\"}]}}";
        assertThat(fallo().rechazoConfirmado()).isFalse();assertThat(fallo().causa()).isEqualTo(Causa.RESULTADO_INCIERTO);
    }
}
