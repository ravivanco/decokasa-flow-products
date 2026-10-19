package com.decokasa.seguimiento.archivos.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarga;
import com.decokasa.seguimiento.archivos.domain.ArchivoDemasiadoGrande;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import com.decokasa.seguimiento.compartido.adapter.in.web.FechasWeb;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CargasController {
    private static final String BASE="/api/v1/archivos/cargas/";
    private static final Pattern RANGO=Pattern.compile("bytes ([0-9]{1,9})-([0-9]{1,9})/([0-9]{1,9})");
    private final GestionarCarga cargas;
    public CargasController(GestionarCarga cargas) { this.cargas=cargas; }
    @PostMapping("/api/v1/pedidos/{pedidoId}/etapas/{numeroEtapa}/archivos/cargas")
    public ResponseEntity<CargaRespuesta> iniciar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable int numeroEtapa, @Valid @RequestBody InicioCuerpo cuerpo) {
        var vista=cargas.iniciar(SolicitantesWeb.de(token),new GestionarCarga.Inicio(pedidoId,numeroEtapa,
                cuerpo.entradaEsperadaId(),cuerpo.claveIdempotencia(),cuerpo.nombre(),cuerpo.tipoDocumento(),cuerpo.tamanoBytes()));
        return ResponseEntity.created(URI.create(BASE+vista.carga().id())).body(CargaRespuesta.de(vista));
    }
    @GetMapping(BASE+"{cargaId}")
    public ResponseEntity<CargaRespuesta> consultar(@AuthenticationPrincipal Jwt token,@PathVariable UUID cargaId) {
        return ResponseEntity.ok().header("Cache-Control","no-store")
                .body(CargaRespuesta.de(cargas.consultar(SolicitantesWeb.de(token),cargaId)));
    }
    @PutMapping(path=BASE+"{cargaId}/fragmentos",consumes="application/octet-stream")
    public CargaRespuesta fragmento(@AuthenticationPrincipal Jwt token,@PathVariable UUID cargaId,
            @RequestHeader("Content-Range") String rango,@RequestHeader("X-Fragmento-SHA256") String sha256,
            HttpServletRequest solicitud) {
        Matcher partes=RANGO.matcher(rango);
        if (!partes.matches()) { throw ProblemaCarga.conflicto(); }
        long inicio=Long.parseLong(partes.group(1)), fin=Long.parseLong(partes.group(2)), total=Long.parseLong(partes.group(3));
        if (fin<inicio || fin-inicio+1>CargaArchivo.FRAGMENTO_BYTES) {
            throw ProblemaCarga.conflicto();
        }
        try (var entrada=solicitud.getInputStream()) {
            byte[] bytes=entrada.readNBytes(CargaArchivo.FRAGMENTO_BYTES+1);
            if (bytes.length>CargaArchivo.FRAGMENTO_BYTES) { throw new ArchivoDemasiadoGrande("El fragmento supera 4 MiB."); }
            if (bytes.length!=fin-inicio+1) { throw ProblemaCarga.conflicto(); }
            return CargaRespuesta.de(cargas.fragmento(SolicitantesWeb.de(token),cargaId,
                    new GestionarCarga.Bloque(inicio,total,sha256,bytes)));
        } catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
    }
    @PostMapping(BASE+"{cargaId}/finalizacion")
    public AdjuntoRespuesta completar(@AuthenticationPrincipal Jwt token,@PathVariable UUID cargaId) {
        return AdjuntoRespuesta.de(cargas.completar(SolicitantesWeb.de(token),cargaId));
    }
    public record InicioCuerpo(@NotNull UUID entradaEsperadaId,@NotNull UUID claveIdempotencia,
            @NotBlank @Size(max=1024) String nombre,@NotBlank @Size(max=100) String tipoDocumento,
            @Min(1) long tamanoBytes) { }
    public record CargaRespuesta(UUID id,UUID pedidoId,int etapa,UUID entradaId,String nombre,String tipoDocumento,
            long tamanoBytes,long offsetConfirmado,int fragmentoBytes,String estado,UUID adjuntoId,UUID eventoId,
            String creadaEn,String actualizadaEn,List<CargaArchivo.Fragmento> fragmentos) {
        static CargaRespuesta de(GestionarCarga.Vista v) {
            CargaArchivo c=v.carga();
            return new CargaRespuesta(c.id(),c.pedidoId(),c.etapa(),c.entradaId(),c.nombre(),c.tipoDocumento(),
                    c.tamanoBytes(),c.offsetConfirmado(),CargaArchivo.FRAGMENTO_BYTES,c.estado().name(),c.adjuntoId(),
                    c.eventoId(),FechasWeb.texto(c.creadaEn()),FechasWeb.texto(c.actualizadaEn()),v.fragmentos());
        }
    }
}
