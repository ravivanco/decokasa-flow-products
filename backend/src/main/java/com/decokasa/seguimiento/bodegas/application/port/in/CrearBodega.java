package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.Solicitante;

/**
 * Alta en el catálogo (contrato: POST /bodegas). F4-D1, provisional del equipo: solo Logística o admin vigentes.
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso sin Logística ni admin vigentes en la base
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si ya existe una bodega con ese nombre
 */
public interface CrearBodega {

    Bodega crear(Solicitante solicitante, Comando comando);

    /** @param activa nulo equivale a activa (contrato: default true) */
    record Comando(String nombre, String ciudad, Boolean activa) {
    }
}
