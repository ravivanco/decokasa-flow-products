package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegaActiva;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.EtapaCambio;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TransicionNoPermitida;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.pedidos.application.port.in.EjecutarAccionEtapa;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.AccionSolicitada;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.EtapasHabilitadas;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.PermisosEtapa;
import com.decokasa.seguimiento.pedidos.domain.ReglaDeTransicion;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;

/**
 * Ejecuta una acción sobre la etapa actual. Protección contra avances dobles (P-11, sin cambiar el contrato): la fila
 * del pedido se bloquea antes de leerlo, así que de dos acciones simultáneas la segunda espera, ve la etapa ya movida
 * y recibe 409 ETAPA_CAMBIO sin cambios parciales. Orden de bloqueos: primero el pedido (como en N1).
 *
 * <p>Documentos (P-05 opción A, P-18): los acreditan solo los {@code adjuntoIds} que el módulo de archivos valida
 * como archivos reales de la entrada actual. Un identificador inválido responde 400 y nunca se ignora; si falta un tipo
 * obligatorio, el dominio responde 422 con los faltantes. El permiso se revisa antes de validar los archivos.
 */
public final class EjecutarAccionEtapaCasoDeUso implements EjecutarAccionEtapa {

    private final PedidoRepositorio pedidos;
    private final ResolverTransicion transiciones;
    private final EtapasHabilitadas habilitadas;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;
    private final ValidarAdjuntosParaAccion adjuntos;
    private final ConsultarBodegaActiva bodegas;

    /** @param bodegas bodegas activas (F4-D2): la bodega elegida al aprobar Bodega debe existir y estar activa */
    public EjecutarAccionEtapaCasoDeUso(PedidoRepositorio pedidos, ResolverTransicion transiciones,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj, ValidarAdjuntosParaAccion adjuntos,
            ConsultarBodegaActiva bodegas) {
        this.pedidos = pedidos;
        this.transiciones = transiciones;
        this.habilitadas = habilitadas;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
        this.adjuntos = adjuntos;
        this.bodegas = bodegas;
    }

    /** Sin módulo de bodegas: cualquier {@code bodegaId} se rechaza (400 en bodegaId); nunca se ignora. */
    public EjecutarAccionEtapaCasoDeUso(PedidoRepositorio pedidos, ResolverTransicion transiciones,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj, ValidarAdjuntosParaAccion adjuntos) {
        this(pedidos, transiciones, habilitadas, responsables, vencimientos, publicador, reloj, adjuntos,
                bodegaId -> Optional.empty());
    }

    /** Sin módulo de archivos: cualquier {@code adjuntoIds} se rechaza (400); nunca se ignora. */
    public EjecutarAccionEtapaCasoDeUso(PedidoRepositorio pedidos, ResolverTransicion transiciones,
            EtapasHabilitadas habilitadas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj) {
        this(pedidos, transiciones, habilitadas, responsables, vencimientos, publicador, reloj,
                (pedidoId, etapa, entradaId, adjuntoIds) -> {
                    if (adjuntoIds == null || adjuntoIds.isEmpty()) {
                        return Set.of();
                    }
                    throw new DatoInvalido("adjuntoIds", "No se pueden verificar los archivos de la acción.");
                });
    }

    @Override
    public Pedido ejecutar(Solicitante solicitante, Comando comando) {
        Pedido pedido = pedidos.buscarParaActualizar(comando.pedidoId())
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        if (pedido.finalizado()) {
            throw new TransicionNoPermitida("El pedido ya terminó.");
        }
        // Antes de resolver la acción: si la etapa cambió, la respuesta es 409 ETAPA_CAMBIO y no "acción no permitida".
        if (comando.etapaEsperada() != pedido.etapaActual()) {
            throw new EtapaCambio("La etapa del pedido cambió. Vuelve a cargarlo antes de actuar.");
        }
        ActorVigente actor = apoyo.solicitante(solicitante);
        ResolverTransicion.Resolucion resolucion = transiciones.resolver(pedido.flujoId(), pedido.etapaActual(),
                comando.accion());
        // ADR-0018: la etapa de llegada está marcada en la definición del flujo; su APROBAR es el check de llegada.
        boolean registraLlegada = resolucion.origenRegistraLlegada() && resolucion.accion() == AccionEtapa.APROBAR
                && resolucion.etapaDestino() != null && resolucion.etapaDestino() > resolucion.etapaOrigen();
        ReglaDeTransicion regla = new ReglaDeTransicion(resolucion.etapaOrigen(), resolucion.accion(),
                resolucion.etapaDestino(), resolucion.requiereObservacion(), resolucion.requiereUrgencia(),
                resolucion.requiereBodega(), resolucion.documentosPendientes(), resolucion.documentosObligatorios(),
                resolucion.plazoDestino(), resolucion.destinoEsRetornoDeUrgencia(), resolucion.rolResponsableOrigen(),
                registraLlegada, resolucion.destinoEsAutomatico());
        Set<String> presentes = Set.of();
        if (!comando.adjuntoIds().isEmpty()) {
            EtapaDePedido actual = etapaActual(pedido);
            // Antes de revisar archivos, el permiso: quien no puede actuar no aprende nada de los identificadores.
            PermisosEtapa.paraActuar(actual, resolucion.rolResponsableOrigen(), actor);
            presentes = adjuntos.tiposValidos(pedido.id(), pedido.etapaActual(), actual.entradaId(),
                    comando.adjuntoIds());
        }
        // F4-D2: la bodega elegida debe existir y estar activa. Solo se consulta si la acción la usa y podría
        // completarse; si no, el dominio responde antes (bodega no admitida o documentos pendientes, F4-D7). Primero el
        // permiso, como con los archivos. El trigger de la base (V9, V12) es la última barrera ante una desactivación
        // simultánea.
        // Si el pedido ya tiene bodega no se consulta: el dominio responde 409 (se asigna una sola vez).
        if (comando.bodegaId() != null && resolucion.requiereBodega() && !resolucion.documentosPendientes()
                && habilitadas.incluye(pedido.etapaActual()) && pedido.bodegaId().isEmpty()) {
            PermisosEtapa.paraActuar(etapaActual(pedido), resolucion.rolResponsableOrigen(), actor);
            if (bodegas.activa(comando.bodegaId()).isEmpty()) {
                throw new DatoInvalido("bodegaId", "La bodega no existe o está inactiva; elige una bodega activa.");
            }
        }
        AccionSolicitada solicitud = new AccionSolicitada(comando.accion(), comando.etapaEsperada(),
                comando.observacion(), comando.nivelUrgencia(), presentes, comando.adjuntoIds(),
                comando.nuevaFechaEstimadaLlegada(), comando.bodegaId());
        Instant ahora = reloj.ahora();
        // Una sola lectura del reloj: la hora del evento y el "hoy" de la demora son del mismo instante.
        pedido.ejecutar(solicitud, regla, habilitadas, actor, ahora, apoyo.vencimientos(),
                ahora.atZone(reloj.zona()).toLocalDate());
        pedidos.actualizar(pedido);
        apoyo.publicarEventos(pedido);
        return pedido;
    }

    private static EtapaDePedido etapaActual(Pedido pedido) {
        return pedido.etapas().stream()
                .filter(etapa -> etapa.numero() == pedido.etapaActual())
                .findFirst()
                .orElseThrow();
    }
}
