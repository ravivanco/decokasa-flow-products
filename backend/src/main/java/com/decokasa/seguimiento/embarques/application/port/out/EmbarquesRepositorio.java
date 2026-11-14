package com.decokasa.seguimiento.embarques.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import com.decokasa.seguimiento.embarques.domain.CodigoBl;
import com.decokasa.seguimiento.embarques.domain.LlegadaEstimada;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Persistencia de BL, Packing Lists y envíos. Siempre dentro de la transacción del caso de uso. */
public interface EmbarquesRepositorio {

    /** Lo necesario para decidir sobre un BL. @param packingListVigente nulo si no tiene */
    record FilaBl(UUID id, CodigoPais pais, String codigo, LocalDate fechaSalida, LocalDate llegadaEstimada,
            String llegadaOrigen, boolean bloqueado, long version, UUID packingListVigente,
            Integer versionPackingList, String importadora) {
    }

    record AdjuntoDelPedido(UUID id, UUID pedidoId, int etapa, UUID entradaId, String tipoDocumento, String nombre) {
    }

    record PackingListDelBl(UUID id, UUID blId, int version, UUID adjuntoId, UUID pedidoDelArchivo, int etapaDelArchivo) {
    }

    /** Fila de envío pendiente. */
    record EnvioPendiente(UUID id, int numero, UUID enviadoPor) {
    }

    /** BL del envío pendiente cuya versión vigente nadie revisó todavía. */
    record RevisionPendiente(UUID blId, String codigo, UUID packingListVigente, int versionVigente) {
    }

    Optional<FilaBl> blParaActualizar(UUID blId);

    Optional<FilaBl> blPorCodigo(CodigoPais pais, String codigoNormalizado);

    /** @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado si el código ya existe en el país */
    void insertarBl(UUID id, CodigoPais pais, CodigoBl codigo, LocalDate salida, LlegadaEstimada llegada,
            String importadora, UUID autor, Instant ahora);

    /** @return falso si la versión ya no es la vigente */
    boolean actualizarBl(UUID id, long versionEsperada, LocalDate salida, LlegadaEstimada llegada, String importadora,
            Instant ahora);

    boolean vinculado(UUID blId, UUID pedidoId);

    List<UUID> pedidosDelBl(UUID blId);

    void vincular(UUID blId, UUID pedidoId, UUID autor, Instant ahora);

    boolean desvincular(UUID blId, UUID pedidoId, UUID autor, Instant ahora);

    Optional<AdjuntoDelPedido> adjunto(UUID adjuntoId);

    /** Versión ya registrada para ese adjunto en ese BL. */
    Optional<Integer> versionDeAdjunto(UUID blId, UUID adjuntoId);

    int registrarPackingList(UUID blId, UUID adjuntoId, UUID autor, Instant ahora);

    Optional<PackingListDelBl> packingList(UUID packingListId);

    /** El BL está vinculado al pedido o formó parte de alguno de sus envíos. */
    boolean relacionado(UUID blId, UUID pedidoId);

    List<GestionarEmbarques.Bl> blsDelPedido(UUID pedidoId, boolean conRevisionPendiente);

    List<GestionarEmbarques.Envio> enviosDelPedido(UUID pedidoId);

    Optional<EnvioPendiente> envioPendiente(UUID pedidoId);

    /** Pedidos con un envío pendiente que incluye el BL con otra versión del Packing List. */
    List<UUID> pedidosARevisar(UUID blId, UUID packingListVigente);

    int registrarEnvio(UUID pedidoId, UUID entradaId, UUID autor, Instant ahora);

    List<RevisionPendiente> revisionesPendientes(UUID envioId);

    void marcarRevisada(UUID envioId, RevisionPendiente revision, UUID autor, Instant ahora);

    void registrarDecision(UUID envioId, String decision, UUID usuario, String motivo, List<UUID> blSenalados,
            Instant ahora);

    /** Bloquea los BL del envío: la versión aprobada ya no se sustituye. */
    void bloquearBls(UUID envioId, Instant ahora);

    /** Códigos de BL del envío que están entre los indicados; sirve para validar y para el historial. */
    List<String> codigosDelEnvio(UUID envioId, List<UUID> blIds);

    GestionarEmbarques.Checklist checklist(UUID flujoId, UUID pedidoId, int etapa, UUID entradaId);

    /** Guarda una marca del checklist; las marcas son de solo inserción y la última es la vigente. */
    void marcarRequisito(UUID id, UUID pedidoId, UUID entradaId, String tipoDocumento, boolean marcado,
            UUID usuarioId, Instant ahora);

    /** Tipos marcados ✓ hoy en la entrada. */
    Set<String> tiposMarcados(UUID entradaId);

    List<GestionarEmbarques.AlertaSimulada> alertasSimuladas(UUID pedidoId);
}
