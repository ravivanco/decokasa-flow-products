package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
public interface ConsultaCopias {
    Optional<String> enlaceConfirmado(UUID adjuntoId);
    /**
     * Copias fallidas de archivos de pedidos de esos países (los asignados hoy al admin; fase 7, ADR-0024), filtradas en
     * la consulta. Un conjunto vacío no devuelve nada. No cambia ni reasigna ningún registro.
     */
    List<UUID> fallidos(Set<CodigoPais> paises,int pagina,int tamano);
}
