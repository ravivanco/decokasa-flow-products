package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.regex.Pattern;

/**
 * Teléfono normalizado: solo dígitos y un {@code +} inicial, de 7 a 15 dígitos (modelo-datos.md 3.1, "Límites de
 * campos"; mismo formato que el CHECK de la base). Así se puede iniciar sesión con o sin espacios.
 */
public record Telefono(String valor) {

    private static final Pattern FORMATO = Pattern.compile("^\\+?[0-9]{7,15}$");
    private static final Pattern SEPARADORES = Pattern.compile("[\\s()\\-]");
    private static final String MENSAJE = "Escribe un teléfono válido, de 7 a 15 dígitos.";

    public Telefono {
        if (valor == null || !FORMATO.matcher(valor).matches()) {
            throw new DatoInvalido("telefono", MENSAJE);
        }
    }

    /**
     * Quita espacios, guiones y paréntesis y valida el resultado.
     *
     * @throws DatoInvalido si no queda un teléfono válido
     */
    public static Telefono normalizar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            throw new DatoInvalido("telefono", MENSAJE);
        }
        return new Telefono(SEPARADORES.matcher(entrada).replaceAll(""));
    }
}
