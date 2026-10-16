package com.decokasa.seguimiento.pedidos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import java.util.UUID;

/**
 * Consultas de pedidos (contrato: GET /pedidos, /pedidos/buscar, /pedidos/bandeja y /pedidos/{id}). P-13, provisional
 * del equipo para desarrollo con datos ficticios: cualquier persona activa con al menos un rol vigente ve todos los
 * pedidos; una cuenta inactiva o sin roles recibe 403. Se decide con el estado en la base, no con el token.
 */
public interface ConsultarPedidos {

    VistasDePedido.Detalle detalle(Solicitante solicitante, UUID pedidoId);

    VistasDePedido.Detalle porNumero(Solicitante solicitante, String numero);

    VistasDePedido.Pagina listar(Solicitante solicitante, Filtro filtro, int pagina, int tamano);

    /** Filtra el trabajo según P-06; no cambia la visibilidad general de P-13. */
    VistasDePedido.Pagina bandeja(Solicitante solicitante, int pagina, int tamano);

    /** Cada campo nulo no filtra. */
    record Filtro(TipoFlujo tipoFlujo, Integer etapa, Boolean vencido, String texto) {
    }
}
