package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.List;
public interface RegistroNotificaciones {
    List<Notificacion> listar(Notificacion.Estado estado, int pagina, int tamano);
    long contar(Notificacion.Estado estado);
}
