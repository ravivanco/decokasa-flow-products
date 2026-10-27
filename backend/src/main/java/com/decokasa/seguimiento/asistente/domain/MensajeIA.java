package com.decokasa.seguimiento.asistente.domain;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Mensaje guardado de una conversación (F7-D5, piloto). El texto nunca va a los logs: {@link #toString()} no lo incluye.
 *
 * @param documentosEnviados PDF enviados al modelo para producir esta respuesta; vacío en las preguntas
 * @param evidenciaSuficiente nulo en las preguntas
 */
public record MensajeIA(int orden, Autor autor, String texto, List<Referencia> referencias,
        Set<UUID> documentosEnviados, List<DocumentoExcluido> documentosExcluidos, Boolean evidenciaSuficiente,
        Instant creadoEn) {

    public MensajeIA {
        if (orden < 1) {
            throw new IllegalArgumentException("Orden inválido");
        }
        Objects.requireNonNull(autor, "autor");
        Objects.requireNonNull(texto, "texto");
        Objects.requireNonNull(creadoEn, "creadoEn");
        referencias = List.copyOf(referencias);
        documentosEnviados = Set.copyOf(documentosEnviados);
        documentosExcluidos = List.copyOf(documentosExcluidos);
        if ((autor == Autor.ASISTENTE) != (evidenciaSuficiente != null)) {
            throw new IllegalArgumentException("La evidencia solo acompaña a las respuestas");
        }
    }

    public static MensajeIA pregunta(int orden, String texto, Instant ahora) {
        return new MensajeIA(orden, Autor.USUARIO, texto, List.of(), Set.of(), List.of(), null, ahora);
    }

    public static MensajeIA respuesta(int orden, String texto, List<Referencia> referencias, Set<UUID> enviados,
            List<DocumentoExcluido> excluidos, boolean evidenciaSuficiente, Instant ahora) {
        return new MensajeIA(orden, Autor.ASISTENTE, texto, referencias, enviados, excluidos, evidenciaSuficiente,
                ahora);
    }

    /** Documentos de los que depende el mensaje: los enviados, los citados y los excluidos (se muestra su nombre). */
    public Set<UUID> fuentes() {
        Set<UUID> fuentes = new HashSet<>(documentosEnviados);
        referencias.stream().map(Referencia::archivoId).filter(Objects::nonNull).forEach(fuentes::add);
        documentosExcluidos.forEach(excluido -> fuentes.add(excluido.archivoId()));
        return Set.copyOf(fuentes);
    }

    /**
     * Precisión 3 de Richard (2026-10-22): si la persona ya no puede descargar alguna fuente, el mensaje se devuelve
     * retenido (sin texto ni referencias) y no se reenvía al modelo.
     */
    public boolean retenidoPara(Predicate<UUID> puedeDescargar) {
        return fuentes().stream().anyMatch(puedeDescargar.negate());
    }

    @Override
    public String toString() {
        return "MensajeIA[orden=" + orden + ", autor=" + autor + "]";
    }
}
