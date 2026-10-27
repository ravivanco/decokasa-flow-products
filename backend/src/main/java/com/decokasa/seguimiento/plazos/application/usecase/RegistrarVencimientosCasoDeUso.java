package com.decokasa.seguimiento.plazos.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import com.decokasa.seguimiento.compartido.domain.eventos.PlazoVencido;
import com.decokasa.seguimiento.plazos.application.port.in.RegistrarVencimientosDePedido;
import com.decokasa.seguimiento.plazos.application.port.out.AvisosVencimientoRepositorio;
import com.decokasa.seguimiento.plazos.application.port.out.PedidosConEtapaActiva;
import com.decokasa.seguimiento.plazos.domain.AvisoVencimiento;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Registra los vencimientos de un pedido (paso 5.2; reglas aprobadas provisionalmente por Richard el 2026-10-20). Corre
 * dentro de una transacción abierta en la configuración: con la fila del pedido bloqueada vuelve a comprobar que el
 * plazo siga vencido en la misma entrada o con la misma urgencia, publica PLAZO_VENCIDO (el historial y el outbox lo
 * insertan de forma síncrona en esa transacción) y registra el aviso único. Los dos plazos (ETAPA y URGENCIA) se
 * tratan por separado. Un plazo pendiente del cliente no tiene vencimiento y no genera nada. Sin Spring (R2).
 */
public final class RegistrarVencimientosCasoDeUso implements RegistrarVencimientosDePedido {

    private final PedidosConEtapaActiva pedidos;
    private final AvisosVencimientoRepositorio avisos;
    private final PublicadorEventos publicador;

    public RegistrarVencimientosCasoDeUso(PedidosConEtapaActiva pedidos, AvisosVencimientoRepositorio avisos,
            PublicadorEventos publicador) {
        this.pedidos = pedidos;
        this.avisos = avisos;
        this.publicador = publicador;
    }

    @Override
    public int registrar(UUID pedidoId, Instant ahora) {
        Optional<PedidosConEtapaActiva.EstadoVigente> leido = pedidos.bloquear(pedidoId);
        if (leido.isEmpty() || leido.get().finalizado()) {
            return 0;
        }
        PedidosConEtapaActiva.EstadoVigente estado = leido.get();
        int registrados = 0;
        if (vencido(estado.entradaId(), estado.venceEn(), ahora)
                && !avisos.existe(ClaseVencimiento.ETAPA, estado.entradaId(), estado.venceEn())) {
            avisar(estado, ClaseVencimiento.ETAPA, estado.venceEn(), estado.entradaId(), null, ahora);
            registrados++;
        }
        if (vencido(estado.urgenciaActivacionId(), estado.urgenciaVenceEn(), ahora)
                && !avisos.existe(ClaseVencimiento.URGENCIA, estado.urgenciaActivacionId(),
                        estado.urgenciaVenceEn())) {
            avisar(estado, ClaseVencimiento.URGENCIA, estado.urgenciaVenceEn(), null, estado.urgenciaActivacionId(),
                    ahora);
            registrados++;
        }
        return registrados;
    }

    private static boolean vencido(UUID referencia, Instant venceEn, Instant ahora) {
        return referencia != null && venceEn != null && ahora.isAfter(venceEn);
    }

    private void avisar(PedidosConEtapaActiva.EstadoVigente estado, ClaseVencimiento clase, Instant venceEn,
            UUID entradaId, UUID urgenciaActivacionId, Instant ahora) {
        UUID eventoId = UUID.randomUUID();
        // Primero el evento (historial y outbox en esta transacción); el aviso apunta a él.
        publicador.publicar(new PlazoVencido(estado.pedido(), estado.etapaActual(), clase, venceEn, entradaId,
                urgenciaActivacionId, ahora, eventoId));
        avisos.registrar(new AvisoVencimiento(UUID.randomUUID(), estado.pedido().pedidoId(), clase,
                estado.etapaActual(), entradaId, urgenciaActivacionId, venceEn, eventoId, ahora));
    }
}
