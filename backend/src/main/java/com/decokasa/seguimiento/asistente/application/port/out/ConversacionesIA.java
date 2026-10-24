package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.MensajeIA;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Conversaciones y mensajes guardados (V20). Cada método es una transacción corta propia (ADR-0012). */
public interface ConversacionesIA {

    void insertar(ConversacionIA conversacion);

    Optional<ConversacionIA> porId(UUID conversacionId);

    /** Las de esa persona, vigentes y de esos países, más recientes primero. Sin mensajes. */
    Pagina deUsuario(UUID usuarioId, UUID pedidoId, Set<CodigoPais> paises, Instant ahora, int pagina, int tamano);

    /** Mensajes en orden. */
    List<MensajeIA> mensajes(UUID conversacionId);

    /**
     * Guarda una pregunta y su respuesta si la conversación sigue vigente, con {@code preguntasEsperadas} preguntas y
     * por debajo del límite (actualización condicional: dos turnos simultáneos no se guardan los dos).
     *
     * @return false si no se guardó
     */
    boolean agregarTurno(UUID conversacionId, int preguntasEsperadas, MensajeIA pregunta, MensajeIA respuesta,
            Instant ahora);

    /** Borra hasta {@code lote} conversaciones vencidas con sus mensajes (cascada). */
    int borrarVencidas(Instant ahora, int lote);

    record Pagina(List<ConversacionIA> elementos, long total) {

        public Pagina {
            elementos = List.copyOf(elementos);
        }
    }
}
