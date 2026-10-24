package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConsultaConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente.EstadoCuenta;
import com.decokasa.seguimiento.asistente.application.port.out.PresupuestoIA;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Consumo del asistente para el admin: por día de Ecuador y usuario, solo de los países asignados hoy al admin
 * (ADR-0024: el admin no tiene excepción). El presupuesto del mes es global del piloto. Sin textos.
 */
public final class ConsultarConsumoIACasoDeUso implements ConsultarConsumoIA {

    private final AccesoAsistente acceso;
    private final ConsultaConsumoIA consumo;
    private final PresupuestoIA presupuesto;
    private final Reloj reloj;

    public ConsultarConsumoIACasoDeUso(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            ConsultaConsumoIA consumo, PresupuestoIA presupuesto, Reloj reloj) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.consumo = consumo;
        this.presupuesto = presupuesto;
        this.reloj = reloj;
    }

    @Override
    public Consumo consultar(Solicitante solicitante, LocalDate desde, LocalDate hasta) {
        EstadoCuenta admin = acceso.admin(solicitante);
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatoInvalido("Indica un periodo válido: la fecha final no puede ser anterior a la inicial.");
        }
        Instant inicio = desde.atStartOfDay(Reloj.ZONA_ECUADOR).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(Reloj.ZONA_ECUADOR).toInstant();
        LocalDate periodo = LocalDate.ofInstant(reloj.ahora(), Reloj.ZONA_ECUADOR).withDayOfMonth(1);
        ConsultaConsumoIA.EstadoPresupuesto mes = consumo.presupuesto(periodo)
                .orElseGet(() -> new ConsultaConsumoIA.EstadoPresupuesto(
                        presupuesto.limites().presupuestoMesMicroUsd(), 0, 0));
        return new Consumo(mes.limiteMicroUsd(), mes.consumidoMicroUsd(), mes.reservadoMicroUsd(),
                consumo.porDiaYUsuario(admin.paises(), inicio, fin));
    }
}
