package com.decokasa.seguimiento.pedidos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.pedidos.application.port.in.ConsultarEstadisticas;
import com.decokasa.seguimiento.pedidos.application.port.out.EstadisticasDePedidos;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ADR-0029. Alcance: cuenta activa con roles (si no, 403) y países asignados hoy, intersectados con el país pedido (un
 * país no asignado es 403). Período: [desde, hasta] o el año elegido, en la zona de Ecuador. Un mes futuro va nulo
 * (no ha ocurrido), nunca cero. Duraciones en segundos con las fechas del servidor. Sin Spring (R2).
 */
public final class ConsultarEstadisticasCasoDeUso implements ConsultarEstadisticas {
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final CodigoRol ADMIN = new CodigoRol("admin");
    private static final Set<String> FLUJOS = Set.of("A", "B");

    private final EstadisticasDePedidos datos;
    private final ConsultarResponsables responsables;
    private final Reloj reloj;

    public ConsultarEstadisticasCasoDeUso(EstadisticasDePedidos datos, ConsultarResponsables responsables, Reloj reloj) {
        this.datos = datos;
        this.responsables = responsables;
        this.reloj = reloj;
    }

    @Override public Estadisticas estadisticas(Solicitante solicitante, Filtro filtro) {
        var actor = vigente(solicitante);
        Filtro f = filtro == null ? new Filtro(null, null, null, null, null) : filtro;
        Set<CodigoPais> paises = paises(actor.paises(), f.pais());
        String flujo = flujo(f.tipoFlujo());
        Instant ahora = reloj.ahora();
        LocalDate hoy = ahora.atZone(ZONA).toLocalDate();
        int anio = f.anio() != null ? f.anio() : hoy.getYear();
        if (anio < 2000 || anio > hoy.getYear() + 1) { throw new DatoInvalido("anio", "Elige un año válido."); }
        LocalDate desde = f.desde() != null ? f.desde() : LocalDate.of(anio, 1, 1);
        LocalDate hasta = f.hasta() != null ? f.hasta() : LocalDate.of(anio, 12, 31);
        if (hasta.isBefore(desde)) { throw new DatoInvalido("hasta", "La fecha final no puede ser anterior a la inicial."); }
        if (desde.plusYears(10).isBefore(hasta)) { throw new DatoInvalido("hasta", "El intervalo máximo es de 10 años."); }
        Instant inicio = desde.atStartOfDay(ZONA).toInstant();
        Instant fin = hasta.plusDays(1).atStartOfDay(ZONA).toInstant();
        if (paises.isEmpty()) {
            return new Estadisticas(new Periodo(anio, desde, hasta), List.of(),
                    new Tarjetas(0, 0, 0, 0, null, 0), List.of(), meses(Map.of(), anio, hoy), List.of(), List.of(), 0,
                    new EstadoPedidos(0, 0, 0, 0, 0), List.of(), ahora);
        }
        var tiempos = datos.tiempoPorPedido(paises, flujo, inicio, fin, ahora, LIMITE_PEDIDOS);
        return new Estadisticas(new Periodo(anio, desde, hasta), paises.stream().sorted().toList(),
                datos.tarjetas(paises, flujo, inicio, fin, ahora), anios(datos.creadosPorAnio(paises, flujo), hoy),
                meses(datos.creadosPorMes(paises, flujo, anio), anio, hoy),
                datos.tiempoPorEtapa(paises, flujo, inicio, fin), tiempos.pedidos(), tiempos.total(),
                datos.estado(paises, flujo, inicio, fin, ahora), datos.retrasosPorEtapa(paises, flujo, ahora), ahora);
    }

