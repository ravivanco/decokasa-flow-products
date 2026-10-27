package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.UsuarioResumenRespuesta;
import java.util.UUID;

/**
 * Esquema Adjunto del contrato 0.7.0: metadatos medidos por el servidor; sin la clave de almacenamiento, la huella
 * ni el tipo detectado, que son internos.
 */
public record AdjuntoRespuesta(UUID id, UUID pedidoId, int etapa, UUID entradaId, UUID eventoId, String nombre,
        String tipoDocumento, long tamanoBytes, UsuarioResumenRespuesta subidoPor, String subidoEn,
        String estadoCopia) {

    static AdjuntoRespuesta de(AdjuntoVista vista) {
        Adjunto adjunto = vista.adjunto();
        AdjuntoVista.Persona persona = vista.subidoPor();
        return new AdjuntoRespuesta(adjunto.id(), adjunto.pedidoId(), adjunto.etapa(), adjunto.entradaId(),
                vista.eventoId(), adjunto.nombre(), adjunto.tipoDocumento(), adjunto.tamanoBytes(),
                UsuarioResumenRespuesta.de(persona.id(), persona.nombre(), persona.roles()),
                FechasWeb.texto(adjunto.subidoEn()), adjunto.estadoCopia().name());
    }
}
