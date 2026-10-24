package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Quién subió un adjunto y con qué rol intervino, tal como lo validó y guardó el servidor al subirlo: el evento
 * ARCHIVO_SUBIDO enlazado al adjunto (relación SUBIDO). El rol sale de la regla P-06 ({@code PermisosEtapa.paraActuar}):
 * el responsable de la etapa si la persona lo tiene; si no, {@code admin}. No cambia aunque después cambien sus roles.
 */
@FunctionalInterface
public interface AutoresDeCopias {

    /** Vacío si el adjunto no tiene su evento de subida o este no coincide con el adjunto (autor o pedido). */
    Optional<Autor> autor(UUID adjuntoId);

    record Autor(UUID usuarioId, CodigoRol rol) {
        public Autor {
            Objects.requireNonNull(usuarioId, "usuarioId");
            Objects.requireNonNull(rol, "rol");
        }
    }
}
