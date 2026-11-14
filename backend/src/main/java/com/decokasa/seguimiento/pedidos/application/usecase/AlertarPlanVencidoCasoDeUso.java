package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.eventos.PlanVencido;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.application.port.out.PlanesVencidos;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Cuenta regresiva desde la creación (pedido del cliente, 2026-11-14): cada etapa tiene la fecha de fin planificada al
 * crear el pedido (cronograma de ADR-0030; las etapas sin plazo usan su duración de referencia). Si la etapa actual pasa
 * ese día en la zona del país del pedido, se publica PLAN_VENCIDO una sola vez; los avisos lo llevan al responsable y
 * a Admin por la aplicación, el correo y WhatsApp. Sin Spring (R2).
 */
public final class AlertarPlanVencidoCasoDeUso {

    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final CalcularCronograma cronogramas;
    private final PlanesVencidos planes;
    private final PublicadorEventos publicador;
    private final Reloj reloj;

    public AlertarPlanVencidoCasoDeUso(ConsultaDePedidos consultas, ConsultarDefinicionFlujo flujos,
            CalcularCronograma cronogramas, PlanesVencidos planes, PublicadorEventos publicador, Reloj reloj) {
        this.consultas = consultas;
        this.flujos = flujos;
        this.cronogramas = cronogramas;
        this.planes = planes;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    public List<UUID> candidatos(int limite) {
        return planes.enCurso(limite);
    }

    /** @return verdadero si publicó la alerta */
    public boolean revisar(UUID pedidoId) {
        Pedido pedido = consultas.porId(pedidoId).orElse(null);
        if (pedido == null || pedido.finalizado()) {
            return false;
        }
        Instant ahora = reloj.ahora();
        CalcularCronograma.Resultado resultado = cronogramas.paraPais(pedido.pais())
                .calcular(DatosDeCronograma.de(pedido, flujos.porId(pedido.flujoId()), ahora));
        CalcularCronograma.Etapa actual = resultado.etapas().stream()
                .filter(etapa -> etapa.numero() == pedido.etapaActual())
                .findFirst()
                .orElse(null);
        if (actual == null || actual.fechaOriginal() == null) {
            return false;
        }
        LocalDate hoy = ahora.atZone(pedido.pais().zona()).toLocalDate();
        if (!hoy.isAfter(actual.fechaOriginal()) || planes.avisado(pedidoId, pedido.etapaActual())) {
            return false;
        }
        publicador.publicar(new PlanVencido(pedido.datosDelPedido(), pedido.etapaActual(), actual.nombre(),
                actual.fechaOriginal(), ahora));
        return true;
    }
}
