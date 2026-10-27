package com.decokasa.seguimiento.pedidos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarEstadisticas;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Agregados de solo lectura sobre pedidos (ADR-0029), siempre limitados a {@code paises} (no vacío) y, si se indica, a un
 * flujo. Los períodos son semiabiertos [inicio, fin). Un retraso vigente es una etapa actual con {@code vence_en} pasado
 * o una urgencia activa vencida.
 */
public interface EstadisticasDePedidos {

    ConsultarEstadisticas.Tarjetas tarjetas(Set<CodigoPais> paises, String flujo, Instant inicio, Instant fin,
            Instant ahora);

    /** Pedidos creados por año (zona de Ecuador), solo años con pedidos. */
    Map<Integer, Long> creadosPorAnio(Set<CodigoPais> paises, String flujo);

    /** Pedidos creados por mes del año indicado (zona de Ecuador), solo meses con pedidos. */
    Map<Integer, Long> creadosPorMes(Set<CodigoPais> paises, String flujo, int anio);

    List<ConsultarEstadisticas.TiempoEtapa> tiempoPorEtapa(Set<CodigoPais> paises, String flujo, Instant inicio,
            Instant fin);

    /** Pedidos creados en [inicio, fin), de mayor a menor tiempo, como mucho {@code limite}; con el total. */
    TiemposPedidos tiempoPorPedido(Set<CodigoPais> paises, String flujo, Instant inicio, Instant fin, Instant ahora,
            int limite);

    record TiemposPedidos(List<ConsultarEstadisticas.TiempoPedido> pedidos, long total) { }

    ConsultarEstadisticas.EstadoPedidos estado(Set<CodigoPais> paises, String flujo, Instant inicio, Instant fin,
            Instant ahora);

    List<ConsultarEstadisticas.RetrasoEtapa> retrasosPorEtapa(Set<CodigoPais> paises, String flujo, Instant ahora);

    /**
     * Pedidos sin finalizar con alguna alerta activa. Sin {@code todas}: solo los de la etapa de la que la persona es
     * responsable (P-06: asignada a ella, o sin asignar con su rol).
     */
    List<FilaAlerta> alertas(Set<CodigoPais> paises, UUID usuarioId, Set<CodigoRol> roles, boolean todas,
            Instant ahora);

    record FilaAlerta(UUID pedidoId, String numero, String nombre, CodigoPais pais, String tipoFlujo, int etapa,
            String nombreEtapa, Instant etapaVence, String motivoRetraso, boolean urgenciaActiva, Instant urgenciaVence,
            Instant demoraReportadaEn, java.time.LocalDate demoraNuevaFecha, String demoraMotivo,
            Instant llegadaAduana, String asignadoA) { }
}
