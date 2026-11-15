package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Documentos confidenciales (V37): los archivos de los ítems del checklist de documentos (Aduana) los ven solo Admin,
 * Compras China y Asistente de Compras, y quien Admin autorice en ese pedido. Toda salida de documentos (lista, archivos
 * de la etapa, descarga, checklist y asistente de IA) aplica esta misma regla en el servidor.
 */
public interface AccesoConfidencial {

    /** Roles que ven los documentos confidenciales sin un acceso otorgado. */
    Set<String> ROLES = Set.of("admin", "compras_china", "asistente_compras");

    /** Adjuntos confidenciales del pedido. */
    Set<UUID> confidenciales(UUID pedidoId);

    /** La persona puede ver los documentos confidenciales de ese pedido (por su rol vigente o por un acceso otorgado). */
    boolean puedeVer(UUID pedidoId, UUID usuarioId);

    /** Quién los ve hoy en el pedido. Solo Admin. */
    List<ConAcceso> conAcceso(Solicitante solicitante, UUID pedidoId);

    /** Cuántos documentos confidenciales hay por etapa y si quien pregunta los puede ver (con el país del pedido). */
    Resumen resumen(Solicitante solicitante, UUID pedidoId);

    /** Solo Admin. Sin efecto si ya tenía acceso. */
    void otorgar(Solicitante solicitante, UUID pedidoId, UUID usuarioId);

    /** Solo Admin. Quita un acceso otorgado; el acceso por rol no se quita aquí. */
    void revocar(Solicitante solicitante, UUID pedidoId, UUID usuarioId);

    /**
     * @param porRol lo ve por su rol (no se puede quitar desde el pedido)
     * @param otorgadoPor nombre de quien dio el acceso; nulo si es por rol
     */
    record ConAcceso(UUID usuarioId, String nombre, boolean porRol, String otorgadoPor, Instant otorgadoEn) {
    }

    record Resumen(boolean puedeVer, List<PorEtapa> etapas) {
    }

    record PorEtapa(int etapa, int cantidad) {
    }
}
