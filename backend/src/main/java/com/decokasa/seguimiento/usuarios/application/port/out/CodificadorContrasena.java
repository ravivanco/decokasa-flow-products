package com.decokasa.seguimiento.usuarios.application.port.out;

/** Hash de contraseñas (adaptador BCrypt, ADR-0002). Nunca recibe más de 72 bytes en UTF-8. */
public interface CodificadorContrasena {

    String codificar(String contrasena);

    boolean coincide(String contrasena, String hash);
}
