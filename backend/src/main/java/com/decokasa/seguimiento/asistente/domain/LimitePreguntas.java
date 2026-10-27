package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/** 400 LIMITE_PREGUNTAS: la conversación llegó a su límite (F7-D2). No se llama al modelo. */
public final class LimitePreguntas extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public LimitePreguntas() {
        super(CodigoError.LIMITE_PREGUNTAS, "La conversación llegó a " + ConversacionIA.LIMITE_PREGUNTAS
                + " preguntas. Abre una conversación nueva.");
    }
}
