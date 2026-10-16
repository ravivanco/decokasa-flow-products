package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.TipoPlazo;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import java.time.Instant;
import java.util.Optional;

/** Calcula vencimientos con el calendario laboral (ADR-0006). Sin Spring (R2). */
public final class CalcularVencimientoCasoDeUso implements CalcularVencimiento {

    /**
     * Las urgencias de 4, 12 y 24 horas se cuentan en horas hábiles: es el valor provisional documentado
     * (requerimientos §5, "Por confirmar con el cliente"; arquitectura 8.1). Si son hábiles o corridas sigue
     * pendiente del cliente (⏸); el cambio queda en esta constante.
     */
    static final TipoPlazo TIPO_PROVISIONAL_URGENCIA = TipoPlazo.HORAS_HABILES;

    private final CalendarioLaboral calendario;

    public CalcularVencimientoCasoDeUso(CalendarioLaboral calendario) {
        this.calendario = calendario;
    }

    @Override
    public Optional<Instant> deEtapa(Instant entrada, PlazoDeEtapa plazo) {
        return switch (plazo) {
            case PlazoDeEtapa.Definido definido -> Optional.of(calendario.vencimiento(entrada, definido));
            case PlazoDeEtapa.SinPlazo sinPlazo -> Optional.empty();
            case PlazoDeEtapa.Pendiente pendiente -> Optional.empty();
        };
    }

    @Override
    public Instant deUrgencia(Instant activada, NivelUrgencia nivel) {
        return calendario.vencimiento(activada, new PlazoDeEtapa.Definido(nivel.horas(), TIPO_PROVISIONAL_URGENCIA));
    }
}
