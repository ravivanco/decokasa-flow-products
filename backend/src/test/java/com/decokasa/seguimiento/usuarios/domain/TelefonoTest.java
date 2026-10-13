package com.decokasa.seguimiento.usuarios.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

// Teléfono normalizado: dígitos y + inicial, de 7 a 15 dígitos; igual que el CHECK de la base (modelo-datos.md 3.1).
class TelefonoTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "+593 99 000 0001 | +593990000001",
        "+593990000001    | +593990000001",
        "099 000 0001     | 0990000001",
        "(02) 245-6789    | 022456789",
        "1234567          | 1234567",
        "+123456789012345 | +123456789012345"
    })
    void quitaEspaciosGuionesYParentesis(String entrada, String esperado) {
        assertThat(Telefono.normalizar(entrada).valor()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "+1234567890123456", "099-ABC-0001", "++593990000001", "593+990000001"})
    void rechazaLoQueNoEsUnTelefonoValido(String entrada) {
        assertThatThrownBy(() -> Telefono.normalizar(entrada)).isInstanceOf(DatoInvalido.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void rechazaUnTelefonoVacio(String entrada) {
        assertThatThrownBy(() -> Telefono.normalizar(entrada)).isInstanceOf(DatoInvalido.class);
    }
}
