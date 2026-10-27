package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Reglas y datos de pedidos que usa el asistente, por los puertos de entrada de pedidos (R4). */
public interface PedidosParaAsistente {

    /**
     * Datos del pedido si la persona puede verlo hoy (P-13 y país asignado, ADR-0024).
     *
     * @throws com.decokasa.seguimiento.compartido.domain.NoEncontrado si no existe o es de otro país
     * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si la cuenta está inactiva o sin roles
     */
    DatosPedido leer(Solicitante solicitante, UUID pedidoId);

    /** P-17: la persona puede descargar hoy los archivos de esa etapa del pedido. */
    boolean puedeDescargarEtapa(Solicitante solicitante, UUID pedidoId, int etapa);

    /**
     * Lo que se envía al modelo como datos del pedido. Sin nombres de personas ni observaciones: lo mínimo para
     * responder sobre etapas y estado.
     */
    record DatosPedido(UUID id, String numero, CodigoPais pais, String flujo, int etapaActual,
            String nombreEtapaActual, boolean finalizado, String descripcion, List<EtapaPedido> etapas) {

        public DatosPedido {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(numero, "numero");
            Objects.requireNonNull(pais, "pais");
            etapas = List.copyOf(etapas);
        }

        /** Sin descripción: es texto libre del pedido. */
        @Override
        public String toString() {
            return "DatosPedido[id=" + id + ", etapas=" + etapas.size() + "]";
        }
    }

    record EtapaPedido(int numero, String nombre, String estado, Instant entrada, Instant salida, Instant venceEn) {
    }
}
