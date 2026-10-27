package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.out.ConfiguracionPlazosRepositorio;
import com.decokasa.seguimiento.plazos.application.port.out.PlanificacionesOriginales;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import com.decokasa.seguimiento.plazos.domain.CalendariosPorPais;
import com.decokasa.seguimiento.plazos.domain.Cronograma;
import com.decokasa.seguimiento.plazos.domain.PlanificacionOriginal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcula el cronograma con el calendario del país, la meta vigente y la planificación original (ADR-0030). Las reglas
 * están en {@link Cronograma}; aquí solo se traduce entre el puerto y el dominio. Un pedido anterior a V25 no tiene
 * planificación guardada: se reconstruye con las reglas vigentes y se identifica como RECONSTRUIDA, sin guardarla.
 * Sin Spring (R2).
 */
public final class CalcularCronogramaCasoDeUso implements CalcularCronograma {

    private final ConfiguracionPlazosRepositorio configuracion;
    private final CalendariosPorPais calendarios;
    private final CalendarioLaboral calendario;
    private final PlanificacionesOriginales planificaciones;

    /**
     * Fase 7: el calendario de cada país; {@link #calcular} usa el de Ecuador y {@link #paraPais} el del pedido. La
     * meta de días sigue siendo una sola (configuracion_plazos).
     */
    public CalcularCronogramaCasoDeUso(ConfiguracionPlazosRepositorio configuracion, CalendariosPorPais calendarios,
            PlanificacionesOriginales planificaciones) {
        this(configuracion, calendarios, calendarios.de(CodigoPais.EC), planificaciones);
    }

    private CalcularCronogramaCasoDeUso(ConfiguracionPlazosRepositorio configuracion, CalendariosPorPais calendarios,
            CalendarioLaboral calendario, PlanificacionesOriginales planificaciones) {
        this.configuracion = configuracion;
        this.calendarios = calendarios;
        this.calendario = calendario;
        this.planificaciones = planificaciones;
    }

    @Override
    public CalcularCronograma paraPais(CodigoPais pais) {
        return new CalcularCronogramaCasoDeUso(configuracion, calendarios, calendarios.de(pais), planificaciones);
    }

    @Override
    public Resultado calcular(Datos datos) {
        Cronograma.DatosDelPedido pedido = aDominio(datos);
        Cronograma cronograma = Cronograma.calcular(pedido, configuracion.leer(), calendario);
        PlanificacionOriginal original = planificaciones.de(datos.pedidoId())
                .orElseGet(() -> PlanificacionOriginal.desde(datos.pedidoId(), PlanificacionOriginal.Origen.RECONSTRUIDA,
                        datos.ahora(), Cronograma.prever(pedido.soloCreacion(), calendario)));
        Cronograma.Llegada llegada = cronograma.llegada();
        Integer diferenciaContraOriginal = llegada == null || original.llegada() == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(original.llegada(), llegada.fecha()));
        return new Resultado(cronograma.pedidoId(), cronograma.fechaPedido(), datos.ahora(),
                PlanificacionOriginal.REGLAS_VIGENTES, cronograma.metaDias(), cronograma.origenMeta().name(),
                llegada == null ? null : new Llegada(llegada.fecha(), llegada.procedencia().name()),
                new Original(original.origen().name(), original.reglas(), original.calculadaEn(), original.llegada(),
                        original.finalizacion()),
                diferenciaContraOriginal, cronograma.finalizacionEstimada(), cronograma.totalDias(),
                cronograma.diferenciaContraMeta(), cronograma.calculable(),
                cronograma.etapas().stream().map(e -> etapa(e, original.finDe(e.numero()))).toList(),
                cronograma.faltantes().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList(),
                cronograma.faltantesFinalizacion().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList());
    }

    /** Traducción del puerto al dominio; la usa también el registro de la planificación original. */
    static Cronograma.DatosDelPedido aDominio(Datos datos) {
        return new Cronograma.DatosDelPedido(datos.pedidoId(), datos.creadoEn(), datos.etapaActual(),
                datos.finalizado(),
                datos.etapas().stream()
                        .map(e -> new Cronograma.EtapaDelPedido(e.numero(), e.nombre(), e.plazo(), e.registraLlegada(),
                                e.entradaEn(), e.salidaEn(), e.referencia()))
                        .toList(),
                datos.llegadaReal(), datos.llegadaEstimada(), datos.ahora());
    }

    private static Etapa etapa(Cronograma.EtapaPlanificada e, LocalDate original) {
        Cronograma.Duracion d = e.duracion();
        return new Etapa(e.numero(), e.nombre(), original, e.fechaReal() != null ? null : e.fin(), e.fechaReal(),
                e.movidaAlLunes(), e.estado().name(), e.procedencia() == null ? null : e.procedencia().name(),
                e.plazoPendiente(),
                new Duracion(d.base().name(), d.cantidad(), d.tipo() == null ? null : d.tipo().name(),
                        d.fuente() == null ? null : d.fuente().name()));
    }
}
