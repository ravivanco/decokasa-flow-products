package com.decokasa.seguimiento.plazos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.Instant;
import java.util.Optional;

/**
 * Vencimientos de etapa y de urgencia (arquitectura.md 4.7: CalcularVencimientoEtapa y CalcularVencimientoUrgencia).
 * Lo usa {@code pedidos} al mover un pedido (R4: solo por este puerto).
 */
public interface CalcularVencimiento {

    /**
     * Vencimiento de una etapa en la que el pedido entra en {@code entrada}. Vacío si la etapa no tiene plazo o si su
     * plazo está pendiente del cliente: un plazo pendiente nunca se convierte en cero ni en un valor inventado.
     */
    Optional<Instant> deEtapa(Instant entrada, PlazoDeEtapa plazo);

    /** Vencimiento propio de una urgencia activada en {@code activada} (requerimientos 4.2 #11). */
    Instant deUrgencia(Instant activada, NivelUrgencia nivel);
}
