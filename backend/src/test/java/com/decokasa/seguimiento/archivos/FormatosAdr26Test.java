package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.VerificadorFormatoEnDisco;
import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;
import com.decokasa.seguimiento.archivos.domain.TipoArchivoNoAceptado;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.zip.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

/** Archivos completos ficticios; los positivos se generan y leen con bibliotecas independientes del detector. */
class FormatosAdr26Test {
    @TempDir Path disco;
    byte[] fixture(String nombre) throws IOException {
        try (var in=getClass().getResourceAsStream("/formatos-adr26/"+nombre)) {
            if(in==null)throw new IOException("Falta fixture ficticio "+nombre);
            return in.readAllBytes();
        }
    }
    void verificar(String recurso,String nombre) throws IOException {
        Files.write(disco.resolve("recibido"),fixture(recurso));
        new VerificadorFormatoEnDisco(disco).verificar("recibido",FormatoArchivo.desdeNombre(nombre));
    }
    static Stream<Arguments> validos() {
        return Stream.of(Arguments.of("valido.pdf","PRUEBA.PDF","application/pdf"),
            Arguments.of("valido.xlsx","prueba.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Arguments.of("valido.xls","prueba.xls","application/vnd.ms-excel"),
            Arguments.of("utf8.csv","prueba.csv","text/csv"),Arguments.of("cp1252.csv","prueba.csv","text/csv"),
            Arguments.of("utf16.csv","prueba.csv","text/csv"),Arguments.of("utf16be.csv","prueba.csv","text/csv"),
            Arguments.of("valido.jpg","prueba.jpeg","image/jpeg"),Arguments.of("valido.png","prueba.png","image/png"),
            Arguments.of("pdf-con-prefijo.pdf","prueba.pdf","application/pdf"),
            // Revalidación 2026-10-26: guardados por Microsoft Excel 16 (sin datos personales); el cifrado con
            // contraseña ficticia es ECMA-376 Agile y Excel lo reabre con ella.
            Arguments.of("excel-real.xlsx","EXCEL.XLSX","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Arguments.of("excel-real.xls","EXCEL.XLS","application/vnd.ms-excel"),
            Arguments.of("excel-cifrado.xlsx","cifrado.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Arguments.of("excel-cifrado.xls","cifrado.xls","application/vnd.ms-excel"),
            Arguments.of("utf8.csv","DATOS.CSV","text/csv"),Arguments.of("valido.jpg","FOTO.JPG","image/jpeg"));
    }
    @ParameterizedTest @MethodSource("validos")
    void admiteArchivosValidosYMimeCanonico(String recurso,String nombre,String mime) {
        assertThatCode(()->verificar(recurso,nombre)).doesNotThrowAnyException();
        assertThat(FormatoArchivo.desdeNombre(nombre).mime()).isEqualTo(mime);
    }
    @ParameterizedTest @ValueSource(strings={"prueba.doc","prueba.docx","prueba.zip","prueba.gif","prueba.txt","prueba.xlsm","sin-extension",".pdf"})
    void extensionProhibidaSeRechazaAntesDeRecibirBytes(String nombre) {
        assertThatThrownBy(()->FormatoArchivo.desdeNombre(nombre)).isInstanceOf(TipoArchivoNoAceptado.class);
    }
    @ParameterizedTest @EnumSource(FormatoArchivo.class)
    void tipoDeclaradoNoDecideElFormatoYDebeSerCoherente(FormatoArchivo formato) {
        assertThatCode(()->formato.exigirTipoDeclarado(formato.mime()+"; charset=UTF-8")).doesNotThrowAnyException();
        assertThatCode(()->formato.exigirTipoDeclarado("application/octet-stream")).doesNotThrowAnyException();
        assertThatCode(()->formato.exigirTipoDeclarado(null)).doesNotThrowAnyException();
        assertThatThrownBy(()->formato.exigirTipoDeclarado("application/x-inventado")).isInstanceOf(TipoArchivoNoAceptado.class);
    }
    static Stream<Arguments> falsificados() {
        return Stream.of(Arguments.of("no-excel.ole","word-renombrado.xls"),
            Arguments.of("no-excel.ole","ole-renombrado.xlsx"),Arguments.of("valido.xls","xls-renombrado.xlsx"),
            Arguments.of("valido.pdf","pdf-renombrado.csv"),Arguments.of("pdf-con-prefijo.pdf","pdf-con-prefijo.csv"),
            Arguments.of("valido.png","png-renombrado.csv"),Arguments.of("valido.xls","xls-renombrado.csv"),
            Arguments.of("zip-sin-libro.zip","zip-renombrado.xlsx"),Arguments.of("macros.xlsx","libro.xlsx"),
            Arguments.of("marca-sin-libro.xlsx","sin-libro.xlsx"),Arguments.of("xml-malformado.xlsx","malformado.xlsx"),
            Arguments.of("entidad-externa.xlsx","externa.xlsx"),Arguments.of("valido.pdf","pdf-renombrado.xlsx"),
            Arguments.of("utf8.csv","csv-renombrado.pdf"),
            // Revalidación 2026-10-26: Word real, almacén (no stream) Workbook, VBA sin declarar, xlsm real y cruces
            // entre XLS y XLSX, también cifrados.
            Arguments.of("word-real.doc","word-real.xls"),Arguments.of("word-real.doc","word-real.xlsx"),
            Arguments.of("almacen-workbook.xls","almacen.xls"),Arguments.of("almacen-workbook.xls","almacen.xlsx"),
            Arguments.of("vba-sin-declarar.xlsx","vba.xlsx"),Arguments.of("excel-real.xlsm","xlsm-renombrado.xlsx"),
            Arguments.of("excel-real.xls","excel-real.xlsx"),Arguments.of("excel-cifrado.xls","xls-cifrado.xlsx"),
            Arguments.of("excel-cifrado.xlsx","xlsx-cifrado.xls"),Arguments.of("excel-real.xlsx","xlsx-real.xls"));
    }
    @ParameterizedTest @MethodSource("falsificados")
    void noConfundeOtroFormatoOContenidoMalformadoConElPermitido(String recurso,String nombre) {
        assertThatThrownBy(()->verificar(recurso,nombre)).as(recurso+" como "+nombre)
            .isInstanceOf(TipoArchivoNoAceptado.class);
    }
    @Test void expansionDeContentTypesSeRechazaConLecturaAcotada() {
        assertTimeout(Duration.ofSeconds(5),()->assertThatThrownBy(()->verificar("expansion.xlsx","prueba.xlsx"))
            .isInstanceOf(TipoArchivoNoAceptado.class));
    }
    @Test void zipTruncadoSeRechazaSinErrorInterno() throws Exception {
        Files.write(disco.resolve("roto"),new byte[]{0x50,0x4b,3,4,0,0,0,0});
        assertThatThrownBy(()->new VerificadorFormatoEnDisco(disco).verificar("roto",FormatoArchivo.XLSX))
            .isInstanceOf(TipoArchivoNoAceptado.class).hasMessageNotContaining(disco.toString());
    }
    @Test void xmlConEntidadNoAccedeAUnArchivoExternoYDebeRechazarse() throws Exception {
        Path privado=disco.resolve("no-leer.txt");Files.writeString(privado,"marcador-local-ficticio");
        String xml="<!DOCTYPE Types [<!ENTITY x SYSTEM '"+privado.toUri()+"'>]><Types>&x;<!-- application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml --></Types>";
        try(var zip=new ZipOutputStream(Files.newOutputStream(disco.resolve("xxe")))) {
            zip.putNextEntry(new ZipEntry("[Content_Types].xml"));zip.write(xml.getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        }
        assertThatThrownBy(()->new VerificadorFormatoEnDisco(disco).verificar("xxe",FormatoArchivo.XLSX))
            .isInstanceOf(TipoArchivoNoAceptado.class);
        assertThat(Files.readString(privado)).isEqualTo("marcador-local-ficticio");
    }
    @Test void noSaleDelDirectorioDeAlmacenamiento() {
        assertThatThrownBy(()->new VerificadorFormatoEnDisco(disco).verificar("../fuera",FormatoArchivo.PDF))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
