package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/** POST /pedidos/{pedidoId}/asistente/conversaciones. No llama al modelo. */
public interface AbrirConversacion {

    /**
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso cuenta inactiva o rol sin acceso (F7-D4)
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado pedido inexistente o de otro país
     * @throws com.decokasa.seguimiento.asistente.domain.IaNoDisponible si el asistente está desactivado
     */
    ConversacionIA abrir(Solicitante solicitante, UUID pedidoId);
}
