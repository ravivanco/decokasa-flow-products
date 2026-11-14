package com.decokasa.seguimiento.embarques.domain;

import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.util.Locale;

/**
 * Código de un BL: se muestra como se escribió y se compara normalizado, en mayúsculas y sin espacios. Los guiones se
 * conservan (pendiente de confirmar con el cliente si MEDU-123 y MEDU123 son el mismo BL). Java puro (R1).
 */
public record CodigoBl(String texto, String normalizado) {

    public static CodigoBl de(String entrada) {
        String limpio = entrada == null ? "" : entrada.strip();
        if (limpio.length() < 3 || limpio.length() > 40) {
            throw new DatoInvalido("codigo", "El código del BL debe tener entre 3 y 40 caracteres.");
        }
        return new CodigoBl(limpio, normalizar(limpio));
    }

    public static String normalizar(String entrada) {
        return entrada == null ? "" : entrada.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
