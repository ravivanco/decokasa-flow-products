package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeEtapa;
import com.decokasa.seguimiento.compartido.domain.flujo.ContratoDeFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.CrearPedido;
import com.decokasa.seguimiento.pedidos.application.port.out.GeneradorNumeroPedido;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.domain.ActorVigente;
import com.decokasa.seguimiento.pedidos.domain.FlujoDelPedido;
import com.decokasa.seguimiento.pedidos.domain.NumeroPedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.PermisosEtapa;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularVencimiento;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;

/**
 * Crea un pedido. Orden: permiso con el estado vigente, definición vigente del flujo, número reservado en esta
 * transacción, agregado, inserción y eventos. Sin Spring (R2); la transacción la abre la configuración (ADR-0012).
 *
 * <p>Del flujo solo usa el contrato compartido {@link ContratoDeFlujo}, nunca las clases internas de {@code flujos}
 * (R4; 2.B1-D01).
 */
public final class CrearPedidoCasoDeUso implements CrearPedido {

    private final ConsultarDefinicionFlujo flujos;
    private final PedidoRepositorio pedidos;
    private final GeneradorNumeroPedido generador;
    private final Reloj reloj;
    private final ApoyoPedidos apoyo;

    public CrearPedidoCasoDeUso(ConsultarDefinicionFlujo flujos, PedidoRepositorio pedidos,
            GeneradorNumeroPedido generador, ConsultarResponsables responsables, CalcularVencimiento vencimientos,
            PublicadorEventos publicador, Reloj reloj) {
        this.flujos = flujos;
        this.pedidos = pedidos;
        this.generador = generador;
        this.reloj = reloj;
        this.apoyo = new ApoyoPedidos(responsables, vencimientos, publicador);
    }

    @Override
    public Pedido crear(Solicitante solicitante, Comando comando) {
        ActorVigente creador = apoyo.solicitante(solicitante);
        if (comando.tipoFlujo() == null) {
            throw new DatoInvalido("tipoFlujo", "Elige el tipo de flujo.");
        }
        CodigoPais pais = comando.pais() == null ? CodigoPais.EC : comando.pais();
        if (pais != CodigoPais.EC) {
            // ADR-0009: en v1 solo Ecuador está activo.
            throw new DatoInvalido("pais", "Por ahora solo se crean pedidos de Ecuador.");
        }
        ContratoDeFlujo flujo = definicionVigente(pais, comando);
        ContratoDeEtapa inicial = flujo.etapaInicial();
        if (inicial.rolResponsable() == null) {
            throw new IllegalStateException("La primera etapa de un flujo debe tener responsable.");
        }
        // El permiso se revisa antes de reservar el número; Pedido.crear lo vuelve a revisar.
        PermisosEtapa.paraCrear(inicial.rolResponsable(), creador);

        Instant ahora = reloj.ahora();
        NumeroPedido numero = generador.siguiente(pais, reloj.ahoraEnEcuador().getYear());
        FlujoDelPedido flujoDelPedido = new FlujoDelPedido(flujo.id(), flujo.tipo(),
                flujo.etapas().stream().map(ContratoDeEtapa::numero).toList(), inicial.rolResponsable(),
                inicial.plazo());
        Pedido pedido = Pedido.crear(numero, pais, flujoDelPedido, comando.nombre(), comando.descripcion(),
                comando.incluyeProductosNuevos(), creador, ahora, apoyo.vencimientos());
        pedidos.insertar(pedido);
        apoyo.publicarEventos(pedido);
        return pedido;
    }

    /**
     * Flujos A (V2) y B (V17) de Ecuador. Crea quien tiene el rol de la primera etapa de esa definición: el admin el A
     * y Compras China el B; si el admin también crea el B sigue pendiente del cliente (P-14, ⏸), así que hoy no lo
     * hace. Sin definición vigente la respuesta es 400, no 404.
     */
    private ContratoDeFlujo definicionVigente(CodigoPais pais, Comando comando) {
        try {
            return flujos.vigente(pais, comando.tipoFlujo());
        } catch (NoEncontrado sinDefinicion) {
            throw new DatoInvalido("tipoFlujo", "Ese tipo de flujo todavía no está disponible.");
        }
    }
}
