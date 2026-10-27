package com.decokasa.seguimiento.usuarios.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import com.decokasa.seguimiento.usuarios.adapter.in.security.ValidadorSelloCredenciales;
import com.decokasa.seguimiento.usuarios.application.port.in.CambiarContrasena;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarSelloCredenciales;
import com.decokasa.seguimiento.usuarios.application.port.in.EnviarCorreosRecuperacion;
import com.decokasa.seguimiento.usuarios.application.port.in.RecuperarContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.CorreoRecuperacion;
import com.decokasa.seguimiento.usuarios.application.port.out.CredencialesRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.application.usecase.CambiarContrasenaCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.EnviarCorreosRecuperacionCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.RecuperarContrasenaCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Composición de la recuperación y el cambio de contraseña (ADR-0033): casos de uso con su transacción, sello de
 * credenciales para el validador de JWT y el trabajador de envío (una transacción por operación, el envío fuera).
 */
@Configuration(proxyBeanMethods = false)
public class ContrasenasConfig {

    @Bean
    ConsultarSelloCredenciales consultarSelloCredenciales(CredencialesRepositorio credenciales) {
        // Lectura por clave primaria sin transacción de negocio: corre en cada solicitud autenticada.
        return credenciales::cambiadasEn;
    }

    @Bean
    OAuth2TokenValidator<Jwt> validadorSelloCredenciales(ConsultarSelloCredenciales sello) {
        return new ValidadorSelloCredenciales(sello);
    }

    @Bean
    RecuperarContrasena recuperarContrasena(Transacciones tx, UsuarioRepositorio usuarios,
            RecuperacionRepositorio recuperaciones, CredencialesRepositorio credenciales,
            TokenRenovacionRepositorio tokens, CodificadorContrasena codificador, GeneradorTokenRenovacion generador,
            Reloj reloj, @Value("${decokasa.recuperacion.duracion:PT30M}") Duration duracion,
            @Value("${decokasa.recuperacion.maximo-por-hora:3}") int maximoPorHora) {
        RecuperarContrasena caso = new RecuperarContrasenaCasoDeUso(usuarios, recuperaciones, credenciales, tokens,
                codificador, generador, reloj, duracion, maximoPorHora);
        return new RecuperarContrasena() {
            @Override
            public void solicitar(String correo) {
                tx.ejecutar(() -> {
                    caso.solicitar(correo);
                    return null;
                });
            }

            @Override
            public void restablecer(String token, String contrasenaNueva, String confirmacion) {
                tx.ejecutar(() -> {
                    caso.restablecer(token, contrasenaNueva, confirmacion);
                    return null;
                });
            }
        };
    }

    @Bean
    CambiarContrasena cambiarContrasena(Transacciones tx, UsuarioRepositorio usuarios,
            CredencialesRepositorio credenciales, RecuperacionRepositorio recuperaciones,
            TokenRenovacionRepositorio tokens, CodificadorContrasena codificador, Reloj reloj,
            PoliticaBloqueo politicaBloqueo) {
        CambiarContrasena caso = new CambiarContrasenaCasoDeUso(usuarios, credenciales, recuperaciones, tokens,
                codificador, reloj, politicaBloqueo);
        // El intento fallido (contraseña actual incorrecta) se guarda aunque la respuesta sea 400 o 423.
        return (usuarioId, actual, nueva, confirmacion) -> tx.ejecutarConfirmandoAnte(() -> {
            caso.cambiar(usuarioId, actual, nueva, confirmacion);
            return null;
        }, error -> error instanceof CuentaBloqueada || (error instanceof DatoInvalido invalido
                && CambiarContrasenaCasoDeUso.CAMPO_ACTUAL.equals(invalido.campo())));
    }

    @Bean
    EnviarCorreosRecuperacion enviarCorreosRecuperacion(PlatformTransactionManager gestor,
            RecuperacionRepositorio recuperaciones, CorreoRecuperacion correo, GeneradorTokenRenovacion generador,
            Reloj reloj,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean oauth,
            @Value("${decokasa.google.gmail.habilitada:false}") boolean gmail,
            @Value("${decokasa.recuperacion.envio-automatico:false}") boolean envio,
            @Value("${decokasa.recuperacion.url-base:}") String urlBase,
            @Value("${decokasa.recuperacion.lote:5}") int lote) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        RecuperacionRepositorio porOperacion = new RecuperacionRepositorio() {
            @Override
            public int contarDesde(UUID usuarioId, Instant desde) {
                return tx.execute(e -> recuperaciones.contarDesde(usuarioId, desde));
            }

            @Override
            public void registrar(UUID id, UUID usuarioId, Instant creadaEn, Instant expiraEn) {
                tx.executeWithoutResult(e -> recuperaciones.registrar(id, usuarioId, creadaEn, expiraEn));
            }

            @Override
            public Optional<Vigente> bloquearVigente(String tokenHash, Instant ahora) {
                throw new UnsupportedOperationException("Solo para restablecer, dentro de su transacción.");
            }

            @Override
            public void marcarUsada(UUID id, Instant ahora) {
                tx.executeWithoutResult(e -> recuperaciones.marcarUsada(id, ahora));
            }

            @Override
            public int anularDeUsuario(UUID usuarioId, Instant ahora) {
                return tx.execute(e -> recuperaciones.anularDeUsuario(usuarioId, ahora));
            }

            @Override
            public int anularCaducadas(Instant ahora) {
                return tx.execute(e -> recuperaciones.anularCaducadas(ahora));
            }

            @Override
            public Optional<Reclamada> reclamar(String tokenHash, Instant ahora) {
                return tx.execute(e -> recuperaciones.reclamar(tokenHash, ahora));
            }

            @Override
            public void marcarEnviada(UUID id, Instant ahora) {
                tx.executeWithoutResult(e -> recuperaciones.marcarEnviada(id, ahora));
            }

            @Override
            public void marcarFallida(UUID id, String codigo, Instant ahora) {
                tx.executeWithoutResult(e -> recuperaciones.marcarFallida(id, codigo, ahora));
            }

            @Override
            public void anular(UUID id, Instant ahora) {
                tx.executeWithoutResult(e -> recuperaciones.anular(id, ahora));
            }
        };
        return new EnviarCorreosRecuperacionCasoDeUso(porOperacion, correo, generador, reloj,
                oauth && gmail && envio, urlBase, lote);
    }
}
