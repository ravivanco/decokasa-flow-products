package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cronograma del pedido contra la meta (contrato: GET /pedidos/{pedidoId}/cronograma, esquema Cronograma 0.11.0-borrador;
 * F5-D1 opción A). Vive en pedidos porque la visibilidad y el 404 son del pedido (P-13); el cálculo es de plazos.
 * Las fechas son días de Ecuador en formato AAAA-MM-DD.
 */
@RestController
public class CronogramaController {

    public record CronogramaRespuesta(UUID pedidoId, String fechaPedido, Meta meta, Llegada llegada, Integer totalDias,
            Integer diferenciaContraMeta, boolean calculable, List<Faltante> faltantes, List<Etapa> etapas) {
    }

    /** {@code pendienteDelCliente}: el valor de la meta sigue sin respuesta del cliente. */
    public record Meta(int dias, String origen, boolean pendienteDelCliente) {
    }

    public record Llegada(String fecha, String procedencia) {
    }

    public record Faltante(int numero, String nombre) {
    }

    public record Etapa(int numero, String nombre, String fechaPlanificada, boolean movidaAlLunes,
            String fechaEstimada, String fechaReal, String estado, String procedencia, boolean plazoPendiente) {
    }

    private final ConsultarCronograma consultar;

    public CronogramaController(ConsultarCronograma consultar) {
        this.consultar = consultar;
    }

    @GetMapping("/api/v1/pedidos/{pedidoId}/cronograma")
    public CronogramaRespuesta cronograma(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return de(consultar.cronograma(SolicitantesWeb.de(token), pedidoId));
    }

    private static CronogramaRespuesta de(CalcularCronograma.Resultado r) {
        CalcularCronograma.Llegada llegada = r.llegada();
        return new CronogramaRespuesta(r.pedidoId(), texto(r.fechaPedido()),
                new Meta(r.metaDias(), r.origenMeta(), true),
                llegada == null ? null : new Llegada(texto(llegada.fecha()), llegada.procedencia()),
                r.totalDias(), r.diferenciaContraMeta(), r.calculable(),
                r.faltantes().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList(),
                r.etapas().stream()
                        .map(e -> new Etapa(e.numero(), e.nombre(), texto(e.fechaPlanificada()), e.movidaAlLunes(),
                                texto(e.fechaEstimada()), texto(e.fechaReal()), e.estado(), e.procedencia(),
                                e.plazoPendiente()))
                        .toList());
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
