package com.decokasa.seguimiento.notificaciones.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.List;
public interface ConsultarNotificaciones {
    Pagina consultar(Solicitante solicitante, Notificacion.Estado estado, int pagina, int tamano);
    record Pagina(List<Notificacion> elementos, int pagina, int tamano, long total) {
        public Pagina { elementos=List.copyOf(elementos); }
    }
}
