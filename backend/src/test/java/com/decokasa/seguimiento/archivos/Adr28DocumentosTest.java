package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.*;
import com.decokasa.seguimiento.archivos.domain.*;
import com.decokasa.seguimiento.compartido.domain.*;
import java.io.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

/** Matriz local/remoto y vigencia; solo dobles, sin credenciales ni red. */
class Adr28DocumentosTest {
    final UUID id=UUID.randomUUID(),pedido=UUID.randomUUID(),autor=UUID.randomUUID(),entrada=UUID.randomUUID();
    final Solicitante actor=new Solicitante(autor,Set.of());
    final byte[] bytes="Documento ficticio íntegro".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    PedidosParaArchivos permisos;AdjuntoRepositorio adjuntos;AlmacenLocal local;DocumentosDePedido documentos;AlmacenArchivos drive;
    DescargarArchivoCasoDeUso descargar;String sha;
    @BeforeEach void preparar()throws Exception {
        sha=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        permisos=mock(PedidosParaArchivos.class);adjuntos=mock(AdjuntoRepositorio.class);local=mock(AlmacenLocal.class);
        documentos=mock(DocumentosDePedido.class);drive=mock(AlmacenArchivos.class);
        when(adjuntos.porId(id)).thenReturn(Optional.of(new Adjunto(id,pedido,1,entrada,"Solicitud","ficticio.pdf",
            bytes.length,sha,"application/pdf","local/ficticio",EstadoCopia.COPIADO,autor,Instant.EPOCH)));
        when(local.tamano(anyString())).thenReturn(OptionalLong.empty());
        when(documentos.driveIdCopiado(id)).thenReturn(Optional.of("remoto-ficticio"));
        when(drive.metadatos(anyString())).thenReturn(Optional.of(remoto("remoto-ficticio",bytes.length,sha,false,true,"")));
        when(drive.descargar(anyString())).thenAnswer(i->new ByteArrayInputStream(bytes));
        descargar=new DescargarArchivoCasoDeUso(permisos,adjuntos,local,documentos,drive);
    }
    AlmacenArchivos.Remoto remoto(String remoto,long tamano,String huella,boolean papelera,boolean propio,String unidad) {
        return new AlmacenArchivos.Remoto(remoto,tamano,"",id.toString(),pedido.toString(),entrada.toString(),List.of("carpeta"),
            "application/pdf",huella,"",papelera,propio,unidad,"","ficticio.pdf");
    }
    byte[] abrir()throws IOException {try(var f=descargar.abrir(descargar.autorizar(actor,id)).flujo()){return f.readAllBytes();}}
    @Test void localIntegroEvitaGoogle()throws Exception {
        when(local.tamano(anyString())).thenReturn(OptionalLong.of(bytes.length));when(local.abrir(anyString())).thenAnswer(i->new ByteArrayInputStream(bytes));
        assertThat(abrir()).containsExactly(bytes);verifyNoInteractions(drive);
    }
    @Test void localAusenteLeeCopiaConfirmadaPorIdPersistido()throws Exception {
        assertThat(abrir()).containsExactly(bytes);verify(drive).descargar("remoto-ficticio");
    }
    @Test void localDanadoDeMismoTamanoSeRecuperaDesdeDrive()throws Exception {
        when(local.tamano(anyString())).thenReturn(OptionalLong.of(bytes.length));when(local.abrir(anyString())).thenReturn(new ByteArrayInputStream(new byte[bytes.length]));
        assertThat(abrir()).containsExactly(bytes);verify(drive).descargar("remoto-ficticio");
    }
    @Test void metadatosSinHuellaAunVerificanLosBytes()throws Exception {
        when(drive.metadatos(anyString())).thenReturn(Optional.of(remoto("remoto-ficticio",bytes.length,"",false,true,"")));
        assertThat(abrir()).containsExactly(bytes);
    }
    @ParameterizedTest @ValueSource(strings={"id","tamano","huella","papelera","propietario","unidad","404"})
    void metadatosDivergentesNuncaAbrenElContenido(String error) {
        when(drive.metadatos(anyString())).thenReturn(error.equals("404")?Optional.empty():Optional.of(remoto(
            error.equals("id")?"ajeno":"remoto-ficticio",error.equals("tamano")?bytes.length+1:bytes.length,
            error.equals("huella")?"0".repeat(64):sha,error.equals("papelera"),!error.equals("propietario"),error.equals("unidad")?"compartida":"")));
        assertThatThrownBy(()->abrir()).isInstanceOf(ArchivoNoDisponible.class);verify(drive,never()).descargar(anyString());
    }
    @ParameterizedTest @ValueSource(strings={"AUTORIZACION_REQUERIDA","DESACTIVADA","TRANSITORIA","CUOTA","PERMANENTE"})
    void falloDeGoogleEsRecuperableSinDetallesNiCambios(String causa) {
        when(drive.metadatos(anyString())).thenThrow(new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.valueOf(causa)));
        assertThatThrownBy(()->abrir()).isInstanceOf(DriveNoDisponible.class).hasMessageNotContaining("token");
        verify(adjuntos,never()).insertar(any());
    }
    @ParameterizedTest @ValueSource(strings={"truncado","exceso","huella"})
    void contenidoRemotoAlteradoNoSeEntregaCompleto(String caso)throws Exception {
        byte[] malo=new byte[bytes.length+(caso.equals("exceso")?1:caso.equals("truncado")?-1:0)];
        when(drive.descargar(anyString())).thenReturn(new ByteArrayInputStream(malo));
        var recibido=new ByteArrayOutputStream();
        try(var f=descargar.abrir(descargar.autorizar(actor,id)).flujo()) {
            assertThatThrownBy(()->f.transferTo(recibido)).isInstanceOf(IOException.class);
            assertThat(recibido.size()).isLessThan(bytes.length);
        }
    }
    @Test void historicoSinContenidoInformaQueElRegistroSeConserva() {
        when(documentos.driveIdCopiado(id)).thenReturn(Optional.empty());
        assertThatThrownBy(()->abrir()).isInstanceOf(ArchivoNoDisponible.class).hasMessageContaining("El registro se conserva");
        verifyNoInteractions(drive);
    }
    @Test void p17AmpliadaSoloTrasAutorizarEtapaActualYSiempreRevalida()throws Exception {
        doThrow(new SinPermiso("Sin P17")).when(permisos).autorizarDescarga(actor,pedido,1);
        assertThat(abrir()).containsExactly(bytes);verify(permisos).autorizarAperturaDesdeEtapaActual(actor,pedido);
        doThrow(new SinPermiso("Revocado")).when(permisos).autorizarAperturaDesdeEtapaActual(actor,pedido);
        clearInvocations(drive);assertThatThrownBy(()->abrir()).isInstanceOf(SinPermiso.class);verifyNoInteractions(drive);
    }
    @Test void paisRevocadoNoSeSustituyePorLaAmpliacionP17() {
        doThrow(new NoEncontrado("Otro país")).when(permisos).autorizarDescarga(actor,pedido,1);
        assertThatThrownBy(()->abrir()).isInstanceOf(NoEncontrado.class).hasMessage("El archivo no existe.");
        verify(permisos,never()).autorizarAperturaDesdeEtapaActual(any(),any());verifyNoInteractions(drive);
    }
    @ParameterizedTest @CsvSource({"ETAPA_ENVIADA,true","ETAPA_APROBADA,true","ETAPA_RECHAZADA,true","PEDIDO_DEVUELTO,true","SIN_TRANSICION,false"})
    void vigenciaSaleDeLaAsociacionConLaTransicion(String tipo,boolean vigente) {
        UUID evento=UUID.randomUUID();var presentacion=new DocumentosDePedido.Presentacion(evento,tipo,1,Instant.EPOCH);
        var d=new DocumentosDePedido.Documento(id,pedido,1,entrada,Instant.EPOCH,"ficticio.pdf","Solicitud","application/pdf",
            bytes.length,"local/ficticio","COPIADO","remoto-ficticio",autor,Instant.EPOCH,List.of(presentacion));
        when(documentos.delPedido(pedido)).thenReturn(List.of(d));
        when(documentos.transicionesVigentes(pedido)).thenReturn(vigente?List.of(new DocumentosDePedido.Transicion(evento,tipo)):List.of());
        var consultar=new ConsultarDocumentosDelPedidoCasoDeUso(permisos,documentos,local,ids->Map.of());
        assertThat(consultar.listar(actor,pedido)).singleElement().satisfies(v->{assertThat(v.vigente()).isEqualTo(vigente);
            assertThat(v.disponibilidad().name()).isEqualTo("DRIVE");assertThat(v.puedeAbrir()).isTrue();});
    }
    @Test void aprobacionSinDocumentosMantieneLosAceptadosPeroUnRechazoSinDocumentosLosRetira() {
        UUID envio=UUID.randomUUID(),aprobacion=UUID.randomUUID(),rechazo=UUID.randomUUID();
        var d=new DocumentosDePedido.Documento(id,pedido,1,entrada,Instant.EPOCH,"ficticio.pdf","Solicitud","application/pdf",bytes.length,
            "local/ficticio","COPIADO","remoto-ficticio",autor,Instant.EPOCH,List.of(new DocumentosDePedido.Presentacion(envio,"ETAPA_ENVIADA",1,Instant.EPOCH)));
        when(documentos.delPedido(pedido)).thenReturn(List.of(d));
        var consulta=new ConsultarDocumentosDelPedidoCasoDeUso(permisos,documentos,local,ids->Map.of());
        when(documentos.transicionesVigentes(pedido)).thenReturn(List.of(new DocumentosDePedido.Transicion(aprobacion,"ETAPA_APROBADA"),new DocumentosDePedido.Transicion(envio,"ETAPA_ENVIADA")));
        assertThat(consulta.listar(actor,pedido).getFirst().vigente()).isTrue();
        when(documentos.transicionesVigentes(pedido)).thenReturn(List.of(new DocumentosDePedido.Transicion(rechazo,"ETAPA_RECHAZADA"),new DocumentosDePedido.Transicion(envio,"ETAPA_ENVIADA")));
        assertThat(consulta.listar(actor,pedido).getFirst().vigente()).isFalse();
    }
}
