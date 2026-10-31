package com.decokasa.seguimiento.archivos.adapter.out.google;

import com.decokasa.seguimiento.archivos.adapter.out.google.AlmacenTokensGoogle.Tokens;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reglas comunes a las dos bóvedas de tokens (archivo local y PostgreSQL, ADR-0034), para que no diverjan: no se
 * sustituye otra cuenta o cliente, un consentimiento sin refresh token conserva el vigente y una renovación nunca
 * prolonga la autorización ni cambia de cuenta.
 */
final class ReglasTokensGoogle {
    private ReglasTokensGoogle() { }

    static Tokens combinar(Tokens anterior, Tokens nuevos, Instant ahora) throws IOException {
        if (anterior != null && (!anterior.cuentaId().equals(nuevos.cuentaId())
                || !anterior.clienteId().equals(nuevos.clienteId()))) {
            throw new IOException("No se sustituye otra cuenta o cliente sin intervención explícita");
        }
        if (!nuevos.refreshToken().isBlank()) { return nuevos; }
        if (anterior == null || !ahora.isBefore(anterior.autorizacionHasta())) {
            throw new IOException("Se requiere un refresh token vigente");
        }
        return new Tokens(nuevos.clienteId(), nuevos.cuentaId(), nuevos.correo(), nuevos.accessToken(),
                anterior.refreshToken(), nuevos.accessHasta(), anterior.autorizacionHasta(), nuevos.ambitos());
    }

    static void exigirAutorizacion(Tokens tokens, Instant ahora) throws IOException {
        if (!ahora.isBefore(tokens.autorizacionHasta())) { throw new IOException("Autorización caducada"); }
    }

    static boolean accesoVigente(Tokens tokens, Instant ahora) {
        return ahora.plusSeconds(30).isBefore(tokens.accessHasta());
    }

    static void validarRenovacion(Tokens anterior, Tokens nuevo, Instant ahora) throws IOException {
        if (!anterior.clienteId().equals(nuevo.clienteId()) || !anterior.cuentaId().equals(nuevo.cuentaId())
                || !anterior.correo().equals(nuevo.correo()) || nuevo.accessToken().isBlank()
                || !ahora.isBefore(nuevo.accessHasta())
                || nuevo.autorizacionHasta().isAfter(anterior.autorizacionHasta())) {
            throw new IOException("Renovación inválida");
        }
    }

    static byte[] serializar(Tokens t, JsonMapper json) {
        return json.writeValueAsBytes(Map.of("clienteId", t.clienteId(), "cuentaId", t.cuentaId(),
                "correo", t.correo(), "accessToken", t.accessToken(), "refreshToken", t.refreshToken(),
                "accessHasta", t.accessHasta().toString(), "autorizacionHasta", t.autorizacionHasta().toString(),
                "ambitos", t.ambitos()));
    }

    static Tokens leer(byte[] claro, JsonMapper json) throws IOException {
        JsonNode doc = json.readTree(claro);
        return new Tokens(texto(doc, "clienteId"), texto(doc, "cuentaId"), texto(doc, "correo"),
                texto(doc, "accessToken"), texto(doc, "refreshToken"),
                Instant.parse(texto(doc, "accessHasta")), Instant.parse(texto(doc, "autorizacionHasta")),
                texto(doc, "ambitos"));
    }

    private static String texto(JsonNode nodo, String campo) throws IOException {
        JsonNode valor = nodo.get(campo);
        if (valor == null || !valor.isString()) { throw new IOException("Contenedor inválido"); }
        return valor.asString();
    }
}
