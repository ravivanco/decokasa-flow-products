package com.decokasa.seguimiento.asistente.adapter.out.claude;

import com.decokasa.seguimiento.asistente.application.port.out.AsistenteIA;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;

/** IA_HABILITADA=false, o sin clave o sin modelo: no se construye ningún cliente y todo responde 503. */
public final class AsistenteIADesactivado implements AsistenteIA {

    @Override
    public Respuesta responder(Consulta consulta) {
        throw new IaNoDisponible();
    }

    @Override
    public long contarTokens(Consulta consulta) {
        throw new IaNoDisponible();
    }
}
