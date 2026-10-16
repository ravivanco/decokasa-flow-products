package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco;
import com.decokasa.seguimiento.archivos.domain.ArchivoDemasiadoGrande;
import java.io.*;
import java.nio.file.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Límite provisional de 100 MiB con bytes reales en disco, sin reservar un array de 100 MiB. */
class AlmacenLocalRealTest {
    @TempDir Path disco;
    static final long LIMITE=100L*1024*1024;
    InputStream ceros(long cantidad) {
        return new InputStream() {
            long quedan=cantidad;
            @Override public int read() { if(quedan==0) return -1; quedan--; return 0; }
            @Override public int read(byte[] b,int offset,int longitud) {
                if(quedan==0) return -1;
                int n=(int)Math.min(quedan,longitud); Arrays.fill(b,offset,offset+n,(byte)0); quedan-=n; return n;
            }
        };
    }
    @Test void cienMiBExactosSeEscribenCompletosYSePuedenLeer() throws Exception {
        var almacen=new AlmacenLocalEnDisco(disco);
        var r=almacen.recibir(ceros(LIMITE),LIMITE);
        assertThat(r.tamanoBytes()).isEqualTo(LIMITE);
        assertThat(Files.size(disco.resolve(r.claveTemporal()))).isEqualTo(LIMITE);
        almacen.confirmar(r,"pedido-ficticio/archivo");
        try(var entrada=almacen.abrir("pedido-ficticio/archivo")) {
            assertThat(entrada.transferTo(OutputStream.nullOutputStream())).isEqualTo(LIMITE);
        }
        assertThat(almacen.tamano("pedido-ficticio/archivo")).hasValue(LIMITE);
        assertThat(disco.resolve(r.claveTemporal())).doesNotExist();
    }
    @Test void cienMiBMasUnByteSeRechazanSinArchivoParcial() throws Exception {
        var almacen=new AlmacenLocalEnDisco(disco);
        assertThatThrownBy(() -> almacen.recibir(ceros(LIMITE+1),LIMITE)).isInstanceOf(ArchivoDemasiadoGrande.class);
        try(var archivos=Files.walk(disco)) { assertThat(archivos.filter(Files::isRegularFile).toList()).isEmpty(); }
    }
    @Test void claveFueraDelDirectorioSeRechaza() {
        var almacen=new AlmacenLocalEnDisco(disco);
        assertThatThrownBy(() -> almacen.abrir("../fuera-ficticio")).isInstanceOf(IllegalArgumentException.class);
    }
}
