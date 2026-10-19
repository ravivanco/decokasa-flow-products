package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.UUID;

/** Datos inmutables de un pedido aceptado; lo implementa pedidos, sin dependencia inversa. */
public interface PedidosParaCopias {
    Destino destino(UUID pedidoId);
    record Destino(CodigoPais pais, String numero) { }
}
