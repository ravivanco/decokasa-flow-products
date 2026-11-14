package com.decokasa.seguimiento.notificaciones.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.notificaciones.application.port.out.ColaWhatsApp;
import java.util.regex.Pattern;

/**
 * Envía la cola de WhatsApp con el proveedor configurado. Un teléfono sin código de país no se adivina: queda FALLIDA
 * con su causa. Un error del proveedor tampoco se reintenta solo, para no duplicar mensajes. Sin Spring (R2).
 */
public final class EnviarWhatsAppCasoDeUso {

    private static final Pattern INTERNACIONAL = Pattern.compile("^\\+[1-9][0-9]{7,14}$");

    private final ColaWhatsApp cola;
    private final ColaWhatsApp.Mensajero mensajero;
    private final Reloj reloj;
    private final int lote;

    public EnviarWhatsAppCasoDeUso(ColaWhatsApp cola, ColaWhatsApp.Mensajero mensajero, Reloj reloj, int lote) {
        this.cola = cola;
        this.mensajero = mensajero;
        this.reloj = reloj;
        this.lote = lote;
    }

    /** @return mensajes tratados */
    public int procesar() {
        int tratados = 0;
        for (ColaWhatsApp.Pendiente pendiente : cola.reclamar(lote)) {
            tratados++;
            if (pendiente.telefono() == null || !INTERNACIONAL.matcher(pendiente.telefono()).matches()) {
                cola.fallida(pendiente.id(), "TELEFONO_SIN_CODIGO_PAIS", reloj.ahora());
                continue;
            }
            try {
                cola.enviada(pendiente.id(), mensajero.enviar(pendiente.telefono(), pendiente.texto()), reloj.ahora());
            } catch (RuntimeException error) {
                cola.fallida(pendiente.id(), error.getClass().getSimpleName(), reloj.ahora());
            }
        }
        return tratados;
    }
}
