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
    /** Bandeja de trabajo (P-06) con los filtros del dashboard (fase 5, paso 5.3). */
    VistasDePedido.Pagina bandeja(Solicitante solicitante, FiltroBandeja filtro, int pagina, int tamano);

    /** Bandeja sin filtros. */
    default VistasDePedido.Pagina bandeja(Solicitante solicitante, int pagina, int tamano) {
        return bandeja(solicitante, FiltroBandeja.SIN_FILTRO, pagina, tamano);
    }

    /**
     * Filtros de la bandeja (paso 5.3). Solo {@code true} filtra; nulo o {@code false} no.
     *
     * @param vencido     solo con el plazo de la etapa actual vencido (el mismo criterio que {@code vencida})
     * @param conUrgencia solo con una urgencia vigente, vencida o no
     */
    record FiltroBandeja(Boolean vencido, Boolean conUrgencia) {
        public static final FiltroBandeja SIN_FILTRO = new FiltroBandeja(null, null);
    }

    /** Cada campo nulo no filtra. */
    record Filtro(TipoFlujo tipoFlujo, Integer etapa, Boolean vencido, String texto) {
    }
}
