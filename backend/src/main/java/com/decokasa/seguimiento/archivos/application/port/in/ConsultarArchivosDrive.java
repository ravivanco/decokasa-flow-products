package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;
public interface ConsultarArchivosDrive {
    String enlace(Solicitante solicitante, UUID adjuntoId);
    List<AdjuntoVista> fallidos(Solicitante solicitante, int pagina, int tamano);
}