    @Override public List<Alerta> alertas(Solicitante solicitante, boolean todas) {
        var actor = vigente(solicitante);
        boolean admin = actor.roles().contains(ADMIN);
        if (todas && !admin) { throw new SinPermiso("Solo el admin puede ver todas las alertas."); }
        Set<CodigoPais> paises = actor.paises().isEmpty() ? Set.of() : EnumSet.copyOf(actor.paises());
        if (paises.isEmpty()) { return List.of(); }
        Instant ahora = reloj.ahora();
        List<Alerta> alertas = new ArrayList<>();
        for (var f : datos.alertas(paises, actor.usuarioId(), actor.roles(), todas, ahora)) {
            if (f.urgenciaActiva() && f.urgenciaVence() != null && f.urgenciaVence().isBefore(ahora)) {
                alertas.add(alerta(f, "URGENCIA_VENCIDA", f.urgenciaVence(), null, null));
            }
            if (f.etapaVence() != null && f.etapaVence().isBefore(ahora)) {
                alertas.add(alerta(f, "ETAPA_VENCIDA", f.etapaVence(), null, f.motivoRetraso()));
            }
            if (f.demoraReportadaEn() != null && f.llegadaAduana() == null) {
                alertas.add(alerta(f, "DEMORA_REPORTADA", null, f.demoraNuevaFecha(), f.demoraMotivo()));
            }
        }
        // Lo más urgente primero: urgencias, después etapas (las más antiguas primero) y al final las demoras.
        alertas.sort(Comparator.comparingInt((Alerta a) -> switch (a.tipo()) {
                    case "URGENCIA_VENCIDA" -> 0;
                    case "ETAPA_VENCIDA" -> 1;
                    default -> 2;
                }).thenComparing(Alerta::vence, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Alerta::numero));
        return alertas;
    }

    private static Alerta alerta(EstadisticasDePedidos.FilaAlerta f, String tipo, Instant vence, LocalDate fecha,
            String motivo) {
        return new Alerta(f.pedidoId(), f.numero(), f.nombre(), f.pais(), f.tipoFlujo(), f.etapa(), f.nombreEtapa(),
                tipo, vence, fecha, motivo, f.asignadoA());
    }

    private ConsultarResponsables.EstadoVigente vigente(Solicitante solicitante) {
        return responsables.estado(solicitante.usuarioId()).filter(e -> e.activo() && !e.roles().isEmpty())
                .orElseThrow(() -> new SinPermiso("Tu cuenta no puede consultar pedidos."));
    }

    private static Set<CodigoPais> paises(Set<CodigoPais> asignados, CodigoPais pedido) {
        if (pedido == null) { return asignados.isEmpty() ? Set.of() : EnumSet.copyOf(asignados); }
        if (!asignados.contains(pedido)) { throw new SinPermiso("No tienes ese país asignado."); }
        return EnumSet.of(pedido);
    }

    private static String flujo(String tipo) {
        if (tipo == null || tipo.isBlank()) { return null; }
        if (!FLUJOS.contains(tipo)) { throw new DatoInvalido("tipoFlujo", "El flujo es A o B."); }
        return tipo;
    }

    /** Del primer año con pedidos al actual; los años intermedios sin pedidos son cero. */
    private static List<PorAnio> anios(Map<Integer, Long> porAnio, LocalDate hoy) {
        if (porAnio.isEmpty()) { return List.of(); }
        int primero = porAnio.keySet().stream().min(Integer::compare).orElseThrow();
        int ultimo = Math.max(hoy.getYear(), porAnio.keySet().stream().max(Integer::compare).orElseThrow());
        List<PorAnio> lista = new ArrayList<>();
        for (int a = primero; a <= ultimo; a++) { lista.add(new PorAnio(a, porAnio.getOrDefault(a, 0L))); }
        return lista;
    }

    /** Enero a diciembre: cero si el mes ya empezó y no tuvo pedidos; nulo solo si todavía no empezó. */
    private static List<PorMes> meses(Map<Integer, Long> porMes, int anio, LocalDate hoy) {
        List<PorMes> lista = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            boolean futuro = LocalDate.of(anio, m, 1).isAfter(hoy);
            lista.add(new PorMes(m, futuro ? null : porMes.getOrDefault(m, 0L)));
        }
        return lista;
    }
}
