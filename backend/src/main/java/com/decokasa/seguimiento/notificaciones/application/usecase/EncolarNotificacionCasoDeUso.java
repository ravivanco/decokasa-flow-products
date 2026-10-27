package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada;
import com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoDePedido;
import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;
import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto;
import com.decokasa.seguimiento.compartido.domain.eventos.PlazoVencido;
import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.Map;
import java.util.Set;

/**
 * Inserción atómica, sin Google ni política de envío/reintentos. Los otros eventos no disparan avisos por deducción.
 * Paso 5.2: cada PLAZO_VENCIDO (ETAPA o URGENCIA) avisa al admin una vez, en la transacción que lo detecta; si no hay
 * destinatario configurado, queda la fila con destinatario nulo (bloqueo explícito: no se pierde ni se inventa
 * un receptor). El motivo del retraso no genera correo (F5-D3).
 */
public final class EncolarNotificacionCasoDeUso implements EncolarNotificacion {
    private final NotificacionRepositorio repositorio;
    private final DirectorioDestinatarios directorio;
    private final boolean transiciones;
    public EncolarNotificacionCasoDeUso(NotificacionRepositorio repositorio, DirectorioDestinatarios directorio) {
        this(repositorio, directorio, true);
    }
    /**
     * @param transiciones false desde ADR-0032: los cambios de etapa los avisa RegistrarAvisosDeEtapaCasoDeUso
     *                     (AVISO_ETAPA); aquí solo quedan los vencimientos, sin duplicar correos.
     */
    public EncolarNotificacionCasoDeUso(NotificacionRepositorio repositorio, DirectorioDestinatarios directorio,
            boolean transiciones) {
        this.repositorio = repositorio; this.directorio = directorio; this.transiciones = transiciones;
    }
    @Override public void encolar(EventoPedidoRegistrado registrado) {
        EventoDePedido evento = registrado.evento();
        if (evento instanceof PlazoVencido vencido) {
            encolarVencimiento(registrado, vencido);
            return;
        }
        Integer destino = switch (evento) {
            case EtapaEnviada e -> e.etapaDestino();
            case EtapaAprobada e -> e.etapaDestino();
            case EtapaRechazada e -> e.etapaDestino();
            case PedidoDevuelto e -> e.etapaDestino();
            default -> null;
        };
        if (destino == null || !transiciones) { return; }
        Map<String,String> datos = Map.of("pedido", evento.numeroPedido(), "pais", evento.pais().name(),
                "etapaOrigen", Integer.toString(evento.etapa()), "etapaDestino", Integer.toString(destino),
                "tipoEvento", evento.getClass().getSimpleName(), "ocurridoEn", evento.ocurridoEn().toString());
        insertar(registrado, datos, "TRANSICION", "Cambio de etapa de un pedido");
    }
    private void encolarVencimiento(EventoPedidoRegistrado registrado, PlazoVencido vencido) {
        Map<String,String> datos = Map.of("pedido", vencido.numeroPedido(), "pais", vencido.pais().name(),
                "etapa", Integer.toString(vencido.etapa()), "claseVencimiento", vencido.clase().name(),
                "venceEn", vencido.venceEn().toString(), "tipoEvento", vencido.getClass().getSimpleName(),
                "ocurridoEn", vencido.ocurridoEn().toString());
        String asunto = vencido.clase() == ClaseVencimiento.URGENCIA
                ? "Plazo de urgencia vencido de un pedido" : "Plazo vencido de una etapa de un pedido";
        insertar(registrado, datos, "VENCIMIENTO", asunto);
    }
    private void insertar(EventoPedidoRegistrado registrado, Map<String,String> datos, String plantilla,
            String asunto) {
        EventoDePedido evento = registrado.evento();
        Set<String> receptores = directorio.administradores(evento.pais());
        if (receptores.isEmpty()) {
            repositorio.insertarSiAusente(Notificacion.pendiente(evento.pedidoId(), registrado.eventoId(), null,
                    datos, evento.ocurridoEn(), plantilla, asunto));
        } else {
            receptores.stream().map(Notificacion::normalizarCorreo).distinct().forEach(correo ->
                    repositorio.insertarSiAusente(Notificacion.pendiente(evento.pedidoId(), registrado.eventoId(),
                            correo, datos, evento.ocurridoEn(), plantilla, asunto)));
        }
    }
}
