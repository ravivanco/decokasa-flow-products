package com.decokasa.seguimiento.asistente.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;

/**
 * 400 CONTEXTO_EXCEDIDO: la pregunta, los datos del pedido y el historial, sin ningún PDF, ya superan el límite de
 * entrada. Los PDF que no caben se excluyen con ese motivo sin rechazar la consulta.
 */
public final class ContextoExcedido extends ErrorDeDominio {

    private static final long serialVersionUID = 1L;

    public ContextoExcedido() {
        super(CodigoError.CONTEXTO_EXCEDIDO, "La pregunta y el historial de esta conversación no caben en el límite "
                + "del asistente. Abre una conversación nueva o acorta la pregunta.");
    }
}
