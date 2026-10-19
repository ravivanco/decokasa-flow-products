package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.Map;
import java.util.Set;

/** Inserción atómica, sin Google ni política de envío/reintentos. Los otros eventos no disparan avisos por deducción. */
public final class EncolarNotificacionCasoDeUso implements EncolarNotificacion {
    private final NotificacionRepositorio repositorio;
    private final DirectorioDestinatarios directorio;
    public EncolarNotificacionCasoDeUso(NotificacionRepositorio repositorio, DirectorioDestinatarios directorio) {
        this.repositorio = repositorio; this.directorio = directorio;
    }
    @Override public void encolar(EventoPedidoRegistrado registrado) {
        EventoDePedido evento = registrado.evento();
        Integer destino = switch (evento) {
            case EtapaEnviada e -> e.etapaDestino();
            case EtapaAprobada e -> e.etapaDestino();
            case EtapaRechazada e -> e.etapaDestino();
            case PedidoDevuelto e -> e.etapaDestino();
            default -> null;
        };
        if (destino == null) { return; }
        Map<String,String> datos = Map.of("pedido", evento.numeroPedido(), "pais", evento.pais().name(),
                "etapaOrigen", Integer.toString(evento.etapa()), "etapaDestino", Integer.toString(destino),
                "tipoEvento", evento.getClass().getSimpleName(), "ocurridoEn", evento.ocurridoEn().toString());
        Set<String> receptores = directorio.administradores(evento.pais());
        if (receptores.isEmpty()) {
            repositorio.insertarSiAusente(Notificacion.pendiente(evento.pedidoId(), registrado.eventoId(), null,
                    datos, evento.ocurridoEn()));
        } else {
            receptores.stream().map(Notificacion::normalizarCorreo).distinct().forEach(correo ->
                    repositorio.insertarSiAusente(Notificacion.pendiente(evento.pedidoId(), registrado.eventoId(),
                            correo, datos, evento.ocurridoEn())));
        }
    }
}
