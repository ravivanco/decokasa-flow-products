package com.decokasa.seguimiento.pedidos.domain;

import static org.assertj.core.api.Assertions.*;
import static com.decokasa.seguimiento.compartido.domain.AccionEtapa.*;
import static com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.VENCE;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.flujos.domain.*;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** Reglas puras con agregado reconstruido en A7: no simula un recorrido que el producto bloquee en A6. */
class Fase4ReglasTest {
    static final CodigoRol ADMIN=new CodigoRol("admin");
    static final ActorVigente ACTOR=new ActorVigente(UUID.randomUUID(),true,Set.of(ADMIN));
    static final Instant ANTES=Instant.parse("2026-10-01T12:00:00Z");
    Pedido fixture(){
        var etapas=IntStream.rangeClosed(1,11).mapToObj(n->new EtapaDePedido(UUID.randomUUID(),n,
                n==7?EstadoEtapa.ACTUAL:EstadoEtapa.PENDIENTE,null,n==7?ANTES:null,null,null,0,n==7?UUID.randomUUID():null)).toList();
        return new Pedido(UUID.randomUUID(),NumeroPedido.componer(CodigoPais.EC,2026,1),CodigoPais.EC,TipoFlujo.A,
                UUID.randomUUID(),"Fixture puro F4",null,false,ACTOR.usuarioId(),ANTES,etapas,7,false,null,null,null,null,null,ANTES,0);
    }
    ReglaDeTransicion demora(){return new ReglaDeTransicion(7,REPORTAR_DEMORA,null,true,false,false,false,Set.of(),null,false,new CodigoRol("compras_china"));}
    @Test void ecuadorCercaDeMedianocheUsaHoyLocalYPrimeroAjustaAlLunes(){
        Pedido p=fixture();Instant ahora=Instant.parse("2026-10-20T04:30:00Z");
        LocalDate hoy=ahora.atZone(ZoneId.of("America/Guayaquil")).toLocalDate();assertThat(hoy).isEqualTo(LocalDate.of(2026,10,19));
        p.ejecutar(new AccionSolicitada(REPORTAR_DEMORA,7,"Domingo pasado ajustado a hoy",null,Set.of(),List.of(),LocalDate.of(2026,10,18)),demora(),EtapasHabilitadas.faseCuatroHastaBodega(),ACTOR,ahora,VENCE,hoy);
        assertThat(p.demora().orElseThrow().nuevaFechaEstimadaLlegada()).isEqualTo(hoy);
        assertThat(p.demora().orElseThrow().reportadaEn()).isEqualTo(ahora);assertThat(p.etapaActual()).isEqualTo(7);
        assertThat(p.etapas().get(6).entradaEn()).isEqualTo(ANTES);assertThat(p.llegadaAduanaEn()).isEmpty();
    }
    @Test void fechaEntreSemanaNoAgregaFechaSolicitadaDistinta(){
        Pedido p=fixture();Instant ahora=Instant.parse("2026-10-19T12:00:00Z");LocalDate hoy=LocalDate.of(2026,10,19);
        p.ejecutar(new AccionSolicitada(REPORTAR_DEMORA,7,"Demora ficticia",null,Set.of(),List.of(),hoy),demora(),EtapasHabilitadas.faseCuatroHastaBodega(),ACTOR,ahora,VENCE,hoy);
        var evento=p.retirarEventos();assertThat(evento).hasSize(1);
        assertThat(evento.getFirst()).isInstanceOfSatisfying(com.decokasa.seguimiento.compartido.domain.eventos.DemoraReportada.class,e->{
            assertThat(e.fechaSolicitada()).isNull();assertThat(e.nuevaFechaEstimadaLlegada()).isEqualTo(hoy);
        });
    }
    @Test void definicionNoPermiteLlegadaAutomaticaNiDosMarcas(){
        assertThatThrownBy(()->new DefinicionEtapa(11,"Fin",null,new PlazoDeEtapa.SinPlazo(),true,null,true)).isInstanceOf(DatoInvalido.class);
        var a=new DefinicionEtapa(7,"Aduana",ADMIN,new PlazoDeEtapa.Pendiente(),false,null,true);
        var b=new DefinicionEtapa(8,"Salida",ADMIN,new PlazoDeEtapa.Pendiente(),false,null,true);
        assertThatThrownBy(()->new DefinicionFlujo(UUID.randomUUID(),CodigoPais.EC,TipoFlujo.A,1,false,List.of(a,b),List.of())).isInstanceOf(DatoInvalido.class);
    }
    @Test void marcaDeLlegadaExigeTransicionQueAvance(){
        assertThatThrownBy(()->new ReglaDeTransicion(7,REPORTAR_DEMORA,null,true,false,false,false,Set.of(),null,false,ADMIN,true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->new ReglaDeTransicion(7,RECHAZAR,6,true,false,false,false,Set.of(),new PlazoDeEtapa.Pendiente(),false,ADMIN,true)).isInstanceOf(IllegalArgumentException.class);
    }
}
