package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.*;
import com.decokasa.seguimiento.archivos.domain.*;
import java.nio.file.*;
import java.util.*;
import java.security.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
class ParcialesB1Test {
    @TempDir Path disco;
    static String sha(byte[] b) throws Exception {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));}
    @Test void pausaReinicioYTailNoConfirmadoConservanPrefijoDurable() throws Exception {
        UUID id=UUID.randomUUID();var a=new AlmacenCargasEnDisco(disco);
        try(var lock=a.bloquear(id)) {a.reconciliar(id,0);a.escribir(id,0,new byte[]{1,2,3});}
        var reinicio=new AlmacenCargasEnDisco(disco);
        try(var lock=reinicio.bloquear(id)) {reinicio.reconciliar(id,2);reinicio.escribir(id,2,new byte[]{4});}
        try(var in=reinicio.abrir(id)) {assertThat(in.readAllBytes()).containsExactly(1,2,4);}
        assertThat(disco.resolve(".cargas/"+id+".parte")).exists();
    }
    @Test void integridadDetectaCorrupcionSinEliminarElParcial() throws Exception {
        UUID id=UUID.randomUUID();var a=new AlmacenCargasEnDisco(disco);a.reconciliar(id,0);a.escribir(id,0,new byte[]{9});
        assertThatThrownBy(()->a.validar(id,List.of(new CargaArchivo.Fragmento(0,1,sha(new byte[]{1}))),1)).isInstanceOf(ProblemaCarga.class);
        assertThat(disco.resolve(".cargas/"+id+".parte")).exists();
    }
    @Test void prefijoFaltanteNuncaSeRecreaComoCeros() {
        UUID id=UUID.randomUUID();var a=new AlmacenCargasEnDisco(disco);
        assertThatThrownBy(()->a.reconciliar(id,5)).isInstanceOf(ProblemaCarga.class);
    }
    @Test void dosInstanciasNoEscribenMismaSesionALaVez() {
        UUID id=UUID.randomUUID();var a=new AlmacenCargasEnDisco(disco);var b=new AlmacenCargasEnDisco(disco);
        try(var lock=a.bloquear(id)) { assertThatThrownBy(()->b.bloquear(id)).isInstanceOf(ProblemaCarga.class); }
        try(var lock=b.bloquear(id)) {assertThat(lock).isNotNull();}
    }
    @Test void cerrojoDeCopiaTambienExcluyeProcesosYNoBorraArchivos() throws Exception {
        UUID id=UUID.randomUUID();Files.write(disco.resolve("pendiente"),new byte[]{1});
        var a=new BloqueoCopiasEnDisco(disco);var b=new BloqueoCopiasEnDisco(disco);
        try(var lock=a.intentar(id).orElseThrow()) {assertThat(b.intentar(id)).isEmpty();}
        try(var lock=b.intentar(id).orElseThrow()) {assertThat(Files.readAllBytes(disco.resolve("pendiente"))).containsExactly(1);}
    }
}
