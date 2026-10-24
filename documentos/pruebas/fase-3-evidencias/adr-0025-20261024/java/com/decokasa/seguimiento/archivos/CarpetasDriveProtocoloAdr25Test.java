package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.Flow;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.json.JsonMapper;

/** Cliente HTTP sustituido antes de construir GoogleDriveAlmacen; cero sockets Google. */
@ExtendWith(OutputCaptureExtension.class)
class CarpetasDriveProtocoloAdr25Test {
    final GoogleDriveProtocoloB1Test fixture=new GoogleDriveProtocoloB1Test();
    @BeforeEach void preparar() { fixture.preparar(); }
    byte[] cuerpoPeticion() throws Exception {
        var terminado=new CompletableFuture<byte[]>();
        fixture.peticion.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            final ByteArrayOutputStream datos=new ByteArrayOutputStream();
            public void onSubscribe(Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            public void onNext(ByteBuffer b) { byte[] bytes=new byte[b.remaining()];b.get(bytes);datos.writeBytes(bytes); }
            public void onError(Throwable t) { terminado.completeExceptionally(t); }
            public void onComplete() { terminado.complete(datos.toByteArray()); }
        });
        return terminado.get(5,TimeUnit.SECONDS);
    }
    @Test void carpetaLlevaSoloIdNombrePadreYClaveSinComparticion(CapturedOutput salida) throws Exception {
        fixture.cuerpo="""
                {"id":"persona-reservada","name":"Persona ficticia protocolo · 12345678",
                 "mimeType":"application/vnd.google-apps.folder","parents":["rol-reservado"],
                 "ownedByMe":true,"appProperties":{"carpetaClave":"d/p/pedido/admin/persona"}}
                """;
        var remoto=fixture.drive.crearCarpeta("persona-reservada","Persona ficticia protocolo · 12345678",
                "rol-reservado","d/p/pedido/admin/persona");
        assertThat(remoto.claveCarpeta()).isEqualTo("d/p/pedido/admin/persona");
        assertThat(fixture.peticion.method()).isEqualTo("POST");
        assertThat(fixture.peticion.uri().getPath()).isEqualTo("/drive/v3/files");
        var json=JsonMapper.builder().build().readTree(cuerpoPeticion());
        assertThat(json.propertyNames()).containsExactlyInAnyOrderElementsOf(Set.of("id","name","mimeType","parents","appProperties"));
        assertThat(json.path("id").asString()).isEqualTo("persona-reservada");
        assertThat(json.path("parents").get(0).asString()).isEqualTo("rol-reservado");
        assertThat(json.path("name").asString()).isEqualTo("Persona ficticia protocolo · 12345678");
        assertThat(json.path("appProperties").path("carpetaClave").asString()).isEqualTo("d/p/pedido/admin/persona");
        verify(fixture.http,times(1)).sendAsync(any(),any());
        assertThat(salida.getAll()).doesNotContain("Persona ficticia protocolo","token-ficticio","d/p/pedido/admin/persona");
    }
    @Test void falloDeTransporteNoFiltraNombreTokenOCuerpoEnExcepcion(CapturedOutput salida) {
        doReturn(CompletableFuture.failedFuture(new java.io.IOException("Persona ficticia confidencial token-ficticio")))
                .when(fixture.http).sendAsync(any(),any());
        assertThatThrownBy(() -> fixture.drive.crearCarpeta("id-reservado","Persona ficticia confidencial","padre","d/p/ficticio"))
                .isInstanceOfSatisfying(FalloIntegracionGoogle.class,e -> {
                    assertThat(e.causa()).isEqualTo(Causa.RESULTADO_INCIERTO);
                    assertThat(e.getCause()).isNull();
                    assertThat(e.getMessage()).doesNotContain("Persona ficticia confidencial","token-ficticio");
                });
        assertThat(salida.getAll()).doesNotContain("Persona ficticia confidencial","token-ficticio");
    }
}
