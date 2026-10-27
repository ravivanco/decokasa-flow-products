package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.RegistrarPlanificacionOriginal;
import com.decokasa.seguimiento.plazos.application.port.out.PlanificacionesOriginales;
import com.decokasa.seguimiento.plazos.domain.CalendariosPorPais;
import com.decokasa.seguimiento.plazos.domain.Cronograma;
import com.decokasa.seguimiento.plazos.domain.PlanificacionOriginal;
import java.util.UUID;

/**
 * Calcula y guarda la planificación original de un pedido nuevo con el calendario de su país (ADR-0030). Usa solo la
 * definición del flujo y la hora de creación, que es también el momento del cálculo. Sin Spring (R2).
 */
public final class RegistrarPlanificacionOriginalCasoDeUso implements RegistrarPlanificacionOriginal {

    private final CalendariosPorPais calendarios;
    private final PlanificacionesOriginales planificaciones;

    public RegistrarPlanificacionOriginalCasoDeUso(CalendariosPorPais calendarios,
            PlanificacionesOriginales planificaciones) {
        this.calendarios = calendarios;
        this.planificaciones = planificaciones;
    }

    @Override
    public void registrar(CodigoPais pais, UUID flujoId, CalcularCronograma.Datos datos) {
        Cronograma.DatosDelPedido alCrear = CalcularCronogramaCasoDeUso.aDominio(datos).soloCreacion();
        Cronograma.Prevision prevision = Cronograma.prever(alCrear, calendarios.de(pais));
        planificaciones.guardar(flujoId, PlanificacionOriginal.desde(datos.pedidoId(),
                PlanificacionOriginal.Origen.GUARDADA, datos.creadoEn(), prevision));
    }
}
