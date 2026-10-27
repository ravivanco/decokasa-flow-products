package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.usuarios.application.port.in.CerrarSesion;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;

/** Cierre de sesión: revoca la familia del token de renovación (ADR-0002, D3b). */
public final class CerrarSesionCasoDeUso implements CerrarSesion {

    private final TokenRenovacionRepositorio tokens;
    private final GeneradorTokenRenovacion generador;
    private final Reloj reloj;

    public CerrarSesionCasoDeUso(TokenRenovacionRepositorio tokens, GeneradorTokenRenovacion generador, Reloj reloj) {
        this.tokens = tokens;
        this.generador = generador;
        this.reloj = reloj;
    }

    @Override
    public void cerrar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            return;
        }
        tokens.buscarPorHash(generador.hash(tokenRenovacion))
                .ifPresent(sesion -> tokens.revocarFamilia(sesion.familiaId(), reloj.ahora()));
    }
}
