package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;
public interface ConsultarArchivosDrive {
    String enlace(Solicitante solicitante, UUID adjuntoId);
    /**
     * Paso 7.3: copias fallidas de pedidos del país seleccionado, que debe estar asignado hoy al admin (403 si no); sin
     * país, de todos sus países asignados (ADR-0024).
     */
    List<AdjuntoVista> fallidos(Solicitante solicitante, CodigoPais pais, int pagina, int tamano);

    /** Compatibilidad anterior a 7.3: sin país seleccionado (todos los asignados). */
    default List<AdjuntoVista> fallidos(Solicitante solicitante, int pagina, int tamano) {
        return fallidos(solicitante, null, pagina, tamano);
    }
}
