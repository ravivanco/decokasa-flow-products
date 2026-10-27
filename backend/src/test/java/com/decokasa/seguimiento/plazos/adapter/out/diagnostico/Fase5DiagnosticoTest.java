package com.decokasa.seguimiento.plazos.adapter.out.diagnostico;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import ch.qos.logback.classic.*;
import ch.qos.logback.core.read.ListAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.decokasa.seguimiento.plazos.application.port.in.*;
import com.decokasa.seguimiento.plazos.application.port.out.*;
import com.decokasa.seguimiento.plazos.application.usecase.DetectarVencimientosCasoDeUso;
import com.decokasa.seguimiento.plazos.adapter.in.tareas.TareaVencimientos;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

class Fase5DiagnosticoTest {
    static final Instant AHORA=Instant.parse("2026-10-20T15:00:00Z");
    static final String PRIVADO="motivo-privado-ficticio correo@ejemplo.test token-ficticio";
    final UUID pasada=UUID.randomUUID(),pedido=UUID.randomUUID();
    final DiagnosticoVencimientosLog diagnostico=new DiagnosticoVencimientosLog();
    ListAppender<ILoggingEvent> appender;
    Logger logger;
    @BeforeEach void capturar(){logger=(Logger)LoggerFactory.getLogger(DiagnosticoVencimientosLog.class);appender=new ListAppender<>();appender.start();logger.addAppender(appender);}
    @AfterEach void cerrar(){logger.detachAppender(appender);appender.stop();}
    String linea(){return appender.list.getLast().getFormattedMessage();}
    void seguro(){assertThat(linea()).doesNotContain(PRIVADO,"correo@ejemplo.test","motivo-privado-ficticio","token-ficticio");assertThat(appender.list).allSatisfy(e->assertThat(e.getThrowableProxy()).isNull());}
    @ParameterizedTest @ValueSource(strings={"55P03","40P01","40001"})
    void bloqueosSqlSonClasificadosSinMensajeNiParametros(String estado){
        diagnostico.falloDePedido(pasada,pedido,AHORA,new RuntimeException(PRIVADO,new SQLException(PRIVADO,estado)));
        assertThat(linea()).contains("categoria=BLOQUEO","codigo="+estado,"pasada="+pasada,"pedido="+pedido,"hora="+AHORA);seguro();
    }
    @Test void versionOptimistaTieneCategoriaDistinta(){
        diagnostico.falloDePedido(pasada,pedido,AHORA,new jakarta.persistence.OptimisticLockException(PRIVADO));
        assertThat(linea()).contains("categoria=CONCURRENCIA");seguro();
    }
    @Test void dominioNoExponeMotivo(){diagnostico.falloDePedido(pasada,pedido,AHORA,new DatoInvalido(PRIVADO));assertThat(linea()).contains("categoria=DOMINIO");seguro();}
    @Test void baseDatosNoExponeSql(){diagnostico.falloDePedido(pasada,pedido,AHORA,new RuntimeException(PRIVADO,new SQLException(PRIVADO,"23505")));assertThat(linea()).contains("categoria=BASE_DE_DATOS","codigo=23505");seguro();}
    @Test void clasificacionRotaTieneRegistroDeUltimoRecurso(){
        RuntimeException causa=new RuntimeException(PRIVADO){@Override public synchronized Throwable getCause(){throw new IllegalStateException(PRIVADO);}};
        diagnostico.falloDePedido(pasada,pedido,AHORA,causa);assertThat(linea()).contains("vencimientos_fallo_sin_clasificar","pasada="+pasada,"pedido="+pedido);seguro();
        diagnostico.falloDePasada(pasada,AHORA,causa);assertThat(linea()).contains("vencimientos_fallo_sin_clasificar");seguro();
    }
    @Test void causaCiclicaNoSeBloqueaYNoExponeMensaje(){
        RuntimeException a=new RuntimeException(PRIVADO),b=new RuntimeException(PRIVADO,a);a.initCause(b);
        diagnostico.falloDePasada(pasada,AHORA,a);assertThat(linea()).contains("categoria=INESPERADO");seguro();
    }
    @Test void falloDeUnPedidoNoDetieneLosSanosYNumerosCoinciden(){
        var pedidos=mock(PedidosConEtapaActiva.class);var registrar=mock(RegistrarVencimientosDePedido.class);UUID sano=UUID.randomUUID();
        when(pedidos.candidatos(AHORA,100)).thenReturn(List.of(pedido,sano));when(registrar.registrar(pedido,AHORA)).thenThrow(new IllegalStateException(PRIVADO));when(registrar.registrar(sano,AHORA)).thenReturn(2);
        var r=new DetectarVencimientosCasoDeUso(pedidos,registrar,()->AHORA,100,diagnostico).detectar();
        assertThat(r.candidatos()).isEqualTo(2);assertThat(r.fallidos()).isEqualTo(1);assertThat(r.avisos()).isEqualTo(2);
        assertThat(linea()).contains("pasada="+r.pasadaId(),"pedido="+pedido);verify(registrar).registrar(sano,AHORA);seguro();
    }
    @Test void candidatosFallidosSeCorrelacionanConTareaSinPropagarDatos(){
        var pedidos=mock(PedidosConEtapaActiva.class);when(pedidos.candidatos(AHORA,100)).thenThrow(new RuntimeException(PRIVADO,new SQLException(PRIVADO,"55P03")));
        var registrar=mock(RegistrarVencimientosDePedido.class);var detector=new DetectarVencimientosCasoDeUso(pedidos,registrar,()->AHORA,100,diagnostico);
        var ex=catchThrowableOfType(DetectarVencimientos.PasadaInterrumpida.class,detector::detectar);
        assertThat(ex).isNotNull();assertThat(ex.getMessage()).doesNotContain(PRIVADO);assertThat(linea()).contains("vencimientos_fallo_pasada","pasada="+ex.pasadaId());verifyNoInteractions(registrar);seguro();
        Logger tarea=(Logger)LoggerFactory.getLogger(TareaVencimientos.class);tarea.addAppender(appender);
        try{new TareaVencimientos(()->{throw ex;}).ejecutar();assertThat(linea()).contains("vencimientos_pasada_interrumpida","pasada="+ex.pasadaId());seguro();}finally{tarea.detachAppender(appender);}
    }
    @Test void diagnosticoCaidoNoImpideSeguirYValidacionLoteEsExplicita(){
        var pedidos=mock(PedidosConEtapaActiva.class);var registrar=mock(RegistrarVencimientosDePedido.class);var roto=mock(DiagnosticoVencimientos.class);
        when(pedidos.candidatos(AHORA,1)).thenReturn(List.of(pedido));when(registrar.registrar(pedido,AHORA)).thenThrow(new IllegalStateException(PRIVADO));doThrow(new IllegalStateException(PRIVADO)).when(roto).falloDePedido(any(),any(),any(),any());
        assertThat(new DetectarVencimientosCasoDeUso(pedidos,registrar,()->AHORA,1,roto).detectar().fallidos()).isEqualTo(1);
        assertThatThrownBy(()->new DetectarVencimientosCasoDeUso(pedidos,registrar,()->AHORA,0,roto)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new DetectarVencimientosCasoDeUso(pedidos,registrar,()->AHORA,1001,roto)).isInstanceOf(IllegalArgumentException.class);
    }
}
