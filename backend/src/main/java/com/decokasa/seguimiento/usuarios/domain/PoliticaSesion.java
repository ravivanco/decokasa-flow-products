package com.decokasa.seguimiento.usuarios.domain;

import java.time.Duration;

/** Duración de la sesión (ADR-0002, decisión del equipo D3). */
public record PoliticaSesion(Duration acceso, Duration renovacion) {

    /** Token de acceso de 15 minutos; renovación hasta 8 horas desde el inicio de sesión, con rotación. */
    public static final PoliticaSesion ACORDADA = new PoliticaSesion(Duration.ofMinutes(15), Duration.ofHours(8));
}
