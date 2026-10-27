package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Inicio común (ADR-0029): estadísticas de los pedidos que la persona puede consultar (P-13, países asignados hoy) y sus
 * alertas de retraso. Los agregados se calculan en el servidor sobre todo el conjunto autorizado.
 */
public interface ConsultarEstadisticas {

    Estadisticas estadisticas(Solicitante solicitante, Filtro filtro);

    /** Con {@code todas}, solo el admin vigente: todas las alertas de su ámbito; si no, las de sus responsabilidades. */
    List<Alerta> alertas(Solicitante solicitante, boolean todas);

    /** Nulos: sin filtro (año actual, todos los países permitidos, ambos flujos, año entero). */
    record Filtro(Integer anio, CodigoPais pais, String tipoFlujo, LocalDate desde, LocalDate hasta) { }

    /**
     * @param porAnio todos los años con pedidos (no depende del año ni de las fechas elegidas, sí del país y el flujo)
     * @param tiempoPorPedido pedidos creados en el período, de mayor a menor tiempo, hasta {@code LIMITE_PEDIDOS}
     * @param tiempoPorPedidoTotal cuántos pedidos hay en ese conjunto, aunque la lista se haya recortado
     */
    record Estadisticas(Periodo periodo, List<CodigoPais> paises, Tarjetas tarjetas, List<PorAnio> porAnio,
            List<PorMes> porMes, List<TiempoEtapa> tiempoPorEtapa, List<TiempoPedido> tiempoPorPedido,
            long tiempoPorPedidoTotal, EstadoPedidos estado, List<RetrasoEtapa> retrasosPorEtapa,
            Instant calculadoEn) { }

    /** Tope de la lista de tiempos por pedido; el total se informa aparte. */
    int LIMITE_PEDIDOS = 2000;

    record Periodo(int anio, LocalDate desde, LocalDate hasta) { }

    /** Duraciones en segundos (fechas reales del servidor); {@code medianaResolucionSegundos} nula sin finalizados. */
    record Tarjetas(long creados, long enCurso, long finalizados, long conRetraso, Double medianaResolucionSegundos,
            long resolucionObservaciones) { }

    record PorAnio(int anio, long pedidos) { }

    /** {@code pedidos} nulo solo para un mes futuro; un mes pasado sin pedidos es cero. */
    record PorMes(int mes, Long pedidos) { }

    /**
     * Cada visita (entrada) a la etapa cuenta aparte, también las reentradas tras un rechazo o una devolución. Mediana y
     * promedio en segundos de las visitas terminadas; nulos si ninguna terminó. Las abiertas no entran.
     */
    record TiempoEtapa(String tipoFlujo, CodigoPais pais, int etapa, String nombreEtapa, Double medianaSegundos,
            Double promedioSegundos, long terminadas, long abiertas) { }

    /**
     * Finalizado: {@code segundos} es la duración real (creación → finalización). Abierto: es la antigüedad hasta el
     * momento del cálculo, no una duración final.
     */
    record TiempoPedido(UUID pedidoId, String numero, String tipoFlujo, CodigoPais pais, Instant creadoEn,
            Instant finalizadoEn, boolean finalizado, long segundos) { }

    /**
     * Pedidos creados en el período según su estado de hoy. {@code enCurso} y {@code finalizados} son excluyentes; los
     * tres «enCursoCon…» son subconjuntos de {@code enCurso} que pueden solaparse entre sí.
     */
    record EstadoPedidos(long enCurso, long finalizados, long enCursoConRetraso, long enCursoConUrgencia,
            long enCursoConDemora) { }

    record RetrasoEtapa(String tipoFlujo, int etapa, String nombreEtapa, long pedidos, long etapaVencida,
            long urgenciaVencida) { }

    /**
     * @param tipo URGENCIA_VENCIDA, ETAPA_VENCIDA o DEMORA_REPORTADA
     * @param vence vencimiento de la etapa o de la urgencia; nulo en una demora
     * @param nuevaFechaLlegada nueva fecha estimada de una demora; nula en otro caso
     */
    record Alerta(UUID pedidoId, String numero, String nombrePedido, CodigoPais pais, String tipoFlujo, int etapa,
            String nombreEtapa, String tipo, Instant vence, LocalDate nuevaFechaLlegada, String motivo,
            String asignadoA) { }
}
