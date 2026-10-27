package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.EstimacionDeEtapa;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.TipoPlazo;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cronograma de un pedido: estimación actual de cada etapa, de la llegada y de la finalización, frente a la meta de
 * días (fase 5, paso 5.1; ADR-0030, que sustituye en parte a F5-D1 opción A). Cada etapa usa su duración para estimar
 * con el {@link CalendarioLaboral} del país (días u horas hábiles con la hora de Ecuador, o corridos si así se
 * declara). La calculadora antigua del cliente sumaba 3 + 8 + 31 + 12 + 45 días corridos entre seis hitos; aquí solo se
 * usan sus 31 y 45 días, como duración de referencia de etapas sin plazo aprobado. Java puro (R1).
 *
 * <p>Reglas (ADR-0030):
 * <ul>
 *   <li>Duración para estimar: el plazo definido de la etapa; si el plazo está pendiente, su duración de referencia
 *       documentada (V25). Una etapa sin ninguna de las dos queda "sin estimar", igual que las que dependen de ella;
 *       nunca vale cero. {@code SIN_PLAZO} (A1, Fin) no añade duración. La referencia no es un plazo: no genera
 *       vencimientos ni cambia las alertas.</li>
 *   <li>Arrastre: una etapa empieza cuando termina la anterior. La actual y las ya recorridas empiezan en su última
 *       entrada real (cada reentrada empieza de nuevo); las que están por delante de la actual no usan entradas
 *       antiguas, porque se volverán a recorrer.</li>
 *   <li>Fin de cada etapa: su salida real; en la etapa de llegada, la llegada real (prevalece) o la fecha de la demora
 *       (F4-D5); si no, inicio + duración.</li>
 *   <li>Proyección: si la etapa actual sigue abierta después de su fin estimado (o del día de la demora), se supone
 *       que termina ahora, el momento más temprano posible, y las siguientes se calculan desde ahí. El vencimiento de
 *       la etapa no cambia. Sin reloj ({@code ahora} nulo) no se proyecta.</li>
 *   <li>Llegada y finalización son distintas: una etapa sin duración después de la llegada (Incidencias) deja sin
 *       estimar la finalización, no la llegada.</li>
 *   <li>El total frente a la meta son días corridos de la fecha del pedido a la llegada, como la métrica del PRD.</li>
 * </ul>
 * Feriados y jornada laboral siguen pendientes del cliente (⏸ F5-D7, F4-D10) y no se descuentan.
 */
