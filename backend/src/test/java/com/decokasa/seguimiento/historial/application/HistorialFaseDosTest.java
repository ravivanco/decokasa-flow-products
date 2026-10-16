package com.decokasa.seguimiento.historial.application;

import static org.assertj.core.api.Assertions.*;
import static com.decokasa.seguimiento.pedidos.domain.NucleoFaseDosTest.*;
import static com.decokasa.seguimiento.compartido.domain.AccionEtapa.*;
import com.decokasa.seguimiento.compartido.domain.NivelUrgencia;
import com.decokasa.seguimiento.compartido.domain.eventos.*;
import com.decokasa.seguimiento.historial.application.port.out.HistorialRepositorio;
import com.decokasa.seguimiento.historial.application.usecase.RegistrarEventoPedidoCasoDeUso;
import com.decokasa.seguimiento.historial.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class HistorialFaseDosTest {
    @Test void insertaCadaEventoSinAlterarLosAnterioresYConservaActorRolHoraYDetalle() {
        var registros=new ArrayList<EventoPedido>();
        HistorialRepositorio repo=new HistorialRepositorio() {
            public void insertar(EventoPedido e) { registros.add(e); }
            public List<EventoPedido> listarPorPedido(UUID id) { return registros.stream().filter(e -> e.pedidoId().equals(id)).toList(); }
        };
        var caso=new RegistrarEventoPedidoCasoDeUso(repo);
        var p=nuevo(); var eventos=new ArrayList<>(p.retirarEventos());
        eventos.add(mover(p,ENVIAR,2,INICIO.plusSeconds(60),null));
        eventos.add(p.reasignar(2,MARKETING,JEFE,actor(MARKETING),"Reasignación ficticia",INICIO.plusSeconds(90)).orElseThrow());
        eventos.add(mover(p,ENVIAR,3,INICIO.plusSeconds(120),null));
        eventos.add(mover(p,RECHAZAR,2,INICIO.plusSeconds(180),null));
        eventos.add(mover(p,ENVIAR,3,INICIO.plusSeconds(240),null));
        eventos.add(mover(p,APROBAR,4,INICIO.plusSeconds(300),null));
        eventos.add(mover(p,DEVOLVER_POR_ERROR,2,INICIO.plusSeconds(360),null));
        eventos.add(mover(p,ENVIAR,3,INICIO.plusSeconds(420),null));
        eventos.add(mover(p,APROBAR,4,INICIO.plusSeconds(480),null));
        eventos.add(mover(p,DEVOLVER_POR_PRODUCTO_NUEVO,2,INICIO.plusSeconds(540),NivelUrgencia.URGENTE));
        eventos.add(mover(p,ENVIAR,3,INICIO.plusSeconds(600),null));
        eventos.add(mover(p,APROBAR,4,INICIO.plusSeconds(660),null));
        for(var e:eventos) {
            var anteriores=List.copyOf(registros);
            caso.registrar(e);
            assertThat(registros.subList(0,anteriores.size())).containsExactlyElementsOf(anteriores);
            var guardado=registros.getLast();
            assertThat(guardado.usuarioId()).isEqualTo(e.actor().usuarioId());
            assertThat(guardado.rolCodigo()).isEqualTo(e.actor().rol());
            assertThat(guardado.ocurridoEn()).isEqualTo(e.ocurridoEn());
            assertThat(guardado.etapa()).isEqualTo(e.etapa());
            assertThat(guardado.pedidoId()).isEqualTo(e.pedidoId());
        }
        assertThat(registros.stream().map(EventoPedido::tipo)).contains(TipoEventoPedido.PEDIDO_CREADO,
                TipoEventoPedido.ETAPA_ENVIADA,TipoEventoPedido.ETAPA_RECHAZADA,TipoEventoPedido.ETAPA_APROBADA,
                TipoEventoPedido.PEDIDO_DEVUELTO,TipoEventoPedido.ETAPA_REASIGNADA);
        var devolucion=registros.stream().filter(e -> e.nivelUrgencia()!=null).findFirst().orElseThrow();
        assertThat(devolucion.nivelUrgencia()).isEqualTo(NivelUrgencia.URGENTE);
        assertThat(devolucion.detalle().motivoDevolucion()).isEqualTo(MotivoDevolucion.PRODUCTO_NUEVO);
        assertThat(devolucion.observacion()).isEqualTo("Observación ficticia");
        assertThat(registros.getLast().detalle().urgenciaTerminada()).isTrue();
        assertThat(registros.getLast().detalle().etapaDestino()).isEqualTo(4);
    }
}
