package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial;
import com.decokasa.seguimiento.archivos.application.port.out.ConfidencialesRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Política de documentos confidenciales (V37). Se decide con los roles vigentes en la base, nunca con los del token
 * (lección A1). Dar o quitar acceso es solo de Admin del país del pedido. Sin Spring (R2).
 */
public final class AccesoConfidencialCasoDeUso implements AccesoConfidencial {

    private final ConfidencialesRepositorio repositorio;
    private final PedidosParaArchivos pedidos;
    private final Reloj reloj;

    public AccesoConfidencialCasoDeUso(ConfidencialesRepositorio repositorio, PedidosParaArchivos pedidos, Reloj reloj) {
        this.repositorio = repositorio;
        this.pedidos = pedidos;
        this.reloj = reloj;
    }

    @Override
    public Set<UUID> confidenciales(UUID pedidoId) {
        return repositorio.confidenciales(pedidoId);
    }

    @Override
    public boolean puedeVer(UUID pedidoId, UUID usuarioId) {
        return usuarioId != null && (repositorio.accesoPorRol(usuarioId) || repositorio.accesoOtorgado(pedidoId, usuarioId));
    }

    @Override
    public Resumen resumen(Solicitante solicitante, UUID pedidoId) {
        pedidos.autorizarConsulta(solicitante, pedidoId);
        return new Resumen(puedeVer(pedidoId, solicitante.usuarioId()), repositorio.porEtapa(pedidoId));
    }

    @Override
    public List<ConAcceso> conAcceso(Solicitante solicitante, UUID pedidoId) {
        exigirAdmin(solicitante, pedidoId);
        return repositorio.conAcceso(pedidoId);
    }

    @Override
    public void otorgar(Solicitante solicitante, UUID pedidoId, UUID usuarioId) {
        exigirAdmin(solicitante, pedidoId);
        if (!repositorio.usuarioActivo(usuarioId)) {
            throw new DatoInvalido("usuarioId", "Elige una persona activa.");
        }
        if (!puedeVer(pedidoId, usuarioId)) {
            repositorio.otorgar(UUID.randomUUID(), pedidoId, usuarioId, solicitante.usuarioId(), reloj.ahora());
        }
    }

    @Override
    public void revocar(Solicitante solicitante, UUID pedidoId, UUID usuarioId) {
        exigirAdmin(solicitante, pedidoId);
        repositorio.revocar(pedidoId, usuarioId, solicitante.usuarioId(), reloj.ahora());
    }

    private void exigirAdmin(Solicitante solicitante, UUID pedidoId) {
        if (solicitante == null || !repositorio.esAdminVigente(solicitante.usuarioId())) {
            throw new SinPermiso("Solo Admin da o quita acceso a los documentos confidenciales.");
        }
        pedidos.autorizarConsulta(solicitante, pedidoId);
    }
}
