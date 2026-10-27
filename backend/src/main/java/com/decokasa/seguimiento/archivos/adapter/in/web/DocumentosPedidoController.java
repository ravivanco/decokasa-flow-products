package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarDocumentosDelPedido;
import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.UsuarioResumenRespuesta;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Documentos del pedido vistos desde DECOKASA (ADR-0028, esquema DocumentoPedido). Sin IDs ni enlaces de Drive, clave
 * local ni huella: el contenido se abre por {@code /archivos/{id}/contenido}, que vuelve a autorizar.
 */
@RestController
public class DocumentosPedidoController {
    private final ConsultarDocumentosDelPedido consultar;

    public DocumentosPedidoController(ConsultarDocumentosDelPedido consultar) { this.consultar = consultar; }

    @GetMapping("/api/v1/pedidos/{pedidoId}/documentos")
    public ResponseEntity<List<DocumentoPedido>> listar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(consultar.listar(SolicitantesWeb.de(token), pedidoId).stream().map(DocumentoPedido::de).toList());
    }

    public record DocumentoPedido(UUID id, UUID pedidoId, int etapa, UUID entradaId, String entradaEn, String nombre,
            String tipoDocumento,
            String tipoContenido, long tamanoBytes, UsuarioResumenRespuesta subidoPor, String subidoEn,
            String estadoCopia, String disponibilidad, boolean puedeAbrir, boolean vigente,
            List<Presentacion> presentaciones) {
        static DocumentoPedido de(ConsultarDocumentosDelPedido.Vista vista) {
            DocumentosDePedido.Documento d = vista.documento();
            return new DocumentoPedido(d.id(), d.pedidoId(), d.etapa(), d.entradaId(), FechasWeb.texto(d.entradaEn()),
                    d.nombre(), d.tipoDocumento(),
                    d.tipoDetectado(), d.tamanoBytes(),
                    UsuarioResumenRespuesta.de(vista.autor().id(), vista.autor().nombre(), vista.autor().roles()),
                    FechasWeb.texto(d.subidoEn()), d.estadoCopia(), vista.disponibilidad().name(), vista.puedeAbrir(),
                    vista.vigente(),
                    d.presentaciones().stream().map(p -> new Presentacion(p.eventoId(), p.tipo(), p.etapa(),
                            FechasWeb.texto(p.fecha()))).toList());
        }
    }

    public record Presentacion(UUID eventoId, String tipo, int etapa, String fecha) { }
}
