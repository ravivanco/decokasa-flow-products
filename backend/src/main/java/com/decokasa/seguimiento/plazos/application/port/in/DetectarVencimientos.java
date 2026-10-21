package com.decokasa.seguimiento.plazos.application.port.in;

/**
 * Una pasada de detección de vencimientos (arquitectura.md 4.7, DetectarVencimientos; fase 5, paso 5.2). La invoca la
 * tarea programada, desactivada por defecto hasta su verificación.
 */
public interface DetectarVencimientos {

    Resultado detectar();

    /**
     * @param pasadaId   identificador técnico de la pasada; acompaña a cada fallo registrado en el diagnóstico
     * @param candidatos pedidos revisados en la pasada
     * @param avisos     avisos registrados (uno por plazo vencido)
     * @param fallidos   pedidos cuya transacción se revirtió (cada uno con su registro de diagnóstico); vuelven a
     *                   ser candidatos en la siguiente pasada
     */
    record Resultado(java.util.UUID pasadaId, int candidatos, int avisos, int fallidos) {
    }

    /**
     * La pasada no pudo leer sus candidatos. Lleva el identificador de la pasada para correlacionarla con su línea de
     * diagnóstico; el mensaje es constante y nunca copia el de la causa.
     */
    final class PasadaInterrumpida extends RuntimeException {

        private static final long serialVersionUID = 1L;

        private final java.util.UUID pasadaId;

        public PasadaInterrumpida(java.util.UUID pasadaId, RuntimeException causa) {
            super("La pasada de vencimientos no pudo leer sus candidatos.", causa);
            this.pasadaId = pasadaId;
        }

        public java.util.UUID pasadaId() {
            return pasadaId;
        }
    }
}
