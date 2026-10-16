package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista y descarga de archivos (contrato 0.7.0). Los permisos los deciden los casos de uso con el estado vigente
 * (R6): ver el pedido (P-13) para la lista y P-17 para la descarga. La subida está en {@link SubidaArchivoController}.
 */
@RestController
public class ArchivoController {

    private final ListarAdjuntosDeEtapa listar;
    private final DescargarArchivo descargar;

    public ArchivoController(ListarAdjuntosDeEtapa listar, DescargarArchivo descargar) {
        this.listar = listar;
        this.descargar = descargar;
    }

    @GetMapping("/api/v1/pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos")
    public List<AdjuntoRespuesta> listar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable int numeroEtapa) {
        return listar.listar(SolicitantesWeb.de(token), pedidoId, numeroEtapa).stream()
                .map(AdjuntoRespuesta::de)
                .toList();
    }

    /**
     * Siempre como binario para descargar: el tipo declarado por quien subió el archivo no se usa y el navegador no
     * debe interpretarlo (nosniff).
     */
    @GetMapping("/api/v1/archivos/{adjuntoId}/contenido")
    public ResponseEntity<InputStreamResource> descargar(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID adjuntoId) {
        DescargarArchivo.Autorizada autorizada = descargar.autorizar(SolicitantesWeb.de(token), adjuntoId);
        DescargarArchivo.Contenido contenido = descargar.abrir(autorizada);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(contenido.tamanoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(contenido.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(contenido.flujo()));
    }
}
