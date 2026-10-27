package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.notificaciones.application.port.out.AvisoRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.out.EtapasDePedido;
import com.decokasa.seguimiento.notificaciones.application.usecase.RegistrarAvisosDeEtapaCasoDeUso;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import java.util.LinkedHashSet;

import com.decokasa.seguimiento.notificaciones.application.port.in.EncolarNotificacion;
import com.decokasa.seguimiento.notificaciones.application.port.out.DirectorioDestinatarios;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.application.usecase.EncolarNotificacionCasoDeUso;
import java.util.Locale;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class NotificacionesConfig {

    /**
     * Único destinatario que Richard autorizó para la prueba real de Gmail (2026-10-24). No es un destinatario
     * definitivo de producción (F3-D8 sigue pendiente).
     */
    static final String DESTINATARIO_PRUEBA_AUTORIZADO = "importadora.latinoamericaworld@gmail.com";

    /**
     * No deducir que todos los admins ni la cuenta remitente son destinatarios. F3-D8 pendiente.
     *
     * <p>Solo para la prueba real, en la configuración local aislada: {@code NOTIFICACIONES_DESTINATARIO_PRUEBA}. Vacía
     * por defecto, el directorio sigue vacío y no se envía nada. El único valor admitido es el buzón autorizado; con
     * cualquier otro la aplicación no arranca. Encolar y enviar siguen revalidando el destinatario contra este
     * directorio.
     */
    @Bean DirectorioDestinatarios directorioDestinatarios(
            @Value("${decokasa.notificaciones.destinatario-prueba:}") String destinatarioPrueba,
            ConsultarContactos contactos) {
        // Con el buzón de prueba configurado, solo él (comportamiento anterior). Si no, ADR-0032: correos registrados.
        return destinatarioPrueba == null || destinatarioPrueba.isBlank() ? registrados(contactos)
                : desde(destinatarioPrueba);
    }

    /**
     * ADR-0032: los correos registrados de cuentas activas con el país del pedido. No se inventa ni se deduce ningún
     * destinatario: se revalida contra la base antes de cada envío.
     */
    static DirectorioDestinatarios registrados(ConsultarContactos contactos) {
        return new DirectorioDestinatarios() {
            @Override public Set<String> administradores(CodigoPais pais) {
                Set<String> correos = new LinkedHashSet<>();
                for (var admin : contactos.activosConRol(ADMIN, pais)) {
                    try { correos.add(Notificacion.normalizarCorreo(admin.correo())); }
                    catch (IllegalArgumentException | NullPointerException invalido) { /* sin correo usable */ }
                }
                return Set.copyOf(correos);
            }
            @Override public boolean autorizado(CodigoPais pais, String correo) {
                return contactos.correoAutorizado(pais, correo);
            }
            @Override public boolean algunoAutorizado() { return true; }
        };
    }

    private static final CodigoRol ADMIN = new CodigoRol("admin");

    static DirectorioDestinatarios desde(String destinatarioPrueba) {
        String valor = destinatarioPrueba == null ? "" : destinatarioPrueba.strip();
        if (valor.isEmpty()) {
            return pais -> Set.of();
        }
        if (!DESTINATARIO_PRUEBA_AUTORIZADO.equals(valor.toLowerCase(Locale.ROOT))) {
            // Sin el valor en el mensaje: no se registran direcciones en los logs.
            throw new IllegalStateException(
                    "decokasa.notificaciones.destinatario-prueba solo admite el buzón autorizado.");
        }
        Set<String> unico = Set.of(DESTINATARIO_PRUEBA_AUTORIZADO);
        return pais -> unico;
    }

    /**
     * ADR-0032: los cambios de etapa los avisa RegistrarAvisosDeEtapaCasoDeUso (aviso_etapa y AVISO_ETAPA); el caso
     * anterior solo encola los vencimientos. Los dos corren en la transacción de la acción.
     */
    @Bean EncolarNotificacion encolarNotificacion(NotificacionRepositorio repositorio,
            DirectorioDestinatarios directorio, EtapasDePedido etapas, ConsultarContactos contactos,
            AvisoRepositorio avisos) {
        EncolarNotificacion vencimientos = new EncolarNotificacionCasoDeUso(repositorio, directorio, false);
        EncolarNotificacion etapa = new RegistrarAvisosDeEtapaCasoDeUso(etapas, contactos, avisos, repositorio);
        return evento -> {
            vencimientos.encolar(evento);
            etapa.encolar(evento);
        };
    }
}
