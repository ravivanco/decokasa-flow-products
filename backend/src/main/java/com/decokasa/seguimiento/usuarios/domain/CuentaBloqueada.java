package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;
import java.time.Instant;
import java.util.Objects;

/** La cuenta está bloqueada por intentos fallidos (ADR-0002; contrato: 423 CUENTA_BLOQUEADA). */
public final class CuentaBloqueada extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    private final Instant hasta;

    public CuentaBloqueada(Instant hasta) {
        super(CodigoError.CUENTA_BLOQUEADA, "La cuenta está bloqueada por 15 minutos tras 5 intentos fallidos.");
        this.hasta = Objects.requireNonNull(hasta, "hasta");
    }

    public Instant hasta() {
        return hasta;
    }
}
