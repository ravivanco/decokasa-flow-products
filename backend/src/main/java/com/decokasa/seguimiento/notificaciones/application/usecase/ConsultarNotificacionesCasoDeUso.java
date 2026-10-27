package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarNotificaciones;
import com.decokasa.seguimiento.notificaciones.application.port.out.RegistroNotificaciones;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;

public final class ConsultarNotificacionesCasoDeUso implements ConsultarNotificaciones {
    private final RegistroNotificaciones registro;
    private final ConsultarResponsables usuarios;
    public ConsultarNotificacionesCasoDeUso(RegistroNotificaciones registro, ConsultarResponsables usuarios) {
        this.registro=registro; this.usuarios=usuarios;
    }
    @Override public Pagina consultar(Solicitante s, CodigoPais pais, Notificacion.Estado estado, int pagina,
            int tamano) {
        var vigente=usuarios.estado(s.usuarioId()).filter(u -> u.activo() && u.roles().contains(new CodigoRol("admin")));
        if (vigente.isEmpty()) { throw new SinPermiso("Solo el admin vigente puede consultar correos."); }
        if (pagina < 0 || tamano < 1 || tamano > 100) { throw new DatoInvalido("pagina","Paginación inválida."); }
        // ADR-0024: solo correos de pedidos de los países asignados hoy al admin; lista y total con el mismo filtro.
        var asignados=vigente.get().paises();
        if (pais != null && !asignados.contains(pais)) { throw new SinPermiso("No tienes asignado ese país."); }
        var paises=pais == null ? asignados : java.util.Set.of(pais);
        return new Pagina(registro.listar(paises,estado,pagina,tamano),pagina,tamano,registro.contar(paises,estado));
    }
}
