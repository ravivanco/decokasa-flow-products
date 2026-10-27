package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Metadatos del mismo módulo archivos. Todas las operaciones participan en la transacción del llamador. */
public interface CargaRepositorio {
    CargaArchivo crearSiAusente(CargaArchivo candidata);
    Optional<CargaArchivo> porId(UUID id, boolean bloquear);
    List<CargaArchivo.Fragmento> fragmentos(UUID id);
    void avanzar(UUID id, long offsetEsperado, CargaArchivo.Fragmento fragmento, Instant ahora);
    void completar(UUID id, UUID adjuntoId, UUID eventoId, Instant ahora);
}
