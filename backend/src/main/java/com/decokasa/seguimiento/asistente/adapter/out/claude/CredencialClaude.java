package com.decokasa.seguimiento.asistente.adapter.out.claude;

/**
 * Clave propia de Decokasa para Claude API (DECOKASA_CLAUDE_API_KEY; decisión de Richard del 2026-10-22). Nunca se
 * registra, se devuelve ni se busca en otra fuente: {@link #toString()} la oculta.
 */
public record CredencialClaude(String valor) {

    public CredencialClaude {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Falta la clave de Claude API de Decokasa");
        }
        valor = valor.strip();
    }

    @Override
    public String toString() {
        return "CredencialClaude[PROTEGIDA]";
    }
}
