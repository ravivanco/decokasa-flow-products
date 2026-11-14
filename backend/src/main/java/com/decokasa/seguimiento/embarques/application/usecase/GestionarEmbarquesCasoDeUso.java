package com.decokasa.seguimiento.embarques.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.EtapaCambio;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoEmbarque;
import com.decokasa.seguimiento.compartido.domain.eventos.Interviniente;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.application.port.out.EmbarquesRepositorio;
import com.decokasa.seguimiento.embarques.application.port.out.PedidosParaEmbarques;
import com.decokasa.seguimiento.embarques.domain.CodigoBl;
import com.decokasa.seguimiento.embarques.domain.LlegadaEstimada;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reglas de BL y Packing Lists. Un BL se gestiona desde un pedido en su etapa de BL, con el permiso de esa etapa. Si un
 * Packing List compartido cambia mientras otro pedido lo tiene en revisión, ese pedido debe revisar la versión nueva;
 * si ya hay una aprobación sobre él, se conserva la versión aprobada y se bloquea su sustitución hasta definir la regla
 * de corrección (pendiente del cliente). Orden de bloqueos: primero el pedido, después el BL. Sin Spring (R2).
 */
public final class GestionarEmbarquesCasoDeUso implements GestionarEmbarques {

    private static final String TIPO_PACKING_LIST = "Packing List";
    private static final String BLOQUEADO = "Ya hay una aprobación sobre este BL: se conserva la versión aprobada y su "
            + "sustitución queda bloqueada hasta definir la regla de corrección.";

    private final EmbarquesRepositorio repositorio;
    private final PedidosParaEmbarques pedidos;
    private final PublicadorEventos publicador;
    private final DescargarArchivo descargas;
    private final Reloj reloj;

    public GestionarEmbarquesCasoDeUso(EmbarquesRepositorio repositorio, PedidosParaEmbarques pedidos,
            PublicadorEventos publicador, DescargarArchivo descargas, Reloj reloj) {
        this.repositorio = repositorio;
        this.pedidos = pedidos;
        this.publicador = publicador;
        this.descargas = descargas;
        this.reloj = reloj;
    }

    @Override
    public Vista consultar(Solicitante solicitante, UUID pedidoId) {
        return vista(solicitante, pedidos.autorizarConsulta(solicitante, pedidoId));
    }

