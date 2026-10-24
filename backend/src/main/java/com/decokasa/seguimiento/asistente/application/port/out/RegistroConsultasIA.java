package com.decokasa.seguimiento.asistente.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Registro de consumo (consulta_ia, V20): actor, fecha, pedido, modelo, tokens, costo, resultado y documentos enviados.
 * **Sin preguntas, respuestas ni citas.** Solo inserción.
 */
public interface RegistroConsultasIA {

    void registrar(ConsultaRegistrada consulta);

    enum Resultado {
        RESPONDIDA,
        SIN_EVIDENCIA,
        INCOMPLETA,
        RECHAZADA,
        LIMITE,
        ERROR_SIN_COBRO,
        ERROR_INCIERTO
    }

    /** @param modelo nulo si el proveedor no llegó a responder */
    record ConsultaRegistrada(UUID id, UUID conversacionId, UUID usuarioId, UUID pedidoId, CodigoPais pais,
            String modelo, AsistenteIA.Consumo consumo, long costoMicroUsd, Resultado resultado,
            Set<UUID> documentosEnviados, Instant creadaEn) {

        public ConsultaRegistrada {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(usuarioId, "usuarioId");
            Objects.requireNonNull(pedidoId, "pedidoId");
            Objects.requireNonNull(pais, "pais");
            Objects.requireNonNull(consumo, "consumo");
            Objects.requireNonNull(resultado, "resultado");
            Objects.requireNonNull(creadaEn, "creadaEn");
            documentosEnviados = Set.copyOf(documentosEnviados);
            if (costoMicroUsd < 0) {
                throw new IllegalArgumentException("Costo inválido");
            }
        }
    }
}
