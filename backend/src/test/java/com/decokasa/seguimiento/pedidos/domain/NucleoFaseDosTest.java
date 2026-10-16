package com.decokasa.seguimiento.pedidos.domain;

import static org.assertj.core.api.Assertions.*;
import static com.decokasa.seguimiento.compartido.domain.AccionEtapa.*;

import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.eventos.*;
import com.decokasa.seguimiento.plazos.application.usecase.CalcularVencimientoCasoDeUso;
import com.decokasa.seguimiento.plazos.domain.CalendarioLaboral;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

/** 2.B1: datos enteramente ficticios. Los documentos presentes son entradas del dominio,
 * no archivos existentes ni autorización para avanzar en el caso de uso real. */
public class NucleoFaseDosTest {
    public static final Instant INICIO = Instant.parse("2026-10-15T15:00:00Z");
    public static final CodigoRol ADMIN = new CodigoRol("admin");
    public static final CodigoRol MARKETING = new CodigoRol("marketing");
    public static final CodigoRol REVISION = new CodigoRol("asistente_compras");
    public static final CodigoRol CHINA = new CodigoRol("compras_china");
    public static final ActorVigente JEFE = actor(ADMIN);
    public static final CalcularVencimientoCasoDeUso CALCULO = new CalcularVencimientoCasoDeUso(
            new CalendarioLaboral(ZoneId.of("America/Guayaquil")));
    public static final Vencimientos VENCE = new Vencimientos() {
        public Optional<Instant> deEtapa(Instant entrada, PlazoDeEtapa plazo) { return CALCULO.deEtapa(entrada, plazo); }
        public Instant deUrgencia(Instant entrada, NivelUrgencia nivel) { return CALCULO.deUrgencia(entrada, nivel); }
    };
    public static ActorVigente actor(CodigoRol rol) { return new ActorVigente(UUID.randomUUID(), true, Set.of(rol)); }
    public static Pedido nuevo() {
        return Pedido.crear(NumeroPedido.componer(CodigoPais.EC, 2026, 7), CodigoPais.EC,
                new FlujoDelPedido(UUID.randomUUID(), TipoFlujo.A, IntStream.rangeClosed(1,11).boxed().toList(),
                        ADMIN, new PlazoDeEtapa.SinPlazo()), "Pedido ficticio B1", null, false, JEFE, INICIO, VENCE);
    }
    public static CodigoRol rol(int etapa) {
        return switch(etapa) { case 1 -> ADMIN; case 2 -> MARKETING; case 3 -> REVISION; default -> CHINA; };
    }
    public static ReglaDeTransicion regla(int origen, AccionEtapa accion, int destino) {
        Set<String> docs = origen == 1 ? Set.of("Solicitud") : origen == 2 ? Set.of("Documentos codificados") : Set.of();
        PlazoDeEtapa plazo = destino == 5 ? new PlazoDeEtapa.Pendiente() : destino == 4
                ? new PlazoDeEtapa.Definido(3, TipoPlazo.DIAS_HABILES)
                : new PlazoDeEtapa.Definido(24, TipoPlazo.HORAS_HABILES);
        return new ReglaDeTransicion(origen, accion, destino, false, false, false, false, docs, plazo,
                destino == 4, rol(origen));
    }
    public static EventoDePedido mover(Pedido p, AccionEtapa accion, int destino, Instant ahora, NivelUrgencia nivel) {
        var r = regla(p.etapaActual(), accion, destino);
        return p.ejecutar(new AccionSolicitada(accion, p.etapaActual(), "Observación ficticia", nivel,
                r.documentosObligatorios()), r, EtapasHabilitadas.faseDos(), JEFE, ahora, VENCE);
    }
    public static Pedido en(int etapa) {
        Pedido p = nuevo();
        for(int i=1; i<etapa; i++) mover(p, i<3 ? ENVIAR : APROBAR, i+1, INICIO.plusSeconds(i*60), null);
        p.retirarEventos();
        return p;
    }
    private static EtapaDePedido actual(Pedido p) { return p.etapas().get(p.etapaActual()-1); }
    private static void unaActual(Pedido p, int esperada) {
        assertThat(p.etapaActual()).isEqualTo(esperada);
        assertThat(p.etapas().stream().filter(e -> e.estado()==EstadoEtapa.ACTUAL).map(EtapaDePedido::numero))
                .containsExactly(esperada);
    }

