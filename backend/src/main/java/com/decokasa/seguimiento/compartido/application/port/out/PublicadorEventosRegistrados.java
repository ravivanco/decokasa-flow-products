package com.decokasa.seguimiento.compartido.application.port.out;

import com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado;

/** Publicación síncrona que participa en la transacción del historial; no hay envío al proveedor aquí. */
public interface PublicadorEventosRegistrados {
    void publicar(EventoPedidoRegistrado evento);
}
