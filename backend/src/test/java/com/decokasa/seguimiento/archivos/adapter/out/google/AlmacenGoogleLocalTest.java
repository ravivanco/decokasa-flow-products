package com.decokasa.seguimiento.archivos.adapter.out.google;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

/** Actual local encryption and durable files; exclusively generated test credentials. */
class AlmacenGoogleLocalTest {
    @TempDir Path disco;
    AlmacenTokensGoogle almacen; Path llave,tokens;
    final Instant ahora=Instant.parse("2026-10-19T15:00:00Z");
    @BeforeEach void preparar() throws Exception {
        FixtureGoogle.privado(disco);llave=disco.resolve("llave-ficticia");tokens=disco.resolve("tokens-ficticios.enc");
        ArchivosPrivadosGoogle.escribirAtomico(llave,new byte[32]);
        almacen=new AlmacenTokensGoogle(tokens,llave,JsonMapper.builder().build());
    }
    AlmacenTokensGoogle.Tokens datos(String acceso,String refresh) {
        return new AlmacenTokensGoogle.Tokens("cliente-ficticio","cuenta-ficticia","ficticio@gmail.com",acceso,refresh,
            ahora.plusSeconds(3600),ahora.plusSeconds(7*86400),"openid");
    }
    @Test void cifradoPersistenteSobreviveReinicioSinExponerTokensEnDiscoOToString() throws Exception {
        var originales=datos("acceso-ficticio","refresh-ficticio");almacen.guardar(originales,ahora);
        assertThat(new String(Files.readAllBytes(tokens),java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("acceso-ficticio","refresh-ficticio");
        almacen=new AlmacenTokensGoogle(tokens,llave,JsonMapper.builder().build());
        assertThat(almacen.vigente(ahora,x->{throw new AssertionError("No renovar acceso vigente");})).isEqualTo(originales);
        assertThat(originales.toString()).doesNotContain(originales.accessToken(),originales.refreshToken());
        ArchivosPrivadosGoogle.comprobar(tokens);assertThat(ArchivosPrivadosGoogle.externo(tokens.toString())).isEqualTo(tokens.toRealPath());
    }
    @Test void nuevaAutorizacionSinRefreshConservaGrantAnteriorPeroNoLoInventa() throws Exception {
        assertThatThrownBy(()->almacen.guardar(datos("acceso-ficticio",""),ahora)).isInstanceOf(IOException.class);
        almacen.guardar(datos("primero","refresh-ficticio"),ahora);
        almacen.guardar(datos("segundo",""),ahora.plusSeconds(5));
        var actual=almacen.vigente(ahora,x->{throw new AssertionError("No renovar");});
        assertThat(actual.accessToken()).isEqualTo("segundo");assertThat(actual.refreshToken()).isEqualTo("refresh-ficticio");
        assertThat(actual.autorizacionHasta()).isEqualTo(ahora.plusSeconds(7*86400));
    }
    @Test void renovacionPersisteSinExtenderCaducidadNiCambiarCuenta() throws Exception {
        almacen.guardar(datos("inicial","refresh-ficticio"),ahora);
        var vencimiento=ahora.plusSeconds(3590);
        var actualizado=almacen.vigente(vencimiento,x->new AlmacenTokensGoogle.Tokens(x.clienteId(),x.cuentaId(),x.correo(),"renovado",x.refreshToken(),
            vencimiento.plusSeconds(3600),x.autorizacionHasta(),x.ambitos()));
        assertThat(actualizado.accessToken()).isEqualTo("renovado");assertThat(actualizado.autorizacionHasta()).isEqualTo(ahora.plusSeconds(7*86400));
        var reinicio=new AlmacenTokensGoogle(tokens,llave,JsonMapper.builder().build());
        assertThat(reinicio.vigente(vencimiento,x->{throw new AssertionError("Renovacion ya persistida");})).isEqualTo(actualizado);
        assertThatThrownBy(()->reinicio.vigente(ahora.plusSeconds(7*86400),x->x)).isInstanceOf(IOException.class);
    }
    @Test void sustitucionDeCuentaYRenovacionQueExtiendeGrantSonRechazadas() throws Exception {
        var original=datos("inicial","refresh-ficticio");almacen.guardar(original,ahora);
        var otra=new AlmacenTokensGoogle.Tokens(original.clienteId(),"otra-cuenta",original.correo(),"nuevo","refresh",original.accessHasta(),original.autorizacionHasta(),original.ambitos());
        assertThatThrownBy(()->almacen.guardar(otra,ahora)).isInstanceOf(IOException.class);
        assertThatThrownBy(()->almacen.vigente(ahora.plusSeconds(3590),x->new AlmacenTokensGoogle.Tokens(x.clienteId(),x.cuentaId(),x.correo(),"nuevo",x.refreshToken(),
            ahora.plusSeconds(7200),x.autorizacionHasta().plusSeconds(1),x.ambitos()))).isInstanceOf(IOException.class);
        assertThat(almacen.vigente(ahora,x->x)).isEqualTo(original);
    }
    @Test void corrupcionOClaveDistintaNoDevuelvenCredencial() throws Exception {
        almacen.guardar(datos("acceso-ficticio","refresh-ficticio"),ahora);
        byte[] bytes=Files.readAllBytes(tokens);bytes[bytes.length-1]^=1;ArchivosPrivadosGoogle.escribirAtomico(tokens,bytes);
        assertThatThrownBy(()->almacen.vigente(ahora,x->x)).isInstanceOf(java.security.GeneralSecurityException.class);
        ArchivosPrivadosGoogle.escribirAtomico(tokens,new byte[8]);assertThatThrownBy(()->almacen.vigente(ahora,x->x)).isInstanceOf(IOException.class);
    }
    @Test void rutasRelativasDentroDeGitYArchivosDemasiadoGrandesSeRechazan() throws Exception {
        assertThatThrownBy(()->ArchivosPrivadosGoogle.externo("relativo/cliente.json")).isInstanceOf(IOException.class);
        Path git=Files.createDirectory(disco.resolve("repositorio"));Files.createDirectory(git.resolve(".git"));
        assertThatThrownBy(()->ArchivosPrivadosGoogle.externo(git.resolve("cliente.json").toString())).isInstanceOf(IOException.class);
        assertThatThrownBy(()->ArchivosPrivadosGoogle.leer(llave,31)).isInstanceOf(IOException.class);
        assertThatThrownBy(()->new AlmacenTokensGoogle(llave,llave,JsonMapper.builder().build())).isInstanceOf(IOException.class);
        ArchivosPrivadosGoogle.escribirAtomico(llave,new byte[31]);assertThatThrownBy(()->almacen.preparar()).isInstanceOf(IOException.class);
    }
    @Test void sesionDriveCifradaEstaVinculadaAlAdjuntoYConservaUriSoloEnMemoria() {
        var protector=new ProtectorSesionDriveAes(llave.toString());UUID id=UUID.randomUUID();
        var sesion=new AlmacenArchivos.Sesion("https://www.googleapis.com/upload/drive/v3/files?upload_id=ficticio");
        byte[] cifrado=protector.cifrar(id,sesion);assertThat(protector.abrir(id,cifrado)).isEqualTo(sesion);
        assertThat(new String(cifrado,java.nio.charset.StandardCharsets.ISO_8859_1)).doesNotContain("upload_id=ficticio");
        assertThat(protector.cifrar(id,sesion)).isNotEqualTo(cifrado);
        assertThatThrownBy(()->protector.abrir(UUID.randomUUID(),cifrado)).isInstanceOf(FalloIntegracionGoogle.class);
        assertThatThrownBy(()->protector.abrir(id,new byte[2])).isInstanceOf(FalloIntegracionGoogle.class);
        assertThatThrownBy(()->protector.abrir(id,null)).isInstanceOf(FalloIntegracionGoogle.class);
    }
}
