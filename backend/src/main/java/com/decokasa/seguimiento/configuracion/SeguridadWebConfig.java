package com.decokasa.seguimiento.configuracion;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Seguridad web de la API (ADR-0002, R6). Sin sesión de servidor, formulario de login ni autenticación básica: el
 * acceso usa un JWT de 15 minutos en {@code Authorization: Bearer} con los roles del usuario. Solo la salud y las
 * rutas de sesión son públicas, además del retorno/resultado OAuth con su protección de estado y navegador;
 * la gestión de usuarios y roles exige el rol admin (y el caso de uso lo revisa de
 * nuevo). Los 401 y 403 usan el formato Problema del contrato. CSRF de Spring no aplica al Bearer; las dos rutas con
 * cookie (/renovar y /cerrar-sesion) se protegen con SameSite=Strict y la verificación de Origin (D3b).
 */
@Configuration
@EnableWebSecurity
public class SeguridadWebConfig {

    static final String EMISOR_JWT = "decokasa";

    private static final String NO_AUTENTICADO = """
            {"type":"about:blank","title":"No autenticado","status":401,\
            "detail":"Inicia sesión para continuar.","codigo":"NO_AUTENTICADO"}""";

    private static final String SIN_PERMISO = """
            {"type":"about:blank","title":"Sin permiso","status":403,\
            "detail":"No tienes permiso para esta acción.","codigo":"SIN_PERMISO"}""";

    @Bean
    SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, JwtDecoder decodificador, Environment entorno)
            throws Exception {
        AuthenticationEntryPoint sinAutenticar = (solicitud, respuesta, error) ->
                escribirProblema(respuesta, HttpServletResponse.SC_UNAUTHORIZED, NO_AUTENTICADO);
        AccessDeniedHandler sinPermiso = (solicitud, respuesta, error) ->
                escribirProblema(respuesta, HttpServletResponse.SC_FORBIDDEN, SIN_PERMISO);
        http
                .csrf(csrf -> csrf.disable())
                // CORS lo aplica filtroCors, antes de esta cadena.
                .cors(cors -> cors.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basica -> basica.disable())
                .formLogin(formulario -> formulario.disable())
                .logout(salida -> salida.disable())
                .authorizeHttpRequests(rutas -> {
                    // Solo documentación en desarrollo; los endpoints de negocio conservan su autenticación.
                    if (entorno.acceptsProfiles(Profiles.of("dev"))) {
                        rutas.requestMatchers(HttpMethod.GET, "/swagger-ui.html", "/swagger-ui/**",
                                "/v3/api-docs", "/v3/api-docs/**").permitAll();
                    }
                    rutas.requestMatchers(HttpMethod.GET, "/api/v1/salud", "/actuator/health", "/actuator/health/**")
                        .permitAll()
                        // OAuth de usuario (paso 3.1): solo el retorno y su resultado no llevan JWT.
                        // El callback exige state de un solo uso, cookie de vínculo y admin vigente en aplicación.
                        .requestMatchers(HttpMethod.GET, "/api/v1/integraciones/google/oauth2/callback",
                                "/api/v1/integraciones/google/oauth2/resultado")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/renovar",
                                "/api/v1/auth/cerrar-sesion")
                        .permitAll()
                        // ADR-0033: recuperación pública (respuesta genérica y límite por IP); cambiar exige sesión.
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/recuperacion",
                                "/api/v1/auth/recuperacion/restablecer")
                        .permitAll()
                        .requestMatchers("/api/v1/usuarios/**", "/api/v1/roles/**").hasAuthority("ROL_admin")
                        .anyRequest().authenticated();
                })
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.decoder(decodificador).jwtAuthenticationConverter(conversorDeRoles()))
                        .authenticationEntryPoint(sinAutenticar)
                        .accessDeniedHandler(sinPermiso))
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(sinAutenticar)
                        .accessDeniedHandler(sinPermiso))
                .headers(cabeceras -> cabeceras
                        .frameOptions(marcos -> marcos.deny())
                        .referrerPolicy(referente -> referente.policy(ReferrerPolicy.NO_REFERRER)));
        return http.build();
    }

    /** Clave HS256 de los JWT. Sale de JWT_SECRETO y debe tener al menos 32 bytes; sin ella la aplicación no arranca. */
    @Bean
    SecretKey claveJwt(@Value("${decokasa.jwt.secreto:}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("Define JWT_SECRETO con al menos 32 bytes al azar (ADR-0002).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    /**
     * Valida firma, emisor y vigencia y, además, los validadores que aporten los módulos (ADR-0033: sello de
     * credenciales de {@code usuarios}, que rechaza los JWT emitidos antes de un cambio de contraseña).
     */
    @Bean
    JwtDecoder jwtDecoder(SecretKey claveJwt, ObjectProvider<OAuth2TokenValidator<Jwt>> adicionales) {
        NimbusJwtDecoder decodificador = NimbusJwtDecoder.withSecretKey(claveJwt)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        List<OAuth2TokenValidator<Jwt>> validadores = new ArrayList<>();
        validadores.add(JwtValidators.createDefaultWithIssuer(EMISOR_JWT));
        adicionales.orderedStream().forEach(validadores::add);
        decodificador.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadores));
        return decodificador;
    }

    /**
     * CORS solo para los orígenes de CORS_ORIGENES, con credenciales para que viaje la cookie de renovación. Corre
     * antes que la seguridad y, si rechaza un origen, responde 403 con el formato Problema del contrato.
     */
    @Bean
    FilterRegistrationBean<CorsFilter> filtroCors(@Value("${decokasa.cors.origenes}") List<String> origenes) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(origenes);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Content-Range", "X-Fragmento-SHA256"));
        cors.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", cors);

        CorsFilter filtro = new CorsFilter(fuente);
        filtro.setCorsProcessor(new DefaultCorsProcessor() {
            @Override
            protected void rejectRequest(ServerHttpResponse respuesta) throws IOException {
                respuesta.setStatusCode(HttpStatus.FORBIDDEN);
                respuesta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
                respuesta.getBody().write(SIN_PERMISO.getBytes(StandardCharsets.UTF_8));
                respuesta.flush();
            }
        });
        FilterRegistrationBean<CorsFilter> registro = new FilterRegistrationBean<>(filtro);
        registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registro;
    }

    // Los roles del token ("roles") pasan a autoridades ROL_<codigo>.
    private static JwtAuthenticationConverter conversorDeRoles() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROL_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(roles);
        return conversor;
    }

    private static void escribirProblema(HttpServletResponse respuesta, int estado, String cuerpo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(cuerpo);
    }
}
