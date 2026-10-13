package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.SesionIniciada;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.EmisorTokens;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.ContrasenaNueva;
import com.decokasa.seguimiento.usuarios.domain.CredencialesInvalidas;
import com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import com.decokasa.seguimiento.usuarios.domain.PoliticaSesion;
import com.decokasa.seguimiento.usuarios.domain.SesionRenovable;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Inicio de sesión (requisito 4.2 #1; ADR-0002). Java puro (R2): se registra en {@code usuarios.config}, que confirma
 * en la base el intento fallido aunque la respuesta sea un error.
 */
public final class IniciarSesionCasoDeUso implements IniciarSesion {

    private final UsuarioRepositorio usuarios;
    private final TokenRenovacionRepositorio tokens;
    private final CodificadorContrasena codificador;
    private final EmisorTokens emisor;
    private final GeneradorTokenRenovacion generador;
    private final Reloj reloj;
    private final PoliticaBloqueo politicaBloqueo;
    private final PoliticaSesion politicaSesion;
    // Hash de una contraseña al azar: si la cuenta no existe se compara igual contra él, para que el tiempo de
    // respuesta no revele qué cuentas existen.
    private final String hashParaCuentaInexistente;

    public IniciarSesionCasoDeUso(UsuarioRepositorio usuarios, TokenRenovacionRepositorio tokens,
            CodificadorContrasena codificador, EmisorTokens emisor, GeneradorTokenRenovacion generador, Reloj reloj,
            PoliticaBloqueo politicaBloqueo, PoliticaSesion politicaSesion) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.codificador = codificador;
        this.emisor = emisor;
        this.generador = generador;
        this.reloj = reloj;
        this.politicaBloqueo = politicaBloqueo;
        this.politicaSesion = politicaSesion;
        this.hashParaCuentaInexistente = codificador.codificar(UUID.randomUUID().toString());
    }

    @Override
    public SesionIniciada iniciar(String identificador, String contrasena) {
        if (identificador == null || identificador.isBlank() || contrasena == null) {
            throw new CredencialesInvalidas();
        }
        Instant ahora = reloj.ahora();
        Optional<Usuario> encontrado = buscar(identificador.strip());
        if (encontrado.isEmpty()) {
            if (!ContrasenaNueva.superaElLimite(contrasena)) {
                codificador.coincide(contrasena, hashParaCuentaInexistente);
            }
            throw new CredencialesInvalidas();
        }
        Usuario leido = encontrado.get();
        if (leido.estaBloqueado(ahora)) {
            throw new CuentaBloqueada(leido.bloqueadoHasta());
        }
        // Más de 72 bytes no puede coincidir con ninguna contraseña guardada: nunca se recorta ni llega a BCrypt.
        boolean coincide = !ContrasenaNueva.superaElLimite(contrasena)
                && codificador.coincide(contrasena, leido.contrasenaHash());

        // Hallazgo A4: el contador se actualiza sobre la fila bloqueada y releída. Los intentos simultáneos de la misma
        // cuenta esperan su turno: cada fallo se suma a lo que guardó el anterior y, si este ya bloqueó la cuenta, se
        // respeta el bloqueo. BCrypt va antes, para no retener la fila mientras compara.
        Usuario usuario = usuarios.buscarParaActualizar(leido.id()).orElseThrow(CredencialesInvalidas::new);
        if (usuario.estaBloqueado(ahora)) {
            throw new CuentaBloqueada(usuario.bloqueadoHasta());
        }
        if (!usuario.contrasenaHash().equals(leido.contrasenaHash())) {
            // La contraseña cambió entre la lectura y el bloqueo: decide la vigente, no la que se leyó antes.
            coincide = !ContrasenaNueva.superaElLimite(contrasena)
                    && codificador.coincide(contrasena, usuario.contrasenaHash());
        }
        if (!coincide) {
            usuario.registrarFallo(ahora, politicaBloqueo);
            usuarios.guardarIntentos(usuario);
            throw new CredencialesInvalidas();
        }
        if (!usuario.puedeIniciarSesion()) {
            throw new CredencialesInvalidas();
        }
        usuario.registrarAcierto();
        // Solo el contador: el inicio de sesión no cambia la versión administrativa del usuario (D10).
        usuarios.guardarIntentos(usuario);

        String tokenRenovacion = generador.nuevo();
        Instant renovableHasta = ahora.plus(politicaSesion.renovacion());
        tokens.guardar(SesionRenovable.nueva(usuario.id(), UUID.randomUUID(), generador.hash(tokenRenovacion), ahora,
                renovableHasta));
        String acceso = emisor.emitirAcceso(usuario, ahora, politicaSesion.acceso());
        return new SesionIniciada(acceso, politicaSesion.acceso().toSeconds(), tokenRenovacion, renovableHasta, usuario);
    }

    // Correo si tiene arroba; si no, nombre de usuario y, si no hay, teléfono normalizado.
    private Optional<Usuario> buscar(String texto) {
        if (texto.indexOf('@') >= 0) {
            return usuarios.buscarPorCorreo(texto);
        }
        Optional<Usuario> porUsuario = usuarios.buscarPorNombreDeUsuario(texto);
        if (porUsuario.isPresent()) {
            return porUsuario;
        }
        try {
            return usuarios.buscarPorTelefono(Telefono.normalizar(texto));
        } catch (DatoInvalido noEsUnTelefono) {
            return Optional.empty();
        }
    }
}
