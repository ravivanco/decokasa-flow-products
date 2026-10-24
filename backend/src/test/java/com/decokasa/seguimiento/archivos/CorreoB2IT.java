package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.decokasa.seguimiento.notificaciones.adapter.out.persistence.NotificacionSqlRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.out.*;
import com.decokasa.seguimiento.notificaciones.application.usecase.EnviarNotificacionesCasoDeUso;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.json.JsonMapper;

/** Reutiliza el fixture V6 de B1. Datos y proveedor ficticios; ninguna conexion a Google. */
@Testcontainers
class CorreoB2IT {
 @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17");
 final Instant ahora=Instant.parse("2026-10-19T15:00:00Z");
 DatosB1 d; NotificacionSqlRepositorio repo; TransactionTemplate tx;
 @BeforeEach void preparar() {
   d=new DatosB1(PG,null); d.poblar(1,"a".repeat(64),"ficticio");
   repo=new NotificacionSqlRepositorio(d.ds,JsonMapper.builder().build());
   tx=new TransactionTemplate(new DataSourceTransactionManager(d.ds));
 }
 Notificacion aviso(String correo) { return Notificacion.pendiente(d.pedido,d.evento,correo,
   Map.of("pais","EC","pedido","DK-EC-2026-0001","tipoEvento","EtapaEnviada"),ahora); }
 void guardar(Notificacion n) { tx.executeWithoutResult(s->assertThat(repo.insertarSiAusente(n)).isTrue()); }
 @Test void rollbackRevierteAccionHistorialYOutboxEnLaMismaTransaccion() {
   var nuevoEvento=UUID.randomUUID();
   var n=Notificacion.pendiente(d.pedido,nuevoEvento,"admin@ejemplo.test",Map.of("pais","EC"),ahora);
   tx.executeWithoutResult(s->{d.sql.update("update pedido set nombre='Cambio ficticio' where id=?",d.pedido);
     d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values (?,?,1,'ETAPA_ENVIADA',?,'admin',now(),'{}')",nuevoEvento,d.pedido,d.usuario);
     repo.insertarSiAusente(n);s.setRollbackOnly();});
   assertThat(repo.listar(null,0,20)).isEmpty();
   assertThat(d.sql.queryForObject("select nombre from pedido where id=?",String.class,d.pedido)).isEqualTo("Pedido B1");
   assertThat(d.sql.queryForObject("select count(*) from evento_pedido where id=?",Integer.class,nuevoEvento)).isZero();
 }
 @Test void claveEventoDestinatarioNormalizadaEvitaDuplicadoSinPerderSnapshot() {
   guardar(aviso(" ADMIN@EJEMPLO.TEST "));
   tx.executeWithoutResult(s->assertThat(repo.insertarSiAusente(aviso("admin@ejemplo.test"))).isFalse());
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(n->{assertThat(n.destinatario()).isEqualTo("admin@ejemplo.test");assertThat(n.datos().get("pedido")).isEqualTo("DK-EC-2026-0001");});
 }
 @Test void intencionSinDestinatarioNoEsReclamableNiConsumeIntento() {
   guardar(aviso(null));
   assertThat(tx.<List<Notificacion>>execute(s->repo.reclamar(1,ahora,ahora.plusSeconds(180),UUID.randomUUID()))).isEmpty();
   assertThat(repo.listar(null,0,20).getFirst().intentos()).isZero();
 }
 @Test void incertidumbreDurableNoSeReenviaAlVencerLeaseNiTrasRecrearRepositorio() {
   guardar(aviso("admin@ejemplo.test"));var owner=UUID.randomUUID();
   var reclamadas=tx.execute(s->repo.reclamar(1,ahora,ahora.plusSeconds(180),owner));
   assertThat(reclamadas).hasSize(1);
   var reiniciado=new NotificacionSqlRepositorio(d.ds,JsonMapper.builder().build());
   assertThat(tx.<List<Notificacion>>execute(s->reiniciado.reclamar(1,ahora.plusSeconds(3600),ahora.plusSeconds(3780),UUID.randomUUID()))).isEmpty();
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(n->{assertThat(n.resultadoIncierto()).isTrue();assertThat(n.estado()).isEqualTo(Notificacion.Estado.FALLIDA);assertThat(n.intentos()).isEqualTo(1);});
 }
 @Test void dosWorkersSoloReclamanUnaVezYPropietarioAjenoNoConfirma() throws Exception {
   var n=aviso("admin@ejemplo.test");guardar(n);var owner=UUID.randomUUID();
   var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
   try {
     Callable<Integer> reclamar=()->{inicio.await();return tx.execute(s->repo.reclamar(1,ahora,ahora.plusSeconds(180),owner)).size();};
     var a=pool.submit(reclamar);var b=pool.submit(reclamar);inicio.countDown();
     assertThat(a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS)).isEqualTo(1);
     assertThat(tx.<Boolean>execute(s->repo.marcarEnviada(n.id(),UUID.randomUUID(),"proveedor-ficticio",ahora))).isFalse();
     assertThat(tx.<Boolean>execute(s->repo.marcarEnviada(n.id(),owner,"proveedor-ficticio",ahora.plusSeconds(181)))).isFalse();
     assertThat(repo.listar(null,0,20).getFirst().intentos()).isEqualTo(1);
   } finally {pool.shutdownNow();}
 }
 TrabajoCorreo trabajo() {
   return new TrabajoCorreo() {
     public Optional<Notificacion> reclamar(Instant a,Instant h,UUID p){return tx.execute(s->repo.reclamar(1,a,h,p).stream().findFirst());}
     public boolean vigente(UUID i,UUID p,Instant a){return tx.execute(s->repo.vigente(i,p,a));}
     public boolean enviada(UUID i,UUID p,String proveedor,Instant a){return tx.execute(s->repo.marcarEnviada(i,p,proveedor,a));}
     public boolean fallo(UUID i,UUID p,Causa c,Instant a){return tx.execute(s->repo.registrarFallo(i,p,c,a,true,a));}
   };
 }
 EnviarNotificacionesCasoDeUso caso(Notificador gmail,boolean activo,boolean destinatarios) {
   return new EnviarNotificacionesCasoDeUso(trabajo(),gmail,p->destinatarios?Set.of("admin@ejemplo.test"):Set.of(),()->ahora,activo,5,Duration.ofMinutes(3));
 }
 @Test void proveedorFicticioSeInvocaSinTransaccionYResultadoSePersiste() {
   var n=aviso("admin@ejemplo.test");guardar(n);
   Notificador gmail=c->{assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
     assertThat(c.messageId()).isEqualTo(n.messageId());return new Notificador.Entrega("gmail-ficticio-1");};
   assertThat(caso(gmail,true,true).procesar()).isEqualTo(1);
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(e->{assertThat(e.estado()).isEqualTo(Notificacion.Estado.ENVIADA);assertThat(e.proveedorId()).isEqualTo("gmail-ficticio-1");assertThat(e.intentos()).isEqualTo(1);assertThat(e.resultadoIncierto()).isFalse();});
   assertThat(caso(gmail,true,true).procesar()).isZero();
 }
 @Test void desactivadoODirectorioVacioNoInvocaGoogleNiReclama() {
   guardar(aviso("admin@ejemplo.test"));var gmail=mock(Notificador.class);
   assertThat(caso(gmail,false,true).procesar()).isZero();assertThat(caso(gmail,true,false).procesar()).isZero();
   verifyNoInteractions(gmail);assertThat(repo.listar(null,0,20).getFirst().intentos()).isZero();
 }
 @Test void errorDeCuotaSeRegistraSinInventarReintentosDeCorreo() {
   guardar(aviso("admin@ejemplo.test"));var gmail=mock(Notificador.class);
   when(gmail.enviar(any())).thenThrow(new FalloIntegracionGoogle(Causa.CUOTA));
   var caso=caso(gmail,true,true);assertThat(caso.procesar()).isEqualTo(1);assertThat(caso.procesar()).isZero();
   verify(gmail,times(1)).enviar(any());
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(n->{assertThat(n.ultimoError()).isEqualTo("CUOTA");assertThat(n.estado()).isEqualTo(Notificacion.Estado.FALLIDA);});
 }
 @Test void respuestaInciertaNoGeneraSegundoPost() {
   guardar(aviso("admin@ejemplo.test"));var gmail=mock(Notificador.class);
   when(gmail.enviar(any())).thenThrow(new FalloIntegracionGoogle(Causa.RESULTADO_INCIERTO));
   var caso=caso(gmail,true,true);caso.procesar();assertThat(caso.procesar()).isZero();verify(gmail,times(1)).enviar(any());
   assertThat(repo.listar(null,0,20).getFirst().resultadoIncierto()).isTrue();
 }

 @Test void registroServidorRevalidaAdminYNoConfiaEnRolesAntiguosDelToken() {
   var registro=mock(RegistroNotificaciones.class);
   var rol=new com.decokasa.seguimiento.compartido.domain.CodigoRol("admin");
   var solicitante=new com.decokasa.seguimiento.compartido.domain.Solicitante(d.usuario,Set.of(rol));
   var usuarios=mock(com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables.class);
   var consulta=new com.decokasa.seguimiento.notificaciones.application.usecase.ConsultarNotificacionesCasoDeUso(registro,usuarios);
   when(usuarios.estado(d.usuario)).thenReturn(Optional.of(new com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables.EstadoVigente(d.usuario,true,Set.of())));
   assertThatThrownBy(()->consulta.consultar(solicitante,null,0,20)).isInstanceOf(com.decokasa.seguimiento.compartido.domain.SinPermiso.class);
   verifyNoInteractions(registro);
   when(usuarios.estado(d.usuario)).thenReturn(Optional.of(new com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables.EstadoVigente(d.usuario,false,Set.of(rol))));
   assertThatThrownBy(()->consulta.consultar(solicitante,null,0,20)).isInstanceOf(com.decokasa.seguimiento.compartido.domain.SinPermiso.class);
   verifyNoInteractions(registro);
   // Fase 7 (ADR-0024): el admin vigente solo consulta los correos de los países asignados hoy, con el mismo filtro
   // en la lista y en el total.
   var ec=com.decokasa.seguimiento.compartido.domain.CodigoPais.EC;
   var co=com.decokasa.seguimiento.compartido.domain.CodigoPais.CO;
   when(usuarios.estado(d.usuario)).thenReturn(Optional.of(new com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables.EstadoVigente(d.usuario,true,Set.of(rol),Set.of(ec))));
   assertThatThrownBy(()->consulta.consultar(solicitante,co,null,0,20)).isInstanceOf(com.decokasa.seguimiento.compartido.domain.SinPermiso.class);
   verifyNoInteractions(registro);
   when(registro.listar(Set.of(ec),null,0,20)).thenReturn(List.of());when(registro.contar(Set.of(ec),null)).thenReturn(0L);
   assertThatCode(()->consulta.consultar(solicitante,null,0,20)).doesNotThrowAnyException();
   verify(registro).listar(Set.of(ec),null,0,20);verify(registro).contar(Set.of(ec),null);
   // Sin países asignados no hay filtro implícito: se consulta con el conjunto vacío, nunca con todos los países.
   clearInvocations(registro);
   when(usuarios.estado(d.usuario)).thenReturn(Optional.of(new com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables.EstadoVigente(d.usuario,true,Set.of(rol),Set.of())));
   assertThatCode(()->consulta.consultar(solicitante,null,0,20)).doesNotThrowAnyException();
   verify(registro).listar(Set.of(),null,0,20);verify(registro).contar(Set.of(),null);
   verify(registro,never()).listar(null,0,20);
 }

 @Test void cuatroTransicionesConservanEventoDestinoYSnapshotSinDuplicar() {
   var pedido=new com.decokasa.seguimiento.compartido.domain.eventos.DatosDelPedido(d.pedido,"DK-EC-2026-0001",com.decokasa.seguimiento.compartido.domain.CodigoPais.EC,com.decokasa.seguimiento.compartido.domain.TipoFlujo.A);
   var actor=new com.decokasa.seguimiento.compartido.domain.eventos.Interviniente(d.usuario,new com.decokasa.seguimiento.compartido.domain.CodigoRol("admin"));
   var eventos=List.of(
     new com.decokasa.seguimiento.compartido.domain.eventos.EtapaEnviada(pedido,1,actor,ahora,2,"Envio ficticio",false),
     new com.decokasa.seguimiento.compartido.domain.eventos.EtapaAprobada(pedido,3,actor,ahora,4,"Aprobacion ficticia",false),
     new com.decokasa.seguimiento.compartido.domain.eventos.EtapaRechazada(pedido,3,actor,ahora,2,"Rechazo ficticio",false),
     new com.decokasa.seguimiento.compartido.domain.eventos.PedidoDevuelto(pedido,4,actor,ahora,2,com.decokasa.seguimiento.compartido.domain.eventos.MotivoDevolucion.ERROR,"Devolucion ficticia",null,List.of()));
   var encolar=new com.decokasa.seguimiento.notificaciones.application.usecase.EncolarNotificacionCasoDeUso(repo,p->Set.of("ADMIN@EJEMPLO.TEST","admin@ejemplo.test"));
   int[] destinos={2,4,2,2}; int indice=0;
   for(var evento:eventos) {
     UUID eventoId=UUID.randomUUID();
     tx.executeWithoutResult(s->{
       d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values (?,?,1,'ETAPA_ENVIADA',?,'admin',now(),'{}')",eventoId,d.pedido,d.usuario);
       var registrado=new com.decokasa.seguimiento.compartido.domain.eventos.EventoPedidoRegistrado(eventoId,evento);
       encolar.encolar(registrado);encolar.encolar(registrado);
     });
     var fila=repo.listar(null,0,20).stream().filter(n->n.eventoId().equals(eventoId)).toList();
     assertThat(fila).hasSize(1);assertThat(fila.getFirst().datos()).containsEntry("tipoEvento",evento.getClass().getSimpleName()).containsEntry("etapaDestino",Integer.toString(destinos[indice++]));
   }
   assertThat(repo.listar(null,0,20)).hasSize(4);
 }

 @Test void caidaDespuesDePostConservaIncertidumbreYNoReenviaTrasReinicio() {
   guardar(aviso("admin@ejemplo.test"));var gmail=mock(Notificador.class);
   when(gmail.enviar(any())).thenThrow(new IllegalStateException("fallo-ficticio-tras-post"));
   assertThatThrownBy(()->caso(gmail,true,true).procesar()).isInstanceOf(IllegalStateException.class);
   assertThat(caso(gmail,true,true).procesar()).isZero();verify(gmail,times(1)).enviar(any());
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(n->{assertThat(n.resultadoIncierto()).isTrue();assertThat(n.intentos()).isEqualTo(1);assertThat(n.ultimoError()).doesNotContain("fallo-ficticio-tras-post");});
 }
 @Test void destinatarioQueYaNoPerteneceAlDirectorioNoSeEnvia() {
   guardar(aviso("retirado@ejemplo.test"));var gmail=mock(Notificador.class);
   assertThat(caso(gmail,true,true).procesar()).isEqualTo(1);verifyNoInteractions(gmail);
   assertThat(repo.listar(null,0,20)).singleElement().satisfies(n->{assertThat(n.estado()).isEqualTo(Notificacion.Estado.FALLIDA);assertThat(n.ultimoError()).isEqualTo("CONFIGURACION");});
 }
}
