package com.decokasa.seguimiento.asistente.adapter.in.web;

import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConversaciones;
import com.decokasa.seguimiento.asistente.application.port.in.GestionarConfiguracionIA;
import com.decokasa.seguimiento.asistente.application.port.in.Preguntar;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA.TarifaRegistrada;
import com.decokasa.seguimiento.asistente.domain.LimitesIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.asistente.domain.DocumentoExcluido;
import com.decokasa.seguimiento.asistente.domain.Referencia;
import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Esquemas del contrato 0.16.0 (etiqueta asistente). Sin modelo, claves ni detalles del proveedor. */
public final class AsistenteRespuestas {

    private AsistenteRespuestas() {
    }

    public record Conversacion(UUID id, UUID pedidoId, String numeroPedido, String creadaEn, String expiraEn,
            int preguntasRestantes) {
    }

    public record Pagina(List<Conversacion> elementos, int pagina, int tamano, long total) {
    }

    public record Detalle(UUID id, UUID pedidoId, String numeroPedido, String creadaEn, String expiraEn,
            int preguntasRestantes, List<Mensaje> mensajes) {
    }

    /** Un mensaje retenido omite texto, evidencia, referencias y documentos excluidos (no se envían como null). */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Mensaje(int orden, String autor, String creadoEn, boolean retenido, String texto,
            Boolean evidenciaSuficiente, List<ReferenciaRespuesta> referencias,
            List<ExcluidoRespuesta> documentosExcluidos) {

        @Override
        public String toString() {
            return "Mensaje[orden=" + orden + ", retenido=" + retenido + "]";
        }
    }

    public record ReferenciaRespuesta(String numeroPedido, Integer etapa, UUID archivoId, String nombreArchivo,
            Integer pagina, String textoCitado) {

        @Override
        public String toString() {
            return "ReferenciaRespuesta[archivoId=" + archivoId + ", pagina=" + pagina + "]";
        }
    }

    public record ExcluidoRespuesta(UUID archivoId, String nombreArchivo, String motivo) {

        @Override
        public String toString() {
            return "ExcluidoRespuesta[archivoId=" + archivoId + ", motivo=" + motivo + "]";
        }
    }

    public record Respuesta(String texto, boolean evidenciaSuficiente, List<ReferenciaRespuesta> referencias,
            List<ExcluidoRespuesta> documentosExcluidos, int preguntasRestantes) {

        @Override
        public String toString() {
            return "Respuesta[evidencia=" + evidenciaSuficiente + "]";
        }
    }

    public record DiaConsumo(String fecha, UUID usuarioId, long consultas, long tokensEntrada, long tokensSalida,
            String costoUsd) {
    }

    /** Importes en USD como texto decimal con 6 decimales (micro-USD), para no perder precisión. */
    public record Consumo(String presupuestoMesUsd, String consumidoMesUsd, String reservadoMesUsd,
            List<DiaConsumo> dias) {
    }

    public record Precios(long entradaMicroUsdMtok, long salidaMicroUsdMtok, long escrituraCacheMicroUsdMtok,
            long lecturaCacheMicroUsdMtok) {
    }

    public record TarifaRespuesta(UUID id, String modelo, String vigenteDesde, Precios base,
            Integer umbralTokensEntrada, Precios superior, String fuente, String consultadoEn, boolean validada,
            UUID registradaPor, String registradaEn, UUID validadaPor, String validadaEn) {
    }

    public record Limites(int tokensEntradaMax, int tokensSalidaMax, int preguntasPorConversacion,
            int consultasUsuarioDia, int enCursoUsuario, int consultasDia, String presupuestoMesUsd,
            long esperaReservaSegundos) {
    }

    /** Estado del asistente para el admin. Sin la clave: solo si el entorno está habilitado. */
    public record Configuracion(boolean habilitada, String motivoDeshabilitada, boolean entornoHabilitado,
            boolean pilotoFicticio, String modelo, boolean disponible, long version, Limites limites,
            TarifaRespuesta tarifa) {
    }

    static Respuesta de(Preguntar.Resultado resultado) {
        return new Respuesta(resultado.texto(), resultado.evidenciaSuficiente(),
                resultado.referencias().stream().map(AsistenteRespuestas::referencia).toList(),
                resultado.documentosExcluidos().stream().map(AsistenteRespuestas::excluido).toList(),
                resultado.preguntasRestantes());
    }

