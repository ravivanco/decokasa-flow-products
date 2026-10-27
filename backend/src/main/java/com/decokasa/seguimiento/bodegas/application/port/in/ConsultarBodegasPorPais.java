package com.decokasa.seguimiento.bodegas.application.port.in;

import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;

/**
 * Catálogo de bodegas visible para quien consulta (contrato: GET /bodegas; fase 7, ADR-0024): solo las de los países
 * que tiene asignados hoy en la base, con cuenta activa y al menos un rol.
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la cuenta no está activa, no tiene roles o pide un
 *         país que no tiene asignado
 */
public interface ConsultarBodegasPorPais {

    /**
     * @param pais   nulo para todos sus países asignados
     * @param activa nulo para todas; verdadero o falso para filtrar. Ordenadas por país y nombre.
     */
    List<Bodega> listar(Solicitante solicitante, CodigoPais pais, Boolean activa);
}
