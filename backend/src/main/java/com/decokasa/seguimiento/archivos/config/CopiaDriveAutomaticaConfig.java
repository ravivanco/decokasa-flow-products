package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.in.tareas.TareaCopiaArchivos;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasPedidos;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * También procesa las carpetas de pedidos pendientes (ADR-0027): mismos tres flags, sin creación masiva al arrancar. Las
 * intenciones globales por usuario de ADR-0026 ya no se procesan.
 */
@Configuration(proxyBeanMethods=false)
@EnableScheduling
@ConditionalOnProperty(name={"decokasa.google.oauth.habilitada","decokasa.google.drive.habilitada",
        "decokasa.google.drive.worker.automatico"},havingValue="true")
public class CopiaDriveAutomaticaConfig {
    @Bean TareaCopiaArchivos tareaCopiaArchivos(ReintentarCopiasPendientes copias,
            GestionarCarpetasPedidos.Procesar carpetas,
            @Value("${decokasa.google.drive.worker.intervalo-ms:30000}") long intervalo) {
        if (intervalo<1000 || intervalo>86400000) { throw new IllegalArgumentException("Intervalo de copia inválido"); }
        return new TareaCopiaArchivos(copias,carpetas::ejecutar);
    }
}
