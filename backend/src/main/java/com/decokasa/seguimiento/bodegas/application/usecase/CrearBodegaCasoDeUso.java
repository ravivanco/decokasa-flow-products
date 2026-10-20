package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.bodegas.application.port.in.CrearBodega;
import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;

/** Alta en el catálogo (F4-D1). Primero el permiso, después los datos. Sin Spring (R2). */
public final class CrearBodegaCasoDeUso implements CrearBodega {

    static final String NOMBRE_REPETIDO = "Ya existe una bodega con ese nombre.";

    private final BodegaRepositorio bodegas;
    private final MantenimientoAutorizado autorizacion;

    public CrearBodegaCasoDeUso(BodegaRepositorio bodegas, ConsultarResponsables usuarios) {
        this.bodegas = bodegas;
        this.autorizacion = new MantenimientoAutorizado(usuarios);
    }

    @Override
    public Bodega crear(Solicitante solicitante, Comando comando) {
        autorizacion.exigir(solicitante);
        Bodega bodega = Bodega.nueva(comando.nombre(), comando.ciudad(), comando.activa());
        // Aviso temprano y claro; una alta simultánea con el mismo nombre la detiene el índice único (409 igual).
        bodegas.buscarPorNombre(bodega.nombre()).ifPresent(otra -> {
            throw new DatoDuplicado("nombre", NOMBRE_REPETIDO);
        });
        return bodegas.guardar(bodega);
    }
}
