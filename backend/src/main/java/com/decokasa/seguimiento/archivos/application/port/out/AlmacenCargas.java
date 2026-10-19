package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/** Parciales duraderos en el MISMO volumen que AlmacenLocal. No es otro catálogo de adjuntos. */
public interface AlmacenCargas {
    Bloqueo bloquear(UUID cargaId);
    void reconciliar(UUID cargaId, long offsetConfirmado);
    void escribir(UUID cargaId, long inicio, byte[] contenido);
    void validar(UUID cargaId, List<CargaArchivo.Fragmento> fragmentos, long total);
    InputStream abrir(UUID cargaId);
    interface Bloqueo extends AutoCloseable { @Override void close(); }
}
