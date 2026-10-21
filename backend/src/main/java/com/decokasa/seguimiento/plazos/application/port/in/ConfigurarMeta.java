package com.decokasa.seguimiento.plazos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;

/**
 * El admin cambia la meta de días (arquitectura.md 4.7, ConfigurarMeta; contrato: PUT /configuracion/plazos). Se decide
 * con el estado vigente del solicitante en la base (lección A1) y con la versión leída (D10: 409 si cambió).
 */
public interface ConfigurarMeta {

    ConsultarConfiguracionPlazos.Configuracion configurar(Solicitante solicitante, Comando comando);

    record Comando(int metaDias, long version) {
    }
}
