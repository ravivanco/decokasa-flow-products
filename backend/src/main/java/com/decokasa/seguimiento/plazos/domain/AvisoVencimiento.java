package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Aviso de un plazo vencido (fase 5, paso 5.2; reglas aprobadas provisionalmente por Richard el 2026-10-20). Identifica
 * el plazo de forma única:
 * <ul>
 *   <li>{@code ETAPA}: la entrada de la etapa (P-18) y su vencimiento. Cada reentrada tiene otra entrada, así que puede
 *       volver a avisar; el mismo vencimiento de la misma entrada nunca repite.</li>
 *   <li>{@code URGENCIA}: la activación (la entrada creada por la devolución con urgencia) y su vencimiento. No depende
 *       solo de la hora: dos activaciones nunca comparten entrada.</li>
 * </ul>
 * Si vencen los dos plazos, hay dos avisos, uno por cada uno. Java puro (R1).
 *
 * @param eventoId evento PLAZO_VENCIDO del historial que registra el vencimiento
 */
public record AvisoVencimiento(UUID id, UUID pedidoId, ClaseVencimiento clase, int etapa, UUID entradaId,
        UUID urgenciaActivacionId, Instant venceEn, UUID eventoId, Instant registradoEn) {

    public AvisoVencimiento {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(pedidoId, "pedidoId");
        Objects.requireNonNull(clase, "clase");
        Objects.requireNonNull(venceEn, "venceEn");
        Objects.requireNonNull(eventoId, "eventoId");
        Objects.requireNonNull(registradoEn, "registradoEn");
        boolean coherente = clase == ClaseVencimiento.ETAPA ? entradaId != null && urgenciaActivacionId == null
                : urgenciaActivacionId != null && entradaId == null;
        if (!coherente) {
            throw new IllegalArgumentException("Un aviso se identifica por su entrada o por su urgencia.");
        }
    }

    /** La entrada ({@code ETAPA}) o la activación ({@code URGENCIA}) que, con el vencimiento, identifica el aviso. */
    public UUID referenciaId() {
        return clase == ClaseVencimiento.ETAPA ? entradaId : urgenciaActivacionId;
    }
}
