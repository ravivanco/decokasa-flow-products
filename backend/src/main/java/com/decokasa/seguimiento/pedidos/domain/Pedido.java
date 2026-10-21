package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.EtapaCambio;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.RequisitoFaltante;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.eventos.BodegaAsignada;
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.DemoraReportada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaReasignada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoRetrasoRegistrado;
import com.decokasa.seguimiento.compartido.domain.eventos.ObservacionAgregada;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoCreado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoFinalizado;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Agregado pedido (arquitectura.md 4.3; modelo-datos.md §3.3). Mueve el pedido por las etapas de su flujo según la
 * regla que da la definición del flujo (ADR-0004). Java puro (R1): no conoce Spring, JPA ni los módulos de flujos,
 * plazos y usuarios (R4).
 *
 * <p>Invariantes (requerimientos 4.2):
 * <ul>
 *   <li>mientras no está finalizado, el pedido tiene exactamente una etapa ACTUAL;</li>
 *   <li>número, país, tipo de flujo y versión del flujo no cambian;</li>
 *   <li>el plazo normal de una etapa se reinicia en cada reentrada; un plazo pendiente no genera vencimiento;</li>
 *   <li>cada cambio deja un evento con actor, rol de intervención y hora del servidor (R7).</li>
 * </ul>
 *
 * <p>Quién actúa lo decide {@link PermisosEtapa} con el estado vigente de la persona. Los eventos quedan pendientes en
 * el agregado: el caso de uso (paso 2.4) guarda el pedido y los publica en la misma transacción, y el historial los
 * inserta en ella (acción e historial son atómicos).
 */
public final class Pedido {

    private static final int NOMBRE_MAXIMO = 200;
    private static final int DESCRIPCION_MAXIMA = 4000;
    private static final int OBSERVACION_MAXIMA = 4000;
    private static final int MOTIVO_REASIGNACION_MAXIMO = 1000;

    private final UUID id;
    private final NumeroPedido numero;
    private final CodigoPais pais;
    private final TipoFlujo tipoFlujo;
    private final UUID flujoId;
    private final String nombre;
    private final String descripcion;
    private final boolean incluyeProductosNuevos;
    private final UUID creadoPor;
    private final Instant creadoEn;
    private final List<EtapaDePedido> etapas;
    private int etapaActual;
    private boolean finalizado;
    private Instant finalizadoEn;
    private UrgenciaActiva urgencia;
    // Identidad estable de la activación vigente (paso 5.2): la entrada que creó la devolución con urgencia.
    private UUID urgenciaActivacionId;
    private Instant llegadaAduanaEn;
    private DemoraAduana demora;
    private UUID bodegaId;
    private Instant actualizadoEn;
    private final long version;
    private final List<EventoDePedido> eventosPendientes = new ArrayList<>();

    /** Reconstruye un pedido guardado que todavía no llegó a aduana ni tiene demora. */
    public Pedido(UUID id, NumeroPedido numero, CodigoPais pais, TipoFlujo tipoFlujo, UUID flujoId, String nombre,
            String descripcion, boolean incluyeProductosNuevos, UUID creadoPor, Instant creadoEn,
            List<EtapaDePedido> etapas, int etapaActual, boolean finalizado, Instant finalizadoEn,
            UrgenciaActiva urgencia, Instant actualizadoEn, long version) {
        this(id, numero, pais, tipoFlujo, flujoId, nombre, descripcion, incluyeProductosNuevos, creadoPor, creadoEn,
                etapas, etapaActual, finalizado, finalizadoEn, urgencia, null, null, null, actualizadoEn, version);
    }

    /**
     * Reconstruye un pedido guardado y comprueba que tenga una sola etapa ACTUAL, la que dice {@code etapaActual}.
     *
     * @param llegadaAduanaEn momento del check de llegada de A7; nulo si todavía no llegó
     * @param demora demora vigente en aduana (F4-D5); nula si no se reportó ninguna
     * @param bodegaId bodega de destino, asignada una sola vez al aprobar Bodega (F4-D2); nula hasta entonces
     */
    public Pedido(UUID id, NumeroPedido numero, CodigoPais pais, TipoFlujo tipoFlujo, UUID flujoId, String nombre,
            String descripcion, boolean incluyeProductosNuevos, UUID creadoPor, Instant creadoEn,
            List<EtapaDePedido> etapas, int etapaActual, boolean finalizado, Instant finalizadoEn,
            UrgenciaActiva urgencia, Instant llegadaAduanaEn, DemoraAduana demora, UUID bodegaId,
            Instant actualizadoEn, long version) {
        this(id, numero, pais, tipoFlujo, flujoId, nombre, descripcion, incluyeProductosNuevos, creadoPor, creadoEn,
                etapas, etapaActual, finalizado, finalizadoEn, urgencia, null, llegadaAduanaEn, demora, bodegaId,
                actualizadoEn, version);
    }

