package com.decokasa.seguimiento.archivos.application.port.in;

/** Una pasada acotada. No autoriza activar Google ni expone un endpoint HTTP. */
public interface ReintentarCopiasPendientes {
    int ejecutar();
}
