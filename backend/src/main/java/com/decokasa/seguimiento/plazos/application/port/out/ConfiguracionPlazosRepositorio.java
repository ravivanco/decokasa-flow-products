package com.decokasa.seguimiento.plazos.application.port.out;

import com.decokasa.seguimiento.plazos.domain.ConfiguracionPlazos;

/** Fila única de configuracion_plazos (V14). La siembra de V14 garantiza que existe. */
public interface ConfiguracionPlazosRepositorio {

    ConfiguracionPlazos leer();

    /** Lee con la fila bloqueada ({@code SELECT … FOR UPDATE}) para compararla y cambiarla. */
    ConfiguracionPlazos leerParaActualizar();

    /** Guarda y devuelve la configuración con su versión nueva. */
    ConfiguracionPlazos guardar(ConfiguracionPlazos configuracion);
}
