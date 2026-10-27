package com.decokasa.seguimiento.pedidos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido;
import com.decokasa.seguimiento.pedidos.domain.EtapaDePedido;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import com.decokasa.seguimiento.pedidos.domain.UrgenciaActiva;
import com.decokasa.seguimiento.plazos.application.port.out.PedidosConEtapaActiva;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador de pedidos para el puerto de salida {@link PedidosConEtapaActiva} de plazos (arquitectura.md 3.1: así la
 * dependencia va de pedidos hacia plazos y no hay ciclo, R9). Paso 5.2.
 *
 * <p>Los candidatos se leen sin bloquear: pedidos en curso con la etapa actual o la urgencia vencidas y sin aviso
 * registrado para ese plazo exacto. El caso de uso vuelve a comprobarlo todo con la fila del pedido bloqueada.
 */
@Component
public class PedidosConEtapaActivaJpa implements PedidosConEtapaActiva {

    // Un plazo pendiente del cliente no tiene vence_en: nunca es candidato. Primero el vencimiento más antiguo, para
    // que un pedido que falla una y otra vez no tape indefinidamente a los demás.
    private static final String CANDIDATOS = """
            select cast(p.id as text) from pedido p
            where not p.finalizado and (
                exists (select 1 from etapa_pedido e
                        where e.pedido_id = p.id and e.estado = 'ACTUAL' and e.entrada_id is not null
                          and e.vence_en is not null and e.vence_en < :ahora
                          and not exists (select 1 from aviso_vencimiento a
                                          where a.clase = 'ETAPA' and a.entrada_id = e.entrada_id
                                            and a.vence_en = e.vence_en))
                or (p.urgencia_vence_en is not null and p.urgencia_vence_en < :ahora
                    and not exists (select 1 from aviso_vencimiento a
                                    where a.clase = 'URGENCIA' and a.urgencia_activacion_id = p.urgencia_activacion_id
                                      and a.vence_en = p.urgencia_vence_en)))
            order by least(
                (select e.vence_en from etapa_pedido e where e.pedido_id = p.id and e.estado = 'ACTUAL'),
                p.urgencia_vence_en) nulls last, p.id
            limit :limite
            """;

    private final EntityManager entityManager;
    private final PedidoJpaRepositorio pedidos;

    public PedidosConEtapaActivaJpa(EntityManager entityManager, PedidoJpaRepositorio pedidos) {
        this.entityManager = entityManager;
        this.pedidos = pedidos;
    }

    @Override
    public List<UUID> candidatos(Instant ahora, int limite) {
        @SuppressWarnings("unchecked")
        List<Object> filas = entityManager.createNativeQuery(CANDIDATOS)
                .setParameter("ahora", ahora)
                .setParameter("limite", limite)
                .getResultList();
        return filas.stream().map(fila -> UUID.fromString((String) fila)).toList();
    }

    /** Mismo bloqueo que las acciones (P-11): primero la fila del pedido, después la relectura. */
    @Override
    public Optional<EstadoVigente> bloquear(UUID pedidoId) {
        return pedidos.buscarParaActualizar(pedidoId).map(PedidosConEtapaActivaJpa::estado);
    }

    private static EstadoVigente estado(Pedido pedido) {
        EtapaDePedido actual = pedido.etapas().stream()
                .filter(etapa -> etapa.numero() == pedido.etapaActual())
                .findFirst()
                .orElseThrow();
        UrgenciaActiva urgencia = pedido.urgencia().orElse(null);
        return new EstadoVigente(
                new DatosDelPedido(pedido.id(), pedido.numero().valor(), pedido.pais(), pedido.tipoFlujo()),
                pedido.finalizado(), pedido.etapaActual(), actual.entradaId(), actual.venceEn(),
                urgencia == null ? null : pedido.urgenciaActivacionId().orElse(null),
                urgencia == null ? null : urgencia.venceEn());
    }
}
