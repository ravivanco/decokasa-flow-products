package com.decokasa.seguimiento.configuracion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Salud de la API (manual, paso 1.3), en la base /api/v1 del contrato. Es pública y no revela detalles internos;
 * el estado de la base de datos lo informa /actuator/health dentro de la red de Docker.
 */
@RestController
@RequestMapping("/api/v1/salud")
public class SaludController {

    /** @param version commit desplegado (7 caracteres, de RENDER_GIT_COMMIT) o "local"; el repositorio es público */
    public record Salud(String estado, String version) {
    }

    private final String version;

    public SaludController(@Value("${RENDER_GIT_COMMIT:local}") String commit) {
        String limpio = commit == null ? "" : commit.strip();
        this.version = limpio.matches("[0-9a-f]{7,64}") ? limpio.substring(0, 7) : "local";
    }

    @GetMapping
    public Salud consultar() {
        return new Salud("ok", version);
    }
}
