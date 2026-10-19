package com.decokasa.seguimiento.notificaciones.application.usecase;

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
    @Override public Pagina consultar(Solicitante s, Notificacion.Estado estado, int pagina, int tamano) {
        var vigente=usuarios.estado(s.usuarioId()).filter(u -> u.activo() && u.roles().contains(new CodigoRol("admin")));
        if (vigente.isEmpty()) { throw new SinPermiso("Solo el admin vigente puede consultar correos."); }
        if (pagina < 0 || tamano < 1 || tamano > 100) { throw new DatoInvalido("pagina","Paginación inválida."); }
        return new Pagina(registro.listar(estado,pagina,tamano),pagina,tamano,registro.contar(estado));
    }
}