    /**
     * Como el anterior, con la identidad de la activación de la urgencia vigente (fase 5, paso 5.2).
     *
     * @param urgenciaActivacionId entrada creada por la devolución que activó la urgencia; se ignora sin urgencia
     */
    public Pedido(UUID id, NumeroPedido numero, CodigoPais pais, TipoFlujo tipoFlujo, UUID flujoId, String nombre,
            String descripcion, boolean incluyeProductosNuevos, UUID creadoPor, Instant creadoEn,
            List<EtapaDePedido> etapas, int etapaActual, boolean finalizado, Instant finalizadoEn,
            UrgenciaActiva urgencia, UUID urgenciaActivacionId, Instant llegadaAduanaEn, DemoraAduana demora,
            UUID bodegaId, Instant actualizadoEn, long version) {
        this.id = Objects.requireNonNull(id, "id");
        this.numero = Objects.requireNonNull(numero, "numero");
        this.pais = Objects.requireNonNull(pais, "pais");
        this.tipoFlujo = Objects.requireNonNull(tipoFlujo, "tipoFlujo");
        this.flujoId = Objects.requireNonNull(flujoId, "flujoId");
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.incluyeProductosNuevos = incluyeProductosNuevos;
        this.creadoPor = Objects.requireNonNull(creadoPor, "creadoPor");
        this.creadoEn = Objects.requireNonNull(creadoEn, "creadoEn");
        this.etapas = new ArrayList<>(etapas);
        this.etapas.sort(Comparator.comparingInt(EtapaDePedido::numero));
        this.etapaActual = etapaActual;
        this.finalizado = finalizado;
        this.finalizadoEn = finalizadoEn;
        this.urgencia = urgencia;
        this.urgenciaActivacionId = urgencia == null ? null : urgenciaActivacionId;
        this.llegadaAduanaEn = llegadaAduanaEn;
        this.demora = demora;
        this.bodegaId = bodegaId;
        this.actualizadoEn = Objects.requireNonNull(actualizadoEn, "actualizadoEn");
        this.version = version;
        if (!finalizado) {
            verificarUnaSolaActual();
        }
    }

    /**
     * Pedido nuevo en la primera etapa de su flujo (A1 / B1), con el evento {@link PedidoCreado} pendiente. El número
     * lo reserva el generador (paso 2.4).
     *
     * @param creador estado vigente de quien crea el pedido
     * @throws SinPermiso si el creador no tiene el rol responsable de la primera etapa (el admin solo crea los flujos
     *         cuya primera etapa es suya, como A1; P-14)
     * @throws DatoInvalido si un dato está fuera de sus límites
     */
    public static Pedido crear(NumeroPedido numero, CodigoPais pais, FlujoDelPedido flujo, String nombre,
            String descripcion, boolean incluyeProductosNuevos, ActorVigente creador, Instant ahora,
            Vencimientos vencimientos) {
        Interviniente interviniente = PermisosEtapa.paraCrear(flujo.rolResponsableInicial(), creador);
        String nombreLimpio = nombre == null ? "" : nombre.strip();
        if (nombreLimpio.isEmpty() || nombreLimpio.length() > NOMBRE_MAXIMO) {
            throw new DatoInvalido("nombre", "El nombre del pedido debe tener entre 1 y 200 caracteres.");
        }
        String descripcionLimpia = descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
        if (descripcionLimpia != null && descripcionLimpia.length() > DESCRIPCION_MAXIMA) {
            throw new DatoInvalido("descripcion", "La descripción tiene como máximo 4000 caracteres.");
        }
        if (incluyeProductosNuevos && flujo.tipo() != TipoFlujo.A) {
            throw new DatoInvalido("incluyeProductosNuevos", "Solo el flujo A mezcla productos existentes y nuevos.");
        }
        List<Integer> numerosDeEtapa = flujo.numerosDeEtapa();
        if (numerosDeEtapa.isEmpty() || numerosDeEtapa.stream().distinct().count() != numerosDeEtapa.size()) {
            throw new DatoInvalido("El flujo del pedido debe tener etapas con números distintos.");
        }
        List<EtapaDePedido> etapas = numerosDeEtapa.stream()
                .sorted()
                .map(EtapaDePedido::pendiente)
                .toList();
        EtapaDePedido primera = etapas.get(0);
        primera.entrar(ahora, vencimientos.deEtapa(ahora, flujo.plazoInicial()).orElse(null));
        Pedido pedido = new Pedido(UUID.randomUUID(), numero, pais, flujo.tipo(), flujo.flujoId(), nombreLimpio,
                descripcionLimpia, incluyeProductosNuevos, creador.usuarioId(), ahora, etapas, primera.numero(), false,
                null, null, ahora, 0);
        pedido.eventosPendientes.add(new PedidoCreado(pedido.datosDelPedido(), primera.numero(), interviniente,
                ahora));
        return pedido;
    }

