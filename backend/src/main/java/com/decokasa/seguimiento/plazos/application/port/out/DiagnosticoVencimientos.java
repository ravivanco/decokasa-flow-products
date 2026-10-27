package com.decokasa.seguimiento.plazos.application.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Diagnóstico de la detección de vencimientos (cierre de la parte A de la fase 5; hallazgo de la revisión de 5.2).
 * Cada fallo queda registrado con identificadores técnicos (pasada y pedido), una categoría y un código estable y la
 * hora del servidor. El adaptador decide cómo clasificar el error sin leer su mensaje: nunca registra motivos escritos
 * por usuarios, correos, tokens, credenciales ni parámetros SQL. Registrar no debe interrumpir la pasada.
 */
public interface DiagnosticoVencimientos {

    /** Falló el registro de un pedido; su transacción se revirtió y vuelve a ser candidato en la pasada siguiente. */
    void falloDePedido(UUID pasadaId, UUID pedidoId, Instant ocurridoEn, RuntimeException error);

    /** Falló la lectura de candidatos: la pasada no procesó ningún pedido. */
    void falloDePasada(UUID pasadaId, Instant ocurridoEn, RuntimeException error);
}
