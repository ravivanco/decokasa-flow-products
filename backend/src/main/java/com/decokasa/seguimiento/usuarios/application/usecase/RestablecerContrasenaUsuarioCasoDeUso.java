package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.RestablecerContrasenaUsuario;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CredencialesRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Instant;
import java.util.UUID;

/**
 * Restablecimiento por Admin (V36). Solo Admin vigente en la base (A1), con la misma regla de longitud que cualquier
 * contraseña nueva. La contraseña temporal no se guarda ni se registra: solo su hash. El sello de credenciales invalida
 * los JWT anteriores de esa cuenta. Sin Spring (R2).
 */
public final class RestablecerContrasenaUsuarioCasoDeUso implements RestablecerContrasenaUsuario {

    public static final String CAMPO = "contrasenaTemporal";

    private final Autorizacion autorizacion;
    private final UsuarioRepositorio usuarios;
    private final CredencialesRepositorio credenciales;
    private final RecuperacionRepositorio recuperaciones;
    private final TokenRenovacionRepositorio tokens;
    private final CodificadorContrasena codificador;
    private final Reloj reloj;

    public RestablecerContrasenaUsuarioCasoDeUso(UsuarioRepositorio usuarios, CredencialesRepositorio credenciales,
            RecuperacionRepositorio recuperaciones, TokenRenovacionRepositorio tokens, CodificadorContrasena codificador,
            Reloj reloj) {
        this.autorizacion = new Autorizacion(usuarios);
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.recuperaciones = recuperaciones;
        this.tokens = tokens;
        this.codificador = codificador;
        this.reloj = reloj;
    }

    @Override
    public void restablecer(Solicitante solicitante, UUID usuarioId, String contrasenaTemporal) {
        autorizacion.exigirAdmin(solicitante);
        try {
            ValidacionContrasena.validarNueva(contrasenaTemporal, contrasenaTemporal);
        } catch (DatoInvalido error) {
            throw new DatoInvalido(CAMPO, error.getMessage());
        }
        Usuario usuario = usuarios.buscarParaActualizar(usuarioId)
                .orElseThrow(() -> new NoEncontrado("El usuario no existe."));
        if (!usuario.activo()) {
            throw new DatoInvalido(CAMPO, "La cuenta está desactivada: actívala antes de restablecer su contraseña.");
        }
        Instant ahora = reloj.ahora();
        credenciales.restablecerTemporal(usuario.id(), codificador.codificar(contrasenaTemporal), ahora);
        recuperaciones.anularDeUsuario(usuario.id(), ahora);
        tokens.revocarDeUsuario(usuario.id(), ahora);
    }
}
