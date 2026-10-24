package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.out.ConfiguracionPlazosRepositorio;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import com.decokasa.seguimiento.plazos.domain.CalendariosPorPais;
import com.decokasa.seguimiento.plazos.domain.Cronograma;

/**
 * Calcula el cronograma con el calendario laboral y la meta vigente (F5-D1 opción A). Las reglas están en
 * {@link Cronograma}; aquí solo se traduce entre el puerto y el dominio. Sin Spring (R2).
 */
public final class CalcularCronogramaCasoDeUso implements CalcularCronograma {

    private final ConfiguracionPlazosRepositorio configuracion;
    private final CalendarioLaboral calendario;
    private final CalendariosPorPais calendarios;

    /** Un solo calendario para todos los pedidos (Ecuador, como hasta la fase 6). */
    public CalcularCronogramaCasoDeUso(ConfiguracionPlazosRepositorio configuracion, CalendarioLaboral calendario) {
        this.configuracion = configuracion;
        this.calendario = calendario;
        this.calendarios = null;
    }

    /**
     * Fase 7: el calendario de cada país; {@link #calcular} usa el de Ecuador y {@link #paraPais} el del pedido. La
     * meta de días sigue siendo una sola (configuracion_plazos).
     */
    public CalcularCronogramaCasoDeUso(ConfiguracionPlazosRepositorio configuracion, CalendariosPorPais calendarios) {
        this.configuracion = configuracion;
        this.calendario = calendarios.de(CodigoPais.EC);
        this.calendarios = calendarios;
    }

    @Override
    public CalcularCronograma paraPais(CodigoPais pais) {
        return calendarios == null ? this : new CalcularCronogramaCasoDeUso(configuracion, calendarios.de(pais));
    }

    @Override
    public Resultado calcular(Datos datos) {
        Cronograma cronograma = Cronograma.calcular(aDominio(datos), configuracion.leer(), calendario);
        Cronograma.Llegada llegada = cronograma.llegada();
        return new Resultado(cronograma.pedidoId(), cronograma.fechaPedido(), cronograma.metaDias(),
                cronograma.origenMeta().name(),
                llegada == null ? null : new Llegada(llegada.fecha(), llegada.procedencia().name()),
                cronograma.totalDias(), cronograma.diferenciaContraMeta(), cronograma.calculable(),
                cronograma.etapas().stream().map(CalcularCronogramaCasoDeUso::etapa).toList(),
                cronograma.faltantes().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList());
    }

    private static Cronograma.DatosDelPedido aDominio(Datos datos) {
        return new Cronograma.DatosDelPedido(datos.pedidoId(), datos.creadoEn(), datos.etapaActual(),
                datos.finalizado(),
                datos.etapas().stream()
                        .map(e -> new Cronograma.EtapaDelPedido(e.numero(), e.nombre(), e.plazo(), e.registraLlegada(),
                                e.entradaEn(), e.salidaEn()))
                        .toList(),
                datos.llegadaReal(), datos.llegadaEstimada());
    }

    private static Etapa etapa(Cronograma.EtapaPlanificada e) {
        return new Etapa(e.numero(), e.nombre(), e.fechaPlanificada(), e.movidaAlLunes(), e.fechaEstimada(),
                e.fechaReal(), e.estado().name(), e.procedencia() == null ? null : e.procedencia().name(),
                e.plazoPendiente());
    }
}
