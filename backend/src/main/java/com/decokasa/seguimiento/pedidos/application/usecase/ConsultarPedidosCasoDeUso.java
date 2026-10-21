package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarAdjuntosDeEntrada;
import com.decokasa.seguimiento.bodegas.application.port.in.DescribirBodega;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarAccionesDeEtapa;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.pedidos.application.port.in.VistasDePedido;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.DemoraAduana;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.PermisosEtapa;
import com.decokasa.seguimiento.pedidos.domain.UrgenciaActiva;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Consultas de pedidos con la visibilidad de P-13 (provisional del equipo) y las acciones disponibles según P-06. Las
 * acciones se calculan con la misma regla de permisos que se usa al ejecutarlas. Sin Spring (R2).
 */
public final class ConsultarPedidosCasoDeUso implements ConsultarPedidos {

    private static final int TAMANO_MAXIMO = 100;

    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final ConsultarAccionesDeEtapa acciones;
    private final ConsultarAdjuntosDeEntrada adjuntos;
    private final ResumirUsuarios usuarios;
    private final ResumirEtapasDePedido cierres;
    private final EtapasHabilitadas habilitadas;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;
    private final DescribirBodega bodegas;

    public ConsultarPedidosCasoDeUso(ConsultaDePedidos consultas, ConsultarDefinicionFlujo flujos,
            ConsultarAccionesDeEtapa acciones, ConsultarAdjuntosDeEntrada adjuntos, ResumirUsuarios usuarios,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj, ResumirEtapasDePedido cierres, DescribirBodega bodegas) {
        this.bodegas = bodegas;
        this.consultas = consultas;
        this.cierres = cierres;
        this.flujos = flujos;
        this.acciones = acciones;
        this.adjuntos = adjuntos;
        this.usuarios = usuarios;
        this.habilitadas = habilitadas;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public VistasDePedido.Detalle detalle(Solicitante solicitante, UUID pedidoId) {
        ActorVigente actor = apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        return detalle(pedido, actor);
    }

    @Override
    public VistasDePedido.Detalle porNumero(Solicitante solicitante, String numero) {
        ActorVigente actor = apoyo.lector(solicitante);
        String limpio = numero == null ? "" : numero.strip();
        if (limpio.isEmpty()) {
            throw new DatoInvalido("numero", "Escribe el número del pedido.");
        }
        Pedido pedido = consultas.porNumero(limpio)
                .orElseThrow(() -> new NoEncontrado("No hay un pedido con ese número."));
        return detalle(pedido, actor);
    }

    @Override
    public VistasDePedido.Pagina listar(Solicitante solicitante, Filtro filtro, int pagina, int tamano) {
        apoyo.lector(solicitante);
        exigirPagina(pagina, tamano);
        String texto = filtro.texto() == null || filtro.texto().isBlank() ? null : filtro.texto().strip();
        ConsultaDePedidos.Pagina resultado = consultas.listar(filtro.tipoFlujo(), filtro.etapa(), filtro.vencido(),
                texto, reloj.ahora(), pagina, tamano);
        return new VistasDePedido.Pagina(resumenes(resultado.pedidos()), pagina, tamano, resultado.total());
    }

    @Override
    public VistasDePedido.Pagina bandeja(Solicitante solicitante, FiltroBandeja filtro, int pagina, int tamano) {
        ActorVigente actor = apoyo.lector(solicitante);
        exigirPagina(pagina, tamano);
        FiltroBandeja aplicado = filtro == null ? FiltroBandeja.SIN_FILTRO : filtro;
        ConsultaDePedidos.Pagina resultado = consultas.bandeja(actor.usuarioId(), actor.roles(),
                actor.tieneRol(PermisosEtapa.ADMIN), Boolean.TRUE.equals(aplicado.vencido()),
                Boolean.TRUE.equals(aplicado.conUrgencia()), reloj.ahora(), pagina, tamano);
        return new VistasDePedido.Pagina(resumenes(resultado.pedidos()), pagina, tamano, resultado.total());
    }

    private static void exigirPagina(int pagina, int tamano) {
        if (pagina < 0) {
            throw new DatoInvalido("pagina", "La página empieza en 0.");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new DatoInvalido("tamano", "El tamaño de página va de 1 a 100.");
        }
    }

    // --- Armado de las vistas ---

    private List<VistasDePedido.Resumen> resumenes(List<Pedido> pedidos) {
        Map<UUID, ContratoDeFlujo> definiciones = new HashMap<>();
        Map<UUID, ResumirUsuarios.Resumen> personas = usuarios.resumenes(personasDe(pedidos));
        Instant ahora = reloj.ahora();
        return pedidos.stream()
                .map(pedido -> resumen(pedido, flujo(definiciones, pedido), personas, ahora))
                .toList();
    }

    private VistasDePedido.Detalle detalle(Pedido pedido, ActorVigente actor) {
        ContratoDeFlujo flujo = flujos.porId(pedido.flujoId());
        Map<UUID, ResumirUsuarios.Resumen> personas = usuarios.resumenes(personasDe(List.of(pedido)));
        Instant ahora = reloj.ahora();
        // Requisito 4.2 #17: lo ocurrido en cada etapa sale del historial (último cierre de cada una).
        Map<Integer, ResumirEtapasDePedido.Cierre> ultimosCierres = cierres.ultimosCierres(pedido.id());
        List<VistasDePedido.Etapa> linea = new ArrayList<>();
        for (ContratoDeEtapa definicion : flujo.etapas()) {
            EtapaDePedido etapa = etapaDe(pedido, definicion.numero());
            linea.add(new VistasDePedido.Etapa(definicion.numero(), definicion.nombre(), etapa.estado(),
                    definicion.rolResponsable(), estadoPlazo(definicion.plazo()),
                    persona(etapa.asignadoA(), personas), asignacionVigente(etapa, definicion, personas),
                    etapa.entradaId(), etapa.entradaEn(), etapa.salidaEn(), etapa.venceEn(), etapa.reentradas(),
                    ultimosCierres.get(definicion.numero())));
        }
        VistasDePedido.Bodega bodega = pedido.bodegaId()
                .flatMap(bodegas::describir)
                .map(vista -> new VistasDePedido.Bodega(vista.id(), vista.nombre(), vista.ciudad(), vista.activa(),
                        vista.version()))
                .orElse(null);
        DemoraAduana demora = pedido.demora().orElse(null);
        VistasDePedido.Demora demoraAduana = demora == null ? null
                : new VistasDePedido.Demora(persona(demora.reportadaPor(), personas), demora.reportadaEn(),
                        demora.motivo(), demora.nuevaFechaEstimadaLlegada());
        return new VistasDePedido.Detalle(resumen(pedido, flujo, personas, ahora), pedido.descripcion(),
                pedido.incluyeProductosNuevos(), persona(pedido.creadoPor(), personas), pedido.creadoEn(),
                flujo.version(), bodega, demoraAduana, pedido.llegadaAduanaEn().orElse(null), List.copyOf(linea),
                accionesDisponibles(pedido, flujo, actor));
    }

    private VistasDePedido.Resumen resumen(Pedido pedido, ContratoDeFlujo flujo,
            Map<UUID, ResumirUsuarios.Resumen> personas, Instant ahora) {
        EtapaDePedido actual = etapaDe(pedido, pedido.etapaActual());
        Instant venceEn = actual.venceEn();
        boolean vencida = !pedido.finalizado() && venceEn != null && ahora.isAfter(venceEn);
        UrgenciaActiva urgencia = pedido.urgencia().orElse(null);
        return new VistasDePedido.Resumen(pedido.id(), pedido.numero().valor(), pedido.nombre(), pedido.pais(),
                pedido.tipoFlujo(), pedido.etapaActual(), flujo.etapa(pedido.etapaActual()).nombre(),
                persona(actual.asignadoA(), personas), venceEn, vencida,
                urgencia == null ? null
                        : new VistasDePedido.Urgencia(urgencia.nivel(), urgencia.activadaEn(), urgencia.venceEn(),
                                !pedido.finalizado() && ahora.isAfter(urgencia.venceEn())),
                pedido.finalizado(), actual.motivoRetraso());
    }

    /**
     * Acciones de la etapa actual que la persona puede pedir ahora (P-06, P-04), con lo que exigen. Los documentos que
     * faltan se calculan con los archivos de la entrada actual (P-18); la observación y la urgencia se envían con la
     * acción y no cuentan aquí.
     */
    private List<VistasDePedido.AccionDisponible> accionesDisponibles(Pedido pedido, ContratoDeFlujo flujo,
            ActorVigente actor) {
        if (pedido.finalizado() || !habilitadas.incluye(pedido.etapaActual())) {
            return List.of();
        }
        EtapaDePedido actual = etapaDe(pedido, pedido.etapaActual());
        CodigoRol rol = flujo.etapa(pedido.etapaActual()).rolResponsable();
        if (!PermisosEtapa.puedeActuar(actual, rol, actor)) {
            return List.of();
        }
        Set<String> subidos = adjuntos.tiposSubidos(actual.entradaId());
        List<VistasDePedido.AccionDisponible> lista = new ArrayList<>();
        for (ResolverTransicion.Resolucion resolucion : acciones.accionesDe(pedido.flujoId(), pedido.etapaActual())) {
            List<String> obligatorios = resolucion.documentosObligatorios().stream().sorted().toList();
            List<String> faltantes = obligatorios.stream().filter(tipo -> !subidos.contains(tipo)).toList();
            // REPORTAR_DEMORA no mueve el pedido y sí está implementada (F4-D5); ELEGIR_BODEGA no se usa (F4-D2). La
            // bodega, como la observación, se envía con la acción y no cuenta aquí.
            boolean implementada = resolucion.etapaDestino() != null
                    || resolucion.accion() == AccionEtapa.REPORTAR_DEMORA;
            boolean puedeCompletarse = !resolucion.documentosPendientes() && implementada && faltantes.isEmpty();
            lista.add(new VistasDePedido.AccionDisponible(resolucion.accion(), resolucion.etapaDestino(),
                    resolucion.requiereObservacion(), resolucion.requiereUrgencia(), resolucion.requiereBodega(),
                    resolucion.documentosPendientes(), obligatorios, faltantes, puedeCompletarse));
        }
        return List.copyOf(lista);
    }

    private ContratoDeFlujo flujo(Map<UUID, ContratoDeFlujo> definiciones, Pedido pedido) {
        return definiciones.computeIfAbsent(pedido.flujoId(), flujos::porId);
    }

    private static Collection<UUID> personasDe(List<Pedido> pedidos) {
        Set<UUID> ids = new HashSet<>();
        for (Pedido pedido : pedidos) {
            ids.add(pedido.creadoPor());
            pedido.demora().ifPresent(demora -> ids.add(demora.reportadaPor()));
            for (EtapaDePedido etapa : pedido.etapas()) {
                if (etapa.asignadoA() != null) {
                    ids.add(etapa.asignadoA());
                }
            }
        }
        return ids;
    }

    private static EtapaDePedido etapaDe(Pedido pedido, int numero) {
        return pedido.etapas().stream()
                .filter(etapa -> etapa.numero() == numero)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El pedido no tiene la etapa " + numero + "."));
    }

    private static VistasDePedido.Persona persona(UUID id, Map<UUID, ResumirUsuarios.Resumen> personas) {
        if (id == null) {
            return null;
        }
        ResumirUsuarios.Resumen resumen = personas.get(id);
        return resumen == null ? new VistasDePedido.Persona(id, "Usuario no disponible", Set.of())
                : new VistasDePedido.Persona(resumen.id(), resumen.nombre(), resumen.roles());
    }

    /** Nulo sin persona asignada; verdadero si está activa y tiene el rol responsable; falso si no (P-06). */
    private static Boolean asignacionVigente(EtapaDePedido etapa, ContratoDeEtapa definicion,
            Map<UUID, ResumirUsuarios.Resumen> personas) {
        if (etapa.asignadoA() == null) {
            return null;
        }
        ResumirUsuarios.Resumen resumen = personas.get(etapa.asignadoA());
        return resumen != null && resumen.activo() && definicion.rolResponsable() != null
                && resumen.roles().contains(definicion.rolResponsable());
    }

    private static VistasDePedido.EstadoPlazo estadoPlazo(PlazoDeEtapa plazo) {
        return switch (plazo) {
            case PlazoDeEtapa.Definido definido -> VistasDePedido.EstadoPlazo.DEFINIDO;
            case PlazoDeEtapa.SinPlazo sinPlazo -> VistasDePedido.EstadoPlazo.SIN_PLAZO;
            case PlazoDeEtapa.Pendiente pendiente -> VistasDePedido.EstadoPlazo.PENDIENTE;
        };
    }
}
