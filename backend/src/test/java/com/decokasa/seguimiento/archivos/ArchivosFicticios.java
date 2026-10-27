package com.decokasa.seguimiento.archivos;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

/** Bytes ficticios válidos, independientes de la validación del producto. Solo para pruebas. */
public final class ArchivosFicticios {
    private ArchivosFicticios() { }
    public static byte[] pdf() {
        try(var in=ArchivosFicticios.class.getResourceAsStream("/formatos-adr26/valido.pdf")) {
            if(in==null)throw new IllegalStateException("Falta el PDF ficticio ADR26");
            return in.readAllBytes();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    /** CSV de una columna, conserva el tamaño exacto para los tests de límites. */
    public static byte[] csv(int bytes) {byte[] data=new byte[bytes];Arrays.fill(data,(byte)'x');return data;}
}
