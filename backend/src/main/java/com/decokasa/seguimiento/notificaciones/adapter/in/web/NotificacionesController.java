package com.decokasa.seguimiento.notificaciones.adapter.in.web;

import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarNotificaciones;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class NotificacionesController {
    private final ConsultarNotificaciones consultar;
    public NotificacionesController(ConsultarNotificaciones consultar) { this.consultar=consultar; }
    @GetMapping("/api/v1/notificaciones")
    public ResponseEntity<Pagina> listar(@AuthenticationPrincipal Jwt token,
            @RequestParam(required=false) Notificacion.Estado estado,
            @RequestParam(required=false) CodigoPais pais,
            @RequestParam(defaultValue="0") int pagina,@RequestParam(defaultValue="20") int tamano) {
        var p=consultar.consultar(SolicitantesWeb.de(token),pais,estado,pagina,tamano);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(new Pagina(p.elementos().stream().map(Fila::de).toList(),p.pagina(),p.tamano(),p.total()));
    }
    public record Pagina(List<Fila> elementos,int pagina,int tamano,long total) {}
    public record Fila(UUID id,UUID pedidoId,UUID eventoId,String destinatario,String asunto,
            Notificacion.Estado estado,String creadaEn,String enviadaEn,int intentos,String ultimoError,
            boolean resultadoIncierto,String bloqueo) {
        static Fila de(Notificacion n) {
            String error=null;
            if (n.ultimoError()!=null) {
                try { error=Causa.valueOf(n.ultimoError()).name(); }
                catch (IllegalArgumentException desconocido) { error="ERROR_NO_CLASIFICADO"; }
            }
            return new Fila(n.id(),n.pedidoId(),n.eventoId(),n.destinatario(),n.asunto(),n.estado(),FechasWeb.texto(n.creadaEn()),
                FechasWeb.texto(n.enviadaEn()),n.intentos(),error,n.resultadoIncierto(),
                n.destinatario()==null ? "DESTINATARIO_PENDIENTE" : null);
        }
    }
}
