package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cronograma de un pedido contra la meta de días (fase 5, paso 5.1; F5-D1 opción A, aprobada por Richard el
 * 2026-10-20). Reemplaza la calculadora antigua del cliente, pero no copia sus fechas: aquella sumaba días corridos
 * (8 + 31, 12 y 45) y solo movía al lunes la fecha final; aquí cada etapa usa su plazo definido con el
 * {@link CalendarioLaboral} (días u horas hábiles con la hora de Ecuador, o corridos si la etapa lo declara). Java puro
 * (R1).
 *
 * <p>Reglas (F5-D1, opción A):
 * <ul>
 *   <li>Cada etapa termina en una fecha <b>real</b> (salida o llegada registrada), <b>estimada</b> (llegada estimada
 *       por demora, F4-D5) o <b>planificada</b> (inicio + plazo definido). Las tres se devuelven por separado.</li>
 *   <li>Arrastre: una etapa empieza cuando termina la anterior (real si existe; si no, estimada o planificada). La
 *       etapa actual y las ya recorridas empiezan en su última entrada real (el plazo se reinicia en cada reentrada).</li>
 *   <li>Un plazo {@code PENDIENTE} del cliente no es cero ni "sin plazo": esa etapa y las que dependen de ella quedan
 *       "sin estimar". {@code SIN_PLAZO} (A1, Fin) no añade duración.</li>
 *   <li>El cálculo se retoma desde un dato de llegada válido de la etapa de llegada (ADR-0018). La llegada real
 *       prevalece sobre la estimada y se conserva su procedencia.</li>
 *   <li>El total y la diferencia contra la meta solo se calculan si se conoce la fecha de llegada; nunca con
 *       duraciones faltantes. El total se cuenta en días corridos de la fecha del pedido a la llegada, como la métrica
 *       del PRD ("fecha de check de Aduana menos fecha de creación"); los plazos de cada etapa siguen siendo hábiles.</li>
 * </ul>
 * Feriados y jornada laboral siguen pendientes del cliente (⏸ F5-D7, F4-D10) y no se descuentan.
 */
