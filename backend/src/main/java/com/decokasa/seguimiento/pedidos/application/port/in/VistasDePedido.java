package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido;
import com.decokasa.seguimiento.pedidos.domain.EstadoEtapa;
import java.time.Instant;
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

    public record Urgencia(NivelUrgencia nivel, Instant activadaEn, Instant venceEn) {
    }

    /** @param responsableActual persona asignada a la etapa actual; nula si no hay */
    public record Resumen(UUID id, String numero, String nombre, CodigoPais pais, TipoFlujo tipoFlujo,
            int etapaActual, String nombreEtapaActual, Persona responsableActual, Instant venceEn, boolean vencida,
            Urgencia urgencia, boolean finalizado) {
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

    /** @param documentosPendientes el cliente no definió los documentos (S-2): no se puede completar */
    public record AccionDisponible(AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean documentosPendientes, List<String> documentosObligatorios,
            List<String> documentosFaltantes, boolean puedeCompletarse) {
    }

    public record Detalle(Resumen resumen, String descripcion, boolean incluyeProductosNuevos, Persona creadoPor,
            Instant creadoEn, int versionFlujo, List<Etapa> lineaDeTiempo,
            List<AccionDisponible> accionesDisponibles) {
    }

    public record Pagina(List<Resumen> elementos, int pagina, int tamano, long total) {
    }
}
