package com.decokasa.seguimiento.configuracion;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

/**
 * Seguridad web de la API. En el paso 1.3 solo la salud es pública; todo lo demás exige autenticación (R6) y, mientras
 * no exista el inicio de sesión (paso 1.7, ADR-0002), responde 401. Sin sesión de servidor, sin formulario de login
 * ni autenticación básica. Los errores 401 y 403 usan el formato Problema del contrato.
 */
@Configuration
@EnableWebSecurity
public class SeguridadWebConfig {

    private static final String NO_AUTENTICADO = """
            {"type":"about:blank","title":"No autenticado","status":401,\
            "detail":"Inicia sesión para continuar.","codigo":"NO_AUTENTICADO"}""";

    private static final String SIN_PERMISO = """
            {"type":"about:blank","title":"Sin permiso","status":403,\
            "detail":"No tienes permiso para esta acción.","codigo":"SIN_PERMISO"}""";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basica -> basica.disable())
                .formLogin(formulario -> formulario.disable())
                .logout(salida -> salida.disable())
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.GET, "/api/v1/salud", "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint((solicitud, respuesta, error) ->
                                escribirProblema(respuesta, HttpServletResponse.SC_UNAUTHORIZED, NO_AUTENTICADO))
                        .accessDeniedHandler((solicitud, respuesta, error) ->
                                escribirProblema(respuesta, HttpServletResponse.SC_FORBIDDEN, SIN_PERMISO)))
                .headers(cabeceras -> cabeceras
                        .frameOptions(marcos -> marcos.deny())
                        .referrerPolicy(referente -> referente.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    private static void escribirProblema(HttpServletResponse respuesta, int estado, String cuerpo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(cuerpo);
    }
}
