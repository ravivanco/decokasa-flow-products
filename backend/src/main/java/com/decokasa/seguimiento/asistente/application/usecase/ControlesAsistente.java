package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;
import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Todos los controles que deben existir antes de cualquier llamada al proveedor, incluida count_tokens: IA habilitada en
 * el entorno (con clave y modelo) y en la base, tarifa vigente del modelo **validada**, límite de entrada bajo el umbral
 * del tramo caro y espera de reserva mayor que la del proveedor. Si falta uno, 503 IA_NO_DISPONIBLE.
 */
final class ControlesAsistente {

    private final ConfiguracionAsistente configuracion;
    private final PresupuestoIA presupuesto;

    ControlesAsistente(ConfiguracionAsistente configuracion, PresupuestoIA presupuesto) {
        this.configuracion = configuracion;
        this.presupuesto = presupuesto;
    }

    record Vigentes(LimitesIA limites, Tarifa tarifa) {
    }

    Vigentes exigir(LocalDate dia) {
        return evaluar(dia).orElseThrow(IaNoDisponible::new);
    }

    Optional<Vigentes> evaluar(LocalDate dia) {
        if (!configuracion.habilitada()) {
            return Optional.empty();
        }
        LimitesIA limites = presupuesto.limites();
        if (!limites.habilitada() || limites.esperaReserva().compareTo(configuracion.esperaProveedor()) <= 0) {
            return Optional.empty();
        }
        return presupuesto.tarifaVigente(configuracion.modelo(), dia)
                .filter(Tarifa::validada)
                .filter(tarifa -> tarifa.admiteLimiteDeEntrada(limites.tokensEntradaMax()))
                .map(tarifa -> new Vigentes(limites, tarifa));
    }
}
