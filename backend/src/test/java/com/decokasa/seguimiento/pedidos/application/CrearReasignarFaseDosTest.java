package com.decokasa.seguimiento.pedidos.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.eventos.*;
import com.decokasa.seguimiento.flujos.application.port.in.ConsultarDefinicionFlujo;
import com.decokasa.seguimiento.flujos.domain.*;
import com.decokasa.seguimiento.pedidos.application.port.in.*;
import com.decokasa.seguimiento.pedidos.application.port.out.*;
import com.decokasa.seguimiento.pedidos.application.usecase.*;
import com.decokasa.seguimiento.pedidos.domain.*;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CrearReasignarFaseDosTest {
    private final PedidoRepositorio repo=mock(PedidoRepositorio.class);
    private final ConsultarDefinicionFlujo flujos=mock(ConsultarDefinicionFlujo.class);
    private final ConsultarResponsables personas=mock(ConsultarResponsables.class);
    private final GeneradorNumeroPedido numeros=mock(GeneradorNumeroPedido.class);
    private final List<EventoDePedido> eventos=new ArrayList<>();
    private final Instant ahora=Instant.parse("2027-01-01T02:00:00Z"); // Aún 2026 en Ecuador.
    private DefinicionFlujo definicion(UUID id) {
        return new DefinicionFlujo(id,CodigoPais.EC,TipoFlujo.A,1,true,List.of(
                new DefinicionEtapa(1,"Solicitud ficticia",ADMIN,new PlazoDeEtapa.SinPlazo(),false),
                new DefinicionEtapa(2,"Codificación ficticia",MARKETING,new PlazoDeEtapa.Definido(24,TipoPlazo.HORAS_HABILES),false)),List.of());
    }
    private void estado(ActorVigente actor) {
        when(personas.estado(actor.usuarioId())).thenReturn(Optional.of(new ConsultarResponsables.EstadoVigente(
                actor.usuarioId(),actor.activo(),actor.roles())));
    }
    private CrearPedidoCasoDeUso crearCaso() {
        return new CrearPedidoCasoDeUso(flujos,repo,numeros,personas,CALCULO,eventos::add,() -> ahora);
    }
    @Test void crearUsaAnioEcuadorNumeroCompletoYRelojServidor() {
        estado(JEFE); var flujo=definicion(UUID.randomUUID());
        when(flujos.vigente(CodigoPais.EC,TipoFlujo.A)).thenReturn(flujo);
        when(numeros.siguiente(CodigoPais.EC,2026)).thenReturn(NumeroPedido.componer(CodigoPais.EC,2026,10000));
        var p=crearCaso().crear(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),
                new CrearPedido.Comando(TipoFlujo.A,CodigoPais.EC,"Pedido ficticio",null,false));
        assertThat(p.numero().valor()).isEqualTo("DK-EC-2026-10000");
        assertThat(p.creadoEn()).isEqualTo(ahora); assertThat(p.etapaActual()).isEqualTo(1);
        verify(repo).insertar(p); verify(numeros).siguiente(CodigoPais.EC,2026);
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.actor()).isEqualTo(new Interviniente(JEFE.usuarioId(),ADMIN));
            assertThat(e.ocurridoEn()).isEqualTo(ahora);
        });
    }
    @ParameterizedTest @CsvSource({"false,admin", "true,marketing"})
    void crearRevalidaAdminAntesDeReservarNumero(boolean activo,String rolActual) {
        var actor=new ActorVigente(UUID.randomUUID(),activo,Set.of(new CodigoRol(rolActual))); estado(actor);
        when(flujos.vigente(CodigoPais.EC,TipoFlujo.A)).thenReturn(definicion(UUID.randomUUID()));
        assertThatThrownBy(() -> crearCaso().crear(new Solicitante(actor.usuarioId(),Set.of(ADMIN)),
                new CrearPedido.Comando(TipoFlujo.A,CodigoPais.EC,"Pedido ficticio",null,false))).isInstanceOf(SinPermiso.class);
        verifyNoInteractions(numeros); verify(repo,never()).insertar(any()); assertThat(eventos).isEmpty();
    }
    @Test void reasignarReleeEstadoDelCandidatoYRegistraAdminYHora() {
        Pedido p=en(2); var candidato=actor(MARKETING); estado(JEFE); estado(candidato);
        when(repo.buscarParaActualizar(p.id())).thenReturn(Optional.of(p));
        when(flujos.porId(p.flujoId())).thenReturn(definicion(p.flujoId()));
        var caso=new ReasignarEtapaCasoDeUso(repo,flujos,personas,CALCULO,eventos::add,() -> ahora);
        var comando=new ReasignarEtapa.Comando(p.id(),2,candidato.usuarioId(),"Motivo ficticio");
        caso.reasignar(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),comando);
        assertThat(eventos).singleElement().satisfies(e -> {
            assertThat(e.actor()).isEqualTo(new Interviniente(JEFE.usuarioId(),ADMIN));
            assertThat(e.ocurridoEn()).isEqualTo(ahora);
        });
        verify(repo).actualizar(p);
        eventos.clear();
        when(personas.estado(candidato.usuarioId())).thenReturn(Optional.of(new ConsultarResponsables.EstadoVigente(
                candidato.usuarioId(),false,Set.of(MARKETING))));
        assertThatThrownBy(() -> caso.reasignar(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),comando)).isInstanceOf(DatoInvalido.class);
        assertThat(eventos).isEmpty(); verify(repo,times(1)).actualizar(p);
    }
}
