package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.GestionarConfiguracionIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA.TarifaRegistrada;
import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import java.time.LocalDate;

/**
 * Interruptor del asistente en la base (ADR-0022: tras el apagado automático lo reactiva un admin). Activar exige la
 * tarifa vigente del modelo validada y bajo el umbral del tramo caro. No cambia IA_HABILITADA ni los límites.
 */
public final class GestionarConfiguracionIACasoDeUso implements GestionarConfiguracionIA {

    private final AccesoAsistente acceso;
    private final ControlesAsistente controles;
    private final PresupuestoIA presupuesto;
    private final TarifasIA tarifas;
    private final ConfiguracionAsistente configuracion;
    private final Reloj reloj;

    public GestionarConfiguracionIACasoDeUso(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            PresupuestoIA presupuesto, TarifasIA tarifas, ConfiguracionAsistente configuracion, Reloj reloj) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.controles = new ControlesAsistente(configuracion, presupuesto);
        this.presupuesto = presupuesto;
        this.tarifas = tarifas;
        this.configuracion = configuracion;
        this.reloj = reloj;
    }

    @Override
    public Estado obtener(Solicitante solicitante) {
        acceso.admin(solicitante);
        return estado();
    }

    @Override
    public Estado cambiarHabilitada(Solicitante solicitante, boolean habilitada, long version) {
        acceso.admin(solicitante);
        LimitesIA limites = presupuesto.limites();
        if (habilitada) {
            Tarifa tarifa = presupuesto.tarifaVigente(configuracion.modelo(), hoy()).orElse(null);
            if (tarifa == null || !tarifa.validada() || !tarifa.admiteLimiteDeEntrada(limites.tokensEntradaMax())) {
                throw new DatoInvalido("Antes de activar el asistente, valida la tarifa vigente del modelo configurado.");
            }
        }
        String motivo = habilitada ? null : "Desactivado por un admin.";
        if (!presupuesto.cambiarHabilitada(habilitada, motivo, version, solicitante.usuarioId(), reloj.ahora())) {
            throw new VersionDesactualizada("Otra persona cambió la configuración del asistente. Revisa el estado "
                    + "actual antes de volver a intentarlo.");
        }
        return estado();
    }

    private Estado estado() {
        LimitesIA limites = presupuesto.limites();
        TarifaRegistrada tarifa = presupuesto.tarifaVigente(configuracion.modelo(), hoy())
                .flatMap(vigente -> tarifas.porId(vigente.id())).orElse(null);
        return new Estado(limites, configuracion.habilitada(), configuracion.pilotoFicticio(), configuracion.modelo(),
                tarifa, controles.evaluar(hoy()).isPresent());
    }

    private LocalDate hoy() {
        return LocalDate.ofInstant(reloj.ahora(), Reloj.ZONA_ECUADOR);
    }
}
