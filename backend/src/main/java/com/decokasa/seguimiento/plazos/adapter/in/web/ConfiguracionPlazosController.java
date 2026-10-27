package com.decokasa.seguimiento.plazos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.plazos.application.port.in.ConfigurarMeta;
import com.decokasa.seguimiento.plazos.application.port.in.ConsultarConfiguracionPlazos;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

/**
 * Configuración de plazos (contrato: /configuracion/plazos, 0.11.0-borrador). La lectura es para cualquier usuario
 * autenticado; el cambio lo decide el caso de uso con el estado vigente (solo el admin). Solo se configura la meta: los
 * feriados siguen pendientes del cliente (⏸ F5-D7) y un PUT que los traiga se rechaza en lugar de ignorarlos.
 */
@RestController
@RequestMapping("/api/v1/configuracion/plazos")
public class ConfiguracionPlazosController {

    /** Esquema ConfiguracionPlazos. {@code feriados} siempre vacío mientras siga pendiente del cliente. */
    public record ConfiguracionPlazosRespuesta(int metaDias, String origenMeta, boolean metaPendienteDelCliente,
            List<Object> feriados, String actualizadoEn, long version) {

        static ConfiguracionPlazosRespuesta de(ConsultarConfiguracionPlazos.Configuracion configuracion) {
            // El valor de la meta sigue pendiente del cliente ("se ajusta") aunque el admin la haya configurado.
            return new ConfiguracionPlazosRespuesta(configuracion.metaDias(), configuracion.origenMeta(), true,
                    List.of(), FechasWeb.texto(configuracion.actualizadoEn()), configuracion.version());
        }
    }

    // Nodos JSON validados aquí, como la versión en usuarios y bodegas (D10): Jackson convertiría "90", 1.5 o true.
    public record SolicitudConfiguracionPlazos(JsonNode metaDias, JsonNode version, JsonNode feriados) {
    }

    private final ConsultarConfiguracionPlazos consultar;
    private final ConfigurarMeta configurar;

    public ConfiguracionPlazosController(ConsultarConfiguracionPlazos consultar, ConfigurarMeta configurar) {
        this.consultar = consultar;
        this.configurar = configurar;
    }

    @GetMapping
    public ConfiguracionPlazosRespuesta consultar() {
        return ConfiguracionPlazosRespuesta.de(consultar.consultar());
    }

    @PutMapping
    public ConfiguracionPlazosRespuesta configurar(@AuthenticationPrincipal Jwt token,
            @RequestBody SolicitudConfiguracionPlazos solicitud) {
        if (solicitud == null) {
            throw new DatoInvalido("Envía la meta de días y la versión que leíste.");
        }
        JsonNode feriados = solicitud.feriados();
        if (feriados != null && !feriados.isNull() && !(feriados.isArray() && feriados.size() == 0)) {
            throw new DatoInvalido("feriados",
                    "Los feriados están pendientes de aprobación del cliente: todavía no se pueden configurar.");
        }
        int metaDias = metaLeida(solicitud.metaDias());
        long version = versionLeida(solicitud.version());
        return ConfiguracionPlazosRespuesta.de(configurar.configurar(SolicitantesWeb.de(token),
                new ConfigurarMeta.Comando(metaDias, version)));
    }

    private static int metaLeida(JsonNode meta) {
        if (meta == null || !meta.isIntegralNumber() || !meta.canConvertToLong() || meta.asLong() < 1
                || meta.asLong() > Integer.MAX_VALUE) {
            throw new DatoInvalido("metaDias", "Escribe la meta en días: un número entero desde 1.");
        }
        return (int) meta.asLong();
    }

    private static long versionLeida(JsonNode version) {
        if (version == null || !version.isIntegralNumber() || !version.canConvertToLong() || version.asLong() < 0) {
            throw new DatoInvalido("version",
                    "Envía la versión de la configuración que leíste: un número entero desde 0.");
        }
        return version.asLong();
    }
}
