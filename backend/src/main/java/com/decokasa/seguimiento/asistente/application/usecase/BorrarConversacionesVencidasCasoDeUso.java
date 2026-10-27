package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.BorrarConversacionesVencidas;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import java.time.Instant;

/**
 * Borrado diario por lotes (F7-D5, piloto): conversación y mensajes, en cascada. No promete el borrado inmediato de
 * copias de seguridad ni de lo que retenga el proveedor (precisión 4 de Richard).
 */
public final class BorrarConversacionesVencidasCasoDeUso implements BorrarConversacionesVencidas {

    static final int LOTE = 500;
    static final int MAX_LOTES = 200;

    private final ConversacionesIA conversaciones;
    private final Reloj reloj;

    public BorrarConversacionesVencidasCasoDeUso(ConversacionesIA conversaciones, Reloj reloj) {
        this.conversaciones = conversaciones;
        this.reloj = reloj;
    }

    @Override
    public int ejecutar() {
        Instant ahora = reloj.ahora();
        int total = 0;
        for (int lote = 0; lote < MAX_LOTES; lote++) {
            int borradas = conversaciones.borrarVencidas(ahora, LOTE);
            total += borradas;
            if (borradas < LOTE) {
                break;
            }
        }
        return total;
    }
}
