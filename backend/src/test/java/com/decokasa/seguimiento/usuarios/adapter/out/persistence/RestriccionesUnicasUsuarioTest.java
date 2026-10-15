package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

// Hallazgo M2 (duplicados): solo las tres restricciones únicas conocidas de usuario se traducen a DUPLICADO; cualquier
// otro error de integridad sigue su curso. El mensaje no expone nombres ni textos de la base.
class RestriccionesUnicasUsuarioTest {

    private static final String UNICIDAD = "23505";
    private static final String CLAVE_FORANEA = "23503";

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "usuario_correo_unico, correo",
        "usuario_usuario_unico, usuario",
        "usuario_telefono_unico, telefono"
    })
    void cadaRestriccionUnicaConocidaSeTraduceASuCampo(String restriccion, String campo) {
        DatoDuplicado duplicado = RestriccionesUnicasUsuario.comoDuplicado(violacion(UNICIDAD, restriccion))
                .orElseThrow();

        assertThat(duplicado.campo()).isEqualTo(campo);
        assertThat(duplicado.getMessage()).doesNotContain(restriccion).doesNotContainIgnoringCase("duplicate key");
    }

    @Test
    void unaRestriccionUnicaDesconocidaNoSeTraduce() {
        assertThat(RestriccionesUnicasUsuario.comoDuplicado(violacion(UNICIDAD, "usuario_pkey"))).isEmpty();
        assertThat(RestriccionesUnicasUsuario.comoDuplicado(violacion(UNICIDAD, null))).isEmpty();
    }

    @Test
    void otroErrorDeIntegridadConUnNombreConocidoNoSeTraduce() {
        assertThat(RestriccionesUnicasUsuario.comoDuplicado(violacion(CLAVE_FORANEA, "usuario_correo_unico")))
                .isEmpty();
    }

    @Test
    void unErrorDeIntegridadSinDetalleDeLaBaseNoSeTraduce() {
        assertThat(RestriccionesUnicasUsuario.comoDuplicado(new DataIntegrityViolationException("sin causa")))
                .isEmpty();
    }

    // Así llega desde Spring Data: DataIntegrityViolationException con la excepción de Hibernate como causa.
    private static DataIntegrityViolationException violacion(String estadoSql, String restriccion) {
        SQLException sql = new SQLException("duplicate key value violates unique constraint", estadoSql);
        return new DataIntegrityViolationException("could not execute statement",
                new ConstraintViolationException("could not execute statement", sql, restriccion));
    }
}
