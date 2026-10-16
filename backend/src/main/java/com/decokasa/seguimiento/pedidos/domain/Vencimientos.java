package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.Instant;
import java.util.Optional;

/**
 * Cálculo de vencimientos que necesita el agregado. El caso de uso (paso 2.4) lo conecta con el puerto
 * CalcularVencimiento del módulo de plazos; el dominio de pedidos no conoce ese módulo (R4).
 */
public interface Vencimientos {

    /** Vacío si la etapa no tiene plazo o si el plazo está pendiente del cliente. */
    Optional<Instant> deEtapa(Instant entrada, PlazoDeEtapa plazo);

    Instant deUrgencia(Instant activada, NivelUrgencia nivel);
}
