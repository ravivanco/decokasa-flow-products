package com.decokasa.seguimiento.pedidos.application.port.out;

import java.util.List;
import java.util.UUID;

/** Lecturas para la alerta de fecha planificada vencida (V34). Dentro de una transacción. */
public interface PlanesVencidos {

    /** Pedidos en curso, del más antiguo al más reciente. */
    List<UUID> enCurso(int limite);

    /** Ya se avisó que esa etapa del pedido superó su fecha planificada. */
    boolean avisado(UUID pedidoId, int etapa);
}
