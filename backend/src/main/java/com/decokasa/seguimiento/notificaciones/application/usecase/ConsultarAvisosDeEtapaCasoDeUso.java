package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarAvisosDeEtapa;
import com.decokasa.seguimiento.notificaciones.application.port.out.AvisoRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Avisos de etapa guardados (ADR-0032) de la persona, filtrados por los países que tiene hoy (ADR-0024). La campana y
 * Mi bandeja usan esta misma consulta. Sin Spring (R2).
 */
public final class ConsultarAvisosDeEtapaCasoDeUso implements ConsultarAvisosDeEtapa {

    static final Duration VENTANA = Duration.ofDays(30);
    static final int LIMITE = 50;

    private final AvisoRepositorio avisos;
    private final ConsultarResponsables responsables;
    private final Reloj reloj;

    public ConsultarAvisosDeEtapaCasoDeUso(AvisoRepositorio avisos, ConsultarResponsables responsables, Reloj reloj) {
        this.avisos = avisos;
        this.responsables = responsables;
        this.reloj = reloj;
    }

    @Override
    public Avisos avisos(Solicitante solicitante) {
        ConsultarResponsables.EstadoVigente actor = vigente(solicitante);
        Instant leidasHasta = avisos.leidasHasta(actor.usuarioId());
        Set<CodigoPais> paises = actor.paises().isEmpty() ? Set.of() : EnumSet.copyOf(actor.paises());
        if (paises.isEmpty()) {
            return new Avisos(List.of(), 0, leidasHasta);
        }
        Instant desde = reloj.ahora().minus(VENTANA);
        List<Aviso> lista = avisos.recientes(actor.usuarioId(), paises, desde, LIMITE).stream()
                .map(a -> new Aviso(a, !a.ocurridoEn().isAfter(leidasHasta)))
                .toList();
        Instant despuesDe = leidasHasta.isAfter(desde) ? leidasHasta : desde;
        return new Avisos(lista, avisos.contarDespuesDe(actor.usuarioId(), paises, despuesDe), leidasHasta);
    }

    @Override
    public Instant marcarLeidos(Solicitante solicitante) {
        ConsultarResponsables.EstadoVigente actor = vigente(solicitante);
        Instant ahora = reloj.ahora();
        avisos.marcarLeidas(actor.usuarioId(), ahora);
        return ahora;
    }

    private ConsultarResponsables.EstadoVigente vigente(Solicitante solicitante) {
        return responsables.estado(solicitante.usuarioId()).filter(e -> e.activo() && !e.roles().isEmpty())
                .orElseThrow(() -> new SinPermiso("Tu cuenta no puede consultar avisos."));
    }
}
