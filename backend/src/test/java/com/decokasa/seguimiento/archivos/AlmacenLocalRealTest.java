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

    @Test void archivoMenorConContenidoDesconocidoNoInventaRestriccionDeFormato() throws Exception {
        var almacen=new AlmacenLocalEnDisco(disco);
        var r=almacen.recibir(new ByteArrayInputStream(new byte[]{1,2,3,4,5}),LIMITE);
        assertThat(r.tamanoBytes()).isEqualTo(5);
        assertThat(r.sha256()).hasSize(64);
        almacen.confirmar(r,"pedido/entrada/adjunto");
        almacen.confirmar(new com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.ArchivoRecibido(
            "pedido/entrada/adjunto",r.tamanoBytes(),r.sha256(),r.tipoDetectado()),"pedido/entrada/adjunto");
        try(var bytes=almacen.abrir("pedido/entrada/adjunto")) { assertThat(bytes.readAllBytes()).containsExactly(1,2,3,4,5); }
    }
    @Test void nombreUsaBasenameQuitaControlesYLimitaPuntosUnicode() {
        var a=com.decokasa.seguimiento.archivos.domain.Adjunto.class;
        assertThat(com.decokasa.seguimiento.archivos.domain.Adjunto.limpiarNombre("C:\\ficticio\\..\\ficha\u0000.pdf")).isEqualTo("ficha.pdf");
        assertThat(com.decokasa.seguimiento.archivos.domain.Adjunto.limpiarNombre(" \n\t")).isEqualTo("archivo");
        String limpio=com.decokasa.seguimiento.archivos.domain.Adjunto.limpiarNombre("😀".repeat(300));
        assertThat(limpio.codePointCount(0,limpio.length())).isEqualTo(255);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "255044462d,application/pdf","504b0304,application/zip","d0cf11e0a1b11ae1,application/x-ole-storage",
        "89504e470d0a1a0a,image/png","ffd8ff,image/jpeg","47494638,image/gif"})
    void firmaTecnicaSoloRegistraMetadatosSinInventarListaDeAceptacion(String hex,String tipo) throws Exception {
        byte[] bytes=java.util.HexFormat.of().parseHex(hex);var almacen=new AlmacenLocalEnDisco(disco);
        var recibido=almacen.recibir(new ByteArrayInputStream(bytes),LIMITE);
        assertThat(recibido.tipoDetectado()).isEqualTo(tipo);assertThat(recibido.tamanoBytes()).isEqualTo(bytes.length);
        assertThat(Files.readAllBytes(disco.resolve(recibido.claveTemporal()))).isEqualTo(bytes);
    }
}
