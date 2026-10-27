package com.decokasa.seguimiento.archivos.application.usecase;

import static org.assertj.core.api.Assertions.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class FlujoVerificadoTest {
    private String sha(byte[] b) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(b));
    }
    @Test void noEntregaElUltimoBloqueSiLaHuellaNoCoincide() throws Exception {
        byte[] correcto = new byte[200_000], alterado = correcto.clone();
        alterado[199_999] = 1;
        var recibido = new ByteArrayOutputStream();
        try (var flujo = new FlujoVerificado(new ByteArrayInputStream(alterado), correcto.length, sha(correcto))) {
            assertThatThrownBy(() -> flujo.transferTo(recibido)).isInstanceOf(IOException.class);
            // Con Content-Length el cliente acepta la respuesta en cuanto recibe todos los bytes.
            assertThat(recibido.size()).isLessThan(correcto.length);
        }
    }
    @Test void rechazaExcesoAntesDeEntregarElTamanoAnunciado() throws Exception {
        byte[] correcto = new byte[200_000];
        var recibido = new ByteArrayOutputStream();
        try (var flujo = new FlujoVerificado(new ByteArrayInputStream(new byte[200_001]), correcto.length, sha(correcto))) {
            assertThatThrownBy(() -> flujo.transferTo(recibido)).isInstanceOf(IOException.class);
            assertThat(recibido.size()).isLessThan(correcto.length);
        }
    }
    @Test void aceptaBytesExactosConLecturasSimplesYPorBloques() throws Exception {
        byte[] esperado = "Documento ficticio íntegro".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        try (var flujo = new FlujoVerificado(new ByteArrayInputStream(esperado), esperado.length, sha(esperado))) {
            assertThat(flujo.read()).isEqualTo(esperado[0]);
            assertThat(flujo.readAllBytes()).containsExactly(java.util.Arrays.copyOfRange(esperado, 1, esperado.length));
        }
    }
    @Test void saltoTambienVerificaLaIntegridadYRechazaTruncado() throws Exception {
        byte[] esperado = new byte[20];
        try (var flujo = new FlujoVerificado(new ByteArrayInputStream(new byte[19]), 20, sha(esperado))) {
            assertThatThrownBy(() -> flujo.skip(20)).isInstanceOf(IOException.class);
            assertThat(flujo.markSupported()).isFalse();
        }
    }
}
