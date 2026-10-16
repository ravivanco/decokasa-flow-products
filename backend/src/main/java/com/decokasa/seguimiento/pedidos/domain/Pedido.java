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
import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaReasignada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion;
import com.decokasa.seguimiento.compartido.domain.eventos.ObservacionAgregada;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoCreado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import java.time.Instant;
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
    private final boolean finalizado;
    private final Instant finalizadoEn;
    private UrgenciaActiva urgencia;
    private Instant actualizadoEn;
    private final long version;
    private final List<EventoDePedido> eventosPendientes = new ArrayList<>();

    /** Reconstruye un pedido guardado y comprueba que tenga una sola etapa ACTUAL, la que dice {@code etapaActual}. */
    public Pedido(UUID id, NumeroPedido numero, CodigoPais pais, TipoFlujo tipoFlujo, UUID flujoId, String nombre,
            String descripcion, boolean incluyeProductosNuevos, UUID creadoPor, Instant creadoEn,
            List<EtapaDePedido> etapas, int etapaActual, boolean finalizado, Instant finalizadoEn,
            UrgenciaActiva urgencia, Instant actualizadoEn, long version) {
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
     * @throws SinPermiso si el creador no es el responsable de la primera etapa ni el admin
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
            throw new TransicionNoPermitida("Esta acción todavía no está implementada.");
        }

        AccionEtapa accion = solicitud.accion();
        String observacion = validarObservacion(solicitud.observacion());
        NivelUrgencia nivel = solicitud.nivelUrgencia();
        if (nivel != null && accion != AccionEtapa.DEVOLVER_POR_PRODUCTO_NUEVO) {
            throw new DatoInvalido("nivelUrgencia", "La urgencia solo se indica al devolver por producto nuevo.");
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
        // (A3 → A2) no la cambia y su vencimiento sigue contando hasta volver a Análisis. No es una regla aprobada.

        EtapaDePedido etapaDestino = etapa(destino);
        etapaDestino.entrar(ahora, vencimientos.deEtapa(ahora, regla.plazoDestino()).orElse(null));
        etapaActual = destino;
        actualizadoEn = ahora;
        verificarUnaSolaActual();

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
        eventosPendientes.add(evento);
        return evento;
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
        if (regla.requiereBodega()) {
            // La bodega llega en la fase 4; ninguna acción habilitada en la fase 2 la pide.
            faltantes.add("bodega");
        }
        regla.documentosObligatorios().stream()
                .filter(tipo -> !solicitud.documentosPresentes().contains(tipo))
                .sorted()
                .forEach(faltantes::add);
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

    public Optional<UrgenciaActiva> urgencia() {
        return Optional.ofNullable(urgencia);
    }

    public Instant actualizadoEn() {
        return actualizadoEn;
    }

    public long version() {
        return version;
    }
}
