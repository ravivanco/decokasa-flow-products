package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.application.port.in.RecuperarContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CredencialesRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.EnlaceInvalido;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Recuperación de contraseña (ADR-0033). Java puro (R2); la transacción la abre {@code usuarios.config}.
 * <ul>
 * <li>Solicitar no revela nada: cuenta inexistente, inactiva, sin roles o con el límite por hora alcanzado terminan
 * igual, sin error. Solo se guarda la solicitud; el token se crea al enviar el correo.</li>
 * <li>Restablecer exige un token vigente; si no, {@link EnlaceInvalido} (siempre el mismo). Al completar anula las
 * demás solicitudes, revoca las sesiones y sella las credenciales. No inicia sesión.</li>
 * </ul>
 */
public final class RecuperarContrasenaCasoDeUso implements RecuperarContrasena {

    private static final Duration UNA_HORA = Duration.ofHours(1);

    private final UsuarioRepositorio usuarios;
    private final RecuperacionRepositorio recuperaciones;
    private final CredencialesRepositorio credenciales;
    private final TokenRenovacionRepositorio tokens;
    private final CodificadorContrasena codificador;
    private final GeneradorTokenRenovacion generador;
    private final Reloj reloj;
    private final Duration duracion;
    private final int maximoPorHora;

    public RecuperarContrasenaCasoDeUso(UsuarioRepositorio usuarios, RecuperacionRepositorio recuperaciones,
            CredencialesRepositorio credenciales, TokenRenovacionRepositorio tokens, CodificadorContrasena codificador,
            GeneradorTokenRenovacion generador, Reloj reloj, Duration duracion, int maximoPorHora) {
        if (duracion.compareTo(Duration.ofMinutes(5)) < 0 || duracion.compareTo(Duration.ofHours(24)) > 0) {
            throw new IllegalArgumentException("La duración del enlace de recuperación debe estar entre 5 min y 24 h.");
        }
        if (maximoPorHora < 1 || maximoPorHora > 10) {
            throw new IllegalArgumentException("El máximo de solicitudes por hora debe estar entre 1 y 10.");
        }
        this.usuarios = usuarios;
        this.recuperaciones = recuperaciones;
        this.credenciales = credenciales;
        this.tokens = tokens;
        this.codificador = codificador;
        this.generador = generador;
        this.reloj = reloj;
        this.duracion = duracion;
        this.maximoPorHora = maximoPorHora;
    }

    @Override
    public void solicitar(String correo) {
        String texto = correo == null ? "" : correo.strip();
        if (texto.isEmpty() || texto.length() > 254 || texto.indexOf('@') < 1) {
            throw new DatoInvalido("correo", "Escribe un correo válido.");
        }
        Instant ahora = reloj.ahora();
        Optional<Usuario> encontrado = usuarios.buscarPorCorreo(texto).filter(Usuario::puedeIniciarSesion);
        if (encontrado.isEmpty()) {
            return;
        }
        // Bloquea la fila de la cuenta: dos solicitudes simultáneas no superan juntas el límite por hora.
        Optional<Usuario> cuenta = usuarios.buscarParaActualizar(encontrado.get().id())
                .filter(Usuario::puedeIniciarSesion);
        if (cuenta.isEmpty() || recuperaciones.contarDesde(cuenta.get().id(), ahora.minus(UNA_HORA)) >= maximoPorHora) {
            return;
        }
        recuperaciones.registrar(UUID.randomUUID(), cuenta.get().id(), ahora, ahora.plus(duracion));
    }

    @Override
    public void restablecer(String token, String contrasenaNueva, String confirmacion) {
        ValidacionContrasena.validarNueva(contrasenaNueva, confirmacion);
        if (token == null || token.isBlank() || token.length() > 200) {
            throw new EnlaceInvalido();
        }
        Instant ahora = reloj.ahora();
        RecuperacionRepositorio.Vigente vigente = recuperaciones.bloquearVigente(generador.hash(token.strip()), ahora)
                .orElseThrow(EnlaceInvalido::new);
        Usuario usuario = usuarios.buscarParaActualizar(vigente.usuarioId())
                .filter(Usuario::puedeIniciarSesion)
                .orElseThrow(EnlaceInvalido::new);
        credenciales.cambiarContrasena(usuario.id(), codificador.codificar(contrasenaNueva), ahora);
        recuperaciones.marcarUsada(vigente.id(), ahora);
        recuperaciones.anularDeUsuario(usuario.id(), ahora);
        tokens.revocarDeUsuario(usuario.id(), ahora);
    }
}
