package com.decokasa.seguimiento.usuarios.adapter.in.tareas;

import com.decokasa.seguimiento.usuarios.application.port.in.EnviarCorreosRecuperacion;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Pasada periódica del envío de enlaces de recuperación (ADR-0033). Solo existe con las tres banderas encendidas. */
public final class TareaCorreosRecuperacion {

    private final EnviarCorreosRecuperacion enviar;

    public TareaCorreosRecuperacion(EnviarCorreosRecuperacion enviar) {
        this.enviar = enviar;
    }

    @Scheduled(fixedDelayString = "${decokasa.recuperacion.intervalo-ms:15000}")
    public void ejecutar() {
        try {
            enviar.procesar();
        } catch (RuntimeException error) {
            // Sin excepción, destinatario, enlace ni token en los logs.
            LoggerFactory.getLogger(TareaCorreosRecuperacion.class)
                    .warn("recuperacion_pasada_interrumpida; revisar recuperacion_contrasena");
        }
    }
}