    @Test void recorridoA1HastaA5YBloqueoDeAccionesPosteriores() {
        Pedido p=nuevo();
        unaActual(p,1);
        for(int i=1;i<=4;i++) {
            mover(p,i<3?ENVIAR:APROBAR,i+1,INICIO.plusSeconds(i*60),null);
            unaActual(p,i+1);
            assertThat(p.etapas().get(i-1).estado()).isEqualTo(EstadoEtapa.COMPLETADA);
        }
        assertThat(actual(p).venceEn()).isNull(); // Fabricación PENDIENTE, no cero ni 31 días.
        assertThatThrownBy(() -> mover(p, APROBAR, 6, INICIO.plusSeconds(500),null))
                .isInstanceOf(TransicionNoPermitida.class);
        unaActual(p,5);
    }

    @Test void etapaEsperadaViejaNoCambiaEstadoNiEmiteEvento() {
        Pedido p=en(3); Instant antes=p.actualizadoEn();
        assertThatThrownBy(() -> p.ejecutar(new AccionSolicitada(ENVIAR,2,null,null,Set.of()),
                regla(3,ENVIAR,4),EtapasHabilitadas.faseDos(),JEFE,INICIO,VENCE)).isInstanceOf(EtapaCambio.class);
        unaActual(p,3); assertThat(p.actualizadoEn()).isEqualTo(antes); assertThat(p.retirarEventos()).isEmpty();
    }

    @ParameterizedTest @CsvSource({"3,RECHAZAR", "4,DEVOLVER_POR_ERROR", "4,DEVOLVER_POR_PRODUCTO_NUEVO"})
    void observacionObligatoriaInclusoParaAdmin(int etapa, AccionEtapa accion) {
        Pedido p=en(etapa);
        assertThatThrownBy(() -> p.ejecutar(new AccionSolicitada(accion,etapa,"  ",
                accion==DEVOLVER_POR_PRODUCTO_NUEVO ? NivelUrgencia.URGENTE : null,Set.of()),
                regla(etapa,accion,2),EtapasHabilitadas.faseDos(),JEFE,INICIO,VENCE))
                .isInstanceOfSatisfying(RequisitoFaltante.class,e -> assertThat(e.faltantes()).containsExactly("observación"));
        unaActual(p,etapa); assertThat(p.retirarEventos()).isEmpty();
    }

    @ParameterizedTest @CsvSource({"1,Solicitud", "2,Documentos codificados"})
    void documentosObligatoriosNoSeSustituyenPorObservacion(int etapa,String documento) {
        Pedido p=en(etapa);
        assertThatThrownBy(() -> p.ejecutar(new AccionSolicitada(ENVIAR,etapa,"Texto ficticio",null,Set.of()),
                regla(etapa,ENVIAR,etapa+1),EtapasHabilitadas.faseDos(),JEFE,INICIO,VENCE))
                .isInstanceOfSatisfying(RequisitoFaltante.class,e -> assertThat(e.faltantes()).containsExactly(documento));
        unaActual(p,etapa); assertThat(p.retirarEventos()).isEmpty();
    }

    @Test void documentosPendientesNoSignificaSinRequisitos() {
        Pedido p=en(3);
        var r=new ReglaDeTransicion(3,APROBAR,4,false,false,false,true,Set.of(),new PlazoDeEtapa.Pendiente(),true,REVISION);
        assertThatThrownBy(() -> p.ejecutar(new AccionSolicitada(APROBAR,3,null,null,Set.of()),r,
                EtapasHabilitadas.faseDos(),JEFE,INICIO,VENCE)).isInstanceOf(TransicionNoPermitida.class);
        unaActual(p,3); assertThat(p.retirarEventos()).isEmpty();
    }

