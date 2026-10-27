
package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.in.tareas.TareaCopiaArchivos;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/** Invocación manual con puerto falso; no registra scheduling ni habilita el worker real. */
class TareaCopiaB1Test {
    @Test void falloDeUnaPasadaNoImpideLaSiguienteNiExponeLaExcepcion() {
        var llamadas=new AtomicInteger();var logger=(Logger)LoggerFactory.getLogger(TareaCopiaArchivos.class);
        var registros=new ListAppender<ILoggingEvent>();registros.start();logger.addAppender(registros);
        try {
            var tarea=new TareaCopiaArchivos(()->{if(llamadas.incrementAndGet()==1)throw new IllegalStateException("sesion-ficticia-privada");return 1;});
            assertThatCode(tarea::ejecutar).doesNotThrowAnyException();assertThatCode(tarea::ejecutar).doesNotThrowAnyException();
            assertThat(llamadas.get()).isEqualTo(2);assertThat(registros.list).hasSize(1);
            assertThat(registros.list.getFirst().getThrowableProxy()).isNull();
            assertThat(registros.list.getFirst().getFormattedMessage()).contains("recuperación").doesNotContain("sesion-ficticia-privada");
        } finally {logger.detachAppender(registros);registros.stop();}
    }
}
