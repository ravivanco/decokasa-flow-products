package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.time.Instant;
import java.util.UUID;

/** Datos inmutables de un pedido aceptado; lo implementa pedidos, sin dependencia inversa. */
public interface PedidosParaCopias {
    Destino destino(UUID pedidoId);

    /**
     * @param creadoEn creación del pedido en la base: fija su carpeta de año, mes y día en la zona de su país
     *        (ADR-0026); nulo solo con el constructor de compatibilidad
     */
    record Destino(CodigoPais pais, String numero, Instant creadoEn) {
        /** Sin fecha: pruebas anteriores al 2026-10-24 (organizaciones anteriores a ADR-0026). */
        public Destino(CodigoPais pais, String numero) { this(pais, numero, null); }
    }
}
