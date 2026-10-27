package com.decokasa.seguimiento.asistente.adapter.in.web;

import com.decokasa.seguimiento.asistente.application.port.in.AbrirConversacion;
import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConsumoIA;
import com.decokasa.seguimiento.asistente.application.port.in.ConsultarConversaciones;
import com.decokasa.seguimiento.asistente.application.port.in.GestionarConfiguracionIA;
import com.decokasa.seguimiento.asistente.application.port.in.GestionarTarifasIA;
import com.decokasa.seguimiento.asistente.application.port.in.Preguntar;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.asistente.domain.ConversacionIA;
import com.decokasa.seguimiento.compartido.adapter.in.web.SolicitantesWeb;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Asistente (contrato 0.19.0, piloto). Los permisos los deciden los casos de uso con el estado vigente en la base.
 *
 * <p><b>Paso 7.5:</b> preguntar se publica porque ya existen todos los controles (reserva atómica, conteo de contexto,
 * tarifa validada). Mientras falte cualquiera (IA_HABILITADA=false, clave o modelo ausentes, interruptor de la base
 * apagado, tarifa sin validar), responde 503 IA_NO_DISPONIBLE **antes** de cualquier llamada al proveedor. Consumo,
 * tarifas y configuración son solo para el admin y no llaman al proveedor.
 */
@RestController
@RequestMapping("/api/v1")
public class AsistenteController {

    private final AbrirConversacion apertura;
    private final ConsultarConversaciones consulta;
    private final Preguntar preguntar;
    private final ConsultarConsumoIA consumo;
    private final GestionarTarifasIA tarifas;
    private final GestionarConfiguracionIA configuracion;

    public AsistenteController(AbrirConversacion apertura, ConsultarConversaciones consulta, Preguntar preguntar,
            ConsultarConsumoIA consumo, GestionarTarifasIA tarifas, GestionarConfiguracionIA configuracion) {
        this.apertura = apertura;
        this.consulta = consulta;
        this.preguntar = preguntar;
        this.consumo = consumo;
        this.tarifas = tarifas;
        this.configuracion = configuracion;
    }

    /** Contrato PreguntaAsistente: 1 a 2000 caracteres. */
    public record PreguntaCuerpo(
            @NotBlank(message = "Escribe una pregunta.")
            @Size(max = 2000, message = "La pregunta admite hasta 2000 caracteres.") String pregunta) {

        @Override
        public String toString() {
            return "PreguntaCuerpo[PROTEGIDA]";
        }
    }

    public record PreciosCuerpo(@NotNull @Positive Long entradaMicroUsdMtok, @NotNull @Positive Long salidaMicroUsdMtok,
            @NotNull @Positive Long escrituraCacheMicroUsdMtok, @NotNull @Positive Long lecturaCacheMicroUsdMtok) {

        Tarifa.Precios aPrecios() {
            return new Tarifa.Precios(entradaMicroUsdMtok, salidaMicroUsdMtok, escrituraCacheMicroUsdMtok,
                    lecturaCacheMicroUsdMtok);
        }
    }

    public record TarifaCuerpo(@NotBlank @Size(max = 100) String modelo, @NotNull LocalDate vigenteDesde,
            @NotNull @Valid PreciosCuerpo base, @Positive Integer umbralTokensEntrada, @Valid PreciosCuerpo superior,
            @NotBlank @Size(max = 500) String fuente, @NotNull LocalDate consultadoEn) {
    }

    public record ConfiguracionCuerpo(@NotNull Boolean habilitada, @NotNull @PositiveOrZero Long version) {
    }

    @PostMapping("/pedidos/{pedidoId}/asistente/conversaciones")
    public ResponseEntity<AsistenteRespuestas.Conversacion> abrir(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID pedidoId) {
        ConversacionIA conversacion = apertura.abrir(SolicitantesWeb.de(token), pedidoId);
        return ResponseEntity.created(URI.create("/api/v1/asistente/conversaciones/" + conversacion.id()))
                .body(AsistenteRespuestas.de(conversacion));
    }

    @GetMapping("/asistente/conversaciones")
    public AsistenteRespuestas.Pagina listar(@AuthenticationPrincipal Jwt token,
            @RequestParam(required = false) UUID pedidoId,
            @RequestParam(required = false) CodigoPais pais,
            @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamano) {
        return AsistenteRespuestas.de(consulta.listar(SolicitantesWeb.de(token), pedidoId, pais, pagina, tamano));
    }

    @GetMapping("/asistente/conversaciones/{conversacionId}")
    public AsistenteRespuestas.Detalle obtener(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID conversacionId) {
        return AsistenteRespuestas.de(consulta.obtener(SolicitantesWeb.de(token), conversacionId));
    }

    @PostMapping("/asistente/conversaciones/{conversacionId}/mensajes")
    public AsistenteRespuestas.Respuesta preguntar(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID conversacionId, @Valid @RequestBody PreguntaCuerpo cuerpo) {
        return AsistenteRespuestas.de(preguntar.preguntar(SolicitantesWeb.de(token), conversacionId,
                cuerpo.pregunta()));
    }

    @GetMapping("/asistente/consumo")
    public AsistenteRespuestas.Consumo consumo(@AuthenticationPrincipal Jwt token,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return AsistenteRespuestas.de(consumo.consultar(SolicitantesWeb.de(token), desde, hasta));
    }

    @GetMapping("/asistente/tarifas")
    public List<AsistenteRespuestas.TarifaRespuesta> tarifas(@AuthenticationPrincipal Jwt token) {
        return tarifas.listar(SolicitantesWeb.de(token)).stream().map(AsistenteRespuestas::de).toList();
    }

    @PostMapping("/asistente/tarifas")
    public ResponseEntity<AsistenteRespuestas.TarifaRespuesta> registrarTarifa(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody TarifaCuerpo cuerpo) {
        AsistenteRespuestas.TarifaRespuesta tarifa = AsistenteRespuestas.de(tarifas.registrar(
                SolicitantesWeb.de(token), new GestionarTarifasIA.NuevaTarifa(cuerpo.modelo(), cuerpo.vigenteDesde(),
                        cuerpo.base().aPrecios(), cuerpo.umbralTokensEntrada(),
                        cuerpo.superior() == null ? null : cuerpo.superior().aPrecios(), cuerpo.fuente(),
                        cuerpo.consultadoEn())));
        return ResponseEntity.created(URI.create("/api/v1/asistente/tarifas/" + tarifa.id())).body(tarifa);
    }

    @PostMapping("/asistente/tarifas/{tarifaId}/validacion")
    public AsistenteRespuestas.TarifaRespuesta validarTarifa(@AuthenticationPrincipal Jwt token,
            @PathVariable UUID tarifaId) {
        return AsistenteRespuestas.de(tarifas.validar(SolicitantesWeb.de(token), tarifaId));
    }

    @GetMapping("/asistente/configuracion")
    public AsistenteRespuestas.Configuracion configuracion(@AuthenticationPrincipal Jwt token) {
        return AsistenteRespuestas.de(configuracion.obtener(SolicitantesWeb.de(token)));
    }

    @PatchMapping("/asistente/configuracion")
    public AsistenteRespuestas.Configuracion cambiarConfiguracion(@AuthenticationPrincipal Jwt token,
            @Valid @RequestBody ConfiguracionCuerpo cuerpo) {
        return AsistenteRespuestas.de(configuracion.cambiarHabilitada(SolicitantesWeb.de(token), cuerpo.habilitada(),
                cuerpo.version()));
    }
}