public record Cronograma(UUID pedidoId, LocalDate fechaPedido, int metaDias, OrigenMeta origenMeta, Llegada llegada,
        Integer totalDias, Integer diferenciaContraMeta, List<EtapaPlanificada> etapas, List<Faltante> faltantes) {

    public Cronograma {
        etapas = List.copyOf(etapas);
        faltantes = List.copyOf(faltantes);
    }

    /** Si el total y la diferencia contra la meta se pudieron calcular. */
    public boolean calculable() {
        return totalDias != null;
    }

    /** De dónde sale la fecha con la que termina una etapa. */
    public enum Procedencia {
        /** Salida registrada en la línea de tiempo del pedido. */
        REAL,
        /** Llegada a aduana registrada con el check de la etapa de llegada (ADR-0018). */
        LLEGADA_REAL,
        /** Llegada estimada que escribió quien reportó la demora (F4-D5); no es un vencimiento. */
        LLEGADA_ESTIMADA,
        /** Inicio + plazo definido, con el calendario laboral. */
        PLANIFICADA
    }

    /** Cómo está la etapa en el cronograma. */
    public enum EstadoEtapa {
        REAL,
        ESTIMADA,
        EN_CURSO,
        PLANIFICADA,
        SIN_ESTIMAR
    }

    /**
     * Una etapa del cronograma.
     *
     * @param fechaPlanificada inicio + plazo definido; nula si el plazo está pendiente o si no se conoce el inicio
     * @param fechaEstimada    solo en la etapa de llegada con demora reportada y sin llegada real (F4-D5)
     * @param fechaReal        salida o llegada registrada
     * @param procedencia      de dónde sale el fin de la etapa con el que empieza la siguiente; nula si no se conoce
     * @param plazoPendiente   el cliente no definió el plazo de esta etapa (⏸)
     */
    public record EtapaPlanificada(int numero, String nombre, LocalDate fechaPlanificada, boolean movidaAlLunes,
            LocalDate fechaEstimada, LocalDate fechaReal, EstadoEtapa estado, Procedencia procedencia,
            boolean plazoPendiente) {
    }

    /** Fecha de llegada que se compara con la meta, con su procedencia. */
    public record Llegada(LocalDate fecha, Procedencia procedencia) {
    }

    /** Etapa cuyo plazo pendiente impidió estimar su fin (y el de las que dependen de ella). */
    public record Faltante(int numero, String nombre) {
    }

    /** Una etapa del pedido con su definición, tal como la entrega el módulo de pedidos. */
    public record EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada,
            Instant entradaEn, Instant salidaEn) {
        public EtapaDelPedido {
            Objects.requireNonNull(plazo, "plazo");
        }
    }

    /**
     * Lo que hace falta del pedido.
     *
     * @param etapas          todas las etapas de su versión de flujo, en orden
     * @param llegadaReal     check de llegada de la etapa de llegada; nulo si no llegó
     * @param llegadaEstimada estimación vigente por demora (F4-D5); nula si no hay demora
     */
    public record DatosDelPedido(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado,
            List<EtapaDelPedido> etapas, Instant llegadaReal, LocalDate llegadaEstimada) {
        public DatosDelPedido {
            Objects.requireNonNull(pedidoId, "pedidoId");
            Objects.requireNonNull(creadoEn, "creadoEn");
            etapas = List.copyOf(etapas);
        }
    }

    /** Calcula el cronograma con la configuración vigente. */
    public static Cronograma calcular(DatosDelPedido pedido, ConfiguracionPlazos configuracion,
            CalendarioLaboral calendario) {
        LocalDate fechaPedido = calendario.fecha(pedido.creadoEn());
        List<EtapaPlanificada> etapas = new ArrayList<>();
        List<Faltante> faltantes = new ArrayList<>();
        Llegada llegada = null;
        // Fin de la etapa anterior: desde ahí empieza la siguiente. Nulo si no se puede estimar.
        Instant finAnterior = pedido.creadoEn();

        for (EtapaDelPedido etapa : pedido.etapas()) {
            boolean recorrida = pedido.finalizado() || etapa.numero() < pedido.etapaActual();
            boolean actual = !pedido.finalizado() && etapa.numero() == pedido.etapaActual();
            // Las etapas por delante de la actual no usan datos de entradas anteriores: se volverán a recorrer.
            Instant inicio = (recorrida || actual) && etapa.entradaEn() != null ? etapa.entradaEn() : finAnterior;
            boolean pendiente = etapa.plazo() instanceof PlazoDeEtapa.Pendiente;

            LocalDate planificada = null;
            boolean movida = false;
            Instant finPlanificado = null;
            if (inicio != null) {
                switch (etapa.plazo()) {
                    case PlazoDeEtapa.Definido definido -> {
                        CalendarioLaboral.FinDePlazo fin = calendario.finDePlazo(inicio, definido);
                        planificada = fin.fecha();
                        movida = fin.movidaAlLunes();
                        finPlanificado = fin.momento();
                    }
                    case PlazoDeEtapa.SinPlazo sinPlazo -> {
                        planificada = calendario.fecha(inicio);
                        finPlanificado = inicio;
                    }
                    case PlazoDeEtapa.Pendiente sinDefinir -> {
                        // Sin duración conocida: no se planifica (no es cero ni "sin plazo").
                    }
                }
            }

            LocalDate real = null;
            LocalDate estimada = null;
            Instant finEfectivo = null;
            Procedencia procedencia = null;
            if (recorrida && etapa.salidaEn() != null) {
                real = calendario.fecha(etapa.salidaEn());
                finEfectivo = etapa.salidaEn();
                procedencia = Procedencia.REAL;
            }
            if (etapa.registraLlegada()) {
                if (pedido.llegadaReal() != null) {
                    // La llegada real prevalece sobre la estimada.
                    real = calendario.fecha(pedido.llegadaReal());
                    finEfectivo = pedido.llegadaReal();
                    procedencia = Procedencia.LLEGADA_REAL;
                } else if (pedido.llegadaEstimada() != null) {
                    estimada = pedido.llegadaEstimada();
                    finEfectivo = calendario.inicioDelDia(estimada);
                    procedencia = Procedencia.LLEGADA_ESTIMADA;
                }
            }
            if (finEfectivo == null && finPlanificado != null) {
                finEfectivo = finPlanificado;
                procedencia = Procedencia.PLANIFICADA;
            }

            EstadoEtapa estado;
            if (real != null) {
                estado = EstadoEtapa.REAL;
            } else if (estimada != null) {
                estado = EstadoEtapa.ESTIMADA;
            } else if (actual) {
                estado = EstadoEtapa.EN_CURSO;
            } else if (planificada != null) {
                estado = EstadoEtapa.PLANIFICADA;
            } else {
                estado = EstadoEtapa.SIN_ESTIMAR;
            }

            if (pendiente && finEfectivo == null) {
                faltantes.add(new Faltante(etapa.numero(), etapa.nombre()));
            }
            if (etapa.registraLlegada() && procedencia != null) {
                LocalDate fechaLlegada = real != null ? real : estimada != null ? estimada : planificada;
                llegada = new Llegada(fechaLlegada, procedencia);
            }

            etapas.add(new EtapaPlanificada(etapa.numero(), etapa.nombre(), planificada, movida, estimada, real, estado,
                    procedencia, pendiente));
            finAnterior = finEfectivo;
        }

        Integer total = llegada == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(fechaPedido, llegada.fecha()));
        Integer diferencia = total == null ? null : total - configuracion.metaDias();
        return new Cronograma(pedido.pedidoId(), fechaPedido, configuracion.metaDias(), configuracion.origenMeta(),
                llegada, total, diferencia, etapas, faltantes);
    }
}
