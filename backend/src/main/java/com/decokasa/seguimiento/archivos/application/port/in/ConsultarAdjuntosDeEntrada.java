package com.decokasa.seguimiento.archivos.application.port.in;

import java.util.Set;
import java.util.UUID;

/** Tipos de documento ya subidos a una entrada de etapa, para mostrar qué falta (solo metadatos; P-18). */
public interface ConsultarAdjuntosDeEntrada {

    Set<String> tiposSubidos(UUID entradaId);
}
