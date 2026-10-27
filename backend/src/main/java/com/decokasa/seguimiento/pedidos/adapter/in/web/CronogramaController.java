package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarCronograma;
import com.decokasa.seguimiento.plazos.application.port.in.CalcularCronograma;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cronograma del pedido: planificación original, estimación actual y fechas reales, frente a la meta (contrato:
 * GET /pedidos/{pedidoId}/cronograma, esquema Cronograma 0.25.0-borrador; ADR-0030). Vive en pedidos porque la
 * visibilidad y el 404 son del pedido (P-13, ADR-0024); el cálculo es de plazos. Las fechas son días del país del
 * pedido en formato AAAA-MM-DD; los instantes, ISO 8601 en UTC.
 */
@RestController
public class CronogramaController {

    public record CronogramaRespuesta(UUID pedidoId, String fechaPedido, String calculadoEn, String reglas, Meta meta,
            Llegada llegada, Original original, Integer diferenciaContraOriginal, String finalizacionEstimada,
            Integer totalDias, Integer diferenciaContraMeta, boolean calculable, List<Faltante> faltantes,
            List<Faltante> faltantesFinalizacion, List<Etapa> etapas) {
    }

    /** {@code pendienteDelCliente}: el valor de la meta sigue sin respuesta del cliente. */
    public record Meta(int dias, String origen, boolean pendienteDelCliente) {
    }

    public record Llegada(String fecha, String procedencia) {
    }

    public record Original(String origen, String reglas, String calculadaEn, String llegada, String finalizacion) {
    }

    public record Faltante(int numero, String nombre) {
    }

    public record Duracion(String base, Integer cantidad, String tipo, String fuente) {
    }

    public record Etapa(int numero, String nombre, String fechaOriginal, String fechaEstimada, String fechaReal,
            boolean movidaAlLunes, String estado, String procedencia, boolean plazoPendiente, Duracion duracion) {
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
        CalcularCronograma.Original original = r.original();
        return new CronogramaRespuesta(r.pedidoId(), texto(r.fechaPedido()), texto(r.calculadoEn()), r.reglas(),
                new Meta(r.metaDias(), r.origenMeta(), true),
                llegada == null ? null : new Llegada(texto(llegada.fecha()), llegada.procedencia()),
                new Original(original.origen(), original.reglas(), texto(original.calculadaEn()),
                        texto(original.llegada()), texto(original.finalizacion())),
                r.diferenciaContraOriginal(), texto(r.finalizacionEstimada()), r.totalDias(),
                r.diferenciaContraMeta(), r.calculable(),
                r.faltantes().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList(),
                r.faltantesFinalizacion().stream().map(f -> new Faltante(f.numero(), f.nombre())).toList(),
                r.etapas().stream()
                        .map(e -> new Etapa(e.numero(), e.nombre(), texto(e.fechaOriginal()), texto(e.fechaEstimada()),
                                texto(e.fechaReal()), e.movidaAlLunes(), e.estado(), e.procedencia(),
                                e.plazoPendiente(), new Duracion(e.duracion().base(), e.duracion().cantidad(),
                                        e.duracion().tipo(), e.duracion().fuente())))
                        .toList());
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }

    private static String texto(Instant instante) {
        return instante == null ? null : instante.toString();
    }
}
