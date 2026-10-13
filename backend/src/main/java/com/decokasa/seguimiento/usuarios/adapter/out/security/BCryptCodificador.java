package com.decokasa.seguimiento.usuarios.adapter.out.security;

import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Hash BCrypt de las contraseñas (ADR-0002). Segunda barrera del límite de 72 bytes en UTF-8: no codifica ni compara
 * nada más largo, sin depender de cómo lo trate la versión de Spring Security; así nunca hay recorte silencioso.
 */
public final class BCryptCodificador implements CodificadorContrasena {

    private final BCryptPasswordEncoder bcrypt;

    public BCryptCodificador(int fuerza) {
        this.bcrypt = new BCryptPasswordEncoder(fuerza);
    }

    @Override
    public String codificar(String contrasena) {
        if (contrasena == null || ContrasenaNueva.superaElLimite(contrasena)) {
            throw new IllegalArgumentException("La contraseña debe tener como máximo 72 bytes en UTF-8.");
        }
        return bcrypt.encode(contrasena);
    }

    @Override
    public boolean coincide(String contrasena, String hash) {
        if (contrasena == null || hash == null || ContrasenaNueva.superaElLimite(contrasena)) {
            return false;
        }
        return bcrypt.matches(contrasena, hash);
    }
}
