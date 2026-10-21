package com.decokasa.seguimiento.pedidos.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.UsuarioResumenRespuesta;
import com.decokasa.seguimiento.pedidos.application.port.in.VistasDePedido;
import java.util.List;
import java.util.UUID;

/** Esquemas PedidoResumen, PedidoDetalle, EtapaEnLinea, AccionDisponible y PaginaPedidos del contrato 0.7.0. */
public final class PedidoRespuestas {

    private PedidoRespuestas() {
    }

    public record Urgencia(String nivel, int horas, String activadaEn, String venceEn, boolean vencida) {
    }

    public record Resumen(UUID id, String numero, String nombre, String pais, String tipoFlujo, int etapaActual,
            String nombreEtapaActual, UsuarioResumenRespuesta responsableActual, String venceEn, boolean vencida,
            String motivoRetraso, Urgencia urgencia, boolean finalizado) {
    }

    public record Etapa(int numero, String nombre, String estado, String rolResponsable, String estadoPlazo,
            UsuarioResumenRespuesta asignadoA, Boolean asignacionVigente, UUID entradaId, String entrada,
            String salida, String venceEn, int reentradas, String resumen) {
    }

    public record AccionDisponible(String accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, String requisitosDocumentales,
            List<String> documentosObligatorios, List<String> documentosFaltantes, boolean puedeCompletarse) {
    }

    /** Esquema Bodega: la asignada al pedido, aunque después se haya desactivado. */
    public record Bodega(UUID id, String nombre, String ciudad, boolean activa, long version) {
    }

    /** Esquema DemoraAduana (F4-D5): la fecha es una estimación de llegada, no un vencimiento. */
    public record DemoraAduana(UsuarioResumenRespuesta reportadaPor, String reportadaEn, String motivo,
            String nuevaFechaEstimadaLlegada) {
    }

    /** PedidoDetalle: PedidoResumen más su detalle. La bodega es nula hasta aprobar Bodega (F4-D2). */
    public record Detalle(UUID id, String numero, String nombre, String pais, String tipoFlujo, int etapaActual,
            String nombreEtapaActual, UsuarioResumenRespuesta responsableActual, String venceEn, boolean vencida,
            String motivoRetraso, Urgencia urgencia, boolean finalizado, String descripcion,
            boolean incluyeProductosNuevos, UsuarioResumenRespuesta creadoPor, String creadoEn, int versionFlujo,
            Bodega bodega, DemoraAduana demoraAduana, String llegadaAduanaEn, List<Etapa> lineaDeTiempo,
            List<AccionDisponible> accionesDisponibles) {
    }

    public record Pagina(List<Resumen> elementos, int pagina, int tamano, long total) {
    }

    static Pagina de(VistasDePedido.Pagina pagina) {
        return new Pagina(pagina.elementos().stream().map(PedidoRespuestas::de).toList(), pagina.pagina(),
                pagina.tamano(), pagina.total());
    }

    static Resumen de(VistasDePedido.Resumen resumen) {
        return new Resumen(resumen.id(), resumen.numero(), resumen.nombre(), resumen.pais().name(),
                resumen.tipoFlujo().name(), resumen.etapaActual(), resumen.nombreEtapaActual(),
                persona(resumen.responsableActual()), FechasWeb.texto(resumen.venceEn()), resumen.vencida(), resumen.motivoRetraso(),
                urgencia(resumen.urgencia()), resumen.finalizado());
    }

    static Detalle de(VistasDePedido.Detalle detalle) {
        VistasDePedido.Resumen r = detalle.resumen();
        return new Detalle(r.id(), r.numero(), r.nombre(), r.pais().name(), r.tipoFlujo().name(), r.etapaActual(),
                r.nombreEtapaActual(), persona(r.responsableActual()), FechasWeb.texto(r.venceEn()), r.vencida(), r.motivoRetraso(),
                urgencia(r.urgencia()), r.finalizado(), detalle.descripcion(), detalle.incluyeProductosNuevos(),
                persona(detalle.creadoPor()), FechasWeb.texto(detalle.creadoEn()), detalle.versionFlujo(),
                bodega(detalle.bodega()), demora(detalle.demoraAduana()), FechasWeb.texto(detalle.llegadaAduanaEn()),
                detalle.lineaDeTiempo().stream().map(etapa -> etapa(r.tipoFlujo().name(), etapa)).toList(),
                detalle.accionesDisponibles().stream().map(PedidoRespuestas::accion).toList());
    }

    private static Etapa etapa(String tipoFlujo, VistasDePedido.Etapa etapa) {
        return new Etapa(etapa.numero(), etapa.nombre(), etapa.estado().name(),
                etapa.rolResponsable() == null ? null : etapa.rolResponsable().valor(), etapa.estadoPlazo().name(),
                persona(etapa.asignadoA()), etapa.asignacionVigente(), etapa.entradaId(),
                FechasWeb.texto(etapa.entrada()), FechasWeb.texto(etapa.salida()), FechasWeb.texto(etapa.venceEn()),
                etapa.reentradas(), ResumenDeEtapa.texto(tipoFlujo, etapa.estado(), etapa.ultimoCierre()));
    }

    private static AccionDisponible accion(VistasDePedido.AccionDisponible accion) {
        return new AccionDisponible(accion.accion().name(), accion.etapaDestino(), accion.requiereObservacion(),
                accion.requiereUrgencia(), accion.requiereBodega(),
                accion.documentosPendientes() ? "PENDIENTES" : "DEFINIDOS",
                accion.documentosObligatorios(), accion.documentosFaltantes(), accion.puedeCompletarse());
    }

    private static Bodega bodega(VistasDePedido.Bodega bodega) {
        return bodega == null ? null
                : new Bodega(bodega.id(), bodega.nombre(), bodega.ciudad(), bodega.activa(), bodega.version());
    }

    private static DemoraAduana demora(VistasDePedido.Demora demora) {
        return demora == null ? null : new DemoraAduana(persona(demora.reportadaPor()),
                FechasWeb.texto(demora.reportadaEn()), demora.motivo(), demora.nuevaFechaEstimadaLlegada().toString());
    }

    private static Urgencia urgencia(VistasDePedido.Urgencia urgencia) {
        return urgencia == null ? null : new Urgencia(urgencia.nivel().name(), urgencia.nivel().horas(),
                FechasWeb.texto(urgencia.activadaEn()), FechasWeb.texto(urgencia.venceEn()), urgencia.vencida());
    }

    private static UsuarioResumenRespuesta persona(VistasDePedido.Persona persona) {
        return persona == null ? null : UsuarioResumenRespuesta.de(persona.id(), persona.nombre(), persona.roles());
    }
}
