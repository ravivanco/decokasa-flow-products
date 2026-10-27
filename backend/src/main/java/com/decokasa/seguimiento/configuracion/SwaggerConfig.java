package com.decokasa.seguimiento.configuracion;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Documentación de los endpoints implementados, con autorización Bearer para probar la API local. */
@Configuration
@Profile("dev")
public class SwaggerConfig {

    @Bean
    OpenAPI documentacionLocal() {
        return new OpenAPI()
                .info(new Info().title("Decokasa — API local").version("fase-1"))
                .components(new Components().addSecuritySchemes("JWT", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"));
    }

    @Bean
    OpenApiCustomizer documentarRutasPublicas() {
        return api -> {
            for (String ruta : List.of("/api/v1/auth/login", "/api/v1/auth/renovar",
                    "/api/v1/auth/cerrar-sesion")) {
                PathItem item = api.getPaths().get(ruta);
                if (item != null && item.getPost() != null) {
                    item.getPost().setSecurity(List.of());
                }
            }
            PathItem salud = api.getPaths().get("/api/v1/salud");
            if (salud != null && salud.getGet() != null) {
                salud.getGet().setSecurity(List.of());
            }
        };
    }
}
