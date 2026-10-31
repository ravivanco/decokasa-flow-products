package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.domain.Adjunto;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Tabla adjunto de V3. Un adjunto no se borra (trigger de V3). */
public interface AdjuntoRepositorio {

    void insertar(Adjunto adjunto);

    /**
     * ADR-0034: inserta un adjunto cuyo contenido está solo en Drive, ya confirmado íntegro: COPIADO, sin clave local y
     * con su ID, enlace y carpeta de Drive. Misma transacción y mismo vaciado inmediato que {@link #insertar}.
     */
    default void insertarEnDrive(Adjunto adjunto, EnDrive drive) {
        throw new UnsupportedOperationException("Este repositorio no registra documentos solo en Drive");
    }

    /** Identificadores de Drive del documento confirmado. Nunca el contenido. */
    record EnDrive(String driveId, String enlace, String carpetaId) {
        public EnDrive {
            if (driveId == null || !driveId.matches("[A-Za-z0-9_-]{1,200}")
                    || carpetaId == null || !carpetaId.matches("[A-Za-z0-9_-]{1,200}")
                    || enlace == null || !enlace.matches("https://drive\\.google\\.com/[^\\s]{1,2000}")) {
                throw new IllegalArgumentException("Datos de Drive inválidos");
            }
        }
    }

    Optional<Adjunto> porId(UUID adjuntoId);

    List<Adjunto> porIds(Collection<UUID> adjuntoIds);

    /** De todas las entradas de la etapa, del más antiguo al más reciente. */
    List<Adjunto> deEtapa(UUID pedidoId, int etapa);

    /** Tipos de documento con al menos un archivo subido a esa entrada (solo metadatos). */
    Set<String> tiposDeEntrada(UUID entradaId);

    /** Evento ARCHIVO_SUBIDO de cada adjunto (relación SUBIDO del historial). */
    Map<UUID, UUID> eventosDeSubida(Collection<UUID> adjuntoIds);
}
