package com.decokasa.seguimiento.archivos.adapter.out.google;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** AES-GCM con llave privada del almacén 3.1; protocolo/AAD separados del contenedor OAuth. Sin I/O al construir. */
public final class ProtectorSesionDriveAes implements ProtectorSesionDrive {
    private final SecretosGoogle.FuenteLlave llave;
    private final SecureRandom azar=new SecureRandom();
    /** Paso 3.1: llave en archivo privado local. */
    public ProtectorSesionDriveAes(String rutaLlave) {
        this.llave=() -> ArchivosPrivadosGoogle.leer(ArchivosPrivadosGoogle.externo(rutaLlave),32);
    }
    /** ADR-0034: la misma llave que los tokens, del origen configurado (archivo o variable de entorno). */
    public ProtectorSesionDriveAes(SecretosGoogle secretos) { this.llave=secretos.llave(); }
    @Override public byte[] cifrar(UUID id,AlmacenArchivos.Sesion sesion) {
        byte[] nonce=new byte[12];azar.nextBytes(nonce);
        byte[] claro=sesion.uri().getBytes(StandardCharsets.UTF_8);
        try {
            byte[] cifrado=transformar(id,Cipher.ENCRYPT_MODE,nonce,claro);
            return ByteBuffer.allocate(13+cifrado.length).put((byte)1).put(nonce).put(cifrado).array();
        } finally { Arrays.fill(claro,(byte)0); }
    }
    @Override public AlmacenArchivos.Sesion abrir(UUID id,byte[] cifrado) {
        if (cifrado==null || cifrado.length<30 || cifrado.length>8192 || cifrado[0]!=1) {
            throw new FalloIntegracionGoogle(Causa.CONFIGURACION);
        }
        byte[] claro=transformar(id,Cipher.DECRYPT_MODE,Arrays.copyOfRange(cifrado,1,13),Arrays.copyOfRange(cifrado,13,cifrado.length));
        try { return new AlmacenArchivos.Sesion(new String(claro,StandardCharsets.UTF_8)); }
        finally { Arrays.fill(claro,(byte)0); }
    }
    private byte[] transformar(UUID id,int modo,byte[] nonce,byte[] datos) {
        byte[] clave=null;
        try {
            clave=llave.leer();
            if (clave.length!=32) { throw new FalloIntegracionGoogle(Causa.CONFIGURACION); }
            Cipher cifrador=Cipher.getInstance("AES/GCM/NoPadding");
            cifrador.init(modo,new SecretKeySpec(clave,"AES"),new GCMParameterSpec(128,nonce));
            cifrador.updateAAD(("decokasa:drive:1:"+id).getBytes(StandardCharsets.UTF_8));
            return cifrador.doFinal(datos);
        } catch (Exception error) { throw new FalloIntegracionGoogle(Causa.CONFIGURACION); }
        finally { if (clave!=null) { Arrays.fill(clave,(byte)0); } }
    }
}
