package com.decokasa.seguimiento.pedidos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.TipoFlujo;
import com.decokasa.seguimiento.pedidos.domain.Pedido;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Lecturas de pedidos sin bloqueo, para consultas y listados. */
public interface ConsultaDePedidos {

    Optional<Pedido> porId(UUID pedidoId);

    Optional<Pedido> porNumero(String numero);

    /**
     * Del más reciente al más antiguo por fecha de creación (nunca por el texto del número). Cada filtro nulo no
     * filtra.
     *
     * @param vencido si la etapa actual de un pedido en curso venció antes de {@code ahora}
     * @param texto busca en el número y el nombre, sin distinguir mayúsculas
     */
    Pagina listar(TipoFlujo tipoFlujo, Integer etapa, Boolean vencido, String texto, Instant ahora, int pagina,
            int tamano);

    /**
     * Trabajo pendiente según P-06: pedidos en curso cuya etapa actual está asignada a la persona (y tiene el rol
     * responsable), o no tiene asignación y la persona tiene el rol responsable. Con {@code admin}, todos los pedidos
     * en curso.
     */
    /**
     * @param soloVencidos    solo pedidos con el plazo de la etapa actual vencido antes de {@code ahora}
     * @param soloConUrgencia solo pedidos con urgencia vigente
     */
    Pagina bandeja(UUID usuarioId, Set<CodigoRol> roles, boolean admin, boolean soloVencidos,
            boolean soloConUrgencia, Instant ahora, int pagina, int tamano);

    record Pagina(List<Pedido> pedidos, long total) {
    }
}
