package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.notificaciones.adapter.out.persistence.NotificacionSqlRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import com.decokasa.seguimiento.notificaciones.application.port.out.*;
import com.decokasa.seguimiento.notificaciones.config.CorreoConfig;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.json.JsonMapper;

/** Real outbox/configuration transactions, fake recipients/transport, controlled clock. */
@Testcontainers
class CorreoReintentosLocalesIT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    final AtomicReference<Instant> ahora=new AtomicReference<>(Instant.parse("2026-10-19T15:00:00Z"));
    DatosB1 d; NotificacionSqlRepositorio repo; TransactionTemplate tx; DataSourceTransactionManager gestor;
    Notificador gmail; Notificacion aviso;
    @BeforeEach void preparar() {
        d=new DatosB1(PG,null);d.poblar(1,"0".repeat(64),"ficticio");
        gestor=new DataSourceTransactionManager(d.ds);tx=new TransactionTemplate(gestor);
        repo=new NotificacionSqlRepositorio(d.ds,JsonMapper.builder().build());gmail=mock(Notificador.class);
        aviso=Notificacion.pendiente(d.pedido,d.evento,"admin@ejemplo.test",Map.of("pais","EC"),ahora.get());
        tx.executeWithoutResult(s->repo.insertarSiAusente(aviso));
    }
    EnviarNotificacionesPendientes worker() {
        DirectorioDestinatarios directorio=pais->Set.of("admin@ejemplo.test");Reloj reloj=ahora::get;
        return ReflectionTestUtils.invokeMethod(new CorreoConfig(),"enviarNotificacionesPendientes",
            gestor,repo,gmail,directorio,reloj,true,true,1,Duration.ofMinutes(3));
    }
    Notificacion fila() {return repo.listar(null,0,10).getFirst();}
    void avanzar(long segundos) {ahora.updateAndGet(t->t.plusSeconds(segundos));}
    @Test void inicialMasDosReintentosPersistenUnoYCincoMinutosSinCuartoEnvio() {
        when(gmail.enviar(any())).thenAnswer(i->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            throw FalloIntegracionGoogle.rechazoTransitorioConfirmado(Causa.CUOTA,Duration.ZERO);});
        assertThat(worker().procesar()).isEqualTo(1);
        assertThat(fila().intentos()).isEqualTo(1);assertThat(fila().proximoIntentoEn()).isEqualTo(ahora.get().plusSeconds(60));
        avanzar(59);assertThat(worker().procesar()).isZero();verify(gmail,times(1)).enviar(any());
        avanzar(1);assertThat(worker().procesar()).isEqualTo(1);
        assertThat(fila().intentos()).isEqualTo(2);assertThat(fila().proximoIntentoEn()).isEqualTo(ahora.get().plusSeconds(300));
        avanzar(299);assertThat(worker().procesar()).isZero();avanzar(1);worker().procesar();
        assertThat(fila().intentos()).isEqualTo(3);assertThat(fila().estado()).isEqualTo(Notificacion.Estado.FALLIDA);
        assertThat(fila().resultadoIncierto()).isFalse();assertThat(fila().proximoIntentoEn()).isNull();
        avanzar(86400);assertThat(worker().procesar()).isZero();verify(gmail,times(3)).enviar(any());
        assertThat(fila().id()).isEqualTo(aviso.id());assertThat(fila().messageId()).isEqualTo(aviso.messageId());
    }
    @ParameterizedTest @ValueSource(longs={0,30,60,1200})
    void retryAfterNuncaAcortaEsperaBase(long segundos) {
        when(gmail.enviar(any())).thenThrow(FalloIntegracionGoogle.rechazoTransitorioConfirmado(Causa.TRANSITORIA,Duration.ofSeconds(segundos)));
        worker().procesar();assertThat(fila().proximoIntentoEn()).isEqualTo(ahora.get().plusSeconds(Math.max(60,segundos)));
        assertThat(fila().estado()).isEqualTo(Notificacion.Estado.PENDIENTE);assertThat(fila().resultadoIncierto()).isFalse();
    }
    @ParameterizedTest @EnumSource(value=Causa.class,names={"PERMANENTE","AUTORIZACION_REQUERIDA","CUOTA","TRANSITORIA","RESULTADO_INCIERTO","RESPUESTA_INVALIDA","CONFIGURACION"})
    void permanenteInciertoOTransitorioNoConfirmadoNuncaSeReenvia(Causa causa) {
        when(gmail.enviar(any())).thenThrow(new FalloIntegracionGoogle(causa));worker().procesar();
        avanzar(86400);assertThat(worker().procesar()).isZero();verify(gmail,times(1)).enviar(any());
        assertThat(fila().estado()).isEqualTo(Notificacion.Estado.FALLIDA);
        assertThat(fila().resultadoIncierto()).isEqualTo(causa==Causa.RESULTADO_INCIERTO);
    }
    @Test void primerReintentoExitosoMantieneEventoDestinatarioYNoVuelveAEnviar() {
        when(gmail.enviar(any())).thenThrow(FalloIntegracionGoogle.rechazoTransitorioConfirmado(Causa.CUOTA,Duration.ZERO))
            .thenReturn(new Notificador.Entrega("id-ficticio"));
        worker().procesar();avanzar(60);worker().procesar();avanzar(86400);assertThat(worker().procesar()).isZero();
        assertThat(fila().estado()).isEqualTo(Notificacion.Estado.ENVIADA);assertThat(fila().intentos()).isEqualTo(2);
        assertThat(fila().eventoId()).isEqualTo(aviso.eventoId());assertThat(fila().destinatario()).isEqualTo(aviso.destinatario());
        verify(gmail,times(2)).enviar(argThat(c->c.notificacionId().equals(aviso.id()) && c.messageId().equals(aviso.messageId())));
    }
    @Test void respuestaAceptadaPeroLeaseVencidoConservaIncertidumbre() {
        when(gmail.enviar(any())).thenAnswer(i->{avanzar(181);return new Notificador.Entrega("id-ficticio");});
        worker().procesar();assertThat(fila().resultadoIncierto()).isTrue();avanzar(86400);
        assertThat(worker().procesar()).isZero();verify(gmail,times(1)).enviar(any());
    }
    @Test void esperaFueraDeRangoDetieneSinReprogramarAntesDeLoPermitido() {
        when(gmail.enviar(any())).thenThrow(FalloIntegracionGoogle.rechazoTransitorioConfirmado(Causa.CUOTA,Duration.ofSeconds(Long.MAX_VALUE)));
        worker().procesar();assertThat(fila().estado()).isEqualTo(Notificacion.Estado.FALLIDA);assertThat(fila().proximoIntentoEn()).isNull();
        avanzar(86400);assertThat(worker().procesar()).isZero();verify(gmail,times(1)).enviar(any());
    }
    @Test void rechazoTardioDeWorkerAntiguoNoRearmaFilaIncierta() {
        UUID propietario=UUID.randomUUID();tx.executeWithoutResult(s->repo.reclamar(1,ahora.get(),ahora.get().plusSeconds(60),propietario));
        avanzar(61);
        assertThat(tx.<Boolean>execute(s->repo.registrarFallo(aviso.id(),propietario,Causa.CUOTA,ahora.get().plusSeconds(60),false,ahora.get()))).isFalse();
        assertThat(tx.<Boolean>execute(s->repo.registrarFallo(aviso.id(),UUID.randomUUID(),Causa.CUOTA,ahora.get().plusSeconds(60),false,ahora.get()))).isFalse();
        assertThat(fila().resultadoIncierto()).isTrue();assertThat(worker().procesar()).isZero();verifyNoInteractions(gmail);
    }
}
