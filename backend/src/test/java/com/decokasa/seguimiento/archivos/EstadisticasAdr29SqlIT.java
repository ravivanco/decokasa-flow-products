package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.pedidos.adapter.out.persistence.EstadisticasDePedidosSql;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Fórmulas con cuatro pedidos sintéticos por esquema; no carga operativa ni base compartida. */
@Testcontainers
class EstadisticasAdr29SqlIT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17").withReuse(false);
    static final Instant INICIO=Instant.parse("2026-01-01T05:00:00Z"),FIN=Instant.parse("2026-01-02T05:00:00Z"),AHORA=FIN.plusSeconds(3600);
    DatosB1 d;EstadisticasDePedidosSql datos;UUID a,b,abierto,co;
    @BeforeEach void preparar() {
        d=new DatosB1(PG,null);datos=new EstadisticasDePedidosSql(d.ds);
        d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values(?,'Estadística ficticia','estadistica@ejemplo.test','estadistica','+593990001111','ficticio',now(),now(),0)",d.usuario);
        a=pedido(1,"EC",INICIO,INICIO.plusSeconds(60));b=pedido(2,"EC",INICIO.plusSeconds(2),INICIO.plusSeconds(122));
        abierto=pedido(3,"EC",INICIO.minusSeconds(3600),null);co=pedido(4,"CO",INICIO,null);
        for(UUID p:List.of(a,b)) {
            Instant inicio=p.equals(a)?INICIO:INICIO.plusSeconds(2),fin=p.equals(a)?INICIO.plusSeconds(60):INICIO.plusSeconds(122);
            entrada(p,1,inicio,UUID.randomUUID());entrada(p,2,inicio.plusSeconds(20),UUID.randomUUID());entrada(p,11,fin,UUID.randomUUID());
        }
        UUID e=UUID.fromString("00000000-0000-4000-8000-000000000001");entrada(abierto,3,INICIO.minusSeconds(3600),e);
        d.sql.update("insert into etapa_pedido(id,pedido_id,numero,estado,entrada_en,entrada_id,vence_en,reentradas) values(?,?,3,'ACTUAL',?,?,?,0)",UUID.randomUUID(),abierto,Timestamp.from(INICIO.minusSeconds(3600)),e,Timestamp.from(AHORA.minusSeconds(1)));
    }
    UUID pedido(int n,String pais,Instant creado,Instant terminado) {
        UUID id=UUID.randomUUID(),flujo=d.sql.queryForObject("select id from definicion_flujo where pais_codigo=? and tipo_flujo='A' and vigente",UUID.class,pais);
        d.sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version,finalizado,finalizado_en) values(?,?,?,'A',?,'Estadística ficticia',?,?,?, ?,0,?,?)",
            id,"DK-"+pais+"-2026-"+String.format(Locale.ROOT,"%04d",n),pais,flujo,d.usuario,Timestamp.from(creado),terminado==null?3:11,Timestamp.from(terminado==null?creado:terminado),terminado!=null,terminado==null?null:Timestamp.from(terminado));
        return id;
    }
    void entrada(UUID p,int etapa,Instant fecha,UUID id) {
        d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values(?,?,?,?)",id,p,etapa,Timestamp.from(fecha));
    }
    @Test void tarjetasCuentanFechasCorrectasYMedianaEnSegundosSinMezclarPais() {
        var t=datos.tarjetas(Set.of(CodigoPais.EC),null,INICIO,FIN,AHORA);
        assertThat(t.creados()).isEqualTo(2);assertThat(t.finalizados()).isEqualTo(2);assertThat(t.enCurso()).isEqualTo(1);
        assertThat(t.conRetraso()).isEqualTo(1);assertThat(t.medianaResolucionSegundos()).isEqualTo(90);assertThat(t.resolucionObservaciones()).isEqualTo(2);
        var estado=datos.estado(Set.of(CodigoPais.EC),null,INICIO,FIN,AHORA);
        assertThat(estado.enCurso()).isZero();assertThat(estado.finalizados()).isEqualTo(2);
    }
    @Test void pedidosDevuelvenDuracionRealOAntiguedadYTotalAntesDelLimite() {
        var tiempos=datos.tiempoPorPedido(Set.of(CodigoPais.EC),"A",INICIO.minusSeconds(7200),FIN,AHORA,2);
        assertThat(tiempos.total()).isEqualTo(3);assertThat(tiempos.pedidos()).hasSize(2);
        assertThat(tiempos.pedidos().getFirst().pedidoId()).isEqualTo(abierto);
        assertThat(tiempos.pedidos().getFirst().finalizado()).isFalse();
        assertThat(tiempos.pedidos().getFirst().segundos()).isEqualTo(Duration.between(INICIO.minusSeconds(3600),AHORA).toSeconds());
        assertThat(tiempos.pedidos().get(1).segundos()).isEqualTo(120);
    }
    @Test void visitasCerradasTienenMedianaYPromedioYAbiertasNoEntranEnDuracion() {
        var lista=datos.tiempoPorEtapa(Set.of(CodigoPais.EC),null,INICIO.minusSeconds(7200),FIN);
        assertThat(lista).extracting(t->t.etapa()).containsExactly(1,2,3);
        assertThat(lista.get(0).medianaSegundos()).isEqualTo(20);assertThat(lista.get(0).terminadas()).isEqualTo(2);
        assertThat(lista.get(1).medianaSegundos()).isEqualTo(70);assertThat(lista.get(1).promedioSegundos()).isEqualTo(70);
        assertThat(lista.get(2).medianaSegundos()).isNull();assertThat(lista.get(2).abiertas()).isEqualTo(1);
    }
    @Test void empateDeHoraNuncaMarcaComoAbiertaUnaVisitaAnteriorEnLugarDeLaActual() {
        entrada(abierto,1,INICIO.minusSeconds(3600),UUID.fromString("ffffffff-ffff-4fff-8fff-ffffffffffff"));
        var lista=datos.tiempoPorEtapa(Set.of(CodigoPais.EC),null,INICIO.minusSeconds(7200),FIN);
        assertThat(lista.stream().filter(t->t.etapa()==3).findFirst().orElseThrow().abiertas()).isEqualTo(1);
        assertThat(lista.stream().filter(t->t.etapa()==1).findFirst().orElseThrow().abiertas()).isZero();
    }
    @Test void anioYMesUsanFechaLocalYVacioNoInventaMediana() {
        assertThat(datos.creadosPorAnio(Set.of(CodigoPais.EC),null)).containsEntry(2025,1L).containsEntry(2026,2L);
        assertThat(datos.creadosPorMes(Set.of(CodigoPais.EC),null,2025)).containsExactlyEntriesOf(Map.of(12,1L));
        var t=datos.tarjetas(Set.of(CodigoPais.CO),null,INICIO,FIN,AHORA);
        assertThat(t.medianaResolucionSegundos()).isNull();assertThat(t.resolucionObservaciones()).isZero();
    }
    @Test void alertasRespetanRolAsignacionYNoIncluyenFinalizados() {
        assertThat(datos.alertas(Set.of(CodigoPais.EC),d.usuario,Set.of(new CodigoRol("asistente_compras")),false,AHORA)).hasSize(1);
        assertThat(datos.alertas(Set.of(CodigoPais.EC),d.usuario,Set.of(new CodigoRol("compras_china")),false,AHORA)).isEmpty();
        assertThat(datos.alertas(Set.of(CodigoPais.EC),d.usuario,Set.of(),true,AHORA)).hasSize(1);
        assertThat(datos.alertas(Set.of(CodigoPais.CO),d.usuario,Set.of(new CodigoRol("marketing")),true,AHORA)).isEmpty();
        assertThat(datos.retrasosPorEtapa(Set.of(CodigoPais.EC),null,AHORA)).singleElement().satisfies(r->{assertThat(r.pedidos()).isEqualTo(1);assertThat(r.etapaVencida()).isEqualTo(1);});
    }
}
