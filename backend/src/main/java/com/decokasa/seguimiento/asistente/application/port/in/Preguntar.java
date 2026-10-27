package com.decokasa.seguimiento.asistente.application.port.in;

import com.decokasa.seguimiento.asistente.domain.DocumentoExcluido;
import com.decokasa.seguimiento.asistente.domain.Referencia;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;

/**
 * POST /asistente/conversaciones/{id}/mensajes. **Paso 7.4: implementado pero sin registrar ni publicar.** No se expone
 * ninguna ruta capaz de gastar hasta que el paso 7.5 implemente la reserva de presupuesto y los controles.
 */
public interface Preguntar {

    Resultado preguntar(Solicitante solicitante, UUID conversacionId, String pregunta);

    record Resultado(String texto, boolean evidenciaSuficiente, List<Referencia> referencias,
            List<DocumentoExcluido> documentosExcluidos, int preguntasRestantes) {

        @Override
        public String toString() {
            return "Resultado[evidencia=" + evidenciaSuficiente + ", referencias=" + referencias.size() + "]";
        }
    }
}
