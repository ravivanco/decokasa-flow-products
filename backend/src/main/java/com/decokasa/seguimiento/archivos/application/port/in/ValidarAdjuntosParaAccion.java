package com.decokasa.seguimiento.archivos.application.port.in;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Valida los {@code adjuntoIds} de una acción (P-05 opción A, P-18). Lo usa {@code pedidos} dentro de la transacción
 * de la acción.
 */
public interface ValidarAdjuntosParaAccion {

    /**
     * Tipos de documento de los adjuntos, si todos son válidos: existen, son del pedido, de la etapa y de la entrada
     * actual, no se repiten y su archivo está en el almacenamiento con el tamaño registrado. Lista vacía: conjunto
     * vacío.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.DatoInvalido en el campo {@code adjuntoIds} si alguno no lo
     *         es; nunca se ignora un identificador inválido
     */
    Set<String> tiposValidos(UUID pedidoId, int etapa, UUID entradaId, List<UUID> adjuntoIds);
}