public record Cronograma(UUID pedidoId, LocalDate fechaPedido, int metaDias, OrigenMeta origenMeta, Llegada llegada,
        Integer totalDias, Integer diferenciaContraMeta, List<EtapaPlanificada> etapas, List<Faltante> faltantes,
        LocalDate finalizacionEstimada, List<Faltante> faltantesFinalizacion) {

    public Cronograma {
        etapas = List.copyOf(etapas);
        faltantes = List.copyOf(faltantes);
        faltantesFinalizacion = List.copyOf(faltantesFinalizacion);
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
        /** Inicio + duración para estimar, con el calendario laboral. */
        CALCULADA,
        /** La etapa actual superó su fin estimado y sigue abierta: se supone que termina ahora. */
        PROYECTADA
    }

    /** Cómo está la etapa en el cronograma. */
    public enum EstadoEtapa {
        REAL,
        EN_CURSO,
        /** Etapa actual abierta después de su fin estimado. */
        EXCEDIDA,
        ESTIMADA,
        SIN_ESTIMAR
    }

    /** De dónde sale la duración con la que se estima una etapa. */
    public enum BaseDuracion {
        /** Plazo definido en el flujo: también genera el vencimiento. */
        PLAZO,
        /** Duración de referencia documentada (V25); el plazo sigue pendiente y no hay vencimiento. */
        REFERENCIA,
        /** La etapa no suma tiempo (A1, Fin). */
        SIN_PLAZO,
        /** Ninguna fuente documenta su duración. */
        SIN_DATO
    }

    /** Duración usada para estimar una etapa; cantidad, tipo y fuente solo cuando corresponden. */
    public record Duracion(BaseDuracion base, Integer cantidad, TipoPlazo tipo, EstimacionDeEtapa.Fuente fuente) {
        public Duracion {
            Objects.requireNonNull(base, "base");
        }
    }

    /**
     * Una etapa del cronograma.
     *
     * @param fechaPlanificada inicio + duración para estimar; nula si no hay duración o no se conoce el inicio
     * @param fechaEstimada    fecha de la demora (F4-D5) o de la proyección de una etapa excedida; nula si no aplica
     * @param fechaReal        salida o llegada registrada
     * @param procedencia      de dónde sale el fin con el que empieza la siguiente; nula si no se conoce
     * @param plazoPendiente   el cliente no definió el plazo de responsabilidad de esta etapa (⏸)
     */
    public record EtapaPlanificada(int numero, String nombre, LocalDate fechaPlanificada, boolean movidaAlLunes,
            LocalDate fechaEstimada, LocalDate fechaReal, EstadoEtapa estado, Procedencia procedencia,
            boolean plazoPendiente, Duracion duracion) {

        /** El fin vigente: real, si no estimado, si no planificado; nulo si no se puede estimar. */
        public LocalDate fin() {
            return fechaReal != null ? fechaReal : fechaEstimada != null ? fechaEstimada : fechaPlanificada;
        }
    }

    /** Fecha de llegada que se compara con la meta, con su procedencia. */
    public record Llegada(LocalDate fecha, Procedencia procedencia) {
    }

    /** Etapa sin duración documentada que impidió estimar su fin (y el de las que dependen de ella). */
    public record Faltante(int numero, String nombre) {
    }

    /**
     * Una etapa del pedido con su definición, tal como la entrega el módulo de pedidos.
     *
     * @param referencia duración de referencia de la etapa (V25); nula si no tiene
     */
    public record EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada,
            Instant entradaEn, Instant salidaEn, EstimacionDeEtapa referencia) {
        public EtapaDelPedido {
            Objects.requireNonNull(plazo, "plazo");
        }

        /** Etapa sin duración de referencia. */
        public EtapaDelPedido(int numero, String nombre, PlazoDeEtapa plazo, boolean registraLlegada,
                Instant entradaEn, Instant salidaEn) {
            this(numero, nombre, plazo, registraLlegada, entradaEn, salidaEn, null);
        }
    }

    /**
     * Lo que hace falta del pedido.
     *
     * @param etapas          todas las etapas de su versión de flujo, en orden
     * @param llegadaReal     check de llegada de la etapa de llegada; nulo si no llegó
     * @param llegadaEstimada estimación vigente por demora (F4-D5); nula si no hay demora
     * @param ahora           momento del cálculo (reloj del servidor); nulo, sin proyección de etapas excedidas
     */
    public record DatosDelPedido(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado,
            List<EtapaDelPedido> etapas, Instant llegadaReal, LocalDate llegadaEstimada, Instant ahora) {
        public DatosDelPedido {
            Objects.requireNonNull(pedidoId, "pedidoId");
            Objects.requireNonNull(creadoEn, "creadoEn");
            etapas = List.copyOf(etapas);
        }

        /** Sin reloj: no se proyectan las etapas excedidas. */
        public DatosDelPedido(UUID pedidoId, Instant creadoEn, int etapaActual, boolean finalizado,
                List<EtapaDelPedido> etapas, Instant llegadaReal, LocalDate llegadaEstimada) {
            this(pedidoId, creadoEn, etapaActual, finalizado, etapas, llegadaReal, llegadaEstimada, null);
        }

        /**
         * El mismo pedido tal como estaba al crearse: en su primera etapa, sin entradas, salidas, demora ni llegada,
         * calculado en el momento de la creación. Es la base de la planificación original (ADR-0030).
         */
        public DatosDelPedido soloCreacion() {
            List<EtapaDelPedido> sinHechos = etapas.stream()
                    .map(e -> new EtapaDelPedido(e.numero(), e.nombre(), e.plazo(), e.registraLlegada(), null, null,
                            e.referencia()))
                    .toList();
            int primera = etapas.isEmpty() ? etapaActual : etapas.get(0).numero();
            return new DatosDelPedido(pedidoId, creadoEn, primera, false, sinHechos, null, null, creadoEn);
        }
    }

    /** Fechas estimadas de un pedido, sin la comparación con la meta. */
    public record Prevision(LocalDate fechaPedido, List<EtapaPlanificada> etapas, Llegada llegada,
            List<Faltante> faltantes, LocalDate finalizacionEstimada, List<Faltante> faltantesFinalizacion) {
        public Prevision {
            etapas = List.copyOf(etapas);
            faltantes = List.copyOf(faltantes);
            faltantesFinalizacion = List.copyOf(faltantesFinalizacion);
        }
    }

    /** Calcula el cronograma con la configuración vigente. */
    public static Cronograma calcular(DatosDelPedido pedido, ConfiguracionPlazos configuracion,
            CalendarioLaboral calendario) {
        Prevision prevision = prever(pedido, calendario);
        Llegada llegada = prevision.llegada();
        Integer total = llegada == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(prevision.fechaPedido(), llegada.fecha()));
        Integer diferencia = total == null ? null : total - configuracion.metaDias();
        return new Cronograma(pedido.pedidoId(), prevision.fechaPedido(), configuracion.metaDias(),
                configuracion.origenMeta(), llegada, total, diferencia, prevision.etapas(), prevision.faltantes(),
                prevision.finalizacionEstimada(), prevision.faltantesFinalizacion());
    }

    /** Las fechas de cada etapa, la llegada y la finalización con las reglas de ADR-0030. */
    public static Prevision prever(DatosDelPedido pedido, CalendarioLaboral calendario) {
        LocalDate fechaPedido = calendario.fecha(pedido.creadoEn());
        Instant ahora = pedido.ahora();
        List<EtapaPlanificada> etapas = new ArrayList<>();
        List<Faltante> faltantes = new ArrayList<>();
        List<Faltante> faltantesFinalizacion = new ArrayList<>();
        Llegada llegada = null;
        boolean despuesDeLlegada = false;
        // Fin de la etapa anterior: desde ahí empieza la siguiente. Nulo si no se puede estimar.
        Instant finAnterior = pedido.creadoEn();

        for (EtapaDelPedido etapa : pedido.etapas()) {
            boolean recorrida = pedido.finalizado() || etapa.numero() < pedido.etapaActual();
            boolean actual = !pedido.finalizado() && etapa.numero() == pedido.etapaActual();
            // Las etapas por delante de la actual no usan datos de entradas anteriores: se volverán a recorrer.
            Instant inicio = (recorrida || actual) && etapa.entradaEn() != null ? etapa.entradaEn() : finAnterior;
            Duracion duracion = duracion(etapa);

            LocalDate planificada = null;
            boolean movida = false;
            Instant finPlanificado = null;
            if (inicio != null) {
                switch (duracion.base()) {
                    case PLAZO, REFERENCIA -> {
                        CalendarioLaboral.FinDePlazo fin = calendario.finDePlazo(inicio,
                                new PlazoDeEtapa.Definido(duracion.cantidad(), duracion.tipo()));
                        planificada = fin.fecha();
                        movida = fin.movidaAlLunes();
                        finPlanificado = fin.momento();
                    }
                    case SIN_PLAZO -> {
                        planificada = calendario.fecha(inicio);
                        finPlanificado = inicio;
                    }
                    case SIN_DATO -> {
                        // Sin duración documentada: no se planifica (no es cero ni "sin plazo").
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
                    // La llegada real prevalece sobre cualquier estimación.
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
                procedencia = Procedencia.CALCULADA;
            }

            // Proyección de la etapa actual abierta después de su fin estimado. La fecha de una demora es un día: se
            // considera superada cuando ese día ya pasó.
            boolean excedida = false;
            if (actual && real == null && ahora != null && finEfectivo != null) {
                excedida = procedencia == Procedencia.LLEGADA_ESTIMADA
                        ? estimada.isBefore(calendario.fecha(ahora))
                        : finEfectivo.isBefore(ahora);
                if (excedida) {
                    finEfectivo = ahora;
                    estimada = calendario.fecha(ahora);
                    procedencia = Procedencia.PROYECTADA;
                }
            }

            EstadoEtapa estado;
            if (real != null) {
                estado = EstadoEtapa.REAL;
            } else if (actual) {
                estado = excedida ? EstadoEtapa.EXCEDIDA : EstadoEtapa.EN_CURSO;
            } else if (finEfectivo != null) {
                estado = EstadoEtapa.ESTIMADA;
            } else {
                estado = EstadoEtapa.SIN_ESTIMAR;
            }

            if (finEfectivo == null && duracion.base() == BaseDuracion.SIN_DATO) {
                (despuesDeLlegada ? faltantesFinalizacion : faltantes).add(new Faltante(etapa.numero(), etapa.nombre()));
            }
            EtapaPlanificada resultado = new EtapaPlanificada(etapa.numero(), etapa.nombre(), planificada, movida,
                    estimada, real, estado, procedencia, etapa.plazo() instanceof PlazoDeEtapa.Pendiente, duracion);
            if (etapa.registraLlegada()) {
                if (procedencia != null) {
                    llegada = new Llegada(resultado.fin(), procedencia);
                }
                despuesDeLlegada = true;
            }
            etapas.add(resultado);
            finAnterior = finEfectivo;
        }

        EtapaPlanificada ultima = etapas.isEmpty() ? null : etapas.get(etapas.size() - 1);
        LocalDate finalizacion = pedido.finalizado() || ultima == null || ultima.procedencia() == null ? null
                : ultima.fin();
        return new Prevision(fechaPedido, etapas, llegada, faltantes, finalizacion, faltantesFinalizacion);
    }

    private static Duracion duracion(EtapaDelPedido etapa) {
        return switch (etapa.plazo()) {
            case PlazoDeEtapa.Definido definido ->
                    new Duracion(BaseDuracion.PLAZO, definido.cantidad(), definido.tipo(), null);
            case PlazoDeEtapa.SinPlazo sinPlazo -> new Duracion(BaseDuracion.SIN_PLAZO, null, null, null);
            case PlazoDeEtapa.Pendiente sinDefinir -> {
                EstimacionDeEtapa referencia = etapa.referencia();
                yield referencia == null ? new Duracion(BaseDuracion.SIN_DATO, null, null, null)
                        : new Duracion(BaseDuracion.REFERENCIA, referencia.cantidad(), referencia.tipo(),
                                referencia.fuente());
            }
        };
    }
}
