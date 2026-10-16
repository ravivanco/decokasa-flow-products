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

    Optional<Adjunto> porId(UUID adjuntoId);

    List<Adjunto> porIds(Collection<UUID> adjuntoIds);

    /** De todas las entradas de la etapa, del más antiguo al más reciente. */
    List<Adjunto> deEtapa(UUID pedidoId, int etapa);

    /** Tipos de documento con al menos un archivo subido a esa entrada (solo metadatos). */
    Set<String> tiposDeEntrada(UUID entradaId);

    /** Evento ARCHIVO_SUBIDO de cada adjunto (relación SUBIDO del historial). */
    Map<UUID, UUID> eventosDeSubida(Collection<UUID> adjuntoIds);
}
