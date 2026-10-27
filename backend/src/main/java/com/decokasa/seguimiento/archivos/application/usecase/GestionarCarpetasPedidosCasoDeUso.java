package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasPedidos;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaPedidoRepositorio;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.UUID;

/**
 * Programación y consulta de la carpeta de Drive de cada pedido (ADR-0027). El alta del pedido la programa sola, dentro
 * de su transacción; los pedidos existentes solo los programa el admin, uno a uno (sin creación masiva). La consulta y
 * la programación exigen al admin vigente en la base (R6, hallazgo A1) con el país del pedido asignado hoy (ADR-0024):
 * fuera de sus países el pedido no existe (404). Nunca llama a Google ni anuncia una carpeta sin la confirmación
 * guardada. Sin Spring (R2).
 */
public final class GestionarCarpetasPedidosCasoDeUso
        implements GestionarCarpetasPedidos, GestionarCarpetasPedidos.ProgramarAlta {
    private static final CodigoRol ADMIN = new CodigoRol("admin");
    private final CarpetaPedidoRepositorio carpetas;
    private final ConsultarResponsables responsables;
    private final Reloj reloj;

    public GestionarCarpetasPedidosCasoDeUso(CarpetaPedidoRepositorio carpetas, ConsultarResponsables responsables,
            Reloj reloj) {
        this.carpetas = carpetas;
        this.responsables = responsables;
        this.reloj = reloj;
    }

    @Override public Estado estado(Solicitante solicitante, UUID pedidoId) {
        return vista(visible(solicitante, pedidoId));
    }

    @Override public Estado programar(Solicitante solicitante, UUID pedidoId) {
        visible(solicitante, pedidoId);
        carpetas.programar(pedidoId, CarpetaPedidoRepositorio.Origen.ADMIN, true, reloj.ahora());
        return vista(carpetas.estado(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe.")));
    }

    /** Alta del pedido: registra la intención en la misma transacción, sin reabrir nada. */
    @Override public void programarAlta(UUID pedidoId, Instant ocurridoEn) {
        carpetas.programar(pedidoId, CarpetaPedidoRepositorio.Origen.ALTA, false, ocurridoEn);
    }

    private CarpetaPedidoRepositorio.Estado visible(Solicitante solicitante, UUID pedidoId) {
        var admin = responsables.estado(solicitante.usuarioId()).filter(u -> u.activo() && u.roles().contains(ADMIN))
                .orElseThrow(() -> new SinPermiso("Solo el admin vigente puede gestionar las carpetas de Drive."));
        return carpetas.estado(pedidoId).filter(e -> admin.paises().contains(e.pais()))
                .orElseThrow(() -> new NoEncontrado("El pedido no existe."));
    }

    private static Estado vista(CarpetaPedidoRepositorio.Estado e) {
        return new Estado(e.pedidoId(), e.estado(), e.intentos(), e.ultimoError(), e.programadaEn(), e.creadaEn());
    }
}
