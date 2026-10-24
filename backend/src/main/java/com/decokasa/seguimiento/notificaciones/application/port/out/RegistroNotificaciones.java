package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.List;
import java.util.Set;
public interface RegistroNotificaciones {
    /** Sin filtro de país. Anterior a la fase 7: ningún caso de uso lo usa para responder a una persona (ADR-0024). */
    List<Notificacion> listar(Notificacion.Estado estado, int pagina, int tamano);
    /** Sin filtro de país. Anterior a la fase 7: ningún caso de uso lo usa para responder a una persona (ADR-0024). */
    long contar(Notificacion.Estado estado);

    /**
     * Fase 7 (ADR-0024): solo correos de pedidos de esos países (los asignados hoy al admin), filtrados en la consulta.
     * Un conjunto vacío no devuelve nada. No cambia ni reasigna ningún registro.
     */
    List<Notificacion> listar(Set<CodigoPais> paises, Notificacion.Estado estado, int pagina, int tamano);

    /** Fase 7 (ADR-0024): total con el mismo filtro de país que {@link #listar(Set, Notificacion.Estado, int, int)}. */
    long contar(Set<CodigoPais> paises, Notificacion.Estado estado);
}
