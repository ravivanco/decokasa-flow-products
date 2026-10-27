package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.usuarios.application.port.in.RenovarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.SesionIniciada;
import com.decokasa.seguimiento.usuarios.application.port.out.EmisorTokens;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.PoliticaSesion;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Instant;
import java.util.Optional;

/**
 * Renovación con rotación y límite absoluto de 8 horas (ADR-0002, D3). Un token ya revocado indica que alguien lo
 * copió: se revoca toda la familia. {@code usuarios.config} confirma esa revocación aunque la respuesta sea 401. El
 * token se consume con una operación atómica, así dos renovaciones simultáneas no emiten dos sucesores (A2).
 */
public final class RenovarSesionCasoDeUso implements RenovarSesion {

    private final UsuarioRepositorio usuarios;
    private final TokenRenovacionRepositorio tokens;
    private final EmisorTokens emisor;
    private final GeneradorTokenRenovacion generador;
    private final Reloj reloj;
    private final PoliticaSesion politicaSesion;

    public RenovarSesionCasoDeUso(UsuarioRepositorio usuarios, TokenRenovacionRepositorio tokens, EmisorTokens emisor,
            GeneradorTokenRenovacion generador, Reloj reloj, PoliticaSesion politicaSesion) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.emisor = emisor;
        this.generador = generador;
        this.reloj = reloj;
        this.politicaSesion = politicaSesion;
    }

    @Override
    public SesionIniciada renovar(String tokenRenovacion) {
        if (tokenRenovacion == null || tokenRenovacion.isBlank()) {
            throw new SesionInvalida();
        }
        Instant ahora = reloj.ahora();
        SesionRenovable actual = tokens.buscarPorHash(generador.hash(tokenRenovacion)).orElseThrow(SesionInvalida::new);
        if (actual.estaRevocada()) {
            tokens.revocarFamilia(actual.familiaId(), ahora);
            throw new SesionInvalida();
        }
        if (!actual.estaVigente(ahora)) {
            throw new SesionInvalida();
        }
        // N1: se bloquea y relee la fila del usuario antes de tocar sus tokens, el mismo orden que la desactivación y
        // el login. Una desactivación simultánea termina antes (y esta renovación la ve) o espera a que esta termine
        // (y revoca también el sucesor). Sin esto, el sucesor podía quedar vigente en una cuenta desactivada.
        Optional<Usuario> usuario = usuarios.buscarParaActualizar(actual.usuarioId());
        if (usuario.isEmpty() || !usuario.get().puedeIniciarSesion()) {
            tokens.revocarFamilia(actual.familiaId(), ahora);
            throw new SesionInvalida();
        }

        // Consumo atómico (hallazgo A2): si otra renovación ya consumió este token entre la lectura y ahora, esta
        // solicitud es una reutilización. Sin margen de reutilización: se revoca toda la familia, como arriba.
        if (!tokens.consumir(actual.id(), ahora)) {
            tokens.revocarFamilia(actual.familiaId(), ahora);
            throw new SesionInvalida();
        }
        String nuevo = generador.nuevo();
        tokens.guardar(SesionRenovable.nueva(actual.usuarioId(), actual.familiaId(), generador.hash(nuevo), ahora,
                actual.expiraEn()));
        String acceso = emisor.emitirAcceso(usuario.get(), ahora, politicaSesion.acceso());
        return new SesionIniciada(acceso, politicaSesion.acceso().toSeconds(), nuevo, actual.expiraEn(), usuario.get());
    }
}
