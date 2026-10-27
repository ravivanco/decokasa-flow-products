package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.drive.GoogleDriveAlmacen;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.compartido.application.port.out.AccesoCuentaGoogle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class DriveConfig {
    @Bean AlmacenArchivos almacenArchivos(AccesoCuentaGoogle acceso, JsonMapper json,
            com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj,
            @Value("${decokasa.google.drive.habilitada:false}") boolean habilitada) {
        return new GoogleDriveAlmacen(habilitada,acceso,json,reloj);
    }
}
