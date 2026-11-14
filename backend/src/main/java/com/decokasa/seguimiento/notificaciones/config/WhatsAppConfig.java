package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.notificaciones.adapter.out.whatsapp.WhatsAppCloud;
import com.decokasa.seguimiento.notificaciones.application.port.out.ColaWhatsApp;
import com.decokasa.seguimiento.notificaciones.application.usecase.EnviarWhatsAppCasoDeUso;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

/** Envío real de WhatsApp: solo con WHATSAPP_MODO=cloud y las credenciales del cliente. Por defecto, simulado. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "decokasa.whatsapp.modo", havingValue = "cloud")
public class WhatsAppConfig {

    @Bean
    TareaWhatsApp tareaWhatsApp(PlatformTransactionManager gestor, ColaWhatsApp cola, JsonMapper json, Reloj reloj,
            @Value("${decokasa.whatsapp.token:}") String token,
            @Value("${decokasa.whatsapp.numero-id:}") String numeroId,
            @Value("${decokasa.whatsapp.plantilla:}") String plantilla,
            @Value("${decokasa.whatsapp.idioma:es}") String idioma,
            @Value("${decokasa.whatsapp.version-api:v21.0}") String version) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        ColaWhatsApp enTransaccion = new ColaWhatsApp() {
            @Override public List<Pendiente> reclamar(int lote) { return tx.execute(e -> cola.reclamar(lote)); }

            @Override public void enviada(UUID id, String proveedorId, Instant ahora) {
                tx.executeWithoutResult(e -> cola.enviada(id, proveedorId, ahora));
            }

            @Override public void fallida(UUID id, String causa, Instant ahora) {
                tx.executeWithoutResult(e -> cola.fallida(id, causa, ahora));
            }
        };
        return new TareaWhatsApp(new EnviarWhatsAppCasoDeUso(enTransaccion,
                new WhatsAppCloud(json, token, numeroId, plantilla, idioma, version), reloj, 20));
    }

    public static final class TareaWhatsApp {
        private final EnviarWhatsAppCasoDeUso caso;

        TareaWhatsApp(EnviarWhatsAppCasoDeUso caso) {
            this.caso = caso;
        }

        @Scheduled(fixedDelayString = "${decokasa.whatsapp.intervalo-ms:30000}")
        public void ejecutar() {
            try {
                caso.procesar();
            } catch (RuntimeException error) {
                // Sin teléfonos ni textos en los logs.
                LoggerFactory.getLogger(TareaWhatsApp.class).warn("whatsapp pasada interrumpida");
            }
        }
    }
}
