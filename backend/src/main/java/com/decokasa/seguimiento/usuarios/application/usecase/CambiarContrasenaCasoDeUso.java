package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.application.port.in.CambiarContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CredencialesRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva;
import com.decokasa.seguimiento.usuarios.domain.CredencialesInvalidas;
import com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Instant;
import java.util.UUID;

/**
 * Cambio de la propia contraseña (ADR-0033). La contraseña actual incorrecta cuenta como intento fallido del bloqueo
 * D2 (la configuración confirma ese intento aunque la respuesta sea un error). Al cambiarla se revocan las sesiones,
 * se anulan las recuperaciones pendientes y se sellan las credenciales: el JWT con el que se pidió deja de valer.
 */
public final class CambiarContrasenaCasoDeUso implements CambiarContrasena {

    /** Campo del contrato cuyo error se confirma (intento fallido guardado). */
    public static final String CAMPO_ACTUAL = "contrasenaActual";

    private final UsuarioRepositorio usuarios;
    private final CredencialesRepositorio credenciales;
    private final RecuperacionRepositorio recuperaciones;
    private final TokenRenovacionRepositorio tokens;
    private final CodificadorContrasena codificador;
    private final Reloj reloj;
    private final PoliticaBloqueo politicaBloqueo;

    public CambiarContrasenaCasoDeUso(UsuarioRepositorio usuarios, CredencialesRepositorio credenciales,
            RecuperacionRepositorio recuperaciones, TokenRenovacionRepositorio tokens, CodificadorContrasena codificador,
            Reloj reloj, PoliticaBloqueo politicaBloqueo) {
        this.usuarios = usuarios;
        this.credenciales = credenciales;
        this.recuperaciones = recuperaciones;
        this.tokens = tokens;
        this.codificador = codificador;
        this.reloj = reloj;
        this.politicaBloqueo = politicaBloqueo;
    }

    @Override
    public void cambiar(UUID usuarioId, String contrasenaActual, String contrasenaNueva, String confirmacion) {
        if (contrasenaActual == null || contrasenaActual.isEmpty()) {
            throw new DatoInvalido(CAMPO_ACTUAL, "Escribe tu contraseña actual.");
        }
        ValidacionContrasena.validarNueva(contrasenaNueva, confirmacion);
        Instant ahora = reloj.ahora();
        Usuario usuario = usuarios.buscarParaActualizar(usuarioId)
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(CredencialesInvalidas::new);
        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueada(usuario.bloqueadoHasta());
        }
        boolean coincide = !ContrasenaNueva.superaElLimite(contrasenaActual)
                && codificador.coincide(contrasenaActual, usuario.contrasenaHash());
        if (!coincide) {
            usuario.registrarFallo(ahora, politicaBloqueo);
            usuarios.guardarIntentos(usuario);
            if (usuario.estaBloqueado(ahora)) {
                throw new CuentaBloqueada(usuario.bloqueadoHasta());
            }
            throw new DatoInvalido(CAMPO_ACTUAL, "La contraseña actual no es correcta.");
        }
        if (codificador.coincide(contrasenaNueva, usuario.contrasenaHash())) {
            throw new DatoInvalido("contrasenaNueva", "La contraseña nueva debe ser distinta de la actual.");
        }
        credenciales.cambiarContrasena(usuario.id(), codificador.codificar(contrasenaNueva), ahora);
        recuperaciones.anularDeUsuario(usuario.id(), ahora);
        tokens.revocarDeUsuario(usuario.id(), ahora);
    }
}
