package com.decokasa.seguimiento.usuarios.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.adapter.in.arranque.CargadorUsuariosIniciales;
import com.decokasa.seguimiento.usuarios.adapter.out.security.BCryptCodificador;
import com.decokasa.seguimiento.usuarios.adapter.out.security.GeneradorTokenSeguro;
import com.decokasa.seguimiento.usuarios.adapter.out.security.JwtEmisor;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ActualizarUsuario;
import com.decokasa.seguimiento.usuarios.application.port.in.CerrarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarRoles;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearRol;
import com.decokasa.seguimiento.usuarios.application.port.in.CrearUsuario;
import com.decokasa.seguimiento.usuarios.application.port.in.IniciarSesion;
import com.decokasa.seguimiento.usuarios.application.port.in.ObtenerUsuarioActual;
import com.decokasa.seguimiento.usuarios.application.port.in.RenovarSesion;
import com.decokasa.seguimiento.usuarios.application.port.out.CodificadorContrasena;
import com.decokasa.seguimiento.usuarios.application.port.out.EmisorTokens;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.TokenRenovacionRepositorio;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.application.usecase.ActualizarRolCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ActualizarUsuarioCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.CerrarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ConsultarRolesCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ConsultarUsuariosCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.CrearRolCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.CrearUsuarioCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.IniciarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.ObtenerUsuarioActualCasoDeUso;
import com.decokasa.seguimiento.usuarios.application.usecase.RenovarSesionCasoDeUso;
import com.decokasa.seguimiento.usuarios.domain.CredencialesInvalidas;
import com.decokasa.seguimiento.usuarios.domain.CuentaBloqueada;
import com.decokasa.seguimiento.usuarios.domain.PoliticaBloqueo;
import com.decokasa.seguimiento.usuarios.domain.PoliticaSesion;
import com.decokasa.seguimiento.usuarios.domain.SesionInvalida;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * Raíz de composición del módulo usuarios (arquitectura.md, sección 1): elige los adaptadores de cada puerto,
 * registra los casos de uso y abre la transacción alrededor de cada uno (ADR-0012). Los valores de sesión y de
 * bloqueo vienen de la configuración, con los aprobados por defecto (ADR-0002: D2 y D3).
 */
@Configuration(proxyBeanMethods = false)
public class UsuariosConfig {

    @Bean
    Transacciones transaccionesUsuarios(PlatformTransactionManager gestor) {
        return new Transacciones(gestor);
    }

    @Bean
    PoliticaBloqueo politicaBloqueo(@Value("${decokasa.bloqueo.intentos-maximos}") int intentosMaximos,
            @Value("${decokasa.bloqueo.duracion}") Duration duracion) {
        return new PoliticaBloqueo(intentosMaximos, duracion);
    }

    @Bean
    PoliticaSesion politicaSesion(@Value("${decokasa.sesion.duracion-acceso}") Duration acceso,
            @Value("${decokasa.sesion.duracion-renovacion}") Duration renovacion) {
        return new PoliticaSesion(acceso, renovacion);
    }

    @Bean
    CodificadorContrasena codificadorContrasena(@Value("${decokasa.bcrypt.fuerza:12}") int fuerza) {
        return new BCryptCodificador(fuerza);
    }

    @Bean
    EmisorTokens emisorTokens(SecretKey claveJwt) {
        return new JwtEmisor(claveJwt);
    }

    @Bean
    GeneradorTokenRenovacion generadorTokenRenovacion() {
        return new GeneradorTokenSeguro();
    }

    // --- Casos de uso, cada uno dentro de su transacción ---

    @Bean
    IniciarSesion iniciarSesion(Transacciones tx, UsuarioRepositorio usuarios, TokenRenovacionRepositorio tokens,
            CodificadorContrasena codificador, EmisorTokens emisor, GeneradorTokenRenovacion generador, Reloj reloj,
            PoliticaBloqueo politicaBloqueo, PoliticaSesion politicaSesion) {
        IniciarSesion caso = new IniciarSesionCasoDeUso(usuarios, tokens, codificador, emisor, generador, reloj,
                politicaBloqueo, politicaSesion);
        // El intento fallido se guarda aunque la respuesta sea 401 o 423 (bloqueo de D2).
        return (identificador, contrasena) -> tx.ejecutarConfirmandoAnte(() -> caso.iniciar(identificador, contrasena),
                error -> error instanceof CredencialesInvalidas || error instanceof CuentaBloqueada);
    }

    @Bean
    RenovarSesion renovarSesion(Transacciones tx, UsuarioRepositorio usuarios, TokenRenovacionRepositorio tokens,
            EmisorTokens emisor, GeneradorTokenRenovacion generador, Reloj reloj, PoliticaSesion politicaSesion) {
        RenovarSesion caso = new RenovarSesionCasoDeUso(usuarios, tokens, emisor, generador, reloj, politicaSesion);
        // La revocación de la familia se guarda aunque la respuesta sea 401.
        return token -> tx.ejecutarConfirmandoAnte(() -> caso.renovar(token), SesionInvalida.class::isInstance);
    }

