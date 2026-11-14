package com.decokasa.seguimiento.notificaciones.adapter.out.buzon;

import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.LoggerFactory;

/**
 * Solo para pruebas locales: entrega cada correo como un archivo de texto y nunca llama a un proveedor. Lo activa
 * {@code NOTIFICACIONES_BUZON_SIMULADO} con una carpeta; en Render no arranca.
 */
public final class BuzonSimuladoNotificador implements Notificador {

    private final Path carpeta;

    public BuzonSimuladoNotificador(Path carpeta) {
        this.carpeta = carpeta;
    }

    @Override
    public Entrega enviar(Correo correo) {
        String contenido = "Para: " + correo.destinatario() + "\nAsunto: " + correo.asunto() + "\n\n" + correo.texto();
        try {
            Files.createDirectories(carpeta);
            Files.writeString(carpeta.resolve(correo.notificacionId() + ".txt"), contenido, StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        // Solo el id: sin destinatario ni contenido en los logs.
        LoggerFactory.getLogger("decokasa.buzon").info("buzon_simulado entrega notificacion={}", correo.notificacionId());
        return new Entrega("simulado-" + correo.notificacionId());
    }
}
