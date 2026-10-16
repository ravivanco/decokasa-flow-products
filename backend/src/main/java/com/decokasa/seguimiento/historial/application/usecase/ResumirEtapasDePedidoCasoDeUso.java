package com.decokasa.seguimiento.historial.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.domain.EventoPedido;
import com.decokasa.seguimiento.usuarios.application.port.in.NombrarRoles;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Busca, para cada etapa, el último evento que cerró una entrada suya. El historial es de solo inserción y se lee en
 * orden de inserción, así que el último de cada etapa es el de su entrada cerrada más reciente. No se crean ni se
 * reparten eventos: si una etapa no tiene uno propio, no tiene resumen. Sin Spring (R2).
 */
public final class ResumirEtapasDePedidoCasoDeUso implements ResumirEtapasDePedido {

    private final HistorialRepositorio historial;
    private final ResumirUsuarios usuarios;
    private final NombrarRoles roles;

    public ResumirEtapasDePedidoCasoDeUso(HistorialRepositorio historial, ResumirUsuarios usuarios,
            NombrarRoles roles) {
        this.historial = historial;
        this.usuarios = usuarios;
        this.roles = roles;
    }

    @Override
    public Map<Integer, Cierre> ultimosCierres(UUID pedidoId) {
        Map<Integer, EventoPedido> ultimos = new LinkedHashMap<>();
        for (EventoPedido evento : historial.listarPorPedido(pedidoId)) {
            if (tipoDeCierre(evento) != null) {
                ultimos.put(evento.etapa(), evento);
            }
        }
        if (ultimos.isEmpty()) {
            return Map.of();
        }
        Map<UUID, ResumirUsuarios.Resumen> personas = usuarios.resumenes(ultimos.values().stream()
                .map(EventoPedido::usuarioId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<CodigoRol, String> nombresDeRol = roles.nombres(ultimos.values().stream()
                .map(EventoPedido::rolCodigo)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Integer, Cierre> cierres = new LinkedHashMap<>();
        ultimos.forEach((etapa, evento) -> {
            ResumirUsuarios.Resumen persona = evento.usuarioId() == null ? null : personas.get(evento.usuarioId());
            cierres.put(etapa, new Cierre(etapa, tipoDeCierre(evento), evento.usuarioId(),
                    persona == null ? null : persona.nombre(), evento.rolCodigo(),
                    evento.rolCodigo() == null ? null : nombresDeRol.get(evento.rolCodigo()), evento.ocurridoEn(),
                    evento.detalle().etapaDestino(), evento.detalle().motivoDevolucion(), evento.observacion(),
                    evento.adjuntoIds().size()));
        });
        return Map.copyOf(cierres);
    }

    /** Solo las acciones que sacan el pedido de la etapa; observaciones, archivos y reasignaciones no la cierran. */
    private static TipoCierre tipoDeCierre(EventoPedido evento) {
        return switch (evento.tipo()) {
            case ETAPA_ENVIADA -> TipoCierre.ENVIADA;
            case ETAPA_APROBADA -> TipoCierre.APROBADA;
            case ETAPA_RECHAZADA -> TipoCierre.RECHAZADA;
            case PEDIDO_DEVUELTO -> TipoCierre.DEVUELTA;
            default -> null;
        };
    }
}
