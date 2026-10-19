package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.UUID;

public interface GestionarCarga {
    Vista iniciar(Solicitante solicitante, Inicio comando);
    Vista consultar(Solicitante solicitante, UUID cargaId);
    Vista fragmento(Solicitante solicitante, UUID cargaId, Bloque bloque);
    AdjuntoVista completar(Solicitante solicitante, UUID cargaId);
    record Inicio(UUID pedidoId, int etapa, UUID entradaEsperadaId, UUID claveIdempotencia,
            String nombre, String tipoDocumento, long tamanoBytes) { }
    record Bloque(long inicio, long total, String sha256, byte[] contenido) { }
    record Vista(CargaArchivo carga, List<CargaArchivo.Fragmento> fragmentos) {
        public Vista { fragmentos = List.copyOf(fragmentos); }
    }
}
