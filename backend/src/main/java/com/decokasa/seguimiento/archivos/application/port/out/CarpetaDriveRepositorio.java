package com.decokasa.seguimiento.archivos.application.port.out;

import java.time.Instant;
import java.util.Optional;

/** Reserva estable por país/pedido; una reserva perdedora no crea un objeto remoto. */
public interface CarpetaDriveRepositorio {
    Optional<Carpeta> buscar(String clave);
    Carpeta reservar(String clave, String nombre, String padre, String driveId, Instant ahora);
    void confirmar(String clave, String driveId);
    record Carpeta(String clave, String nombre, String padreId, String driveId, boolean confirmada) { }
}
