package com.decokasa.seguimiento.compartido.domain;

/**
 * Qué plazo venció (fase 5, paso 5.2; aprobado provisionalmente por Richard el 2026-10-20): el plazo de la entrada
 * actual de una etapa o el vencimiento propio de una urgencia (requerimientos 4.2 #11). Son avisos distintos: si los
 * dos vencen, hay dos avisos, uno por cada plazo.
 */
public enum ClaseVencimiento {
    ETAPA,
    URGENCIA
}
