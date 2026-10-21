package com.decokasa.seguimiento.plazos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import com.decokasa.seguimiento.plazos.domain.AvisoVencimiento;
import java.time.Instant;
import java.util.UUID;

/** Registro de avisos de vencimiento (tabla aviso_vencimiento de V15, solo inserción). */
public interface AvisosVencimientoRepositorio {

    /**
     * Si ya hay un aviso para ese plazo: (entrada, vencimiento) en {@code ETAPA}; (activación, vencimiento) en
     * {@code URGENCIA}.
     */
    boolean existe(ClaseVencimiento clase, UUID referenciaId, Instant venceEn);

    /**
     * Inserta el aviso en la transacción actual, después del evento del historial al que apunta. La base rechaza un
     * segundo aviso del mismo plazo (índices únicos de V15).
     */
    void registrar(AvisoVencimiento aviso);
}
