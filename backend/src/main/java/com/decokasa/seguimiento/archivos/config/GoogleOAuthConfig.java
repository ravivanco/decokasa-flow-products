package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.google.GoogleOAuthUsuario;
import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle;
import com.decokasa.seguimiento.archivos.application.usecase.AutorizarCuentaGoogleCasoDeUso;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** Consentimiento 3.1 y credencial interna 3.2. Sin workers ni acceso a secretos/red al arrancar. */
@Configuration(proxyBeanMethods = false)
public class GoogleOAuthConfig {
    @Bean
    GoogleOAuthUsuario googleOAuthUsuario(ConsultarResponsables usuarios, Reloj reloj, JsonMapper json,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean habilitada,
            @Value("${decokasa.google.oauth.cliente-ruta:}") String cliente,
            @Value("${decokasa.google.oauth.tokens-ruta:}") String tokens,
            @Value("${decokasa.google.oauth.llave-ruta:}") String llave,
            @Value("${decokasa.google.oauth.cuenta-email:}") String correo,
            @Value("${decokasa.google.oauth.redirect-uri}") String retorno) {
        return new GoogleOAuthUsuario(habilitada, cliente, tokens, llave, correo, retorno,
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
