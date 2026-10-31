package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.google.GoogleOAuthUsuario;
import com.decokasa.seguimiento.archivos.adapter.out.google.SecretosGoogle;
import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle;
import com.decokasa.seguimiento.archivos.application.usecase.AutorizarCuentaGoogleCasoDeUso;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.util.Locale;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Consentimiento 3.1 y credencial interna 3.2. Sin workers ni acceso a secretos/red al arrancar. */
@Configuration(proxyBeanMethods = false)
public class GoogleOAuthConfig {

    /**
     * ADR-0034: {@code archivo} (paso 3.1, disco persistente) o {@code base} (Render: cliente y llave en variables de
     * entorno, tokens cifrados en PostgreSQL). Cualquier otro valor impide arrancar.
     */
    @Bean
    SecretosGoogle secretosGoogle(DataSource fuente,
            @Value("${decokasa.google.oauth.almacen:archivo}") String almacen,
            @Value("${decokasa.google.oauth.cliente-ruta:}") String cliente,
            @Value("${decokasa.google.oauth.tokens-ruta:}") String tokens,
            @Value("${decokasa.google.oauth.llave-ruta:}") String llave,
            @Value("${decokasa.google.oauth.cliente-id:}") String clienteId,
            @Value("${decokasa.google.oauth.cliente-secreto:}") String clienteSecreto,
            @Value("${decokasa.google.oauth.llave:}") String llaveBase64) {
        return switch (almacen.strip().toLowerCase(Locale.ROOT)) {
            case "archivo", "" -> SecretosGoogle.archivos(cliente, tokens, llave);
            case "base" -> SecretosGoogle.entorno(clienteId, clienteSecreto, llaveBase64, fuente);
            default -> throw new IllegalArgumentException("GOOGLE_OAUTH_ALMACEN debe ser 'archivo' o 'base'.");
        };
    }

    @Bean
    GoogleOAuthUsuario googleOAuthUsuario(ConsultarResponsables usuarios, Reloj reloj, JsonMapper json,
            SecretosGoogle secretos,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean habilitada,
            @Value("${decokasa.google.oauth.cuenta-email:}") String correo,
            @Value("${decokasa.google.oauth.redirect-uri}") String retorno,
            @Value("${decokasa.google.oauth.publicacion:EN_PRUEBAS}") String publicacion) {
        boolean produccion = switch (publicacion.strip().toUpperCase(Locale.ROOT)) {
            case "EN_PRUEBAS", "" -> false;
            case "PRODUCCION" -> true;
            default -> throw new IllegalArgumentException("GOOGLE_OAUTH_PUBLICACION debe ser EN_PRUEBAS o PRODUCCION.");
        };
        return new GoogleOAuthUsuario(habilitada, secretos, correo, retorno, produccion,
                json, reloj, adminId -> {
                    boolean vigente = usuarios.estado(adminId).filter(estado -> estado.activo()
                            && estado.roles().contains(new CodigoRol("admin"))).isPresent();
                    if (!vigente) { throw new SinPermiso("El admin que inició la conexión ya no tiene permiso."); }
                });
    }

    @Bean
    AutorizarCuentaGoogle autorizarCuentaGoogle(ConsultarResponsables usuarios, GoogleOAuthUsuario adaptador) {
        return new AutorizarCuentaGoogleCasoDeUso(usuarios, adaptador);
    }
}
