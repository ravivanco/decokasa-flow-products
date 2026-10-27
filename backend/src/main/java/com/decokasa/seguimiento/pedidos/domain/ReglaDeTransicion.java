package com.decokasa.seguimiento.pedidos.domain;

import com.decokasa.seguimiento.compartido.domain.AccionEtapa;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.PlazoDeEtapa;
import java.util.Objects;
import java.util.Set;

/**
 * Lo que la definición del flujo dice de una acción en la etapa actual. El caso de uso (paso 2.4) la arma con la
 * respuesta del puerto ResolverTransicion de {@code flujos}; el dominio de pedidos no conoce el de flujos (R4).
 *
 * @param etapaDestino nulo si el pedido se queda en la etapa
 * @param documentosPendientes el cliente no definió los documentos obligatorios: la acción no se puede ejecutar
 * @param plazoDestino plazo de la etapa de destino; nulo solo si no hay destino
 * @param destinoEsRetornoDeUrgencia al entrar en el destino termina la urgencia (A4 / B5)
 * @param rolResponsableOrigen rol responsable de la etapa actual; nulo si es automática y nadie actúa en ella
 * @param registraLlegada al salir de la etapa queda registrada la llegada a aduana (A7 → A8, "✓ Llegada"); la etapa
 *        de llegada está marcada en la definición del flujo (ADR-0018)
 * @param destinoEsAutomatico el destino es Fin automático según la definición versionada del flujo
 * @param exigeAlgunDocumento hace falta al menos un documento de la entrada actual, de cualquier tipo (A9 y A10, V16)
 */
public record ReglaDeTransicion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino,
        boolean requiereObservacion, boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
        Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
        CodigoRol rolResponsableOrigen, boolean registraLlegada, boolean destinoEsAutomatico,
        boolean exigeAlgunDocumento) {

    /** Texto del requisito faltante cuando la acción exige al menos un documento y no se adjuntó ninguno. */
    public static final String FALTA_ALGUN_DOCUMENTO = "al menos un documento";

    /** Compatibilidad con reglas anteriores a V16: solo los tipos de {@code documentosObligatorios}. */
    public ReglaDeTransicion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
            Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
            CodigoRol rolResponsableOrigen, boolean registraLlegada, boolean destinoEsAutomatico) {
        this(etapaOrigen, accion, etapaDestino, requiereObservacion, requiereUrgencia, requiereBodega,
                documentosPendientes, documentosObligatorios, plazoDestino, destinoEsRetornoDeUrgencia,
                rolResponsableOrigen, registraLlegada, destinoEsAutomatico, false);
    }

    /** Compatibilidad con reglas anteriores al cierre automático. */
    public ReglaDeTransicion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
            Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
            CodigoRol rolResponsableOrigen, boolean registraLlegada) {
        this(etapaOrigen, accion, etapaDestino, requiereObservacion, requiereUrgencia, requiereBodega,
                documentosPendientes, documentosObligatorios, plazoDestino, destinoEsRetornoDeUrgencia,
                rolResponsableOrigen, registraLlegada, false);
    }

    /** Una acción que no registra la llegada a aduana. */
    public ReglaDeTransicion(int etapaOrigen, AccionEtapa accion, Integer etapaDestino, boolean requiereObservacion,
            boolean requiereUrgencia, boolean requiereBodega, boolean documentosPendientes,
            Set<String> documentosObligatorios, PlazoDeEtapa plazoDestino, boolean destinoEsRetornoDeUrgencia,
            CodigoRol rolResponsableOrigen) {
        this(etapaOrigen, accion, etapaDestino, requiereObservacion, requiereUrgencia, requiereBodega,
                documentosPendientes, documentosObligatorios, plazoDestino, destinoEsRetornoDeUrgencia,
                rolResponsableOrigen, false);
    }

    public ReglaDeTransicion {
        Objects.requireNonNull(accion, "accion");
        documentosObligatorios = Set.copyOf(documentosObligatorios);
        if (etapaDestino != null) {
            Objects.requireNonNull(plazoDestino, "plazoDestino");
        }
        if (registraLlegada && (etapaDestino == null || etapaDestino <= etapaOrigen)) {
            throw new IllegalArgumentException("Solo una acción que avanza el pedido registra la llegada.");
        }
        if (destinoEsAutomatico && (etapaDestino == null || etapaDestino <= etapaOrigen)) {
            throw new IllegalArgumentException("Solo una acción que avanza puede entrar en Fin automático.");
        }
    }
}
