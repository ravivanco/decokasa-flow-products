package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * La acción que alguien pide sobre la etapa actual (contrato: SolicitudAccion).
 *
 * @param etapaEsperada la etapa que la persona ve en pantalla (propuesta del contrato): si ya no es la actual, 409
 * @param documentosPresentes tipos de documento de los archivos presentados, ya validados por el módulo de archivos
 *        (archivo real en la entrada actual; P-05 opción A, P-18). El dominio no los simula ni los da por presentes
 * @param adjuntoIds archivos ya validados por el módulo de archivos (entrada actual, P-18); quedan en el evento
 */
public record AccionSolicitada(AccionEtapa accion, int etapaEsperada, String observacion, NivelUrgencia nivelUrgencia,
        Set<String> documentosPresentes, List<UUID> adjuntoIds) {

    public AccionSolicitada {
        Objects.requireNonNull(accion, "accion");
        documentosPresentes = documentosPresentes == null ? Set.of() : Set.copyOf(documentosPresentes);
        adjuntoIds = adjuntoIds == null ? List.of() : List.copyOf(adjuntoIds);
    }

    /** Sin archivos presentados. */
    public AccionSolicitada(AccionEtapa accion, int etapaEsperada, String observacion, NivelUrgencia nivelUrgencia,
            Set<String> documentosPresentes) {
        this(accion, etapaEsperada, observacion, nivelUrgencia, documentosPresentes, List.of());
    }
}
