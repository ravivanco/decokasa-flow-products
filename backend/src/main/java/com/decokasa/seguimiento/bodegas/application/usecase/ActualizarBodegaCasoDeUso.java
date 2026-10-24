package com.decokasa.seguimiento.bodegas.application.usecase;

import com.decokasa.seguimiento.bodegas.application.port.in.ActualizarBodega;
import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;

/**
 * Edición de una bodega (F4-D1), con la misma pauta que D10 en usuarios: se bloquea y relee la fila y la versión se
 * compara antes de cambiar nada; si otra persona la cambió, 409 VERSION_DESACTUALIZADA sin cambios. Desactivar no toca
 * los pedidos que ya la tienen. Sin Spring (R2).
 */
public final class ActualizarBodegaCasoDeUso implements ActualizarBodega {

    static final String BODEGA_CAMBIO = "Otra persona modificó esta bodega. Vuelve a cargarla y repite el cambio.";

    private final BodegaRepositorio bodegas;
    private final MantenimientoAutorizado autorizacion;

    public ActualizarBodegaCasoDeUso(BodegaRepositorio bodegas, ConsultarResponsables usuarios) {
        this.bodegas = bodegas;
        this.autorizacion = new MantenimientoAutorizado(usuarios);
    }

    @Override
    public Bodega actualizar(Solicitante solicitante, Comando comando) {
        // Primero el rol (403), antes de bloquear ninguna fila.
        var estado = autorizacion.exigirRol(solicitante);
        Bodega actual = bodegas.buscarParaActualizar(comando.bodegaId())
                .orElseThrow(() -> new NoEncontrado("La bodega no existe."));
        // ADR-0024: una bodega de un país no asignado no existe para quien edita (404, como una inexistente).
        if (!estado.paises().contains(actual.pais())) {
            throw new NoEncontrado("La bodega no existe.");
        }
        if (actual.version() != comando.version()) {
            throw new VersionDesactualizada(BODEGA_CAMBIO);
        }
        Bodega cambiada = actual.cambiar(comando.nombre(), comando.ciudad(), comando.activa());
        if (comando.nombre() != null) {
            bodegas.buscarPorNombre(cambiada.pais(), cambiada.nombre())
                    .filter(otra -> !otra.id().equals(actual.id()))
                    .ifPresent(otra -> {
                        throw new DatoDuplicado("nombre", CrearBodegaCasoDeUso.NOMBRE_REPETIDO);
                    });
        }
        return bodegas.guardar(cambiada);
    }
}
