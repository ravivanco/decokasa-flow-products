package com.decokasa.seguimiento.plazos.domain;

/**
 * De dónde sale la meta de días vigente (columna configuracion_plazos.meta_origen).
 *
 * <ul>
 *   <li>{@code PROPUESTA}: el valor de la siembra, 90 días. Es la propuesta documentada mientras el cliente responde
 *       ("se ajusta", requerimientos §5); no tiene aprobación del cliente ni del equipo.</li>
 *   <li>{@code ADMIN}: el admin la cambió desde la web. Es la meta configurada; la respuesta del cliente sobre su
 *       valor sigue pendiente igual (⏸).</li>
 * </ul>
 */
public enum OrigenMeta {
    PROPUESTA,
    ADMIN
}
