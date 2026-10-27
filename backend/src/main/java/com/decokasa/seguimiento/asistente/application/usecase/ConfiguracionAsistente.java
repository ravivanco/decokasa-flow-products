package com.decokasa.seguimiento.asistente.application.usecase;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuración del asistente leída del entorno (config/AsistenteConfig). La clave nunca pasa por aquí.
 *
 * @param habilitada IA_HABILITADA=true con clave y modelo configurados; si no, 503 IA_NO_DISPONIBLE
 * @param pilotoFicticio {@code decokasa.ia.entorno=PILOTO_FICTICIO} sin documentos reales autorizados (F7-D3). Si es
 *        falso no se envía ningún documento (bloqueo hasta F7-D6)
 * @param conservacion vida de una conversación (F7-D5, piloto: 30 días como máximo)
 * @param maxTokensRespuesta max_tokens (F7-D2: 4 000 como máximo)
 * @param limiteBytesDocumento tamaño máximo de un PDF enviado
 * @param modelo modelo configurado (IA_MODELO); vacío si no hay
 * @param esperaProveedor tiempo de espera de una llamada; la espera de reserva de la base debe superarlo
 */
public record ConfiguracionAsistente(boolean habilitada, boolean pilotoFicticio, Duration conservacion,
        int maxTokensRespuesta, long limiteBytesDocumento, String modelo, Duration esperaProveedor) {

    public static final Duration CONSERVACION_MAXIMA = Duration.ofDays(30);
    public static final int MAX_TOKENS_RESPUESTA = 4_000;
    /** Límite de una petición de Messages API (32 MB), con margen para el base64. */
    public static final long LIMITE_BYTES_MAXIMO = 20L * 1024 * 1024;

    public ConfiguracionAsistente {
        Objects.requireNonNull(conservacion, "conservacion");
        if (conservacion.isNegative() || conservacion.isZero() || conservacion.compareTo(CONSERVACION_MAXIMA) > 0) {
            throw new IllegalArgumentException("La conservación del piloto es de 1 a 30 días (F7-D5)");
        }
        if (maxTokensRespuesta < 1 || maxTokensRespuesta > MAX_TOKENS_RESPUESTA) {
            throw new IllegalArgumentException("max_tokens fuera del límite aprobado (F7-D2)");
        }
        if (limiteBytesDocumento < 1 || limiteBytesDocumento > LIMITE_BYTES_MAXIMO) {
            throw new IllegalArgumentException("Límite de documento inválido");
        }
        modelo = modelo == null ? "" : modelo.strip();
        Objects.requireNonNull(esperaProveedor, "esperaProveedor");
    }
}
