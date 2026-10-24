package com.decokasa.seguimiento.asistente.application.usecase;

import com.decokasa.seguimiento.asistente.application.port.in.GestionarTarifasIA;
import com.decokasa.seguimiento.asistente.application.port.out.ConversacionesIA;
import com.decokasa.seguimiento.asistente.application.port.out.CuentasParaAsistente;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA;
import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA.TarifaRegistrada;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Tarifas configurables por modelo y fecha (ADR-0022). Registrar exige una fuente https de la documentación oficial de
 * Anthropic; validar es el acto del admin que la comparó con esa fuente. Nunca se inventan precios: sin una tarifa
 * validada para el modelo configurado, la IA no llama al proveedor.
 */
public final class GestionarTarifasIACasoDeUso implements GestionarTarifasIA {

    private static final Pattern MODELO = Pattern.compile("^[a-z0-9][a-z0-9.\\-]{0,99}$");
    /** Propuesta del equipo: dominios de la documentación y precios oficiales de Anthropic. */
    static final Set<String> FUENTES_OFICIALES = Set.of("platform.claude.com", "docs.claude.com", "claude.com",
            "www.claude.com", "anthropic.com", "www.anthropic.com", "docs.anthropic.com");

    private final AccesoAsistente acceso;
    private final TarifasIA tarifas;
    private final Reloj reloj;

    public GestionarTarifasIACasoDeUso(CuentasParaAsistente cuentas, ConversacionesIA conversaciones,
            TarifasIA tarifas, Reloj reloj) {
        this.acceso = new AccesoAsistente(cuentas, conversaciones);
        this.tarifas = tarifas;
        this.reloj = reloj;
    }

    @Override
    public List<TarifaRegistrada> listar(Solicitante solicitante) {
        acceso.admin(solicitante);
        return tarifas.listar();
    }

    @Override
    public TarifaRegistrada registrar(Solicitante solicitante, NuevaTarifa nueva) {
        acceso.admin(solicitante);
        LocalDate hoy = LocalDate.ofInstant(reloj.ahora(), Reloj.ZONA_ECUADOR);
        if (nueva == null || nueva.modelo() == null || !MODELO.matcher(nueva.modelo()).matches()) {
            throw new DatoInvalido("Indica el identificador del modelo, por ejemplo claude-haiku-5-5.");
        }
        if (nueva.vigenteDesde() == null || nueva.consultadoEn() == null || nueva.consultadoEn().isAfter(hoy)) {
            throw new DatoInvalido("Indica desde qué fecha rige la tarifa y cuándo se consultó la fuente.");
        }
        if (nueva.base() == null || (nueva.umbralTokensEntrada() == null) != (nueva.superior() == null)) {
            throw new DatoInvalido("Completa los precios; el tramo superior lleva umbral y sus cuatro precios.");
        }
        if (!esFuenteOficial(nueva.fuente())) {
            throw new DatoInvalido("La fuente debe ser una página https de la documentación oficial de Anthropic.");
        }
        UUID id = UUID.randomUUID();
        tarifas.registrar(new Tarifa(id, nueva.modelo(), nueva.vigenteDesde(), nueva.base(),
                nueva.umbralTokensEntrada(), nueva.superior(), nueva.fuente(), nueva.consultadoEn(), false),
                solicitante.usuarioId(), reloj.ahora());
        return tarifas.porId(id).orElseThrow();
    }

    @Override
    public TarifaRegistrada validar(Solicitante solicitante, UUID tarifaId) {
        acceso.admin(solicitante);
        TarifaRegistrada actual = tarifas.porId(tarifaId)
                .orElseThrow(() -> new NoEncontrado("La tarifa no existe."));
        if (actual.tarifa().validada() || !tarifas.validar(tarifaId, solicitante.usuarioId(), reloj.ahora())) {
            throw new DatoInvalido("La tarifa ya está validada.");
        }
        return tarifas.porId(tarifaId).orElseThrow();
    }

    static boolean esFuenteOficial(String fuente) {
        if (fuente == null || fuente.length() > 500) {
            return false;
        }
        try {
            URI uri = new URI(fuente);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                    && FUENTES_OFICIALES.contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (URISyntaxException error) {
            return false;
        }
    }
}