    @ParameterizedTest @CsvSource({"3,RECHAZAR", "4,DEVOLVER_POR_ERROR"})
    void reentradaReiniciaPlazoNormal(int etapa, AccionEtapa accion) {
        Pedido p=en(etapa); Instant ahora=INICIO.plusSeconds(3600);
        mover(p,accion,2,ahora,null);
        unaActual(p,2); assertThat(actual(p).reentradas()).isEqualTo(1);
        assertThat(actual(p).entradaEn()).isEqualTo(ahora);
        assertThat(actual(p).salidaEn()).isNull();
        assertThat(actual(p).venceEn()).isEqualTo(Instant.parse("2026-10-16T16:00:00Z"));
        assertThat(p.urgencia()).isEmpty();
        assertThat(p.etapas().get(etapa-1).estado()).isEqualTo(EstadoEtapa.DEVUELTA);
        if(etapa==4) assertThat(p.etapas().get(2).estado()).isEqualTo(EstadoEtapa.DEVUELTA);
    }

    @ParameterizedTest @EnumSource(NivelUrgencia.class)
    void urgenciaConservadaHastaA4YRechazoProvisionalNoLaReinicia(NivelUrgencia nivel) {
        Pedido p=en(4); Instant ahora=INICIO.plusSeconds(3600);
        mover(p,DEVOLVER_POR_PRODUCTO_NUEVO,2,ahora,nivel);
        var urgencia=p.urgencia().orElseThrow();
        assertThat(urgencia.nivel()).isEqualTo(nivel);
        assertThat(urgencia.venceEn()).isEqualTo(CALCULO.deUrgencia(ahora,nivel));
        mover(p,ENVIAR,3,ahora.plusSeconds(60),null);
        assertThat(p.urgencia()).contains(urgencia);
        mover(p,RECHAZAR,2,ahora.plusSeconds(120),null); // P-10 provisional, pendiente del cliente.
        assertThat(p.urgencia()).contains(urgencia);
        mover(p,ENVIAR,3,ahora.plusSeconds(180),null);
        var evento=(EtapaAprobada)mover(p,APROBAR,4,ahora.plusSeconds(240),null);
        assertThat(p.urgencia()).isEmpty(); assertThat(evento.urgenciaTerminada()).isTrue(); unaActual(p,4);
    }

    @Test void productoNuevoSinUrgenciaSeRechazaSinCambios() {
        Pedido p=en(4);
        assertThatThrownBy(() -> mover(p,DEVOLVER_POR_PRODUCTO_NUEVO,2,INICIO,null))
                .isInstanceOfSatisfying(RequisitoFaltante.class,e -> assertThat(e.faltantes()).containsExactly("urgencia"));
        unaActual(p,4); assertThat(p.urgencia()).isEmpty(); assertThat(p.retirarEventos()).isEmpty();
    }

