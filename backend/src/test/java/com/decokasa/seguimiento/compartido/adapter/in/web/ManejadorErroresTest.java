package com.decokasa.seguimiento.compartido.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.decokasa.seguimiento.compartido.domain.CodigoError;
import com.decokasa.seguimiento.compartido.domain.ErrorDeDominio;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

// Contrato (openapi.yaml, Problema): todo error sale como application/problem+json (RFC 9457) con un "codigo" estable.
class ManejadorErroresTest {

    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new ManejadorErrores())
                .build();
    }

    @ParameterizedTest
    @CsvSource({
        "VALIDACION, 400",
        "NO_AUTENTICADO, 401",
        "SIN_PERMISO, 403",
        "NO_ENCONTRADO, 404",
        "ETAPA_CAMBIO, 409",
        "TRANSICION_NO_PERMITIDA, 409",
        "DUPLICADO, 409",
        "COPIA_PENDIENTE, 409",
        "ARCHIVO_DEMASIADO_GRANDE, 413",
        "TIPO_ARCHIVO_NO_ACEPTADO, 415",
        "REQUISITO_FALTANTE, 422",
        "CUENTA_BLOQUEADA, 423"
    })
    void traduceCadaCodigoDelContratoASuEstadoHttp(CodigoError codigo, int estado) throws Exception {
        mvc.perform(get("/prueba/error/{codigo}", codigo.name()))
                .andExpect(status().is(estado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(estado))
                .andExpect(jsonPath("$.codigo").value(codigo.name()))
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.detail").value("Mensaje de prueba"));
    }

    @Test
    void informaLosCamposInvalidosDeUnaSolicitud() throws Exception {
        mvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores[0].campo").value("nombre"))
                .andExpect(jsonPath("$.errores[0].mensaje").isNotEmpty());
    }

    @Test
    void unJsonIlegibleRespondeValidacionConSuCodigo() throws Exception {
        mvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{esto no es json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void unMetodoNoPermitidoRespondeProblemaSinInventarCodigo() throws Exception {
        mvc.perform(post("/prueba/inesperado"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").doesNotExist());
    }

    @Test
    void unErrorInesperadoNoRevelaDetallesInternos() throws Exception {
        mvc.perform(get("/prueba/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(content().string(not(containsString("clave-interna-secreta"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    record SolicitudDePrueba(@NotBlank String nombre) {
    }

    static final class ErrorDePrueba extends ErrorDeDominio {
        ErrorDePrueba(CodigoError codigo) {
            super(codigo, "Mensaje de prueba");
        }
    }

    @RestController
    static class ControladorDePrueba {

        @GetMapping("/prueba/error/{codigo}")
        void lanzar(@PathVariable CodigoError codigo) {
            throw new ErrorDePrueba(codigo);
        }

        @PostMapping("/prueba/validacion")
        void validar(@Valid @RequestBody SolicitudDePrueba solicitud) {
            // Solo valida el cuerpo.
        }

        @GetMapping("/prueba/inesperado")
        void fallar() {
            throw new IllegalStateException("clave-interna-secreta");
        }
    }
}
