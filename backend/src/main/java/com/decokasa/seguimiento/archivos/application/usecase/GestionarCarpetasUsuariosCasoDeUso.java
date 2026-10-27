package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasUsuarios;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaUsuarioRepositorio;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Programación y consulta de la carpeta de Drive de cada usuario (ADR-0026). El alta la programa sola, dentro de su
 * transacción; los usuarios existentes y los de la carga inicial solo los programa el admin, uno a uno (sin creación
 * masiva). La consulta y la programación exigen al admin vigente en la base (R6, hallazgo A1), no solo el rol del JWT.
 * Nunca llama a Google ni anuncia una carpeta sin la confirmación guardada. Sin Spring (R2).
 */
public final class GestionarCarpetasUsuariosCasoDeUso
        implements GestionarCarpetasUsuarios, GestionarCarpetasUsuarios.ProgramarAlta {
    private static final CodigoRol ADMIN = new CodigoRol("admin");
    private final CarpetaUsuarioRepositorio carpetas;
    private final ConsultarResponsables responsables;
    private final Reloj reloj;

    public GestionarCarpetasUsuariosCasoDeUso(CarpetaUsuarioRepositorio carpetas, ConsultarResponsables responsables,
            Reloj reloj) {
        this.carpetas = carpetas;
        this.responsables = responsables;
        this.reloj = reloj;
    }

    @Override public List<Estado> listar(Solicitante solicitante) {
        exigirAdmin(solicitante);
        return carpetas.estados().stream().map(GestionarCarpetasUsuariosCasoDeUso::vista).toList();
    }

    @Override public Estado estado(Solicitante solicitante, UUID usuarioId) {
        exigirAdmin(solicitante);
        return actual(usuarioId);
    }

    @Override public Estado programar(Solicitante solicitante, UUID usuarioId) {
        exigirAdmin(solicitante);
        actual(usuarioId);
        carpetas.programar(usuarioId, CarpetaUsuarioRepositorio.Origen.ADMIN, true, reloj.ahora());
        return actual(usuarioId);
    }

    /** Alta de un usuario por el admin: registra la intención en la misma transacción, sin reabrir nada. */
    @Override public void programarAlta(UUID usuarioId, Instant ocurridoEn) {
        carpetas.programar(usuarioId, CarpetaUsuarioRepositorio.Origen.ALTA, false, ocurridoEn);
    }

    private Estado actual(UUID usuarioId) {
        return carpetas.estado(usuarioId).map(GestionarCarpetasUsuariosCasoDeUso::vista)
                .orElseThrow(() -> new NoEncontrado("El usuario no existe."));
    }

    private void exigirAdmin(Solicitante solicitante) {
        responsables.estado(solicitante.usuarioId()).filter(u -> u.activo() && u.roles().contains(ADMIN))
                .orElseThrow(() -> new SinPermiso("Solo el admin vigente puede gestionar las carpetas de Drive."));
    }

    private static Estado vista(CarpetaUsuarioRepositorio.Estado e) {
        return new Estado(e.usuarioId(), e.estado(), e.intentos(), e.ultimoError(), e.programadaEn(), e.creadaEn());
    }
}
