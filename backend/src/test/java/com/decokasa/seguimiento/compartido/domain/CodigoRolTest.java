package com.decokasa.seguimiento.compartido.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// CodigoRol es texto libre porque el admin puede crear roles (requisito 4.6), pero con un formato estable:
// minúsculas y guion bajo, de 3 a 40 caracteres (contrato: CodigoRol).
class CodigoRolTest {

    @ParameterizedTest
    @ValueSource(strings = {"admin", "asistente_compras", "compras_china", "consulta"})
    void aceptaLosCodigosConFormatoValido(String valor) {
        assertThat(new CodigoRol(valor).valor()).isEqualTo(valor);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "ab", "Admin", "compras-china", "rol con espacios", "rol_demasiado_largo_para_el_formato_permitido"})
    void rechazaLosCodigosConFormatoInvalido(String valor) {
        assertThatThrownBy(() -> new CodigoRol(valor))
                .isInstanceOf(ErrorDeDominio.class)
                .extracting(error -> ((ErrorDeDominio) error).codigo())
                .isEqualTo(CodigoError.VALIDACION);
    }

    @Test
    void rechazaElValorNulo() {
        assertThatThrownBy(() -> new CodigoRol(null)).isInstanceOf(ErrorDeDominio.class);
    }
}
