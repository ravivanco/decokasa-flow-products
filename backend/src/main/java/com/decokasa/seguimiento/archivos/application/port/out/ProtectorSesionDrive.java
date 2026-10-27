package com.decokasa.seguimiento.archivos.application.port.out;

import java.util.UUID;

/** Cifrado autenticado, vinculado al UUID de adjunto. Llave externa a Git y PostgreSQL. */
public interface ProtectorSesionDrive {
    byte[] cifrar(UUID adjuntoId, AlmacenArchivos.Sesion sesion);
    AlmacenArchivos.Sesion abrir(UUID adjuntoId, byte[] cifrado);
}
