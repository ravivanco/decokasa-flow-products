package com.decokasa.seguimiento.configuracion;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Paso 1.3 del manual: endpoint de salud público en la base /api/v1 del contrato.
// El resto de la API exige autenticación desde el primer día (R6); sin token responde 401 con formato Problema.
@WebMvcTest(SaludController.class)
@Import(SeguridadWebConfig.class)
class SaludControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void laSaludRespondeSinAutenticacion() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.estado").value("ok"));
    }

    @Test
    void otraRutaDeLaApiSinTokenRespondeNoAutenticado() throws Exception {
        mvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    void laRespuestaLlevaCabecerasDeSeguridad() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
    }
}
