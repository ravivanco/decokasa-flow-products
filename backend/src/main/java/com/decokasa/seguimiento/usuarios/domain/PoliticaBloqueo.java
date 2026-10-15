package com.decokasa.seguimiento.usuarios.domain;

import java.time.Duration;

/** Bloqueo por intentos fallidos de contraseña (ADR-0002, decisión del equipo D2). */
public record PoliticaBloqueo(int intentosMaximos, Duration duracion) {

    /** 5 intentos fallidos bloquean la cuenta durante 15 minutos. */
    public static final PoliticaBloqueo ACORDADA = new PoliticaBloqueo(5, Duration.ofMinutes(15));
}
