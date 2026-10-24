package com.decokasa.seguimiento.plazos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cronograma de un pedido contra la meta de días (arquitectura.md 4.7, CalcularCronograma; fase 5, paso 5.1, F5-D1
 * opción A). Lo usa {@code pedidos}, que entrega los datos de su pedido (R4: solo por este puerto; R9: plazos no
 * depende de pedidos). Es un cálculo: no decide quién puede ver el pedido.
 *
 * <p>Los nombres de {@code estado} y {@code procedencia} son los del contrato (esquema Cronograma, 0.11.0-borrador).
 */
public interface CalcularCronograma {

    Resultado calcular(Datos datos);

    /**
     * Fase 7: el mismo cálculo con el calendario configurado para el país del pedido. Una implementación con un solo
     * calendario (Ecuador, como hasta la fase 6) se devuelve a sí misma.
     *
     * @throws IllegalArgumentException si el país no tiene calendario configurado
     */
    default CalcularCronograma paraPais(CodigoPais pais) {
        return this;
    }

    /** Una etapa del pedido con su definición (plazo y marca de llegada de ADR-0018). */
    record EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada, Instant entradaEn,
            Instant salidaEn) {
    }

    /**
     * Datos del pedido.
     *
     * @param etapas          todas las etapas de su versión de flujo, en orden
     * @param llegadaReal     check de llegada (pedido.llegada_aduana_en); nulo si no llegó
     * @param llegadaEstimada estimación vigente por demora (pedido.demora_nueva_fecha_llegada, F4-D5); nula si no hay
     */
    record Datos(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado, List<EtapaDelPedido> etapas,
            Instant llegadaReal, LocalDate llegadaEstimada) {
    }

    /**
     * Una etapa del cronograma: fechas planificada, estimada y real por separado.
     *
     * @param estado      REAL, ESTIMADA, EN_CURSO, PLANIFICADA o SIN_ESTIMAR
     * @param procedencia REAL, LLEGADA_REAL, LLEGADA_ESTIMADA o PLANIFICADA; nula si el fin no se puede estimar
     */
    record Etapa(int numero, String nombre, LocalDate fechaPlanificada, boolean movidaAlLunes, LocalDate fechaEstimada,
            LocalDate fechaReal, String estado, String procedencia, boolean plazoPendiente) {
    }

    /** Fecha de llegada comparada con la meta y de dónde sale (LLEGADA_REAL, LLEGADA_ESTIMADA, PLANIFICADA o REAL). */
    record Llegada(LocalDate fecha, String procedencia) {
    }

    /** Etapa con plazo pendiente del cliente que impidió estimar fechas. */
    record Faltante(int numero, String nombre) {
    }

    /**
     * Cronograma calculado.
     *
     * @param origenMeta           PROPUESTA (90, pendiente del cliente) o ADMIN (configurada por el admin)
     * @param llegada              nula si no se puede estimar
     * @param totalDias            días corridos de la fecha del pedido a la llegada; nulo si no es calculable
     * @param diferenciaContraMeta positiva si supera la meta; nula si no es calculable
     */
    record Resultado(UUID pedidoId, LocalDate fechaPedido, int metaDias, String origenMeta, Llegada llegada,
            Integer totalDias, Integer diferenciaContraMeta, boolean calculable, List<Etapa> etapas,
            List<Faltante> faltantes) {
    }
}
