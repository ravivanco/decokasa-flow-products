package com.decokasa.seguimiento.usuarios.application.port.out;

import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Duration;
import java.time.Instant;

/** Emite el token de acceso (JWT, ADR-0002) con el id y los roles del usuario. */
public interface EmisorTokens {

    String emitirAcceso(Usuario usuario, Instant emitidoEn, Duration duracion);
}
