package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** 429 LIMITE_CONSUMO (ADR-0022): no se llamó al modelo. */
public final class LimiteConsumo extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public enum Motivo {
        EN_CURSO("Ya tienes una consulta en curso. Espera a que termine."),
        USUARIO_DIA("Llegaste al límite de consultas de hoy. Vuelve a intentarlo mañana."),
        DIA("El asistente llegó al límite de consultas de hoy. Vuelve a intentarlo mañana."),
        PRESUPUESTO("El asistente llegó al presupuesto de este mes.");

        private final String mensaje;

        Motivo(String mensaje) {
            this.mensaje = mensaje;
        }
    }

    private final Motivo motivo;

    public LimiteConsumo(Motivo motivo) {
        super(CodigoError.LIMITE_CONSUMO, motivo.mensaje);
        this.motivo = motivo;
    }

    public Motivo motivo() {
        return motivo;
    }
}