    static Consumo de(ConsultarConsumoIA.Consumo consumo) {
        return new Consumo(usd(consumo.presupuestoMesMicroUsd()), usd(consumo.consumidoMesMicroUsd()),
                usd(consumo.reservadoMesMicroUsd()), consumo.dias().stream()
                        .map(dia -> new DiaConsumo(dia.fecha().toString(), dia.usuarioId(), dia.consultas(),
                                dia.tokensEntrada(), dia.tokensSalida(), usd(dia.costoMicroUsd())))
                        .toList());
    }

    static TarifaRespuesta de(TarifaRegistrada registrada) {
        Tarifa tarifa = registrada.tarifa();
        return new TarifaRespuesta(tarifa.id(), tarifa.modelo(), tarifa.vigenteDesde().toString(),
                precios(tarifa.base()), tarifa.umbralTokensEntrada(),
                tarifa.superior() == null ? null : precios(tarifa.superior()), tarifa.fuente(),
                tarifa.consultadoEn().toString(), tarifa.validada(), registrada.registradaPor(),
                fecha(registrada.registradaEn()), registrada.validadaPor(), fecha(registrada.validadaEn()));
    }

    static Configuracion de(GestionarConfiguracionIA.Estado estado) {
        LimitesIA limites = estado.limites();
        return new Configuracion(limites.habilitada(), limites.motivoDeshabilitada(), estado.entornoHabilitado(),
                estado.pilotoFicticio(), estado.modelo(), estado.disponible(), limites.version(),
                new Limites(limites.tokensEntradaMax(), limites.tokensSalidaMax(), limites.preguntasPorConversacion(),
                        limites.consultasUsuarioDia(), limites.enCursoUsuario(), limites.consultasDia(),
                        usd(limites.presupuestoMesMicroUsd()), limites.esperaReserva().toSeconds()),
                estado.tarifa() == null ? null : de(estado.tarifa()));
    }

    private static Precios precios(Tarifa.Precios precios) {
        return new Precios(precios.entrada(), precios.salida(), precios.escrituraCache(), precios.lecturaCache());
    }

    private static String fecha(Instant instante) {
        return instante == null ? null : FechasWeb.texto(instante);
    }

    static String usd(long microUsd) {
        return BigDecimal.valueOf(microUsd, 6).toPlainString();
    }

    static Conversacion de(ConversacionIA conversacion) {
        return new Conversacion(conversacion.id(), conversacion.pedidoId(), conversacion.numeroPedido(),
                FechasWeb.texto(conversacion.creadaEn()), FechasWeb.texto(conversacion.expiraEn()),
                conversacion.preguntasRestantes());
    }

    static Pagina de(ConsultarConversaciones.Pagina pagina) {
        return new Pagina(pagina.elementos().stream().map(AsistenteRespuestas::de).toList(), pagina.pagina(),
                pagina.tamano(), pagina.total());
    }

    static Detalle de(ConsultarConversaciones.Detalle detalle) {
        ConversacionIA conversacion = detalle.conversacion();
        return new Detalle(conversacion.id(), conversacion.pedidoId(), conversacion.numeroPedido(),
                FechasWeb.texto(conversacion.creadaEn()), FechasWeb.texto(conversacion.expiraEn()),
                conversacion.preguntasRestantes(),
                detalle.mensajes().stream().map(AsistenteRespuestas::mensaje).toList());
    }

    private static Mensaje mensaje(ConsultarConversaciones.VistaMensaje mensaje) {
        if (mensaje.retenido()) {
            return new Mensaje(mensaje.orden(), mensaje.autor().name(), FechasWeb.texto(mensaje.creadoEn()), true,
                    null, null, null, null);
        }
        return new Mensaje(mensaje.orden(), mensaje.autor().name(), FechasWeb.texto(mensaje.creadoEn()), false,
                mensaje.texto(), mensaje.evidenciaSuficiente(),
                mensaje.referencias().stream().map(AsistenteRespuestas::referencia).toList(),
                mensaje.documentosExcluidos().stream().map(AsistenteRespuestas::excluido).toList());
    }

    private static ReferenciaRespuesta referencia(Referencia referencia) {
        return new ReferenciaRespuesta(referencia.numeroPedido(), referencia.etapa(), referencia.archivoId(),
                referencia.nombreArchivo(), referencia.pagina(), referencia.textoCitado());
    }

    private static ExcluidoRespuesta excluido(DocumentoExcluido excluido) {
        return new ExcluidoRespuesta(excluido.archivoId(), excluido.nombreArchivo(), excluido.motivo().name());
    }
}
