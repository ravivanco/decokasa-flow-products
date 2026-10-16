package com.decokasa.seguimiento.flujos.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.decokasa.seguimiento.compartido.domain.AccionEtapa.*;
import static com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.flujos.domain.*;
import com.decokasa.seguimiento.flujos.application.port.out.DefinicionFlujoRepositorio;
import com.decokasa.seguimiento.flujos.application.usecase.ResolverTransicionCasoDeUso;
import com.decokasa.seguimiento.flujos.application.usecase.ConsultarDefinicionFlujoCasoDeUso;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FlujoFaseDosTest {
    private final DefinicionFlujoRepositorio repo=mock(DefinicionFlujoRepositorio.class);
    private final ResolverTransicionCasoDeUso caso=new ResolverTransicionCasoDeUso(repo);
    private final UUID id=UUID.randomUUID();
    private DefinicionFlujo flujo() {
        var etapas=java.util.stream.IntStream.rangeClosed(1,11).mapToObj(n -> new DefinicionEtapa(n,"Etapa ficticia "+n,
                n==11?null:rol(n),n==1||n==11?new PlazoDeEtapa.SinPlazo():n>=5?new PlazoDeEtapa.Pendiente():
                n==4?new PlazoDeEtapa.Definido(3,TipoPlazo.DIAS_HABILES):new PlazoDeEtapa.Definido(24,TipoPlazo.HORAS_HABILES),n==11)).toList();
        return new DefinicionFlujo(id,CodigoPais.EC,TipoFlujo.A,1,false,etapas,List.of(
                transicion(1,ENVIAR,2),transicion(2,ENVIAR,3),transicion(3,APROBAR,4),transicion(3,RECHAZAR,2),
                transicion(4,APROBAR,5),transicion(4,DEVOLVER_POR_ERROR,2),transicion(4,DEVOLVER_POR_PRODUCTO_NUEVO,2)));
    }
    private TransicionPermitida transicion(int de,AccionEtapa accion,int a) {
        var r=regla(de,accion,a);
        return new TransicionPermitida(de,accion,a,accion==RECHAZAR||accion==DEVOLVER_POR_ERROR||accion==DEVOLVER_POR_PRODUCTO_NUEVO,
                accion==DEVOLVER_POR_PRODUCTO_NUEVO,false,RequisitosDocumentales.DEFINIDOS,r.documentosObligatorios());
    }
    @ParameterizedTest @CsvSource({"1,ENVIAR,2", "2,ENVIAR,3", "3,APROBAR,4", "3,RECHAZAR,2",
            "4,APROBAR,5", "4,DEVOLVER_POR_ERROR,2", "4,DEVOLVER_POR_PRODUCTO_NUEVO,2"})
    void resuelveTransicionesDeVersionDelPedidoAunqueYaNoSeaVigente(int origen,AccionEtapa accion,int destino) {
        when(repo.buscarPorId(id)).thenReturn(Optional.of(flujo()));
        var r=caso.resolver(id,origen,accion);
        assertThat(r.etapaDestino()).isEqualTo(destino); assertThat(r.rolResponsableOrigen()).isEqualTo(rol(origen));
        assertThat(r.destinoEsRetornoDeUrgencia()).isEqualTo(destino==4);
        assertThat(r.documentosObligatorios()).isEqualTo(regla(origen,accion,destino).documentosObligatorios());
        verify(repo,never()).buscarVigente(any(),any());
    }
    @Test void accionNoConfiguradaSeRechaza() {
        when(repo.buscarPorId(id)).thenReturn(Optional.of(flujo()));
        assertThatThrownBy(() -> caso.resolver(id,1,APROBAR)).isInstanceOf(TransicionNoPermitida.class);
    }
    @Test void retornoDeUrgenciaSeDeduceDeDatosNoDelNumeroCuatro() {
        var etapas=List.of(new DefinicionEtapa(8,"Análisis ficticio",CHINA,new PlazoDeEtapa.Pendiente(),false),
                new DefinicionEtapa(2,"Codificación ficticia",MARKETING,new PlazoDeEtapa.SinPlazo(),false));
        var flujo=new DefinicionFlujo(id,CodigoPais.EC,TipoFlujo.A,2,true,etapas,
                List.of(transicion(8,DEVOLVER_POR_PRODUCTO_NUEVO,2)));
        assertThat(flujo.esRetornoDeUrgencia(8)).isTrue(); assertThat(flujo.esRetornoDeUrgencia(4)).isFalse();
    }

    @Test void consultaDistingueVersionHistoricaDeDefinicionVigente() {
        var historico=flujo();
        var vigente=new DefinicionFlujo(UUID.randomUUID(),CodigoPais.EC,TipoFlujo.A,2,true,
                historico.etapas(),historico.transiciones());
        when(repo.buscarVersion(CodigoPais.EC,TipoFlujo.A,1)).thenReturn(Optional.of(historico));
        when(repo.buscarPorId(id)).thenReturn(Optional.of(historico));
        when(repo.buscarVigente(CodigoPais.EC,TipoFlujo.A)).thenReturn(Optional.of(vigente));
        var consulta=new ConsultarDefinicionFlujoCasoDeUso(repo);
        assertThat(consulta.porId(id)).isSameAs(historico);
        assertThat(consulta.version(CodigoPais.EC,TipoFlujo.A,1)).isSameAs(historico);
        assertThat(consulta.vigente(CodigoPais.EC,TipoFlujo.A)).isSameAs(vigente);
        assertThat(consulta.porId(id).version()).isEqualTo(1);
    }

    @Test void definicionesAusentesNoSeSustituyenPorOtraVersion() {
        var consulta=new ConsultarDefinicionFlujoCasoDeUso(repo);
        assertThatThrownBy(() -> consulta.porId(id)).isInstanceOf(NoEncontrado.class);
        assertThatThrownBy(() -> consulta.version(CodigoPais.EC,TipoFlujo.A,99)).isInstanceOf(NoEncontrado.class);
        assertThatThrownBy(() -> consulta.vigente(CodigoPais.EC,TipoFlujo.B)).isInstanceOf(NoEncontrado.class);
        assertThatThrownBy(() -> caso.resolver(id,1,ENVIAR)).isInstanceOf(NoEncontrado.class);
    }
}
