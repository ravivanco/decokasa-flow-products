package com.decokasa.seguimiento.notificaciones.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.List;
public interface ConsultarNotificaciones {
    /**
     * Paso 7.3: correos de pedidos del país seleccionado, que debe estar asignado hoy al admin (403 si no); sin país,
     * de todos sus países asignados (ADR-0024).
     */
    Pagina consultar(Solicitante solicitante, CodigoPais pais, Notificacion.Estado estado, int pagina, int tamano);

    /** Compatibilidad anterior a 7.3: sin país seleccionado (todos los asignados). */
    default Pagina consultar(Solicitante solicitante, Notificacion.Estado estado, int pagina, int tamano) {
        return consultar(solicitante, null, estado, pagina, tamano);
    }
    record Pagina(List<Notificacion> elementos, int pagina, int tamano, long total) {
        public Pagina { elementos=List.copyOf(elementos); }
    }
}
