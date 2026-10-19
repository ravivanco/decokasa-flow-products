package com.decokasa.seguimiento.archivos.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface ConsultaCopias {
    Optional<String> enlaceConfirmado(UUID adjuntoId);
    List<UUID> fallidos(int pagina,int tamano);
}