    @Bean
    CerrarSesion cerrarSesion(Transacciones tx, TokenRenovacionRepositorio tokens, GeneradorTokenRenovacion generador,
            Reloj reloj) {
        CerrarSesion caso = new CerrarSesionCasoDeUso(tokens, generador, reloj);
        return token -> tx.ejecutar(() -> {
            caso.cerrar(token);
            return null;
        });
    }

    @Bean
    ObtenerUsuarioActual obtenerUsuarioActual(Transacciones tx, UsuarioRepositorio usuarios, RolRepositorio roles) {
        ObtenerUsuarioActual caso = new ObtenerUsuarioActualCasoDeUso(usuarios, roles);
        return usuarioId -> tx.ejecutar(() -> caso.obtener(usuarioId));
    }

    @Bean
    ConsultarUsuarios consultarUsuarios(Transacciones tx, UsuarioRepositorio usuarios) {
        ConsultarUsuarios caso = new ConsultarUsuariosCasoDeUso(usuarios);
        return new ConsultarUsuarios() {
            @Override
            public List<Usuario> listar(Solicitante solicitante, Boolean activo) {
                return tx.ejecutar(() -> caso.listar(solicitante, activo));
            }

            @Override
            public Usuario obtener(Solicitante solicitante, UUID usuarioId) {
                return tx.ejecutar(() -> caso.obtener(solicitante, usuarioId));
            }
        };
    }

    @Bean
    CrearUsuario crearUsuario(Transacciones tx, UsuarioRepositorio usuarios, RolRepositorio roles,
            CodificadorContrasena codificador) {
        CrearUsuario caso = new CrearUsuarioCasoDeUso(usuarios, roles, codificador);
        return (solicitante, comando) -> tx.ejecutar(() -> caso.crear(solicitante, comando));
    }

    @Bean
    ActualizarUsuario actualizarUsuario(Transacciones tx, UsuarioRepositorio usuarios, RolRepositorio roles,
            TokenRenovacionRepositorio tokens, Reloj reloj) {
        ActualizarUsuario caso = new ActualizarUsuarioCasoDeUso(usuarios, roles, tokens, reloj);
        return (solicitante, comando) -> tx.ejecutar(() -> caso.actualizar(solicitante, comando));
    }

    @Bean
    ConsultarRoles consultarRoles(Transacciones tx, RolRepositorio roles, UsuarioRepositorio usuarios) {
        ConsultarRoles caso = new ConsultarRolesCasoDeUso(roles, usuarios);
        return solicitante -> tx.ejecutar(() -> caso.listar(solicitante));
    }

    @Bean
    CrearRol crearRol(Transacciones tx, RolRepositorio roles, UsuarioRepositorio usuarios) {
        CrearRol caso = new CrearRolCasoDeUso(roles, usuarios);
        return (solicitante, comando) -> tx.ejecutar(() -> caso.crear(solicitante, comando));
    }

    @Bean
    ActualizarRol actualizarRol(Transacciones tx, RolRepositorio roles, UsuarioRepositorio usuarios) {
        ActualizarRol caso = new ActualizarRolCasoDeUso(roles, usuarios);
        return (solicitante, comando) -> tx.ejecutar(() -> caso.actualizar(solicitante, comando));
    }

    // --- Usuarios iniciales fuera de Git (ADR-0015) ---

    @Bean
    @ConditionalOnProperty(name = "decokasa.semilla.habilitada", havingValue = "true")
    ApplicationRunner cargadorUsuariosIniciales(Transacciones tx, UsuarioRepositorio usuarios, RolRepositorio roles,
            CodificadorContrasena codificador, JsonMapper json, @Value("${decokasa.semilla.archivo:}") String archivo,
            @Value("${decokasa.semilla.contrasena-temporal:}") String contrasenaTemporal) {
        // Alta propia de la carga inicial: no es un bean, así que la API no puede usarla (A1).
        CrearUsuario cargaInicial = CrearUsuarioCasoDeUso.paraCargaInicial(usuarios, roles, codificador);
        CrearUsuario crearUsuario = (solicitante, comando) -> tx.ejecutar(() -> cargaInicial.crear(solicitante,
                comando));
        CargadorUsuariosIniciales cargador = new CargadorUsuariosIniciales(crearUsuario,
                () -> tx.ejecutar(() -> !usuarios.listar().isEmpty()), json,
                archivo.isBlank() ? null : Path.of(archivo), contrasenaTemporal);
        return argumentos -> cargador.cargar();
    }
}
