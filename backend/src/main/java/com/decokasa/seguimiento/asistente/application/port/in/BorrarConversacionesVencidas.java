package com.decokasa.seguimiento.asistente.application.port.in;

/** F7-D5 (piloto): borra las conversaciones vencidas con sus mensajes. consulta_ia se conserva sin texto. */
public interface BorrarConversacionesVencidas {

    /** @return conversaciones borradas */
    int ejecutar();
}
