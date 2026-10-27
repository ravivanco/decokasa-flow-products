package com.decokasa.seguimiento.asistente.adapter.in.tareas;

import com.decokasa.seguimiento.asistente.application.port.in.BorrarConversacionesVencidas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** F7-D5 (piloto): una pasada diaria. Se registra solo por BorradoAsistenteConfig. */
public final class TareaBorradoConversaciones {

    private static final Logger LOG = LoggerFactory.getLogger(TareaBorradoConversaciones.class);

    private final BorrarConversacionesVencidas borrar;

    public TareaBorradoConversaciones(BorrarConversacionesVencidas borrar) {
        this.borrar = borrar;
    }

    @Scheduled(cron = "${decokasa.ia.borrado.cron:0 15 3 * * *}", zone = "America/Guayaquil")
    public void ejecutar() {
        try {
            int borradas = borrar.ejecutar();
            if (borradas > 0) {
                LOG.info("Conversaciones vencidas del asistente eliminadas: {}", borradas);
            }
        } catch (RuntimeException error) {
            // Sin adjuntar la excepción: un error JDBC podría incluir valores de las filas.
            LOG.warn("No se completó el borrado de conversaciones vencidas del asistente; se reintenta mañana.");
        }
    }
}
