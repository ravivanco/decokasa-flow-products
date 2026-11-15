package com.decokasa.seguimiento.usuarios.application.port.in;

import java.util.UUID;

/** La cuenta tiene una contraseña temporal puesta por Admin y debe cambiarla antes de seguir (V36). */
public interface ConsultarCambioObligatorio {

    boolean debeCambiar(UUID usuarioId);
}