    /**
     * Ejecuta una acción sobre la etapa actual y deja su evento pendiente. No aplica ningún cambio si algo no se
     * cumple. El admin pasa por las mismas validaciones que el responsable.
     *
     * @param regla lo que dice la definición del flujo de esa acción en la etapa actual
     * @param habilitadas etapas cuyas acciones están implementadas en esta fase (P-04)
     * @param actor estado vigente de quien pide la acción
     * @return el evento de la acción, que también queda en {@link #retirarEventos()}
     * @throws TransicionNoPermitida si el pedido terminó, la etapa no está habilitada en esta fase o es automática,
     *         sus documentos obligatorios siguen pendientes de definir o la acción deja el pedido en la misma etapa
     * @throws EtapaCambio si la etapa que la persona veía ya no es la actual
     * @throws SinPermiso si la persona no puede actuar en la etapa
     * @throws DatoInvalido si la observación es demasiado larga o llega una urgencia que la acción no pide
     * @throws RequisitoFaltante si falta la observación, la urgencia, la bodega o un documento obligatorio
     */
    public EventoDePedido ejecutar(AccionSolicitada solicitud, ReglaDeTransicion regla, EtapasHabilitadas habilitadas,
            ActorVigente actor, Instant ahora, Vencimientos vencimientos) {
        return ejecutar(solicitud, regla, habilitadas, actor, ahora, vencimientos, null);
    }

