package com.decokasa.seguimiento.archivos.adapter.out.drive;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FlujoDriveConEsperaTest {
    @Test void cabecerasRecibidasSinCuerpoNoDejanUnaLecturaBloqueada()throws Exception {
        var bloqueado=new InputStream(){
            boolean cerrado;
            @Override public synchronized int read()throws IOException {
                while(!cerrado)try{wait();}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException();}
                throw new IOException("detalle-privado-del-proveedor");
            }
            @Override public synchronized void close(){cerrado=true;notifyAll();}
        };
        try(var flujo=new FlujoDriveConEspera(bloqueado,Duration.ofMillis(30))) {
            assertThatThrownBy(flujo::read).isInstanceOf(IOException.class).hasMessage("Drive no respondió a tiempo.");
            assertThatThrownBy(flujo::read).isInstanceOf(IOException.class).hasMessageNotContaining("privado");
        }
    }
    @Test void contenidoDisponibleConservaBytesYCierraOrigen()throws Exception {
        try(var flujo=new FlujoDriveConEspera(new ByteArrayInputStream(new byte[]{1,2,3}),Duration.ofSeconds(1))) {
            assertThat(flujo.read()).isEqualTo(1);assertThat(flujo.readAllBytes()).containsExactly(2,3);
        }
    }
}
