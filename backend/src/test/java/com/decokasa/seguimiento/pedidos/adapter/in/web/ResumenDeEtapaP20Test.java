package com.decokasa.seguimiento.pedidos.adapter.in.web;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido.Cierre;
import com.decokasa.seguimiento.historial.application.port.in.ResumirEtapasDePedido.TipoCierre;
import com.decokasa.seguimiento.pedidos.domain.EstadoEtapa;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Casos de presentación que no requieren fabricar usuarios inexistentes en PostgreSQL. */
class ResumenDeEtapaP20Test {
    @Test void nombreNoDisponibleUsaIdentificadorYRolHistoricoSinInventarPersona() {
        UUID actor=UUID.randomUUID();
        var cierre=new Cierre(3,TipoCierre.RECHAZADA,actor,null,new CodigoRol("asistente_compras"),null,
                Instant.parse("2026-10-16T02:30:00Z"),2,null,"Corregir",0);
        assertThat(ResumenDeEtapa.texto("A",EstadoEtapa.DEVUELTA,cierre)).isEqualTo(
                "Rechazada por "+actor+" (asistente_compras) el 15/10/2026 21:30 · vuelve a A2. Observación: «Corregir».");
    }
    @Test void sinActorNoInventaNombreNiRolYCuentaArchivosPresentados() {
        var cierre=new Cierre(2,TipoCierre.ENVIADA,null,null,null,null,
                Instant.parse("2026-10-16T12:00:00Z"),3,null,null,2);
        assertThat(ResumenDeEtapa.texto("A",EstadoEtapa.COMPLETADA,cierre))
                .isEqualTo("Enviada el 16/10/2026 07:00 · pasa a A3. 2 archivos presentados.");
    }
    @ParameterizedTest @ValueSource(ints={199,200,201})
    void recorteRespetaLimitePorPuntosDeCodigo(int longitud) {
        String original="\uD83D\uDE00".repeat(longitud);
        String esperado=longitud<=200?original:"\uD83D\uDE00".repeat(200)+"…";
        assertThat(ResumenDeEtapa.recortar(original)).isEqualTo(esperado);
    }
    @Test void sinEventoDeCierreNoInventaResumen() {
        for(EstadoEtapa estado:EstadoEtapa.values()) assertThat(ResumenDeEtapa.texto("A",estado,null)).isNull();
    }
}
