package com.decokasa.seguimiento.plazos.adapter.in.tareas;

import com.decokasa.seguimiento.plazos.application.port.in.DetectarVencimientos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Tarea programada de vencimientos (paso 5.2). Solo la registra VencimientosAutomaticosConfig con
 * {@code decokasa.plazos.vencimientos.automatico=true}; desactivada por defecto hasta su verificación. Registra solo
 * contadores: nunca pedidos, personas ni correos.
 */
public final class TareaVencimientos {

    private static final Logger LOG = LoggerFactory.getLogger(TareaVencimientos.class);

    private final DetectarVencimientos detectar;

    public TareaVencimientos(DetectarVencimientos detectar) {
        this.detectar = detectar;
    }

    @Scheduled(fixedDelayString = "${decokasa.plazos.vencimientos.intervalo-ms:300000}")
    public void ejecutar() {
        try {
            DetectarVencimientos.Resultado resultado = detectar.detectar();
            if (resultado.fallidos() > 0) {
                // Cada fallo tiene su línea vencimientos_fallo_pedido con la misma pasada (DiagnosticoVencimientosLog).
                LOG.warn("vencimientos_pasada pasada={} candidatos={} avisos={} fallidos={}; los fallidos se reintentan",
                        resultado.pasadaId(), resultado.candidatos(), resultado.avisos(), resultado.fallidos());
            } else if (resultado.avisos() > 0) {
                LOG.info("vencimientos_pasada pasada={} candidatos={} avisos={}", resultado.pasadaId(),
                        resultado.candidatos(), resultado.avisos());
            }
        } catch (DetectarVencimientos.PasadaInterrumpida interrumpida) {
            // La categoría y el código ya quedaron en vencimientos_fallo_pasada con la misma pasada.
            LOG.warn("vencimientos_pasada_interrumpida pasada={}; se reintenta en la siguiente pasada",
                    interrumpida.pasadaId());
        } catch (RuntimeException error) {
            // Sin el mensaje ni la traza: podrían incluir datos. Los vencimientos sin aviso se reintentan.
            LOG.warn("vencimientos_pasada_interrumpida excepcion={}; se reintenta en la siguiente pasada",
                    error.getClass().getName());
        }
    }
}
