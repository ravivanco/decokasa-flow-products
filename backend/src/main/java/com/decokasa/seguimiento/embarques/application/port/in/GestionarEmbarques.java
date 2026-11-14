package com.decokasa.seguimiento.embarques.application.port.in;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * BL, Packing Lists y envíos de un pedido, vistos desde ese pedido. El servidor decide cada permiso con el estado
 * vigente de la persona; la pantalla solo lo refleja.
 */
public interface GestionarEmbarques {

    Vista consultar(Solicitante solicitante, UUID pedidoId);

    Vista crear(Solicitante solicitante, UUID pedidoId, NuevoBl nuevo);

    Vista actualizar(Solicitante solicitante, UUID pedidoId, UUID blId, CambioBl cambio);

    /** Liga al BL un adjunto ya confirmado por el servidor: nace una versión. Repetirlo con el mismo adjunto no duplica. */
    Vista registrarPackingList(Solicitante solicitante, UUID pedidoId, UUID blId, UUID adjuntoId);

    Vista vincularPorCodigo(Solicitante solicitante, UUID pedidoId, String codigo);

    /** Añade otro pedido a un BL de este pedido, incluidos pedidos anteriores en la versión 1 del flujo. */
    Vista anadirPedido(Solicitante solicitante, UUID pedidoId, UUID blId, String numeroPedido);

    Vista desvincular(Solicitante solicitante, UUID pedidoId, UUID blId);

    /** Quien aprueba deja constancia de haber revisado la versión nueva de un Packing List compartido. */
    Vista marcarRevisada(Solicitante solicitante, UUID pedidoId, UUID blId);

    DescargarArchivo.Autorizada packingList(Solicitante solicitante, UUID pedidoId, UUID blId, UUID packingListId);

    Checklist checklist(Solicitante solicitante, UUID pedidoId);

    /**
     * Marca o desmarca un requisito del checklist (V35). Solo quien actúa en la etapa, en la entrada que tiene a la
     * vista. @throws com.decokasa.seguimiento.compartido.domain.EtapaCambio si la etapa avanzó o volvió a entrar
     */
    Checklist marcarRequisito(Solicitante solicitante, UUID pedidoId, String tipoDocumento, boolean marcado,
            UUID entradaEsperadaId);

    List<AlertaSimulada> alertasSimuladas(Solicitante solicitante, UUID pedidoId);

    record NuevoBl(String codigo, LocalDate fechaSalida, LocalDate llegadaEstimada, String importadora) {
    }

    /** @param llegadaOrigen AUTOMATICA vuelve a calcular la llegada; nulo o MANUAL conserva la escrita */
    record CambioBl(long version, LocalDate fechaSalida, LocalDate llegadaEstimada, String llegadaOrigen,
            String importadora) {
    }

    /**
     * @param puedeEditar la persona puede actuar en la etapa de BL de este pedido
     * @param puedeRevisar la persona puede actuar en la aprobación de este pedido
     * @param solicitudes hay documentos de solicitud que comparar (los lista la pantalla de documentos)
     */
    record Vista(boolean gestionaEmbarques, boolean apruebaEmbarques, boolean puedeEditar, boolean puedeRevisar,
            Integer transitoDias, List<Bl> bls, List<Envio> envios) {
    }

    /**
     * @param estado LISTO, SIN_PACKING_LIST o SIN_SALIDA
     * @param enAprobacionEn pedidos con un envío pendiente que incluye este BL
     * @param revisionPendiente hay una versión nueva que revisar en el envío pendiente de este pedido
     * @param aprobadoEnVersion versión aprobada; nula si ningún pedido aprobó este BL
     */
    record Bl(UUID id, String codigo, LocalDate fechaSalida, LocalDate llegadaEstimada, String llegadaOrigen,
            Integer transitoDias, long version, boolean bloqueado, String estado, PackingList packingList,
            List<PackingList> versiones, List<PedidoDelBl> pedidos, List<String> enAprobacionEn,
            boolean revisionPendiente, Integer aprobadoEnVersion, String importadora) {
    }

    record PackingList(UUID id, int version, UUID adjuntoId, String nombre, String subidoPor, Instant subidoEn) {
    }

    record PedidoDelBl(UUID id, String numero, String vinculadoPor, Instant vinculadoEn) {
    }

    record Envio(UUID id, int numero, String estado, String enviadoPor, Instant enviadoEn, Instant cerradoEn,
            List<FilaEnvio> filas, List<Decision> decisiones, List<Revision> revisiones) {
    }

    record FilaEnvio(UUID blId, String codigo, LocalDate fechaSalida, LocalDate llegadaEstimada, String llegadaOrigen,
            UUID packingListId, int version, String nombreArchivo, List<PedidoDelBl> pedidos, String importadora) {
    }

    record Decision(String decision, String usuario, String motivo, List<String> blSenalados, Instant decididoEn) {
    }

    record Revision(UUID blId, String codigo, int version, String revisadoPor, Instant revisadoEn) {
    }

    /**
     * @param pendienteDefinicion la lista de requisitos no está definida: la etapa no se puede aprobar
     * @param confirmados requisitos marcados o con archivo
     * @param puedeMarcar la persona puede marcar y subir archivos hoy
     */
    record Checklist(boolean aplica, boolean pendienteDefinicion, int total, int confirmados,
            List<Requisito> requisitos, UUID entradaId, boolean puedeMarcar) {

        public Checklist conPermiso(boolean puede) {
            return new Checklist(aplica, pendienteDefinicion, total, confirmados, requisitos, entradaId, puede);
        }
    }

    /**
     * Un requisito se cumple con la marca ✓ o con un archivo de ese tipo en la entrada actual (V35).
     * @param adjuntoId archivo confirmado por el servidor en la entrada actual; nulo si no hay
     * @param marcado la última marca de la entrada actual es ✓
     */
    record Requisito(String tipoDocumento, UUID adjuntoId, String nombreArchivo, boolean marcado, String marcadoPor,
            Instant marcadoEn) {

        public boolean cumplido() {
            return marcado || adjuntoId != null;
        }
    }

    record AlertaSimulada(UUID id, String destinatario, String texto, Instant creadoEn) {
    }
}
