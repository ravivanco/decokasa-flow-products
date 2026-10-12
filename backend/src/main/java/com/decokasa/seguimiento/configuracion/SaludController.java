package com.decokasa.seguimiento.configuracion;

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

    public record Salud(String estado) {
    }

    @GetMapping
    public Salud consultar() {
        return new Salud("ok");
    }
}