    @Override
    public Vista crear(Solicitante solicitante, UUID pedidoId, NuevoBl nuevo) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        CodigoBl codigo = CodigoBl.de(nuevo.codigo());
        var pais = gestion.pedido().pedido().pais();
        if (repositorio.blPorCodigo(pais, codigo.normalizado()).isPresent()) {
            throw new DatoDuplicado("codigo", "Ese código de BL ya existe. Puedes vincularlo a este pedido.");
        }
        LlegadaEstimada llegada = LlegadaEstimada.resolver(nuevo.fechaSalida(), nuevo.llegadaEstimada(), null,
                gestion.pedido().transitoDias());
        Instant ahora = reloj.ahora();
        UUID id = UUID.randomUUID();
        UUID autor = gestion.actor().usuarioId();
        repositorio.insertarBl(id, pais, codigo, nuevo.fechaSalida(), llegada, importadora(nuevo.importadora()), autor,
                ahora);
        repositorio.vincular(id, pedidoId, autor, ahora);
        publicar(gestion.pedido(), gestion.actor(), ahora, "BL " + codigo.texto() + " guardado.", false);
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista actualizar(Solicitante solicitante, UUID pedidoId, UUID blId, CambioBl cambio) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        EmbarquesRepositorio.FilaBl bl = blVinculado(blId, pedidoId);
        if (bl.bloqueado()) {
            throw new TransicionNoPermitida(BLOQUEADO);
        }
        LlegadaEstimada.Origen origen = cambio.llegadaOrigen() == null ? null : origen(cambio.llegadaOrigen());
        LlegadaEstimada llegada = LlegadaEstimada.resolver(cambio.fechaSalida(), cambio.llegadaEstimada(), origen,
                gestion.pedido().transitoDias());
        Instant ahora = reloj.ahora();
        if (!repositorio.actualizarBl(blId, cambio.version(), cambio.fechaSalida(), llegada,
                importadora(cambio.importadora()), ahora)) {
            throw new VersionDesactualizada("Otra persona cambió este BL. Vuelve a cargarlo.");
        }
        registrarEnPedidos(blId, gestion.actor(), ahora, "BL " + bl.codigo() + ": salida " + cambio.fechaSalida()
                + ", llegada " + (llegada.fecha() == null ? "por definir" : llegada.fecha()) + ".", null);
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista registrarPackingList(Solicitante solicitante, UUID pedidoId, UUID blId, UUID adjuntoId) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        EmbarquesRepositorio.FilaBl bl = blVinculado(blId, pedidoId);
        if (repositorio.versionDeAdjunto(blId, adjuntoId).isPresent()) {
            return vista(solicitante, gestion.pedido());
        }
        if (bl.bloqueado()) {
            throw new TransicionNoPermitida(BLOQUEADO);
        }
        EmbarquesRepositorio.AdjuntoDelPedido adjunto = repositorio.adjunto(adjuntoId)
                .filter(archivo -> archivo.pedidoId().equals(pedidoId))
                .filter(archivo -> archivo.entradaId().equals(gestion.pedido().entradaActualId()))
                .filter(archivo -> TIPO_PACKING_LIST.equals(archivo.tipoDocumento()))
                .orElseThrow(() -> new DatoInvalido("adjuntoId",
                        "El archivo debe ser un Packing List confirmado en la entrada actual de este pedido."));
        Instant ahora = reloj.ahora();
        int version = repositorio.registrarPackingList(blId, adjunto.id(), gestion.actor().usuarioId(), ahora);
        String anterior = version > 1 ? " Sustituye a la v" + (version - 1) + ", que se conserva." : "";
        String texto = "BL " + bl.codigo() + ": Packing List v" + version + " (" + adjunto.nombre() + ")." + anterior;
        EmbarquesRepositorio.FilaBl actualizado = repositorio.blParaActualizar(blId).orElseThrow();
        registrarEnPedidos(blId, gestion.actor(), ahora, texto, actualizado.packingListVigente());
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista vincularPorCodigo(Solicitante solicitante, UUID pedidoId, String codigo) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        EmbarquesRepositorio.FilaBl bl = repositorio
                .blPorCodigo(gestion.pedido().pedido().pais(), CodigoBl.normalizar(codigo))
                .orElseThrow(() -> new NoEncontrado("No hay un BL con ese código en este país."));
        vincular(gestion, bl, gestion.pedido());
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista anadirPedido(Solicitante solicitante, UUID pedidoId, UUID blId, String numeroPedido) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        EmbarquesRepositorio.FilaBl bl = blVinculado(blId, pedidoId);
        PedidosParaEmbarques.Referencia destino = pedidos.vinculable(solicitante, numeroPedido)
                .filter(pedido -> pedido.pedido().pais() == bl.pais())
                .orElseThrow(() -> new NoEncontrado(
                        "No hay un pedido en curso de este país, con ese número, que admita vincular BL."));
        vincular(gestion, bl, destino);
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista desvincular(Solicitante solicitante, UUID pedidoId, UUID blId) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarGestion(solicitante, pedidoId);
        EmbarquesRepositorio.FilaBl bl = blVinculado(blId, pedidoId);
        Instant ahora = reloj.ahora();
        repositorio.desvincular(blId, pedidoId, gestion.actor().usuarioId(), ahora);
        publicar(gestion.pedido(), gestion.actor(), ahora, "BL " + bl.codigo() + " desvinculado de este pedido.", false);
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public Vista marcarRevisada(Solicitante solicitante, UUID pedidoId, UUID blId) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarRevision(solicitante, pedidoId);
        EmbarquesRepositorio.EnvioPendiente envio = repositorio.envioPendiente(pedidoId)
                .orElseThrow(() -> new TransicionNoPermitida("Este pedido no tiene un envío pendiente."));
        EmbarquesRepositorio.RevisionPendiente revision = repositorio.revisionesPendientes(envio.id()).stream()
                .filter(pendiente -> pendiente.blId().equals(blId))
                .findFirst()
                .orElseThrow(() -> new TransicionNoPermitida("Ese BL no tiene una versión nueva que revisar."));
        Instant ahora = reloj.ahora();
        repositorio.marcarRevisada(envio.id(), revision, gestion.actor().usuarioId(), ahora);
        publicar(gestion.pedido(), gestion.actor(), ahora, "Envío " + envio.numero() + ": revisada la versión v"
                + revision.versionVigente() + " del Packing List de " + revision.codigo() + ".", false);
        return vista(solicitante, gestion.pedido());
    }

    @Override
    public DescargarArchivo.Autorizada packingList(Solicitante solicitante, UUID pedidoId, UUID blId,
            UUID packingListId) {
        pedidos.autorizarConsulta(solicitante, pedidoId);
        EmbarquesRepositorio.PackingListDelBl archivo = repositorio.packingList(packingListId)
                .filter(fila -> fila.blId().equals(blId))
                .filter(fila -> repositorio.relacionado(blId, pedidoId))
                .orElseThrow(() -> new NoEncontrado("El Packing List no existe en este pedido."));
        pedidos.autorizarPackingList(solicitante, pedidoId, archivo.pedidoDelArchivo(), archivo.etapaDelArchivo());
        return descargas.delAdjunto(archivo.adjuntoId());
    }

    @Override
    public Checklist checklist(Solicitante solicitante, UUID pedidoId) {
        PedidosParaEmbarques.Referencia pedido = pedidos.autorizarConsulta(solicitante, pedidoId);
        if (!pedido.etapaChecklist()) {
            return new Checklist(false, false, 0, 0, List.of(), null, false);
        }
        return repositorio.checklist(pedido.flujoId(), pedidoId, pedido.etapaActual(), pedido.entradaActualId())
                .conPermiso(!pedido.finalizado() && pedidos.puedeActuar(solicitante, pedidoId));
    }

    @Override
    public Checklist marcarRequisito(Solicitante solicitante, UUID pedidoId, String tipoDocumento, boolean marcado,
            UUID entradaEsperadaId) {
        PedidosParaEmbarques.Gestion gestion = pedidos.autorizarChecklist(solicitante, pedidoId);
        PedidosParaEmbarques.Referencia pedido = gestion.pedido();
        if (entradaEsperadaId == null || !entradaEsperadaId.equals(pedido.entradaActualId())) {
            throw new EtapaCambio("La etapa cambió: vuelve a cargar el checklist.");
        }
        Checklist actual = repositorio.checklist(pedido.flujoId(), pedidoId, pedido.etapaActual(),
                pedido.entradaActualId());
        Requisito requisito = actual.requisitos().stream()
                .filter(fila -> fila.tipoDocumento().equals(tipoDocumento))
                .findFirst()
                .orElseThrow(() -> new DatoInvalido("tipoDocumento", "Ese documento no está en el checklist."));
        if (requisito.marcado() != marcado) {
            repositorio.marcarRequisito(UUID.randomUUID(), pedidoId, pedido.entradaActualId(), tipoDocumento, marcado,
                    gestion.actor().usuarioId(), reloj.ahora());
        }
        return repositorio.checklist(pedido.flujoId(), pedidoId, pedido.etapaActual(), pedido.entradaActualId())
                .conPermiso(true);
    }

    @Override
    public List<AlertaSimulada> alertasSimuladas(Solicitante solicitante, UUID pedidoId) {
        pedidos.autorizarConsulta(solicitante, pedidoId);
        return repositorio.alertasSimuladas(pedidoId);
    }

    private void vincular(PedidosParaEmbarques.Gestion gestion, EmbarquesRepositorio.FilaBl bl,
            PedidosParaEmbarques.Referencia destino) {
        UUID pedidoId = destino.pedido().pedidoId();
        if (repositorio.vinculado(bl.id(), pedidoId)) {
            throw new DatoDuplicado("codigo", "Ese BL ya está en el pedido " + destino.pedido().numeroPedido() + ".");
        }
        Instant ahora = reloj.ahora();
        repositorio.vincular(bl.id(), pedidoId, gestion.actor().usuarioId(), ahora);
        registrarEnPedidos(bl.id(), gestion.actor(), ahora, "Pedido " + destino.pedido().numeroPedido()
                + " vinculado al BL " + bl.codigo() + ".", null);
    }

    /**
     * Deja el evento en cada pedido del BL. Si {@code versionNueva} no es nula, los pedidos que la tienen en revisión con
     * otra versión reciben además la solicitud de revisar la nueva, con aviso a sus responsables.
     */
    private void registrarEnPedidos(UUID blId, Interviniente actor, Instant ahora, String texto, UUID versionNueva) {
        for (UUID pedidoId : repositorio.pedidosDelBl(blId)) {
            publicar(pedidos.referencia(pedidoId), actor, ahora, texto, false);
        }
        if (versionNueva != null) {
            for (UUID pedidoId : repositorio.pedidosARevisar(blId, versionNueva)) {
                publicar(pedidos.referencia(pedidoId), actor, ahora,
                        "Revisa la versión nueva antes de decidir. " + texto, true);
            }
        }
    }

    private void publicar(PedidosParaEmbarques.Referencia pedido, Interviniente actor, Instant ahora, String texto,
            boolean avisa) {
        publicador.publicar(new EventoEmbarque(pedido.pedido(), pedido.etapaActual(), actor, ahora, texto, avisa));
    }

    private EmbarquesRepositorio.FilaBl blVinculado(UUID blId, UUID pedidoId) {
        EmbarquesRepositorio.FilaBl bl = repositorio.blParaActualizar(blId)
                .orElseThrow(() -> new NoEncontrado("El BL no existe."));
        if (!repositorio.vinculado(blId, pedidoId)) {
            throw new NoEncontrado("El BL no está vinculado a este pedido.");
        }
        return bl;
    }

    private Vista vista(Solicitante solicitante, PedidosParaEmbarques.Referencia pedido) {
        UUID id = pedido.pedido().pedidoId();
        boolean puedeActuar = !pedido.finalizado() && pedidos.puedeActuar(solicitante, id);
        return new Vista(pedido.flujoGestiona(), pedido.etapaAprueba(), pedido.etapaGestiona() && puedeActuar,
                pedido.etapaAprueba() && puedeActuar, pedido.transitoDias(),
                repositorio.blsDelPedido(id, pedido.etapaAprueba()), repositorio.enviosDelPedido(id));
    }

    /** Importadora del BL (pedido del cliente, 2026-11-14): DECOKASA o Latinoamérica; vacía hasta elegirla. */
    private static String importadora(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        String limpio = valor.strip().toUpperCase(java.util.Locale.ROOT);
        if (!limpio.equals("DECOKASA") && !limpio.equals("LATINOAMERICA")) {
            throw new DatoInvalido("importadora", "Elige DECOKASA o Latinoamérica.");
        }
        return limpio;
    }

    private static LlegadaEstimada.Origen origen(String valor) {
        try {
            return LlegadaEstimada.Origen.valueOf(Objects.requireNonNull(valor));
        } catch (IllegalArgumentException invalido) {
            throw new DatoInvalido("llegadaOrigen", "El origen de la llegada no es válido.");
        }
    }
}