    /**
     * Como {@link #ejecutar(AccionSolicitada, ReglaDeTransicion, EtapasHabilitadas, ActorVigente, Instant,
     * Vencimientos)}, y además REPORTAR_DEMORA (F4-D5), que no mueve el pedido y necesita el día de hoy en Ecuador.
     *
     * @param hoyEnEcuador fecha de {@code ahora} en Ecuador; obligatoria solo para REPORTAR_DEMORA
     * @throws DatoInvalido además, si la fecha estimada de una demora queda antes de hoy o no es posterior a la
     *         estimación vigente, o si llega una fecha estimada con otra acción
     */
    public EventoDePedido ejecutar(AccionSolicitada solicitud, ReglaDeTransicion regla, EtapasHabilitadas habilitadas,
            ActorVigente actor, Instant ahora, Vencimientos vencimientos, LocalDate hoyEnEcuador) {
        if (finalizado) {
            throw new TransicionNoPermitida("El pedido ya terminó.");
        }
        if (solicitud.etapaEsperada() != etapaActual) {
            throw new EtapaCambio("La etapa del pedido cambió. Vuelve a cargarlo antes de actuar.");
        }
        if (regla.etapaOrigen() != etapaActual || regla.accion() != solicitud.accion()) {
            throw new IllegalArgumentException("La regla no corresponde a la acción ni a la etapa actual.");
        }
        if (!habilitadas.incluye(etapaActual)) {
            throw new TransicionNoPermitida("Las acciones de esta etapa todavía no están habilitadas.");
        }
        EtapaDePedido origen = etapa(etapaActual);
        Interviniente interviniente = PermisosEtapa.paraActuar(origen, regla.rolResponsableOrigen(), actor);
        if (regla.documentosPendientes()) {
            // S-2: que no haya documentos configurados no significa que no se pidan.
            throw new TransicionNoPermitida(
                    "Los documentos obligatorios de esta etapa todavía no están definidos; no se puede avanzar.");
        }
        Integer destino = regla.etapaDestino();
        if (destino == null) {
            if (solicitud.accion() == AccionEtapa.REPORTAR_DEMORA) {
                return reportarDemora(solicitud, regla, interviniente, ahora, hoyEnEcuador);
            }
            throw new TransicionNoPermitida("Esta acción todavía no está implementada.");
        }

        AccionEtapa accion = solicitud.accion();
        String observacion = validarObservacion(solicitud.observacion());
        NivelUrgencia nivel = solicitud.nivelUrgencia();
        if (nivel != null && accion != AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO) {
            throw new DatoInvalido("nivelUrgencia", "La urgencia solo se indica al devolver por producto nuevo.");
        }
        if (solicitud.nuevaFechaEstimadaLlegada() != null) {
            throw new DatoInvalido("nuevaFechaEstimadaLlegada",
                    "La fecha estimada de llegada solo se indica al reportar una demora.");
        }
        UUID bodegaElegida = solicitud.bodegaId();
        if (bodegaElegida != null && !regla.requiereBodega()) {
            throw new DatoInvalido("bodegaId", "La bodega solo se elige al aprobar la etapa de Bodega.");
        }
        if (regla.requiereBodega() && bodegaId != null) {
            // F4-D2: la bodega se asigna una sola vez; cambiarla después queda pendiente del cliente.
            throw new TransicionNoPermitida("El pedido ya tiene una bodega asignada; no se cambia.");
        }
        exigirRequisitos(regla, observacion, nivel, solicitud);

        if (destino > origen.numero()) {
            origen.salir(ahora, EstadoEtapa.COMPLETADA);
        } else {
            // El pedido vuelve atrás: la etapa de origen y las completadas entre el destino y el origen quedan
            // DEVUELTAS.
            origen.salir(ahora, EstadoEtapa.DEVUELTA);
            for (EtapaDePedido intermedia : etapas) {
                if (intermedia.numero() > destino && intermedia.numero() < origen.numero()
                        && intermedia.estado() == EstadoEtapa.COMPLETADA) {
                    intermedia.marcarDevuelta();
                }
            }
        }

        boolean urgenciaTerminada = false;
        if (accion == AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO) {
            // La urgencia tiene su propio vencimiento desde la devolución hasta volver a Análisis (4.2 #11). Una
            // devolución nueva reemplaza a la anterior.
            urgencia = new UrgenciaActiva(nivel, ahora, vencimientos.deUrgencia(ahora, nivel));
        } else if (urgencia != null && regla.destinoEsRetornoDeUrgencia()) {
            urgencia = null;
            urgenciaTerminada = true;
        }
        // PROVISIONAL mientras P-10 siga pendiente del cliente (⏸): un rechazo de Revisión durante una urgencia
        // (A3 → A2, B4 → B3) no la cambia y su vencimiento sigue contando hasta volver a Análisis (A4 / B5). No es una
        // regla aprobada.

        EtapaDePedido etapaDestino = etapa(destino);
        etapaDestino.entrar(ahora, regla.destinoEsAutomatico() ? null
                : vencimientos.deEtapa(ahora, regla.plazoDestino()).orElse(null));
        if (accion == AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO) {
            // Paso 5.2: la identidad de esta activación es la entrada que acaba de crear la devolución. Dos
            // activaciones nunca comparten entrada, aunque compartan la hora del servidor.
            urgenciaActivacionId = etapaDestino.entradaId();
        } else if (urgencia == null) {
            urgenciaActivacionId = null;
        }
        etapaActual = destino;
        if (regla.destinoEsAutomatico()) {
            // Fin es visual: entrada y salida coinciden con la confirmación, sin etapa ACTUAL ni vencimiento.
            etapaDestino.salir(ahora, EstadoEtapa.COMPLETADA);
            finalizado = true;
            finalizadoEn = ahora;
        }
        if (regla.registraLlegada()) {
            // "David da check cuando llega la mercadería" (4.4, A7): la llegada es la hora del servidor de ese check.
            llegadaAduanaEn = ahora;
        }
        if (regla.requiereBodega()) {
            // F4-D2: la bodega se guarda solo al aprobar Bodega, con el avance y sus eventos (una sola transacción).
            bodegaId = bodegaElegida;
        }
        actualizadoEn = ahora;
        if (finalizado) {
            verificarEtapasAlFinalizar();
        } else {
            verificarUnaSolaActual();
        }

        DatosDelPedido datos = datosDelPedido();
        int etapaOrigen = origen.numero();
        List<UUID> adjuntos = solicitud.adjuntoIds();
        EventoDePedido evento = switch (accion) {
            case ENVIAR -> new EtapaEnviada(datos, etapaOrigen, interviniente, ahora, destino, observacion,
                    urgenciaTerminada, adjuntos);
            case APROBAR -> new EtapaAprobada(datos, etapaOrigen, interviniente, ahora, destino, observacion,
                    urgenciaTerminada, adjuntos);
            case RECHAZAR -> new EtapaRechazada(datos, etapaOrigen, interviniente, ahora, destino, observacion,
                    urgenciaTerminada, adjuntos);
            case DEVOLVER_POR_ERROR -> new PedidoDevuelto(datos, etapaOrigen, interviniente, ahora, destino,
                    MotivoDevolucion.ERROR, observacion, null, adjuntos);
            case DEVOLVER_POR_PRODUCTO_NUEVO -> new PedidoDevuelto(datos, etapaOrigen, interviniente, ahora, destino,
                    MotivoDevolucion.PRODUCTO_NUEVO, observacion, nivel, adjuntos);
            case REPORTAR_DEMORA, ELEGIR_BODEGA ->
                    throw new IllegalStateException("Una acción sin etapa de destino no mueve el pedido.");
        };
        if (regla.requiereBodega()) {
            eventosPendientes.add(new BodegaAsignada(datos, etapaOrigen, interviniente, ahora, bodegaElegida));
        }
        eventosPendientes.add(evento);
        if (regla.destinoEsAutomatico()) {
            // Misma hora del servidor que la acción. El orden de lectura por UUID sigue pendiente (F4-H01).
            eventosPendientes.add(new PedidoFinalizado(datos, destino, ahora));
        }
        return evento;
    }

