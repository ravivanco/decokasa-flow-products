package com.decokasa.seguimiento.plazos.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuración de plazos (tabla configuracion_plazos de V14, una sola fila): la meta de días de pedido a llegada
 * (PRD, métrica "días de pedido a llegada"). La meta es configurable; su valor sigue pendiente del cliente (⏸ "se
 * ajusta") y la siembra es solo la propuesta de 90 días ({@link OrigenMeta#PROPUESTA}). Los feriados no forman parte
 * de esta configuración mientras el cliente no los apruebe (⏸; modelo-datos 3.7). Java puro (R1).
 *
 * @param actualizadoEn  nulo mientras rige la siembra
 * @param actualizadoPor nulo mientras rige la siembra
 */
public record ConfiguracionPlazos(int metaDias, OrigenMeta origenMeta, Instant actualizadoEn, UUID actualizadoPor,
        long version) {

    public ConfiguracionPlazos {
        if (metaDias < 1) {
            throw new DatoInvalido("metaDias", "La meta debe ser de al menos 1 día.");
        }
        Objects.requireNonNull(origenMeta, "origenMeta");
    }

    /** La meta que fija el admin. La versión la incrementa la persistencia. */
    public ConfiguracionPlazos conMeta(int dias, UUID admin, Instant ahora) {
        return new ConfiguracionPlazos(dias, OrigenMeta.ADMIN, Objects.requireNonNull(ahora, "ahora"),
                Objects.requireNonNull(admin, "admin"), version);
    }
}
