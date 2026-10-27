package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.AbrirConversacion;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente.EstadoCuenta;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.PedidosParaAsistente.DatosPedido;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.IaNoDisponible;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.UUID;

/**
 * Abre una conversación sobre un pedido (F7-D1 opción C). Cada apertura crea una conversación nueva: el alcance es un
 * único pedido y un único autor. No llama al modelo ni gasta.
 */
public final class AbrirConversacionCasoDeUso implements AbrirConversacion {

    private final AccesoAsistente acceso;
    private final PedidosParaAsistente pedidos;
    private final ConversacionesIA conversaciones;
    private final Reloj reloj;
    private final ConfiguracionAsistente configuracion;

    public AbrirConversacionCasoDeUso(CuentasParaAsistente cuentas, PedidosParaAsistente pedidos,
            ConversacionesIA conversaciones, Reloj reloj, ConfiguracionAsistente configuracion) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.pedidos = pedidos;
        this.conversaciones = conversaciones;
        this.reloj = reloj;
        this.configuracion = configuracion;
    }

    @Override
    public ConversacionIA abrir(Solicitante solicitante, UUID pedidoId) {
        EstadoCuenta cuenta = acceso.habilitado(solicitante);
        DatosPedido pedido = pedidos.leer(solicitante, pedidoId);
        // Defensa adicional: pedidos ya responde 404 para otro país (ADR-0024).
        if (!cuenta.paises().contains(pedido.pais())) {
            throw new NoEncontrado("El pedido no existe.");
        }
        if (!configuracion.habilitada()) {
            throw new IaNoDisponible();
        }
        ConversacionIA conversacion = ConversacionIA.nueva(UUID.randomUUID(), solicitante.usuarioId(), pedido.id(),
                pedido.numero(), pedido.pais(), reloj.ahora(), configuracion.conservacion());
        conversaciones.insertar(conversacion);
        return conversacion;
    }
}
