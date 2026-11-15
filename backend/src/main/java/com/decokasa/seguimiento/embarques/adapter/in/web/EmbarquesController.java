package com.decokasa.seguimiento.embarques.adapter.in.web;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.embarques.application.port.in.GestionarEmbarques;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Rutas de BL, Packing Lists, checklist y alertas simuladas de un pedido. Los permisos los decide el caso de uso. */
@RestController
@RequestMapping("/api/v1/pedidos/{pedidoId}")
public class EmbarquesController {

    private final GestionarEmbarques embarques;
    private final DescargarArchivo descargas;

    public EmbarquesController(GestionarEmbarques embarques, DescargarArchivo descargas) {
        this.embarques = embarques;
        this.descargas = descargas;
    }

    public record SolicitudBl(@NotBlank @Size(max = 200) String codigo, LocalDate fechaSalida,
            LocalDate llegadaEstimada, @Size(max = 20) String importadora) {
    }

    public record CambioBl(@NotNull Long version, LocalDate fechaSalida, LocalDate llegadaEstimada,
            @Size(max = 20) String llegadaOrigen, @Size(max = 20) String importadora) {
    }

    public record SolicitudPackingList(@NotNull UUID adjuntoId) {
    }

    public record SolicitudVinculo(@NotBlank @Size(max = 200) String codigo) {
    }

    public record SolicitudPedido(@NotBlank @Size(max = 40) String numero) {
    }

    @GetMapping("/embarques")
    public GestionarEmbarques.Vista consultar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return embarques.consultar(SolicitantesWeb.de(token), pedidoId);
    }

    @PostMapping("/embarques")
    @ResponseStatus(HttpStatus.CREATED)
    public GestionarEmbarques.Vista crear(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @Valid @RequestBody SolicitudBl solicitud) {
        return embarques.crear(SolicitantesWeb.de(token), pedidoId, new GestionarEmbarques.NuevoBl(solicitud.codigo(),
                solicitud.fechaSalida(), solicitud.llegadaEstimada(), solicitud.importadora()));
    }

    @PatchMapping("/embarques/{blId}")
    public GestionarEmbarques.Vista actualizar(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable UUID blId, @Valid @RequestBody CambioBl cambio) {
        return embarques.actualizar(SolicitantesWeb.de(token), pedidoId, blId, new GestionarEmbarques.CambioBl(
                cambio.version(), cambio.fechaSalida(), cambio.llegadaEstimada(), cambio.llegadaOrigen(),
                cambio.importadora()));
    }

    @PostMapping("/embarques/{blId}/packing-list")
    public GestionarEmbarques.Vista registrarPackingList(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID pedidoId, @PathVariable UUID blId, @Valid @RequestBody SolicitudPackingList solicitud) {
        return embarques.registrarPackingList(SolicitantesWeb.de(token), pedidoId, blId, solicitud.adjuntoId());
    }

    @PostMapping("/embarques/vinculos")
    public GestionarEmbarques.Vista vincular(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @Valid @RequestBody SolicitudVinculo solicitud) {
        return embarques.vincularPorCodigo(SolicitantesWeb.de(token), pedidoId, solicitud.codigo());
    }

    @PostMapping("/embarques/{blId}/pedidos")
    public GestionarEmbarques.Vista anadirPedido(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable UUID blId, @Valid @RequestBody SolicitudPedido solicitud) {
        return embarques.anadirPedido(SolicitantesWeb.de(token), pedidoId, blId, solicitud.numero());
    }

    @DeleteMapping("/embarques/{blId}/vinculo")
    public GestionarEmbarques.Vista desvincular(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable UUID blId) {
        return embarques.desvincular(SolicitantesWeb.de(token), pedidoId, blId);
    }

    @PostMapping("/embarques/{blId}/revision")
    public GestionarEmbarques.Vista marcarRevisada(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId,
            @PathVariable UUID blId) {
        return embarques.marcarRevisada(SolicitantesWeb.de(token), pedidoId, blId);
    }

    /** Siempre como binario, igual que la descarga de archivos: el navegador no interpreta el contenido. */
    @GetMapping("/embarques/{blId}/packing-lists/{packingListId}/contenido")
    public ResponseEntity<InputStreamResource> packingList(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID pedidoId, @PathVariable UUID blId, @PathVariable UUID packingListId) {
        DescargarArchivo.Autorizada autorizada = embarques.packingList(SolicitantesWeb.de(token), pedidoId, blId,
                packingListId);
        DescargarArchivo.Contenido contenido = descargas.abrir(autorizada);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(contenido.tamanoBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(contenido.nombre(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(contenido.flujo()));
    }

    @GetMapping("/checklist-documentos")
    public GestionarEmbarques.Checklist checklist(@AuthenticationPrincipal Jwt token, @PathVariable UUID pedidoId) {
        return embarques.checklist(SolicitantesWeb.de(token), pedidoId);
    }

    @GetMapping("/alertas-simuladas")
    public List<GestionarEmbarques.AlertaSimulada> alertasSimuladas(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID pedidoId) {
        return embarques.alertasSimuladas(SolicitantesWeb.de(token), pedidoId);
    }
}
