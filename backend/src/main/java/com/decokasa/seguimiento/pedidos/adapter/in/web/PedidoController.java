package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarPedidos;
import com.decokasa.seguimiento.pedidos.application.port.in.CrearPedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Crear, listar, buscar y ver pedidos (contrato 0.7.0). Los permisos los deciden los casos de uso con el estado
 * vigente (R6): crear según la primera etapa (P-06) y consultar según P-13 (provisional del equipo). Los límites de
 * los parámetros los valida Spring MVC sin proxy (400 VALIDACION).
 */
@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final CrearPedido crear;
    private final ConsultarPedidos consultar;

    public PedidoController(CrearPedido crear, ConsultarPedidos consultar) {
        this.crear = crear;
        this.consultar = consultar;
    }

    /** El número se lee después del commit de la creación (lectura A): la respuesta sale de una consulta nueva. */
    @PostMapping
    public ResponseEntity<PedidoRespuestas.Detalle> crear(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody SolicitudCrearPedido solicitud) {
        Solicitante solicitante = SolicitantesWeb.de(token);
        Pedido pedido = crear.crear(solicitante, new CrearPedido.Comando(solicitud.tipoFlujo(), solicitud.pais(),
                solicitud.nombre(), solicitud.descripcion(), Boolean.TRUE.equals(solicitud.incluyeProductosNuevos())));
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + pedido.id()))
                .body(PedidoRespuestas.de(consultar.detalle(solicitante, pedido.id())));
    }

    @GetMapping
    public PedidoRespuestas.Pagina listar(@AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) TipoFlujo tipoFlujo,
            @RequestParam(required = false) @Min(1) Integer etapa,
            @RequestParam(required = false) Boolean vencido,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PedidoRespuestas.de(consultar.listar(SolicitantesWeb.de(token),
                new ConsultarPedidos.Filtro(tipoFlujo, etapa, vencido, texto), pagina, tamano));
    }

    @GetMapping("/buscar")
    public PedidoRespuestas.Detalle buscar(@AuthenticationPrincipal Jwt token, @RequestParam String numero) {
        return PedidoRespuestas.de(consultar.porNumero(SolicitantesWeb.de(token), numero));
    }

    @GetMapping("/bandeja")
    public PedidoRespuestas.Pagina bandeja(@AuthenticationPrincipal Jwt token,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return PedidoRespuestas.de(consultar.bandeja(SolicitantesWeb.de(token), pagina, tamano));
    }

    @GetMapping("/{pedidoId}")
    public PedidoRespuestas.Detalle detalle(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return PedidoRespuestas.de(consultar.detalle(SolicitantesWeb.de(token), pedidoId));
    }
}
