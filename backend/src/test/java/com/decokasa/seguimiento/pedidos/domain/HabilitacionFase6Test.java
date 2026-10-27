package com.decokasa.seguimiento.pedidos.domain;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.flujos.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class HabilitacionFase6Test {
    List<DefinicionEtapa> etapas(int fin){return java.util.stream.IntStream.rangeClosed(1,fin).mapToObj(n->new DefinicionEtapa(n,"Etapa "+n,n==fin?null:new CodigoRol("compras_china"),new PlazoDeEtapa.SinPlazo(),n==fin)).toList();}
    @Test void sinConcretarFallaYAlConcretarExcluyeSoloAutomaticas(){var h=EtapasHabilitadas.segunDefinicion();assertThatThrownBy(()->h.incluye(11)).isInstanceOf(IllegalStateException.class);
        assertThat(h.para(etapas(11)).numeros()).containsExactlyInAnyOrderElementsOf(java.util.stream.IntStream.range(1,11).boxed().toList());
        assertThat(h.para(etapas(12)).numeros()).contains(11).doesNotContain(12);assertThat(h.paraEtapa(12,false).numeros()).isEmpty();assertThat(h.paraEtapa(11,true).incluye(11)).isTrue();
        var fija=new EtapasHabilitadas(Set.of(2,4));assertThat(fija.para(etapas(12))).isSameAs(fija);assertThat(fija.paraEtapa(11,true)).isSameAs(fija);
        assertThatThrownBy(()->new EtapasHabilitadas(Set.of(1),true)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void ningunaDefinicionAceptaTransicionDesdeFinAutomatico(){for(int fin:List.of(11,12)){
        var transicion=new TransicionPermitida(fin,AccionEtapa.APROBAR,1,false,false,false,RequisitosDocumentales.DEFINIDOS,Set.of());
        assertThatThrownBy(()->new DefinicionFlujo(UUID.randomUUID(),CodigoPais.EC,fin==11?TipoFlujo.A:TipoFlujo.B,1,true,etapas(fin),List.of(transicion))).isInstanceOf(DatoInvalido.class).hasMessageContaining("automática");
    }}
}
