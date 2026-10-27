package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA.TarifaRegistrada;
import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.compartido.domain.Solicitante;

/** Estado del asistente y su interruptor en la base (solo admin). Los límites no se editan aquí (F7-D2). */
public interface GestionarConfiguracionIA {

    Estado obtener(Solicitante solicitante);

    /**
     * Activar exige la tarifa vigente del modelo validada. Aun activada en la base, la IA no responde mientras
     * IA_HABILITADA=false o falten la clave o el modelo.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.VersionDesactualizada si otra persona la cambió antes
     */
    Estado cambiarHabilitada(Solicitante solicitante, boolean habilitada, long version);

    /**
     * @param entornoHabilitado IA_HABILITADA=true con clave y modelo
     * @param tarifa tarifa vigente del modelo configurado; nula si no hay
     */
    record Estado(LimitesIA limites, boolean entornoHabilitado, boolean pilotoFicticio, String modelo,
            TarifaRegistrada tarifa, boolean disponible) {
    }
}
