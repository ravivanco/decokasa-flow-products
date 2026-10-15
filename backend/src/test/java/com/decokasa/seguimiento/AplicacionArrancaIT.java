package com.decokasa.seguimiento;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

// La aplicación completa arranca contra PostgreSQL 17 real (Testcontainers), igual que docker-compose.yml.
// Requiere Docker encendido.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AplicacionArrancaIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void laSaludDeLaApiRespondeConLaBaseDeDatosReal() throws Exception {
        mvc.perform(get("/api/v1/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ok"));
    }

    @Test
    void elEstadoDeActuatorEstaArriba() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void laDocumentacionDeLaApiNoEsPublicaFueraDelPerfilDev() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().is4xxClientError());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().is4xxClientError());
    }

    @Test
    void losEndpointsDeActuatorDistintosDeHealthNoSeExponen() throws Exception {
        mvc.perform(get("/actuator/env")).andExpect(status().is4xxClientError());
        mvc.perform(get("/actuator/beans")).andExpect(status().is4xxClientError());
    }
}
