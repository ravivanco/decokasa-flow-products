package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarCronograma;
import com.decokasa.seguimiento.pedidos.application.port.out.ConsultaDePedidos;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.UUID;

/**
 * Entrega a plazos los datos del pedido para su cronograma, con la visibilidad de P-13 (provisional del equipo): quien
 * tiene cuenta activa con roles vigentes ve cualquier pedido de sus países (ADR-0024). El momento del cálculo sale del
 * reloj del servidor (ADR-0030: una etapa abierta después de su fin estimado se proyecta hasta ese momento). Sin
 * Spring (R2).
 */
public final class ConsultarCronogramaCasoDeUso implements ConsultarCronograma {

    private final ConsultaDePedidos consultas;
    private final ConsultarDefinicionFlujo flujos;
    private final CalcularCronograma cronogramas;
    private final ApoyoPedidos apoyo;
    private final Reloj reloj;

    public ConsultarCronogramaCasoDeUso(ConsultaDePedidos consultas, ConsultarDefinicionFlujo flujos,
            CalcularCronograma cronogramas, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj) {
        this.consultas = consultas;
        this.flujos = flujos;
        this.cronogramas = cronogramas;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
        this.reloj = reloj;
    }

    @Override
    public CalcularCronograma.Resultado cronograma(Solicitante solicitante, UUID pedidoId) {
        ActorVigente actor = apoyo.lector(solicitante);
        Pedido pedido = consultas.porId(pedidoId).orElseThrow(() -> new NoEncontrado("El pedido no existe."));
        apoyo.exigirPais(actor, pedido);
        // Fase 7: el calendario del país del pedido.
        return cronogramas.paraPais(pedido.pais())
                .calcular(DatosDeCronograma.de(pedido, flujos.porId(pedido.flujoId()), reloj.ahora()));
    }
}
