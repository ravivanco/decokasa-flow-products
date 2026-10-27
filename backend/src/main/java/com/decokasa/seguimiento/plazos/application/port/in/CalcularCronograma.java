package com.decokasa.seguimiento.plazos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.EstimacionDeEtapa;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cronograma de un pedido: planificación original, estimación actual y fechas reales, frente a la meta (arquitectura.md
 * 4.7, CalcularCronograma; fase 5, paso 5.1; ADR-0030). Lo usa {@code pedidos}, que entrega los datos de su pedido (R4:
 * solo por este puerto; R9: plazos no depende de pedidos). Es un cálculo: no decide quién puede ver el pedido.
 *
 * <p>Los nombres de {@code estado}, {@code procedencia}, {@code base} y {@code origen} son los del contrato (esquema
 * Cronograma, 0.25.0-borrador).
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

    /**
     * Una etapa del pedido con su definición (plazo, marca de llegada de ADR-0018 y duración de referencia de V25).
     *
     * @param referencia duración de referencia; nula si la etapa no tiene
     */
    record EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada, Instant entradaEn,
            Instant salidaEn, EstimacionDeEtapa referencia) {

        /** Etapa sin duración de referencia. */
        public EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada,
                Instant entradaEn, Instant salidaEn) {
            this(numero, nombre, plazo, registraLlegada, entradaEn, salidaEn, null);
        }
    }

    /**
     * Datos del pedido.
     *
     * @param etapas          todas las etapas de su versión de flujo, en orden
     * @param llegadaReal     check de llegada (pedido.llegada_aduana_en); nulo si no llegó
     * @param llegadaEstimada estimación vigente por demora (pedido.demora_nueva_fecha_llegada, F4-D5); nula si no hay
     * @param ahora           momento del cálculo según el reloj del servidor; nulo, sin proyección de etapas excedidas
     */
    record Datos(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado, List<EtapaDelPedido> etapas,
            Instant llegadaReal, LocalDate llegadaEstimada, Instant ahora) {

        /** Sin reloj: no se proyectan las etapas excedidas. */
        public Datos(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado,
                List<EtapaDelPedido> etapas, Instant llegadaReal, LocalDate llegadaEstimada) {
            this(pedidoId, creadoEn, etapaActual, finalizado, etapas, llegadaReal, llegadaEstimada, null);
        }
    }

    /**
     * Duración usada para estimar una etapa.
     *
     * @param base   PLAZO, REFERENCIA, SIN_PLAZO o SIN_DATO
     * @param fuente CALCULADORA_ORIGINAL o REQUERIMIENTOS_4_5 si es una referencia; nula si no
     */
    record Duracion(String base, Integer cantidad, String tipo, String fuente) {
    }

    /**
     * Una etapa del cronograma: fechas original, estimada y real por separado.
     *
     * @param fechaOriginal fin según la planificación original; nulo si no se podía estimar
     * @param fechaEstimada fin según la estimación actual; nulo si la etapa ya tiene fecha real o no se puede estimar
     * @param estado        REAL, EN_CURSO, EXCEDIDA, ESTIMADA o SIN_ESTIMAR
     * @param procedencia   REAL, LLEGADA_REAL, LLEGADA_ESTIMADA, CALCULADA o PROYECTADA; nula si no se conoce el fin
     */
    record Etapa(int numero, String nombre, LocalDate fechaOriginal, LocalDate fechaEstimada, LocalDate fechaReal,
            boolean movidaAlLunes, String estado, String procedencia, boolean plazoPendiente, Duracion duracion) {
    }

    /** Fecha de llegada comparada con la meta y de dónde sale (LLEGADA_REAL, LLEGADA_ESTIMADA, CALCULADA, ...). */
    record Llegada(LocalDate fecha, String procedencia) {
    }

    /** Etapa sin duración documentada que impidió estimar fechas. */
    record Faltante(int numero, String nombre) {
    }

    /**
     * Planificación original.
     *
     * @param origen GUARDADA (al crear el pedido) o RECONSTRUIDA (pedido anterior, calculada al consultar)
     */
    record Original(String origen, String reglas, Instant calculadaEn, LocalDate llegada, LocalDate finalizacion) {
    }

    /**
     * Cronograma calculado.
     *
     * @param calculadoEn              momento del cálculo (reloj del servidor)
     * @param reglas                   versión de las reglas de estimación
     * @param origenMeta               PROPUESTA (90, pendiente del cliente) o ADMIN (configurada por el admin)
     * @param llegada                  llegada real o estimada actual; nula si no se puede estimar
     * @param diferenciaContraOriginal días corridos de la llegada original a la actual; nulo si falta alguna
     * @param finalizacionEstimada     nula si el pedido terminó o falta una duración
     * @param totalDias                días corridos de la fecha del pedido a la llegada; nulo si no es calculable
     * @param diferenciaContraMeta     positiva si supera la meta; nula si no es calculable
     * @param faltantes                etapas sin duración que impiden estimar la llegada
     * @param faltantesFinalizacion    etapas sin duración posteriores a la llegada
     */
    record Resultado(UUID pedidoId, LocalDate fechaPedido, Instant calculadoEn, String reglas, int metaDias,
            String origenMeta, Llegada llegada, Original original, Integer diferenciaContraOriginal,
            LocalDate finalizacionEstimada, Integer totalDias, Integer diferenciaContraMeta, boolean calculable,
            List<Etapa> etapas, List<Faltante> faltantes, List<Faltante> faltantesFinalizacion) {
    }
}
