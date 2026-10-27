package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Documentos de un pedido para consultarlos desde DECOKASA (ADR-0028): quien puede ver el pedido (P-13) los ve todos;
 * {@code puedeAbrir} dice si además puede abrirlos (P-17 ampliada), y se vuelve a comprobar al abrir.
 */
public interface ConsultarDocumentosDelPedido {

    List<Vista> listar(Solicitante solicitante, UUID pedidoId);

    enum Disponibilidad { LOCAL, DRIVE, NO_DISPONIBLE }

    record Persona(UUID id, String nombre, Set<CodigoRol> roles) { }

    /** @param vigente lo entregó a la etapa actual la última transición (ADR-0029) */
    record Vista(DocumentosDePedido.Documento documento, Persona autor, Disponibilidad disponibilidad,
            boolean puedeAbrir, boolean vigente) { }
}
