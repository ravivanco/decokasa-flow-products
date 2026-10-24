package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegaActiva;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegas;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegasPorPais;
import com.decokasa.seguimiento.bodegas.application.port.in.DescribirBodega;
import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Lecturas del catálogo. Sin Spring (R2). */
public final class ConsultarBodegasCasoDeUso
        implements ConsultarBodegas, ConsultarBodegasPorPais, ConsultarBodegaActiva, DescribirBodega {

    private final BodegaRepositorio bodegas;
    private final ConsultarResponsables usuarios;

    /** Sin usuarios: solo las lecturas para otros módulos y la lista sin filtro (firma anterior a la fase 7). */
    public ConsultarBodegasCasoDeUso(BodegaRepositorio bodegas) {
        this(bodegas, null);
    }

    public ConsultarBodegasCasoDeUso(BodegaRepositorio bodegas, ConsultarResponsables usuarios) {
        this.bodegas = bodegas;
        this.usuarios = usuarios;
    }

    /** ADR-0024: con el estado vigente en la base; solo los países asignados hoy. */
    @Override
    public List<Bodega> listar(Solicitante solicitante, CodigoPais pais, Boolean activa) {
        ConsultarResponsables.EstadoVigente estado = solicitante == null || solicitante.usuarioId() == null
                || usuarios == null
                ? null
                : usuarios.estado(solicitante.usuarioId())
                        .filter(vigente -> vigente.activo() && !vigente.roles().isEmpty())
                        .orElse(null);
        if (estado == null) {
            throw new SinPermiso("Tu cuenta no está activa o no tiene roles.");
        }
        if (pais != null && !estado.paises().contains(pais)) {
            throw new SinPermiso("No tienes asignado ese país.");
        }
        Set<CodigoPais> paises = pais == null ? estado.paises() : Set.of(pais);
        return paises.isEmpty() ? List.of() : bodegas.listar(paises, activa);
    }

    @Override
    public List<Bodega> listar(Boolean activa) {
        return bodegas.listar(activa);
    }

    @Override
    public Optional<BodegaActiva> activa(UUID bodegaId) {
        return bodegas.buscarPorId(bodegaId)
                .filter(Bodega::activa)
                .map(bodega -> new BodegaActiva(bodega.id(), bodega.nombre(), bodega.ciudad(), bodega.pais()));
    }

    @Override
    public Optional<Vista> describir(UUID bodegaId) {
        return bodegas.buscarPorId(bodegaId)
                .map(bodega -> new Vista(bodega.id(), bodega.nombre(), bodega.ciudad(), bodega.activa(),
                        bodega.version(), bodega.pais()));
    }
}