    @Test void permisosSinAsignacionYConAsignacion() {
        Pedido p=en(2); var persona=actor(MARKETING); var otro=actor(MARKETING);
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,persona)).isTrue();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,actor(CHINA))).isFalse();
        p.reasignar(2,MARKETING,JEFE,persona,"Motivo ficticio",INICIO);
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,persona)).isTrue();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,otro)).isFalse();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,JEFE)).isTrue();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,new ActorVigente(persona.usuarioId(),false,Set.of(MARKETING)))).isFalse();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,new ActorVigente(persona.usuarioId(),true,Set.of(CHINA)))).isFalse();
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,new ActorVigente(JEFE.usuarioId(),false,Set.of(ADMIN)))).isFalse();
        p.reasignar(2,MARKETING,JEFE,otro,null,INICIO);
        assertThat(PermisosEtapa.puedeActuar(actual(p),MARKETING,otro)).isTrue();
    }

    @Test void reasignacionSoloAdminACandidatoActivoDelRolProvisional() {
        Pedido p=en(2); var candidato=actor(MARKETING);
        assertThatThrownBy(() -> p.reasignar(2,MARKETING,candidato,candidato,null,INICIO)).isInstanceOf(SinPermiso.class);
        assertThatThrownBy(() -> p.reasignar(2,MARKETING,JEFE,actor(CHINA),null,INICIO)).isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> p.reasignar(2,MARKETING,JEFE,new ActorVigente(candidato.usuarioId(),false,Set.of(MARKETING)),null,INICIO)).isInstanceOf(DatoInvalido.class);
        assertThatThrownBy(() -> p.reasignar(3,REVISION,JEFE,actor(REVISION),null,INICIO)).isInstanceOf(EtapaCambio.class);
        assertThat(p.retirarEventos()).isEmpty(); assertThat(actual(p).asignadoA()).isNull();
        p.reasignar(2,MARKETING,JEFE,candidato,null,INICIO);
        p.retirarEventos();
        assertThat(p.reasignar(2,MARKETING,JEFE,candidato,null,INICIO)).isEmpty();
        assertThat(p.retirarEventos()).isEmpty();
        mover(p,ENVIAR,3,INICIO.plusSeconds(60),null); mover(p,RECHAZAR,2,INICIO.plusSeconds(120),null);
        assertThat(actual(p).asignadoA()).isEqualTo(candidato.usuarioId()); // Conservación provisional.
    }

    @ParameterizedTest @CsvSource({"1,0001", "7,0007", "9999,9999", "10000,10000", "10001,10001"})
    void numeracionMinimoCuatroDigitosSinTruncar(int n,String sufijo) {
        assertThat(NumeroPedido.componer(CodigoPais.EC,2026,n).valor()).isEqualTo("DK-EC-2026-"+sufijo);
    }

    @Test void eventosConActorRolInstanteYListaInmutable() {
        Pedido p=nuevo(); var creado=p.retirarEventos();
        assertThat(creado).singleElement().satisfies(e -> {
            assertThat(e.actor().usuarioId()).isEqualTo(JEFE.usuarioId());
            assertThat(e.actor().rol()).isEqualTo(ADMIN); assertThat(e.ocurridoEn()).isEqualTo(INICIO);
        });
        assertThatThrownBy(() -> creado.clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(p.retirarEventos()).isEmpty();
        mover(p,ENVIAR,2,INICIO.plusSeconds(60),null);
        var persona=actor(MARKETING); var r=regla(2,ENVIAR,3);
        var evento=p.ejecutar(new AccionSolicitada(ENVIAR,2,null,null,r.documentosObligatorios()),r,
                EtapasHabilitadas.faseDos(),persona,INICIO.plusSeconds(120),VENCE);
        assertThat(evento.actor().usuarioId()).isEqualTo(persona.usuarioId());
        assertThat(evento.actor().rol()).isEqualTo(MARKETING);
        assertThat(evento.ocurridoEn()).isEqualTo(INICIO.plusSeconds(120));
        assertThat(creado.getFirst().etapa()).isEqualTo(1);
    }

    @Test void reconstruccionRechazaCeroODosEtapasActuales() {
        Pedido p=nuevo();
        var ninguna=List.of(new EtapaDePedido(UUID.randomUUID(),1,EstadoEtapa.PENDIENTE,null,null,null,null,0));
        var dos=List.of(new EtapaDePedido(UUID.randomUUID(),1,EstadoEtapa.ACTUAL,null,INICIO,null,null,0),
                new EtapaDePedido(UUID.randomUUID(),2,EstadoEtapa.ACTUAL,null,INICIO,null,null,0));
        for(var etapas:List.of(ninguna,dos)) {
            assertThatThrownBy(() -> new Pedido(p.id(),p.numero(),p.pais(),p.tipoFlujo(),p.flujoId(),p.nombre(),null,
                    false,JEFE.usuarioId(),INICIO,etapas,1,false,null,null,INICIO,0))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test void asignacionSeExigeAlEjecutarYAdminPuedeActuarSobreAsignacionAjena() {
        Pedido p=en(3); var asignado=actor(REVISION); var otro=actor(REVISION);
        p.reasignar(3,REVISION,JEFE,asignado,null,INICIO); p.retirarEventos();
        var solicitud=new AccionSolicitada(APROBAR,3,null,null,Set.of()); var r=regla(3,APROBAR,4);
        assertThatThrownBy(() -> p.ejecutar(solicitud,r,EtapasHabilitadas.faseDos(),otro,INICIO,VENCE)).isInstanceOf(SinPermiso.class);
        unaActual(p,3); assertThat(p.retirarEventos()).isEmpty();
        var evento=p.ejecutar(solicitud,r,EtapasHabilitadas.faseDos(),JEFE,INICIO,VENCE);
        unaActual(p,4); assertThat(evento.actor().rol()).isEqualTo(ADMIN);
    }
}
