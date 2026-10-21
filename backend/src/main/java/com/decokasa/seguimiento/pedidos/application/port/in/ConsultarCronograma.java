package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.util.UUID;

/**
 * Cronograma de un pedido contra la meta (contrato: GET /pedidos/{id}/cronograma; fase 5, paso 5.1). Lo ve quien puede
 * consultar el pedido (P-13, provisional del equipo). El cálculo es de plazos (F5-D1 opción A).
 */
public interface ConsultarCronograma {

    CalcularCronograma.Resultado cronograma(Solicitante solicitante, UUID pedidoId);
}
