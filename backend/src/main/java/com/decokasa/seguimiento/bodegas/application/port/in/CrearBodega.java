package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;

/**
 * Alta en el catálogo (contrato: POST /bodegas). F4-D1, provisional del equipo: solo Logística o admin vigentes, y
 * desde la fase 7 (ADR-0024) con el país de la bodega asignado.
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso sin Logística ni admin vigentes en la base
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si ya existe una bodega con ese nombre
 */
public interface CrearBodega {

    Bodega crear(Solicitante solicitante, Comando comando);

    /**
     * @param activa nulo equivale a activa (contrato: default true)
     * @param pais   nulo equivale a Ecuador (contrato: default EC)
     */
    record Comando(String nombre, String ciudad, Boolean activa, CodigoPais pais) {

        /**
         * Solo compatibilidad documentada (anterior a la fase 7): sin país, el alta es de Ecuador. La autorización no
         * cambia: se exige Logística o admin con Ecuador asignado; sin él, 403 (ADR-0024).
         */
        public Comando(String nombre, String ciudad, Boolean activa) {
            this(nombre, ciudad, activa, null);
        }
    }
}
