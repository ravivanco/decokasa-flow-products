package com.decokasa.seguimiento.pedidos.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.*;
import static com.decokasa.seguimiento.compartido.domain.AccionEtapa.*;

import com.decokasa.seguimiento.compartido.application.port.out.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.eventos.*;
import com.decokasa.seguimiento.flujos.application.port.in.ResolverTransicion;
import com.decokasa.seguimiento.pedidos.application.port.in.EjecutarAccionEtapa;
import com.decokasa.seguimiento.pedidos.application.port.out.PedidoRepositorio;
import com.decokasa.seguimiento.pedidos.application.usecase.EjecutarAccionEtapaCasoDeUso;
import com.decokasa.seguimiento.pedidos.domain.*;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccionNucleoFaseDosTest {
    private final PedidoRepositorio repo=mock(PedidoRepositorio.class);
    private final ResolverTransicion resolver=mock(ResolverTransicion.class);
    private final ConsultarResponsables personas=mock(ConsultarResponsables.class);
    private final List<EventoDePedido> publicados=new ArrayList<>();
    private final Instant ahora=INICIO.plusSeconds(7200);
    private final EjecutarAccionEtapaCasoDeUso caso=new EjecutarAccionEtapaCasoDeUso(repo,resolver,
            EtapasHabilitadas.faseDos(),personas,CALCULO,publicados::add,() -> ahora);
    private void preparar(Pedido p, ActorVigente actor, AccionEtapa accion,int destino) {
        when(repo.buscarParaActualizar(p.id())).thenReturn(Optional.of(p));
        when(personas.estado(actor.usuarioId())).thenReturn(Optional.of(
                new ConsultarResponsables.EstadoVigente(actor.usuarioId(),actor.activo(),actor.roles())));
        var r=regla(p.etapaActual(),accion,destino);
        when(resolver.resolver(p.flujoId(),p.etapaActual(),accion)).thenReturn(new ResolverTransicion.Resolucion(
                r.etapaOrigen(),r.accion(),r.etapaDestino(),r.requiereObservacion(),r.requiereUrgencia(),
                r.requiereBodega(),r.documentosPendientes(),r.documentosObligatorios(),r.plazoDestino(),
                r.destinoEsRetornoDeUrgencia(),r.rolResponsableOrigen()));
    }
    private EjecutarAccionEtapa.Comando comando(Pedido p,AccionEtapa accion,int etapa) {
        return new EjecutarAccionEtapa.Comando(p.id(),accion,etapa,"Observación ficticia",null);
    }
    @ParameterizedTest @CsvSource({"1,Solicitud", "2,Documentos codificados"})
    void sinArchivosA1YA2PermanecenBloqueadas(int etapa,String documento) {
        Pedido p=en(etapa); preparar(p,JEFE,ENVIAR,etapa+1);
        assertThatThrownBy(() -> caso.ejecutar(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),comando(p,ENVIAR,etapa)))
                .isInstanceOfSatisfying(RequisitoFaltante.class,e -> assertThat(e.faltantes()).containsExactly(documento));
        assertThat(p.etapaActual()).isEqualTo(etapa); verify(repo,never()).actualizar(any()); assertThat(publicados).isEmpty();
    }
    @Test void actorYRolVigentesYHoraDelPuertoLleganAlEvento() {
        Pedido p=en(3); var actor=actor(REVISION); preparar(p,actor,APROBAR,4);
        caso.ejecutar(new Solicitante(actor.usuarioId(),Set.of(ADMIN)),comando(p,APROBAR,3));
        assertThat(p.etapaActual()).isEqualTo(4); verify(repo).actualizar(p);
        assertThat(publicados).singleElement().satisfies(e -> {
            assertThat(e.actor()).isEqualTo(new Interviniente(actor.usuarioId(),REVISION));
            assertThat(e.ocurridoEn()).isEqualTo(ahora);
            assertThat(e.pedidoId()).isEqualTo(p.id()); assertThat(e.etapa()).isEqualTo(3);
        });
        assertThat(p.retirarEventos()).isEmpty();
    }
    @ParameterizedTest @CsvSource({"false,asistente_compras", "true,consulta"})
    void jwtAnteriorNoPermiteActuarSiSeDesactivaOPierdeRol(boolean activo,String rolActual) {
        Pedido p=en(3); var actor=new ActorVigente(UUID.randomUUID(),activo,Set.of(new CodigoRol(rolActual)));
        preparar(p,actor,APROBAR,4);
        assertThatThrownBy(() -> caso.ejecutar(new Solicitante(actor.usuarioId(),Set.of(ADMIN,REVISION)),comando(p,APROBAR,3)))
                .isInstanceOf(SinPermiso.class);
        assertThat(p.etapaActual()).isEqualTo(3); verify(repo,never()).actualizar(any()); assertThat(publicados).isEmpty();
    }
    @Test void segundaAccionTrasPrimerAvanceEsRechazadaSinOtroEvento() {
        Pedido p=en(3); preparar(p,JEFE,APROBAR,4); var comando=comando(p,APROBAR,3);
        caso.ejecutar(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),comando);
        assertThatThrownBy(() -> caso.ejecutar(new Solicitante(JEFE.usuarioId(),Set.of(ADMIN)),comando))
                .isInstanceOf(EtapaCambio.class);
        verify(repo,times(1)).actualizar(p); assertThat(publicados).hasSize(1); assertThat(p.etapaActual()).isEqualTo(4);
        // Modela la segunda lectura después del bloqueo, no demuestra bloqueo real ni concurrencia PostgreSQL (2.B2).
    }
}
