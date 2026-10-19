package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.CuotaSqlRepositorio;
import com.decokasa.seguimiento.archivos.domain.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Real SQL and independent connections; no filesystem or Google shared with another case. */
@Testcontainers
class CuotasLocalesIT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    DatosB1 d; TransactionTemplate tx; CuotaSqlRepositorio cuotas;
    @BeforeEach void preparar() {
        d=new DatosB1(PG,null); tx=new TransactionTemplate(new DataSourceTransactionManager(d.ds));
        usuario(d.usuario); cuotas=new CuotaSqlRepositorio(d.ds,100,50,10,2);
    }
    void usuario(UUID id) {
        d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,'Ficticio',?,?,?,'ficticio',now(),now(),0)",
            id,id+"@ejemplo.test",id.toString(),"+"+id.toString().replace("-","").replaceAll("[a-f]","1").substring(0,14));
    }
    void reservar(UUID id,UUID usuario,long bytes) { tx.executeWithoutResult(s->cuotas.reservar(id,usuario,bytes)); }
    void confirmar(UUID id,UUID usuario,long bytes) { tx.executeWithoutResult(s->cuotas.confirmar(id,usuario,bytes)); }
    long total() { return d.sql.queryForObject("select coalesce(sum(bytes_comprometidos),0) from archivo_cuota",Long.class); }
    long activos() { return d.sql.queryForObject("select count(*) from archivo_cuota where estado='RESERVADA'",Long.class); }
    @ParameterizedTest @CsvSource({"0,1,1,1","1,0,1,1","1,2,1,1","1,1,0,1","1,1,1,0","1,1,1,2"})
    void configuracionInvalidaNoPuedeAdmitir(long global,long personal,int globales,int personales) {
        assertThatThrownBy(()->new CuotaSqlRepositorio(d.ds,global,personal,globales,personales)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void limitesBinariosExactosYUsuarioIndependienteConConfiguracionReducida() {
        cuotas=new CuotaSqlRepositorio(d.ds,10L*1024*1024*1024,2L*1024*1024*1024,10,2);
        reservar(UUID.randomUUID(),d.usuario,2L*1024*1024*1024);
        assertThatThrownBy(()->reservar(UUID.randomUUID(),d.usuario,1)).isInstanceOf(CuotaAlmacenamientoExcedida.class);
        UUID otro=UUID.randomUUID();usuario(otro);reservar(UUID.randomUUID(),otro,1);
        assertThat(total()).isEqualTo(2147483649L);
    }
    @Test void presupuestoGlobalExactoYUnByteExtra() {
        cuotas=new CuotaSqlRepositorio(d.ds,100,100,10,2);
        UUID otro=UUID.randomUUID();usuario(otro);
        reservar(UUID.randomUUID(),d.usuario,60);reservar(UUID.randomUUID(),otro,40);
        assertThatThrownBy(()->reservar(UUID.randomUUID(),otro,1)).isInstanceOf(CuotaAlmacenamientoExcedida.class);
        assertThat(total()).isEqualTo(100);assertThat(activos()).isEqualTo(2);
    }
    @Test void idempotenciaYConversionNoDuplicanBytesNiCupos() {
        UUID id=UUID.randomUUID();reservar(id,d.usuario,40);reservar(id,d.usuario,40);
        assertThat(total()).isEqualTo(40);assertThat(activos()).isEqualTo(1);
        confirmar(id,d.usuario,40);confirmar(id,d.usuario,40);reservar(id,d.usuario,40);
        assertThat(total()).isEqualTo(40);assertThat(activos()).isZero();
        assertThat(d.sql.queryForObject("select count(*) from archivo_cuota",Long.class)).isEqualTo(1);
    }
    @Test void multipartReservaMaximoYAjustaAlMedidoSinCobrarAmbos() {
        UUID id=UUID.randomUUID();reservar(id,d.usuario,50);confirmar(id,d.usuario,3);
        assertThat(total()).isEqualTo(3);assertThat(activos()).isZero();
        assertThatThrownBy(()->confirmar(id,d.usuario,2)).isInstanceOf(ProblemaCarga.class);
        assertThat(total()).isEqualTo(3);
    }
    @Test void terceraCargaPersonalSeRechazaYFinalizarLiberaUnCupo() {
        UUID a=UUID.randomUUID();reservar(a,d.usuario,1);reservar(UUID.randomUUID(),d.usuario,1);
        assertThatThrownBy(()->reservar(UUID.randomUUID(),d.usuario,1)).isInstanceOf(CuotaAlmacenamientoExcedida.class);
        confirmar(a,d.usuario,1);reservar(UUID.randomUUID(),d.usuario,1);
        assertThat(activos()).isEqualTo(2);assertThat(total()).isEqualTo(3);
    }
    @Test void decimaCargaGlobalAdmitidaUndecimaRechazada() {
        for(int n=0;n<5;n++) {UUID u=UUID.randomUUID();usuario(u);reservar(UUID.randomUUID(),u,1);reservar(UUID.randomUUID(),u,1);}
        UUID ultimo=UUID.randomUUID();usuario(ultimo);
        assertThatThrownBy(()->reservar(UUID.randomUUID(),ultimo,1)).isInstanceOf(CuotaAlmacenamientoExcedida.class);
        assertThat(activos()).isEqualTo(10);assertThat(total()).isEqualTo(10);
    }
    @Test void pausaFalloYReinicioConservanAsignacionYPropietario() {
        UUID id=UUID.randomUUID();reservar(id,d.usuario,40);
        tx.executeWithoutResult(s->{cuotas.confirmar(id,d.usuario,40);s.setRollbackOnly();});
        cuotas=new CuotaSqlRepositorio(d.ds,10,10,1,1); // Lowering limits cannot destroy existing reservations.
        reservar(id,d.usuario,40);assertThat(total()).isEqualTo(40);assertThat(activos()).isEqualTo(1);
        confirmar(id,d.usuario,40);assertThat(total()).isEqualTo(40);assertThat(activos()).isZero();
        assertThatThrownBy(()->reservar(UUID.randomUUID(),d.usuario,1)).isInstanceOf(CuotaAlmacenamientoExcedida.class);
    }
    @Test void rollbackDeAdmisionNoDejaReservaYPropietarioAjenoNoConfirma() {
        UUID id=UUID.randomUUID();tx.executeWithoutResult(s->{cuotas.reservar(id,d.usuario,40);s.setRollbackOnly();});
        assertThat(total()).isZero();assertThat(activos()).isZero();
        reservar(id,d.usuario,40);
        assertThatThrownBy(()->confirmar(id,UUID.randomUUID(),40)).isInstanceOf(ProblemaCarga.class);
        assertThatThrownBy(()->confirmar(id,d.usuario,41)).isInstanceOf(ProblemaCarga.class);
        assertThatThrownBy(()->reservar(id,d.usuario,39)).isInstanceOf(ProblemaCarga.class);
        assertThatThrownBy(()->reservar(id,UUID.randomUUID(),40)).isInstanceOf(ProblemaCarga.class);
        assertThatThrownBy(()->confirmar(UUID.randomUUID(),d.usuario,1)).isInstanceOf(ProblemaCarga.class);
        assertThat(total()).isEqualTo(40);assertThat(activos()).isEqualTo(1);
    }
    @Test void identidadConfirmadaNoPuedeReasignarseNiBorrarsePorSql() {
        UUID id=UUID.randomUUID(),otro=UUID.randomUUID();usuario(otro);reservar(id,d.usuario,40);confirmar(id,d.usuario,40);
        assertThatThrownBy(()->d.sql.update("update archivo_cuota set propietario_id=? where adjunto_id=?",otro,id)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThatThrownBy(()->d.sql.update("delete from archivo_cuota where adjunto_id=?",id)).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThatThrownBy(()->d.sql.execute("truncate archivo_cuota")).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(total()).isEqualTo(40);
    }
    @Test void concurrenciaEnUltimoCupoGlobalSoloAdmiteUnaConexion() throws Exception {
        cuotas=new CuotaSqlRepositorio(d.ds,100,100,1,1);UUID otro=UUID.randomUUID();usuario(otro);
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try {
            var a=pool.submit(()->admitir(inicio,d.usuario));var b=pool.submit(()->admitir(inicio,otro));inicio.countDown();
            assertThat(a.get(10,TimeUnit.SECONDS)+b.get(10,TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(activos()).isEqualTo(1);assertThat(total()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    int admitir(CountDownLatch inicio,UUID u) throws Exception {
        inicio.await(5,TimeUnit.SECONDS);
        try {reservar(UUID.randomUUID(),u,1);return 1;}catch(CuotaAlmacenamientoExcedida esperado){return 0;}
    }
    @Test void concurrenciaMismaIdempotenciaNoDuplicaReserva() throws Exception {
        UUID id=UUID.randomUUID();var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try {
            Callable<Void> accion=()->{inicio.await(5,TimeUnit.SECONDS);reservar(id,d.usuario,40);return null;};
            var a=pool.submit(accion);var b=pool.submit(accion);inicio.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
            assertThat(total()).isEqualTo(40);assertThat(activos()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }
    @Test void concurrenciaUltimosBytesNoSobreasignaAunqueQuedenCupos() throws Exception {
        cuotas=new CuotaSqlRepositorio(d.ds,50,50,10,2);UUID otro=UUID.randomUUID();usuario(otro);
        var pool=Executors.newFixedThreadPool(2);var inicio=new CountDownLatch(1);
        try {
            Callable<Integer> a=()->{inicio.await();try{reservar(UUID.randomUUID(),d.usuario,40);return 1;}catch(CuotaAlmacenamientoExcedida e){return 0;}};
            Callable<Integer> b=()->{inicio.await();try{reservar(UUID.randomUUID(),otro,40);return 1;}catch(CuotaAlmacenamientoExcedida e){return 0;}};
            var x=pool.submit(a);var y=pool.submit(b);inicio.countDown();assertThat(x.get(10,TimeUnit.SECONDS)+y.get(10,TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(total()).isEqualTo(40);
        } finally {pool.shutdownNow();}
    }
    @Test void repeatableReadConSnapshotAntiguoAbortaEnLugarDeSobreasignar() throws Exception {
        cuotas=new CuotaSqlRepositorio(d.ds,1,1,1,1);UUID otro=UUID.randomUUID();usuario(otro);
        var rr=new TransactionTemplate(new DataSourceTransactionManager(d.ds));rr.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        var observado=new CountDownLatch(1);var continuar=new CountDownLatch(1);var pool=Executors.newSingleThreadExecutor();
        try {
            var antiguo=pool.submit(()->rr.execute(s->{d.sql.queryForObject("select version from archivo_cuota_cerrojo where id=1",Long.class);observado.countDown();
                try {if(!continuar.await(5,TimeUnit.SECONDS))throw new IllegalStateException("Sin sincronizacion");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}
                cuotas.reservar(UUID.randomUUID(),otro,1);return true;}));
            assertThat(observado.await(5,TimeUnit.SECONDS)).isTrue();reservar(UUID.randomUUID(),d.usuario,1);continuar.countDown();
            assertThatThrownBy(()->antiguo.get(10,TimeUnit.SECONDS)).hasRootCauseInstanceOf(java.sql.SQLException.class);
            assertThat(total()).isEqualTo(1);assertThat(activos()).isEqualTo(1);
        } finally {continuar.countDown();pool.shutdownNow();}
    }
}
