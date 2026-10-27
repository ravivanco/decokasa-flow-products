package com.decokasa.seguimiento.notificaciones.config;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.notificaciones.application.port.out.Notificador;
import java.nio.file.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;

/** Soporte opt-in del recorrido de Chrome; comparte solo fixtures, no servicios ni base operativa. */
@Tag("navegador-avisos")
@TestPropertySource(properties={"decokasa.cors.origenes=http://127.0.0.1:5187", "decokasa.bcrypt.fuerza=4"})
class AvisosNavegadorIT extends AvisosEscenariosIT {
    @Test void servirApiParaRecorridoAislado()throws Exception{
        String clave="Avisos-Ficticios-2026";
        sql.update("update usuario set contrasena_hash=? where id in (?,?)",new BCryptPasswordEncoder(4).encode(clave),admin,marketing);
        var a=avance("A",1,2,admin,null);ahora=T.plusSeconds(1);avance("A",2,3,marketing,null);
        sql.update("update etapa_pedido set vence_en=? where pedido_id=?",Timestamp.from(T.minusSeconds(3600)),a.evento().pedidoId());
        // El validador JWT usa el reloj del sistema: el login necesita una emisión vigente durante el recorrido.
        // Los escenarios de negocio/corte temporal conservan T fijo en AvisosEscenariosIT.
        ahora=Instant.now();
        Path preparado=Path.of("navegador.json"),fin=Path.of("fin-navegador");
        Files.deleteIfExists(fin);
        Files.writeString(preparado,json.writeValueAsString(Map.of("puerto",puerto,"admin",admin.toString(),"marketing",marketing.toString(),"clave",clave,"pedido",a.evento().pedidoId().toString())));
        System.out.printf("NAVEGADOR API aislada lista puerto=%d; usuarios ficticios en %s%n",puerto,preparado.toAbsolutePath());
        Instant limite=Instant.now().plusSeconds(600);
        while(!Files.exists(fin)&&Instant.now().isBefore(limite))Thread.sleep(250);
        assertThat(Files.exists(fin)).as("Recorrido Chrome finalizado explícitamente").isTrue();
        verifyNoInteractions(gmail,drive);
        System.out.println("NAVEGADOR terminado; cero llamadas a Google/Gmail");
    }
}
