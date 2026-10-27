package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.plazos.application.port.in.ConfigurarMeta;
import com.decokasa.seguimiento.plazos.application.port.in.ConsultarConfiguracionPlazos;
import com.decokasa.seguimiento.plazos.application.port.out.ConfiguracionPlazosRepositorio;
import com.decokasa.seguimiento.plazos.domain.ConfiguracionPlazos;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;

/**
 * Meta de días (F5-D6). La meta es configurable por el admin; su valor sigue pendiente del cliente ("se ajusta") y la
 * siembra de V14 es solo la propuesta de 90 días, sin aprobación provisional documentada. Cambiarla exige el estado
 * vigente del admin en la base, no los roles del JWT (lección A1), y la versión leída (D10). Sin Spring (R2).
 */
public final class ConfiguracionPlazosCasoDeUso implements ConsultarConfiguracionPlazos, ConfigurarMeta {

    static final String CONFIGURACION_CAMBIO =
            "Otra persona cambió la configuración de plazos. Vuelve a cargarla y repite el cambio.";
    private static final CodigoRol ADMIN = new CodigoRol("admin");

    private final ConfiguracionPlazosRepositorio repositorio;
    private final ConsultarResponsables usuarios;
    private final Reloj reloj;

    public ConfiguracionPlazosCasoDeUso(ConfiguracionPlazosRepositorio repositorio, ConsultarResponsables usuarios,
            Reloj reloj) {
        this.repositorio = repositorio;
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    @Override
    public Configuracion consultar() {
        return vista(repositorio.leer());
    }

    @Override
    public Configuracion configurar(Solicitante solicitante, Comando comando) {
        exigirAdmin(solicitante);
        ConfiguracionPlazos actual = repositorio.leerParaActualizar();
        if (actual.version() != comando.version()) {
            throw new VersionDesactualizada(CONFIGURACION_CAMBIO);
        }
        return vista(repositorio.guardar(actual.conMeta(comando.metaDias(), solicitante.usuarioId(), reloj.ahora())));
    }

    private void exigirAdmin(Solicitante solicitante) {
        boolean puede = solicitante != null && solicitante.usuarioId() != null
                && usuarios.estado(solicitante.usuarioId())
                        .filter(ConsultarResponsables.EstadoVigente::activo)
                        .filter(estado -> estado.roles().contains(ADMIN))
                        .isPresent();
        if (!puede) {
            throw new SinPermiso("Solo el admin cambia la meta de días.");
        }
    }

    private static Configuracion vista(ConfiguracionPlazos configuracion) {
        return new Configuracion(configuracion.metaDias(), configuracion.origenMeta().name(),
                configuracion.actualizadoEn(), configuracion.version());
    }
}
