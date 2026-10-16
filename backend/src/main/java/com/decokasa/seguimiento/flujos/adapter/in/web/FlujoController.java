package com.decokasa.seguimiento.flujos.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.flujos.application.port.in.DescribirFlujo;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Definición de un flujo (contrato: GET /flujos/{pais}/{tipoFlujo}). Según el contrato la lee cualquier usuario
 * autenticado: son datos de configuración, no de pedidos. Sin {@code version}, la vigente.
 */
@RestController
public class FlujoController {

    private final DescribirFlujo describir;

    public FlujoController(DescribirFlujo describir) {
        this.describir = describir;
    }

    @GetMapping("/api/v1/flujos/{pais}/{tipoFlujo}")
    public DefinicionFlujoRespuesta definicion(@PathVariable CodigoPais pais, @PathVariable TipoFlujo tipoFlujo,
            @RequestParam(required = false) @Min(1) Integer version) {
        return DefinicionFlujoRespuesta.de(describir.describir(pais, tipoFlujo, version));
    }
}
