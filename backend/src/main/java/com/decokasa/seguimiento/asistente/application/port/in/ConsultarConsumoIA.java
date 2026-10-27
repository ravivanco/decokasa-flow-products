package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.application.port.out.ConsultaConsumoIA.FilaConsumo;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.LocalDate;
import java.util.List;

/** GET /asistente/consumo (solo admin, con sus países asignados hoy). */
public interface ConsultarConsumoIA {

    Consumo consultar(Solicitante solicitante, LocalDate desde, LocalDate hasta);

    /** Presupuesto del mes actual (global del piloto) y consumo por día y usuario del periodo pedido. */
    record Consumo(long presupuestoMesMicroUsd, long consumidoMesMicroUsd, long reservadoMesMicroUsd,
            List<FilaConsumo> dias) {
    }
}
