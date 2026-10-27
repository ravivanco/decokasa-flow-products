package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.pedidos.domain.EstadoEtapa;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Lo que muestran las consultas de pedidos (contrato 0.7.0: PedidoResumen, PedidoDetalle y sus partes). */
public final class VistasDePedido {

    private VistasDePedido() {
    }

    /** Contrato: UsuarioResumen. */
    public record Persona(UUID id, String nombre, Set<CodigoRol> roles) {
    }

    public enum EstadoPlazo {
        DEFINIDO,
        SIN_PLAZO,
        PENDIENTE
    }

    /** @param vencida la urgencia sigue vigente y su vencimiento ya pasó (lo decide el servidor, paso 5.3) */
    public record Urgencia(NivelUrgencia nivel, Instant activadaEn, Instant venceEn, boolean vencida) {
    }

    /**
     * @param responsableActual persona asignada a la etapa actual; nula si no hay
     * @param motivoRetraso motivo vigente de la entrada actual (F5-D3, paso 5.2); nulo si no hay
     */
    public record Resumen(UUID id, String numero, String nombre, CodigoPais pais, TipoFlujo tipoFlujo,
            int etapaActual, String nombreEtapaActual, Persona responsableActual, Instant venceEn, boolean vencida,
            Urgencia urgencia, boolean finalizado, String motivoRetraso) {
    }

    /**
     * @param asignacionVigente nulo sin persona asignada; verdadero si puede actuar; falso si ya no puede (P-06)
     * @param entradaId entrada actual (o la última) de la etapa (P-18)
     * @param ultimoCierre lo ocurrido al cerrar la última entrada de la etapa (requisito 4.2 #17); nulo si nunca se
     *        cerró una entrada suya o no hay un evento que lo reconstruya
     */
    public record Etapa(int numero, String nombre, EstadoEtapa estado, CodigoRol rolResponsable,
            EstadoPlazo estadoPlazo, Persona asignadoA, Boolean asignacionVigente, UUID entradaId, Instant entrada,
            Instant salida, Instant venceEn, int reentradas, ResumirEtapasDePedido.Cierre ultimoCierre) {
    }

    /**
     * @param requiereBodega la acción se envía con {@code bodegaId} (APROBAR de Bodega, F4-D2)
     * @param documentosPendientes el cliente no definió los documentos (S-2): no se puede completar
     * @param exigeAlgunDocumento hace falta al menos un documento de la entrada actual, de cualquier tipo (V16)
     */
    public record AccionDisponible(AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
            List<String> documentosObligatorios, List<String> documentosFaltantes, boolean puedeCompletarse,
            boolean exigeAlgunDocumento) {

        /** Compatibilidad con vistas anteriores a V16. */
        public AccionDisponible(AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
                boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
                List<String> documentosObligatorios, List<String> documentosFaltantes, boolean puedeCompletarse) {
            this(accion, etapaDestino, requiereObservacion, requiereUrgencia, requiereBodega, documentosPendientes,
                    documentosObligatorios, documentosFaltantes, puedeCompletarse, false);
        }
    }

    /** Esquema Bodega del contrato: la bodega asignada, aunque después se haya desactivado (F4-D1). */
    public record Bodega(UUID id, String nombre, String ciudad, boolean activa, long version) {
    }

    /**
     * Demora vigente en aduana (F4-D5, provisional). La fecha es una estimación, no un vencimiento; es el dato que usará
     * el cronograma de la fase 5 para recalcular las etapas siguientes (todavía no implementado).
     */
    public record Demora(Persona reportadaPor, Instant reportadaEn, String motivo,
            LocalDate nuevaFechaEstimadaLlegada) {
    }

    /**
     * @param bodega bodega de destino; nula hasta aprobar Bodega (F4-D2)
     * @param demoraAduana nula si no se reportó ninguna demora
     * @param llegadaAduanaEn check de llegada de A7; nulo si todavía no llegó
     */
    public record Detalle(Resumen resumen, String descripcion, boolean incluyeProductosNuevos, Persona creadoPor,
            Instant creadoEn, int versionFlujo, Bodega bodega, Demora demoraAduana, Instant llegadaAduanaEn,
            List<Etapa> lineaDeTiempo, List<AccionDisponible> accionesDisponibles) {
    }

    public record Pagina(List<Resumen> elementos, int pagina, int tamano, long total) {
    }
}