    /**
     * Demora en aduana (F4-D5, provisional del equipo, pendiente de confirmación del cliente). No mueve el pedido, no
     * reinicia plazos ni cambia vencimientos: la fecha estimada no es un vencimiento (el plazo de Aduana sigue
     * pendiente, F4-D6). Primero la fecha pasa al lunes si cae en fin de semana; después se comprueba que la fecha
     * efectiva no sea anterior a hoy en Ecuador y que sea posterior a la estimación vigente, si hay una. El pedido
     * guarda la demora vigente y el evento conserva la estimación anterior y, si es distinta, la fecha solicitada.
     */
    private DemoraReportada reportarDemora(AccionSolicitada solicitud, ReglaDeTransicion regla,
            Interviniente interviniente, Instant ahora, LocalDate hoyEnEcuador) {
        if (hoyEnEcuador == null) {
            // Error de programación: REPORTAR_DEMORA se ejecuta con la sobrecarga que recibe el día de hoy en Ecuador.
            throw new IllegalStateException("REPORTAR_DEMORA necesita el día de hoy en Ecuador.");
        }
        String observacion = validarObservacion(solicitud.observacion());
        if (solicitud.nivelUrgencia() != null) {
            throw new DatoInvalido("nivelUrgencia", "La urgencia solo se indica al devolver por producto nuevo.");
        }
        if (solicitud.bodegaId() != null) {
            throw new DatoInvalido("bodegaId", "La bodega solo se elige al aprobar la etapa de Bodega.");
        }
        LocalDate solicitada = solicitud.nuevaFechaEstimadaLlegada();
        // El requisito pide observación con la demora, digan lo que digan los datos; la fecha es parte de F4-D5.
        List<String> faltantes = new ArrayList<>();
        if (observacion == null) {
            faltantes.add("observación");
        }
        if (solicitada == null) {
            faltantes.add("fecha estimada de llegada");
        }
        regla.documentosObligatorios().stream()
                .filter(tipo -> !solicitud.documentosPresentes().contains(tipo))
                .sorted()
                .forEach(faltantes::add);
        if (regla.exigeAlgunDocumento() && solicitud.documentosPresentes().isEmpty()) {
            faltantes.add(ReglaDeTransicion.FALTA_ALGUN_DOCUMENTO);
        }
        if (!faltantes.isEmpty()) {
            throw new RequisitoFaltante(faltantes);
        }
        LocalDate efectiva = alLunesSiEsFinDeSemana(solicitada);
        if (efectiva.isBefore(hoyEnEcuador)) {
            throw new DatoInvalido("nuevaFechaEstimadaLlegada",
                    "La fecha estimada de llegada no puede ser anterior a hoy.");
        }
        LocalDate anterior = demora == null ? null : demora.nuevaFechaEstimadaLlegada();
        if (anterior != null && !efectiva.isAfter(anterior)) {
            throw new DatoInvalido("nuevaFechaEstimadaLlegada",
                    "Una demora deja la llegada después de la estimación vigente (" + anterior + ").");
        }
        demora = new DemoraAduana(observacion, efectiva, interviniente.usuarioId(), ahora);
        actualizadoEn = ahora;
        DemoraReportada evento = new DemoraReportada(datosDelPedido(), etapaActual, interviniente, ahora, observacion,
                anterior, solicitada.equals(efectiva) ? null : solicitada, efectiva, solicitud.adjuntoIds());
        eventosPendientes.add(evento);
        return evento;
    }

    /** Requerimientos §5: una fecha que cae en fin de semana pasa al lunes (también la llegada del barco). */
    private static LocalDate alLunesSiEsFinDeSemana(LocalDate fecha) {
        return switch (fecha.getDayOfWeek()) {
            case SATURDAY -> fecha.plusDays(2);
            case SUNDAY -> fecha.plusDays(1);
            default -> fecha;
        };
    }

    /**
     * El admin reasigna la etapa actual (P-07, valor provisional del equipo). Si la etapa ya es de esa persona, no
     * cambia nada ni deja evento.
     *
     * @param numeroEtapa la etapa que el admin quiere reasignar
     * @param rolResponsable rol responsable de esa etapa según la definición del flujo; nulo si es automática
     * @param admin estado vigente de quien reasigna
     * @param candidato estado vigente de quien recibe la etapa
     * @param motivo opcional, hasta 1000 caracteres
     * @throws SinPermiso si quien reasigna no es un admin vigente
     * @throws TransicionNoPermitida si el pedido terminó o la etapa es automática
     * @throws EtapaCambio si {@code numeroEtapa} no es la etapa actual
     * @throws DatoInvalido si la persona no está activa, no tiene el rol responsable o el motivo es demasiado largo
     */
    public Optional<EtapaReasignada> reasignar(int numeroEtapa, CodigoRol rolResponsable, ActorVigente admin,
            ActorVigente candidato, String motivo, Instant ahora) {
        Interviniente interviniente = PermisosEtapa.paraReasignar(admin);
        if (finalizado) {
            throw new TransicionNoPermitida("El pedido ya terminó.");
        }
        if (numeroEtapa != etapaActual) {
            throw new EtapaCambio("Solo se reasigna la etapa actual, y la etapa del pedido cambió.");
        }
        if (rolResponsable == null) {
            throw new TransicionNoPermitida("Esta etapa avanza sola; no se asigna a nadie.");
        }
        PermisosEtapa.exigirAsignable(rolResponsable, candidato);
        String motivoLimpio = motivo == null || motivo.isBlank() ? null : motivo.strip();
        if (motivoLimpio != null && motivoLimpio.length() > MOTIVO_REASIGNACION_MAXIMO) {
            throw new DatoInvalido("motivo", "El motivo tiene como máximo 1000 caracteres.");
        }
        EtapaDePedido actual = etapa(etapaActual);
        UUID anterior = actual.asignadoA();
        if (candidato.usuarioId().equals(anterior)) {
            return Optional.empty();
        }
        actual.asignar(candidato.usuarioId());
        actualizadoEn = ahora;
        EtapaReasignada evento = new EtapaReasignada(datosDelPedido(), etapaActual, interviniente, ahora, anterior,
                candidato.usuarioId(), motivoLimpio);
        eventosPendientes.add(evento);
        return Optional.of(evento);
    }

