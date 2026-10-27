package com.decokasa.seguimiento.notificaciones.application.port.out;

import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Una fila por evento/destinatario normalizado. Mutaciones requieren la transacción del llamador. */
public interface NotificacionRepositorio {
    boolean insertarSiAusente(Notificacion notificacion);
    List<Notificacion> reclamar(int limite, Instant ahora, Instant leaseHasta, UUID propietario);
    boolean marcarEnviada(UUID id, UUID propietario, String proveedorId, Instant ahora);
    boolean registrarFallo(UUID id, UUID propietario, Causa causa, Instant siguiente, boolean terminal, Instant ahora);
    boolean resolverDestinatario(UUID id, String destinatario);
    List<Notificacion> listar(Notificacion.Estado estado, int pagina, int tamano);
}
