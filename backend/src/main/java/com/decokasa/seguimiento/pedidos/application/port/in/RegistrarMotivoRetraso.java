package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/**
 * Registra o corrige el motivo del retraso de la etapa actual (contrato: PUT
 * /pedidos/{id}/etapas/{numeroEtapa}/motivo-retraso; F5-D3, aprobada provisionalmente por Richard el 2026-10-20).
 */
public interface RegistrarMotivoRetraso {

    /** @return el identificador del evento MOTIVO_RETRASO_REGISTRADO del historial */
    UUID registrar(Solicitante solicitante, Comando comando);

    /**
     * @param etapaEsperada     etapa que la persona ve en pantalla
     * @param entradaEsperadaId entrada de esa etapa que la persona ve (P-18): un motivo nunca cae en otra entrada
     */
    record Comando(UUID pedidoId, int etapaEsperada, UUID entradaEsperadaId, String motivo) {
    }
}
