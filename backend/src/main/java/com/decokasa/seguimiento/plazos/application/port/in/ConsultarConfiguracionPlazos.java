package com.decokasa.seguimiento.plazos.application.port.in;

import java.time.Instant;

/**
 * Configuración vigente de plazos (contrato: GET /configuracion/plazos). Solo la meta de días: los feriados siguen
 * pendientes del cliente (⏸ F5-D7) y no se guardan.
 */
public interface ConsultarConfiguracionPlazos {

    Configuracion consultar();

    /**
     * @param origenMeta    PROPUESTA (siembra de 90, pendiente del cliente) o ADMIN
     * @param actualizadoEn nulo mientras rige la siembra
     */
    record Configuracion(int metaDias, String origenMeta, Instant actualizadoEn, long version) {
    }
}
