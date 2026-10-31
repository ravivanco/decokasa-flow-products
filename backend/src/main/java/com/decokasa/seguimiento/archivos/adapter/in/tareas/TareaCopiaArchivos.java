package com.decokasa.seguimiento.archivos.adapter.in.tareas;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasUsuarios;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Se registra únicamente por CopiaDriveAutomaticaConfig con los tres flags explícitos habilitados. En cada pasada procesa
 * primero las carpetas de pedidos pendientes (ADR-0027) y después las copias; un fallo de una no impide la otra.
 */
public final class TareaCopiaArchivos {
    private static final Logger LOG=LoggerFactory.getLogger(TareaCopiaArchivos.class);
    private final ReintentarCopiasPendientes copias;
    private final GestionarCarpetasUsuarios.Procesar carpetas;
    /** Solo copias: pruebas anteriores al 2026-10-24. */
    public TareaCopiaArchivos(ReintentarCopiasPendientes copias) { this(copias,() -> 0); }
    public TareaCopiaArchivos(ReintentarCopiasPendientes copias,GestionarCarpetasUsuarios.Procesar carpetas) {
        this.copias=copias;this.carpetas=carpetas;
    }
    @Scheduled(fixedDelayString="${decokasa.google.drive.worker.intervalo-ms:30000}")
    public void ejecutar() {
        org.slf4j.MDC.put("corr","tarea-"+Long.toHexString(System.nanoTime() & 0xffffffL));
        long inicio=System.nanoTime();
        int carpetasProcesadas=0;
        int copiasProcesadas=0;
        try {
            try { carpetasProcesadas=carpetas.ejecutar(); }
            catch (RuntimeException error) {
                // No adjuntar throwable: excepciones JDBC/HTTP podrían incluir metadatos o datos privados.
                LOG.warn("No se completó la pasada de carpetas de pedidos en Drive; las intenciones quedan pendientes. tipo={}",
                        error.getClass().getSimpleName());
            }
            try { copiasProcesadas=copias.ejecutar(); }
            catch (RuntimeException error) {
                // No adjuntar throwable: excepciones JDBC/HTTP podrían incluir metadatos o una sesión privada.
                LOG.warn("No se completó la pasada de copias Drive; los leases y archivos quedan para recuperación. tipo={}",
                        error.getClass().getSimpleName());
            }
            long ms=(System.nanoTime()-inicio)/1_000_000;
            if (carpetasProcesadas>0 || copiasProcesadas>0) {
                LOG.info("tarea.drive carpetas={} copias={} ms={}",carpetasProcesadas,copiasProcesadas,ms);
            } else {
                LOG.debug("tarea.drive sin pendientes ms={}",ms);
            }
        } finally {
            org.slf4j.MDC.remove("corr");
        }
    }
}
