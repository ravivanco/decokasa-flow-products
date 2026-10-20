package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegaActiva;
import com.decokasa.seguimiento.bodegas.application.port.in.ConsultarBodegas;
import com.decokasa.seguimiento.bodegas.application.port.in.DescribirBodega;
import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lecturas del catálogo. Sin Spring (R2). */
public final class ConsultarBodegasCasoDeUso implements ConsultarBodegas, ConsultarBodegaActiva, DescribirBodega {

    private final BodegaRepositorio bodegas;

    public ConsultarBodegasCasoDeUso(BodegaRepositorio bodegas) {
        this.bodegas = bodegas;
    }

    @Override
    public List<Bodega> listar(Boolean activa) {
        return bodegas.listar(activa);
    }

    @Override
    public Optional<BodegaActiva> activa(UUID bodegaId) {
        return bodegas.buscarPorId(bodegaId)
                .filter(Bodega::activa)
                .map(bodega -> new BodegaActiva(bodega.id(), bodega.nombre(), bodega.ciudad()));
    }

    @Override
    public Optional<Vista> describir(UUID bodegaId) {
        return bodegas.buscarPorId(bodegaId)
                .map(bodega -> new Vista(bodega.id(), bodega.nombre(), bodega.ciudad(), bodega.activa(),
                        bodega.version()));
    }
}