    /**
     * Comprueba que la persona puede subir un archivo a la entrada actual de la etapa actual (P-05, P-06, P-18) y
     * devuelve con qué rol interviene. No cambia el pedido: el archivo lo guarda el módulo de archivos.
     *
     * @param entradaEsperadaId la entrada que la persona ve en pantalla
     * @param rolResponsable rol responsable de la etapa según la definición del flujo; nulo si es automática
     * @throws TransicionNoPermitida si el pedido terminó o la etapa no está habilitada en esta fase
     * @throws EtapaCambio si la etapa ya no es la actual o volvió a entrar (aunque tenga el mismo número)
     * @throws SinPermiso si la persona no puede actuar en la etapa
     */
    public Interviniente autorizarSubida(int numeroEtapa, UUID entradaEsperadaId, CodigoRol rolResponsable,
            EtapasHabilitadas habilitadas, ActorVigente actor) {
        return autorizarEnEntradaActual(numeroEtapa, entradaEsperadaId, rolResponsable, habilitadas, actor,
                "La etapa del pedido cambió. Vuelve a cargarlo antes de subir el archivo.");
    }

    /**
     * Comprueba que la persona puede agregar una observación a la entrada actual de la etapa actual (P-19, provisional
     * del equipo): la misma regla que para actuar (P-06) o el admin, con estado, roles y asignación vigentes. No cambia
     * el pedido.
     *
     * @throws TransicionNoPermitida si el pedido terminó o la etapa no está habilitada en esta fase
     * @throws EtapaCambio si la etapa ya no es la actual o volvió a entrar (aunque tenga el mismo número)
     * @throws SinPermiso si la persona no puede actuar en la etapa
     */
    public Interviniente autorizarObservacion(int numeroEtapa, UUID entradaEsperadaId, CodigoRol rolResponsable,
            EtapasHabilitadas habilitadas, ActorVigente actor) {
        return autorizarEnEntradaActual(numeroEtapa, entradaEsperadaId, rolResponsable, habilitadas, actor,
                "La etapa del pedido cambió. Vuelve a cargarlo antes de agregar la observación.");
    }

    /**
     * Agrega una observación a la entrada actual de la etapa actual (P-19). No avanza la etapa, no reinicia plazos ni
     * cambia el pedido: solo deja el evento, con actor, rol de intervención y hora del servidor. Los archivos ya deben
     * estar validados contra la entrada actual (P-05, P-18).
     *
     * @param texto obligatorio, hasta 4000 caracteres
     * @return el evento, que también queda en {@link #retirarEventos()}
     * @throws DatoInvalido si el texto está vacío o es demasiado largo
     */
    public ObservacionAgregada agregarObservacion(int numeroEtapa, UUID entradaEsperadaId, String texto,
            List<UUID> adjuntoIds, CodigoRol rolResponsable, EtapasHabilitadas habilitadas, ActorVigente actor,
            Instant ahora) {
        Interviniente interviniente = autorizarObservacion(numeroEtapa, entradaEsperadaId, rolResponsable,
                habilitadas, actor);
        String limpia = validarObservacion(texto);
        if (limpia == null) {
            throw new DatoInvalido("texto", "La observación no puede estar vacía.");
        }
        ObservacionAgregada evento = new ObservacionAgregada(datosDelPedido(), etapaActual, interviniente, ahora,
                limpia, UUID.randomUUID(), adjuntoIds);
        eventosPendientes.add(evento);
        return evento;
    }

