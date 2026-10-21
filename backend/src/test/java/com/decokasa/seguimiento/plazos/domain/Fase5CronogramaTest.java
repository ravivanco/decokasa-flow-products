package com.decokasa.seguimiento.plazos.domain;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.compartido.domain.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Reglas aprobadas F5-D1 y ADR-0006; ejemplos independientes del calendario demo. */
class Fase5CronogramaTest {
    final CalendarioLaboral calendario=new CalendarioLaboral(ZoneId.of("America/Guayaquil"));
    static Instant instante(String s){return Instant.parse(s);}
    static PlazoDeEtapa.Definido plazo(int n,TipoPlazo t){return new PlazoDeEtapa.Definido(n,t);}
    static Cronograma.EtapaDelPedido etapa(int n,PlazoDeEtapa p,boolean llegada,Instant entrada,Instant salida){
        return new Cronograma.EtapaDelPedido(n,"Etapa ficticia "+n,p,llegada,entrada,salida);
    }
    Cronograma calcular(int actual,List<Cronograma.EtapaDelPedido> etapas,Instant real,LocalDate estimada){
        return Cronograma.calcular(new Cronograma.DatosDelPedido(UUID.randomUUID(),instante("2026-10-16T22:00:00Z"),actual,false,etapas,real,estimada),new ConfiguracionPlazos(90,OrigenMeta.PROPUESTA,null,null,0),calendario);
    }
    @ParameterizedTest @CsvSource({
        "2026-10-16T22:00:00Z,DIAS_HABILES,2,2026-10-20T22:00:00Z,false",
        "2026-10-16T22:00:00Z,DIAS_CORRIDOS,2,2026-10-19T22:00:00Z,true",
        "2026-10-16T22:00:00Z,HORAS_HABILES,24,2026-10-19T22:00:00Z,false",
        "2026-10-17T04:00:00Z,HORAS_HABILES,1,2026-10-19T05:00:00Z,true",
        "2026-10-17T05:00:00Z,HORAS_HABILES,1,2026-10-19T06:00:00Z,false",
        "2026-10-18T18:00:00Z,DIAS_HABILES,1,2026-10-19T18:00:00Z,false"
    }) void calendarioDistingueTiposYFronteras(String inicio,TipoPlazo tipo,int cantidad,String fin,boolean movida){
        var r=calendario.finDePlazo(instante(inicio),plazo(cantidad,tipo));
        assertThat(r.momento()).isEqualTo(instante(fin));assertThat(r.movidaAlLunes()).isEqualTo(movida);
        assertThat(r.fecha()).isEqualTo(instante(fin).atZone(ZoneId.of("America/Guayaquil")).toLocalDate());
        assertThat(calendario.vencimiento(instante(inicio),plazo(cantidad,tipo))).isEqualTo(r.momento());
    }
    @Test void medianocheEsDeEcuadorYNoDelNavegador(){
        assertThat(calendario.fecha(instante("2026-10-20T04:59:59.999999999Z"))).isEqualTo(LocalDate.of(2026,10,19));
        assertThat(calendario.fecha(instante("2026-10-20T05:00:00Z"))).isEqualTo(LocalDate.of(2026,10,20));
        assertThat(calendario.inicioDelDia(LocalDate.of(2026,10,20))).isEqualTo(instante("2026-10-20T05:00:00Z"));
    }
    @Test void pendienteNoEsCeroNiSinPlazoYPropagaSinEstimar(){
        var r=calcular(1,List.of(etapa(1,new PlazoDeEtapa.SinPlazo(),false,null,null),etapa(2,new PlazoDeEtapa.Pendiente(),false,null,null),etapa(3,plazo(2,TipoPlazo.DIAS_HABILES),true,null,null)),null,null);
        assertThat(r.etapas().get(0).fechaPlanificada()).isEqualTo(LocalDate.of(2026,10,16));
        assertThat(r.etapas().get(1).fechaPlanificada()).isNull();assertThat(r.etapas().get(2).estado()).isEqualTo(Cronograma.EstadoEtapa.SIN_ESTIMAR);
        assertThat(r.etapas().get(2).fechaPlanificada()).isNull();assertThat(r.calculable()).isFalse();
        assertThat(r.totalDias()).isNull();assertThat(r.diferenciaContraMeta()).isNull();assertThat(r.llegada()).isNull();
        assertThat(r.faltantes()).extracting(Cronograma.Faltante::numero).containsExactly(2);
    }
    @Test void salidaRealArrastraYReentradaUsaUltimaEntradaSinArrastrarEntradasFuturasViejas(){
        var r=calcular(2,List.of(etapa(1,new PlazoDeEtapa.SinPlazo(),false,null,instante("2026-10-19T15:00:00Z")),
                etapa(2,plazo(1,TipoPlazo.DIAS_HABILES),false,instante("2026-10-21T18:00:00Z"),null),
                etapa(3,plazo(1,TipoPlazo.DIAS_HABILES),true,instante("2020-01-01T12:00:00Z"),instante("2020-01-02T12:00:00Z"))),null,null);
        assertThat(r.etapas().get(0).fechaReal()).isEqualTo(LocalDate.of(2026,10,19));
        assertThat(r.etapas().get(1).fechaPlanificada()).isEqualTo(LocalDate.of(2026,10,22));
        assertThat(r.etapas().get(2).fechaPlanificada()).isEqualTo(LocalDate.of(2026,10,23));
        assertThat(r.totalDias()).isEqualTo(7);assertThat(r.diferenciaContraMeta()).isEqualTo(-83);
        // Diferencia favorable contra PROPUESTA no significa objetivo aprobado por el cliente.
        assertThat(r.origenMeta()).isEqualTo(OrigenMeta.PROPUESTA);
    }
    @Test void anclaEstimadaRetomaCalculoTrasPendientesYRealTienePrioridad(){
        var etapas=List.of(etapa(1,new PlazoDeEtapa.Pendiente(),false,null,null),etapa(2,new PlazoDeEtapa.Pendiente(),true,null,null),etapa(3,plazo(24,TipoPlazo.HORAS_HABILES),false,null,null));
        var e=calcular(2,etapas,null,LocalDate.of(2026,10,19));
        assertThat(e.llegada().procedencia()).isEqualTo(Cronograma.Procedencia.LLEGADA_ESTIMADA);
        assertThat(e.etapas().get(2).fechaPlanificada()).isEqualTo(LocalDate.of(2026,10,20));
        assertThat(e.totalDias()).isEqualTo(3);assertThat(e.etapas().get(1).plazoPendiente()).isTrue();
        var real=calcular(3,etapas,instante("2026-10-22T04:00:00Z"),LocalDate.of(2026,10,19));
        assertThat(real.llegada().fecha()).isEqualTo(LocalDate.of(2026,10,21));
        assertThat(real.llegada().procedencia()).isEqualTo(Cronograma.Procedencia.LLEGADA_REAL);
        assertThat(real.etapas().get(1).fechaEstimada()).isNull();
        assertThat(real.etapas().get(2).fechaPlanificada()).isEqualTo(LocalDate.of(2026,10,22));
        assertThat(real.totalDias()).isEqualTo(5);
    }
    @Test void calculableNoInventaMetaAprobadaYListasSonInmutables(){
        var etapas=new ArrayList<Cronograma.EtapaDelPedido>();etapas.add(etapa(1,plazo(1,TipoPlazo.DIAS_CORRIDOS),true,null,null));
        var r=calcular(1,etapas,null,null);etapas.clear();assertThat(r.etapas()).hasSize(1);
        assertThatThrownBy(()->r.etapas().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(()->r.faltantes().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(r.etapas().get(0).movidaAlLunes()).isTrue();
        assertThat(r.metaDias()).isEqualTo(90);assertThat(r.origenMeta()).isEqualTo(OrigenMeta.PROPUESTA);
    }
}
