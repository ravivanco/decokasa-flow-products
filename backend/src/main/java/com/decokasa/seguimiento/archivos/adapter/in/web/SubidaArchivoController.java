package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Subida de archivos (contrato 0.7.0). Límite provisional del equipo de 100 MiB por archivo, configurable y pendiente
 * del cliente ({@code decokasa.archivos.tamano-maximo}). Formatos (ADR-0026): el {@code Content-Type} de la parte solo
 * debe ser coherente con la extensión; el formato lo deciden la extensión y el contenido.
 */
@RestController
public class SubidaArchivoController {

    private final SubirArchivo subir;

    public SubidaArchivoController(SubirArchivo subir) {
        this.subir = subir;
    }

    @PostMapping(path = "/api/v1/pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AdjuntoRespuesta> subir(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable int numeroEtapa, @RequestPart("archivo") MultipartFile archivo,
            @RequestParam("tipoDocumento") String tipoDocumento,
            @RequestParam("entradaEsperadaId") UUID entradaEsperadaId) {
        try (InputStream contenido = archivo.getInputStream()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(AdjuntoRespuesta.de(subir.subir(
                    SolicitantesWeb.de(token), new SubirArchivo.Comando(pedidoId, numeroEtapa, entradaEsperadaId,
                            tipoDocumento, archivo.getOriginalFilename(), contenido, archivo.getContentType()))));
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo leer el archivo recibido.", error);
        }
    }
}