    /**
     * Registra o corrige el motivo del retraso de la entrada actual de la etapa actual (F5-D3, aprobada
     * provisionalmente por Richard el 2026-10-20). Puede hacerlo quien puede actuar en la etapa (P-06) o el admin, con
     * estado, roles y asignación vigentes; {@code consulta} y {@code asistente_china} no conceden el permiso ni quitan
     * el de otros roles. Solo con un vencimiento activo: el de la etapa o el de la urgencia. Guarda el motivo vigente;
     * cada registro o corrección deja su propio evento en el historial (los anteriores no se tocan). No mueve el
     * pedido, no cambia plazos y no genera correo.
     *
     * @param texto obligatorio, hasta 4000 caracteres
     * @return el evento, que también queda en {@link #retirarEventos()}
     * @throws EtapaCambio si la etapa ya no es la actual o volvió a entrar
     * @throws TransicionNoPermitida si no hay un vencimiento activo o el pedido terminó
     * @throws DatoInvalido si el texto está vacío o es demasiado largo
     */
    public MotivoRetrasoRegistrado registrarMotivoRetraso(int numeroEtapa, UUID entradaEsperadaId, String texto,
            CodigoRol rolResponsable, EtapasHabilitadas habilitadas, ActorVigente actor, Instant ahora) {
        Interviniente interviniente = autorizarEnEntradaActual(numeroEtapa, entradaEsperadaId, rolResponsable,
                habilitadas, actor, "La etapa del pedido cambió. Vuelve a cargarlo antes de registrar el motivo.");
        if (!tieneVencimientoActivo(ahora)) {
            throw new TransicionNoPermitida(
                    "No hay un plazo vencido en esta etapa: el motivo del retraso se registra con un vencimiento activo.");
        }
        String limpio = texto == null ? "" : texto.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalido("motivo", "Escribe el motivo del retraso.");
        }
        if (limpio.length() > 4000) {
            throw new DatoInvalido("motivo", "El motivo del retraso admite hasta 4000 caracteres.");
        }
        EtapaDePedido actual = etapa(etapaActual);
        actual.registrarMotivo(limpio);
        actualizadoEn = ahora;
        MotivoRetrasoRegistrado evento = new MotivoRetrasoRegistrado(datosDelPedido(), etapaActual, interviniente,
                ahora, limpio, actual.entradaId(), UUID.randomUUID());
        eventosPendientes.add(evento);
        return evento;
    }

    /**
     * Si la etapa actual o la urgencia vigente vencieron antes de {@code ahora}. Un plazo pendiente del cliente no
     * tiene vencimiento y nunca vence.
     */
    public boolean tieneVencimientoActivo(Instant ahora) {
        if (finalizado) {
            return false;
        }
        Instant venceEtapa = etapa(etapaActual).venceEn();
        boolean etapaVencida = venceEtapa != null && ahora.isAfter(venceEtapa);
        boolean urgenciaVencida = urgencia != null && ahora.isAfter(urgencia.venceEn());
        return etapaVencida || urgenciaVencida;
    }

    private Interviniente autorizarEnEntradaActual(int numeroEtapa, UUID entradaEsperadaId, CodigoRol rolResponsable,
            EtapasHabilitadas habilitadas, ActorVigente actor, String mensajeCambio) {
        if (finalizado) {
            throw new TransicionNoPermitida("El pedido ya terminó.");
        }
        EtapaDePedido actual = etapa(etapaActual);
        if (numeroEtapa != etapaActual || entradaEsperadaId == null || !entradaEsperadaId.equals(actual.entradaId())) {
            throw new EtapaCambio(mensajeCambio);
        }
        if (!habilitadas.incluye(etapaActual)) {
            throw new TransicionNoPermitida("Las acciones de esta etapa todavía no están habilitadas.");
        }
        return PermisosEtapa.paraActuar(actual, rolResponsable, actor);
    }

    /**
     * Si la persona puede descargar un archivo subido a {@code etapaDelArchivo} (P-17, provisional del equipo): el
     * admin, quien puede actuar en esa etapa y quien puede actuar en una etapa que la revisa. Las asignaciones son las
     * de cada etapa del pedido. Una cuenta inactiva o sin roles nunca descarga.
     *
     * @param rolEtapaDelArchivo rol responsable de la etapa del archivo
     * @param revisoras etapas que revisan la del archivo, con su rol responsable (de los datos del flujo)
     */
    public boolean puedeDescargar(int etapaDelArchivo, CodigoRol rolEtapaDelArchivo, Map<Integer, CodigoRol> revisoras,
            ActorVigente actor) {
        if (actor == null || !actor.puedeActuar()) {
            return false;
        }
        if (actor.tieneRol(PermisosEtapa.ADMIN)) {
            return true;
        }
        if (rolEtapaDelArchivo != null
                && PermisosEtapa.puedeActuar(etapa(etapaDelArchivo), rolEtapaDelArchivo, actor)) {
            return true;
        }
        return revisoras.entrySet().stream()
                .anyMatch(revisora -> revisora.getValue() != null
                        && PermisosEtapa.puedeActuar(etapa(revisora.getKey()), revisora.getValue(), actor));
    }

    /** Entrega los eventos pendientes, en orden, y los quita del agregado. Lo llama el caso de uso al guardar. */
    public List<EventoDePedido> retirarEventos() {
        List<EventoDePedido> eventos = List.copyOf(eventosPendientes);
        eventosPendientes.clear();
        return eventos;
    }

    /** Identidad del pedido para los eventos: número, país y flujo no cambian. */
    public DatosDelPedido datosDelPedido() {
        return new DatosDelPedido(id, numero.valor(), pais, tipoFlujo);
    }

    private static String validarObservacion(String observacion) {
        if (observacion == null || observacion.isBlank()) {
            return null;
        }
        String limpia = observacion.strip();
        if (limpia.length() > OBSERVACION_MAXIMA) {
            throw new DatoInvalido("observacion", "La observación tiene como máximo 4000 caracteres.");
        }
        return limpia;
    }

    private static void exigirRequisitos(ReglaDeTransicion regla, String observacion, NivelUrgencia nivel,
            AccionSolicitada solicitud) {
        AccionEtapa accion = solicitud.accion();
        // Además de lo que digan los datos, un rechazo y una devolución siempre llevan observación, y la devolución
        // por producto nuevo siempre lleva urgencia (requerimientos 4.4, A3 y A4): el evento no se puede registrar
        // sin ellas.
        boolean pideObservacion = regla.requiereObservacion() || accion == AccionEtapa.RECHAZAR
                || accion == AccionEtapa.DEVOLVER_POR_ERROR || accion == AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO;
        boolean pideUrgencia = regla.requiereUrgencia() || accion == AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO;
        List<String> faltantes = new ArrayList<>();
        if (pideObservacion && observacion == null) {
            faltantes.add("observación");
        }
        if (pideUrgencia && nivel == null) {
            faltantes.add("urgencia");
        }
        if (regla.requiereBodega() && solicitud.bodegaId() == null) {
            // F4-D2: la bodega se elige con el APROBAR de Bodega.
            faltantes.add("bodega");
        }
        regla.documentosObligatorios().stream()
                .filter(tipo -> !solicitud.documentosPresentes().contains(tipo))
                .sorted()
                .forEach(faltantes::add);
        if (regla.exigeAlgunDocumento() && solicitud.documentosPresentes().isEmpty()) {
            faltantes.add(ReglaDeTransicion.FALTA_ALGUN_DOCUMENTO);
        }
        if (!faltantes.isEmpty()) {
            throw new RequisitoFaltante(faltantes);
        }
    }

    private EtapaDePedido etapa(int numeroEtapa) {
        return etapas.stream()
                .filter(etapa -> etapa.numero() == numeroEtapa)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El pedido no tiene la etapa " + numeroEtapa + "."));
    }

    private void verificarEtapasAlFinalizar() {
        if (finalizadoEn == null || etapa(etapaActual).estado() != EstadoEtapa.COMPLETADA
                || etapas.stream().anyMatch(etapa -> etapa.estado() == EstadoEtapa.ACTUAL)) {
            throw new IllegalStateException("Un pedido finalizado tiene Fin completada y ninguna etapa actual.");
        }
    }

    private void verificarUnaSolaActual() {
        List<EtapaDePedido> actuales = etapas.stream().filter(etapa -> etapa.estado() == EstadoEtapa.ACTUAL).toList();
        if (actuales.size() != 1 || actuales.get(0).numero() != etapaActual) {
            throw new IllegalStateException("Un pedido en curso tiene exactamente una etapa actual.");
        }
    }

    public UUID id() {
        return id;
    }

    public NumeroPedido numero() {
        return numero;
    }

    public CodigoPais pais() {
        return pais;
    }

    public TipoFlujo tipoFlujo() {
        return tipoFlujo;
    }

    public UUID flujoId() {
        return flujoId;
    }

    public String nombre() {
        return nombre;
    }

    public String descripcion() {
        return descripcion;
    }

    public boolean incluyeProductosNuevos() {
        return incluyeProductosNuevos;
    }

    public UUID creadoPor() {
        return creadoPor;
    }

    public Instant creadoEn() {
        return creadoEn;
    }

    public List<EtapaDePedido> etapas() {
        return List.copyOf(etapas);
    }

    public int etapaActual() {
        return etapaActual;
    }

    public boolean finalizado() {
        return finalizado;
    }

    public Instant finalizadoEn() {
        return finalizadoEn;
    }

    /** Entrada que activó la urgencia vigente (paso 5.2); vacía sin urgencia. */
    public Optional<UUID> urgenciaActivacionId() {
        return Optional.ofNullable(urgenciaActivacionId);
    }

    public Optional<UrgenciaActiva> urgencia() {
        return Optional.ofNullable(urgencia);
    }

    /** Momento del check de llegada de A7, con la hora del servidor; vacío si todavía no llegó. */
    public Optional<Instant> llegadaAduanaEn() {
        return Optional.ofNullable(llegadaAduanaEn);
    }

    /**
     * Demora vigente en aduana. Es el dato que usará el cronograma de la fase 5 como punto de partida para recalcular
     * las etapas siguientes (todavía no implementado); en la fase 4 no es un vencimiento.
     */
    public Optional<DemoraAduana> demora() {
        return Optional.ofNullable(demora);
    }

    /** Bodega de destino (F4-D2): vacía hasta aprobar Bodega; después no cambia. */
    public Optional<UUID> bodegaId() {
        return Optional.ofNullable(bodegaId);
    }

    public Instant actualizadoEn() {
        return actualizadoEn;
    }

    public long version() {
        return version;
    }
}
