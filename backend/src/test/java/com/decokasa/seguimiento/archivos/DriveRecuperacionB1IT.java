package com.decokasa.seguimiento.archivos;
import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.*;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.*;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.CopiarPendientesCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class DriveRecuperacionB1IT {
    @Container static final PostgreSQLContainer PG=new PostgreSQLContainer("postgres:17");
    @TempDir Path disco;
    DatosB1 d; AlmacenLocal local; CopiaAdjuntoRepositorio copias; CarpetaDriveRepositorio carpetas;
    final ClockB1 reloj=new ClockB1(); DriveFalso drive; byte[] contenido;
    final PoliticaCopia politica=new PoliticaCopia(Duration.ofMinutes(3),List.of(Duration.ofMinutes(1),Duration.ofMinutes(5),Duration.ofMinutes(15)));
    static final class ClockB1 implements Reloj {Instant instante=Instant.parse("2026-10-17T12:00:00Z"); public Instant ahora(){return instante;}void avanzar(Duration x){instante=instante.plus(x);}}
    @SuppressWarnings("unchecked") <T> T transaccional(Class<T> tipo,T repo) {
        var tx=new TransactionTemplate(new DataSourceTransactionManager(d.ds));
        return (T)Proxy.newProxyInstance(tipo.getClassLoader(),new Class<?>[]{tipo},(p,m,a)->tx.execute(e->{
            try{return m.invoke(repo,a);}catch(InvocationTargetException error){throw (RuntimeException)error.getCause();}
            catch(IllegalAccessException error){throw new IllegalStateException(error);}
        }));
    }
    void preparar(int cantidad) throws Exception {
        contenido=new byte[cantidad];Arrays.fill(contenido,(byte)11);
        local=new AlmacenLocalEnDisco(disco);var r=local.recibir(new ByteArrayInputStream(contenido),104857600);
        local.confirmar(r,"pendiente/archivo");d=new DatosB1(PG,null);d.poblar(cantidad,r.sha256(),"pendiente/archivo");
        copias=transaccional(CopiaAdjuntoRepositorio.class,new CopiaAdjuntoSqlRepositorio(d.ds));
        carpetas=transaccional(CarpetaDriveRepositorio.class,new CarpetaDriveSqlRepositorio(d.ds));
        drive=new DriveFalso();
    }
    CopiarPendientesCasoDeUso worker() {
        ProtectorSesionDrive protector=new ProtectorSesionDrive(){
            public byte[] cifrar(UUID id,AlmacenArchivos.Sesion s){return new byte[]{7};}
            public AlmacenArchivos.Sesion abrir(UUID id,byte[] b){return new AlmacenArchivos.Sesion("sesion-ficticia");}
        };
        return new CopiarPendientesCasoDeUso(true,1,copias,carpetas,id->new PedidosParaCopias.Destino(CodigoPais.EC,"DK-EC-2026-0001"),
            drive,local,new BloqueoCopiasEnDisco(disco),protector,reloj,politica);
    }
    int intentos(){return d.sql.queryForObject("select intentos from adjunto where id=?",Integer.class,d.adjunto);}
    String estado(){return d.sql.queryForObject("select estado_copia from adjunto where id=?",String.class,d.adjunto);}
    @Test void inicialMasTresReintentosPersistidosSinDormirNiQuintoIntento() throws Exception {
        preparar(5);drive.falloReserva=new FalloIntegracionGoogle(Causa.CUOTA);
        var esperas=List.of(Duration.ofMinutes(1),Duration.ofMinutes(5),Duration.ofMinutes(15));
        for(int n=1;n<=4;n++) {
            assertThat(worker().ejecutar()).isEqualTo(1);assertThat(intentos()).isEqualTo(n);
            if(n<4){assertThat(estado()).isEqualTo("PENDIENTE");assertThat(worker().ejecutar()).isZero();
                assertThat(d.sql.queryForObject("select proximo_intento_en from adjunto where id=?",java.sql.Timestamp.class,d.adjunto).toInstant()).isEqualTo(reloj.ahora().plus(esperas.get(n-1)));
                reloj.avanzar(esperas.get(n-1));}
        }
        assertThat(estado()).isEqualTo("FALLIDO");reloj.avanzar(Duration.ofDays(1));assertThat(worker().ejecutar()).isZero();
        assertThat(Files.readAllBytes(disco.resolve("pendiente/archivo"))).isEqualTo(contenido);
    }
    @Test void retryAfterMayorYErrorPermanenteNoConsumenTresIntentosInutiles() throws Exception {
        preparar(3);drive.falloReserva=new FalloIntegracionGoogle(Causa.CUOTA,Duration.ofMinutes(20));worker().ejecutar();
        reloj.avanzar(Duration.ofMinutes(19));assertThat(worker().ejecutar()).isZero();
        reloj.avanzar(Duration.ofMinutes(1));drive.falloReserva=new FalloIntegracionGoogle(Causa.AUTORIZACION_REQUERIDA);worker().ejecutar();
        assertThat(estado()).isEqualTo("FALLIDO");assertThat(intentos()).isEqualTo(2);
    }
    @Test void respuestaInciertaDespuesDeAceptarSeReconciliaSinSegundoArchivo() throws Exception {
        preparar(5);drive.inciertoAlFinal=true;worker().ejecutar();
        assertThat(estado()).isEqualTo("COPIADO");assertThat(drive.inicios).isEqualTo(1);assertThat(drive.envios).isEqualTo(1);
        assertThat(d.sql.queryForObject("select drive_id from adjunto where id=?",String.class,d.adjunto)).isEqualTo(drive.copia.idReservado());
        assertThat(d.sql.queryForObject("select drive_enlace from adjunto where id=?",String.class,d.adjunto)).isEqualTo(drive.remoto().enlace());
        assertThat(worker().ejecutar()).isZero();assertThat(Files.readAllBytes(disco.resolve("pendiente/archivo"))).isEqualTo(contenido);
    }
    @Test void reinicioRecuperaMismoIntentoDesdeOffsetRemotoSinPerderBytes() throws Exception {
        preparar(4194304+3);drive.caerPrimerBloque=true;
        assertThatThrownBy(()->worker().ejecutar()).isInstanceOf(AssertionError.class);
        assertThat(intentos()).isEqualTo(1);assertThat(drive.offset).isEqualTo(4194304);
        assertThat(d.sql.queryForObject("select drive_offset from adjunto where id=?",Long.class,d.adjunto)).isZero();
        reloj.avanzar(Duration.ofMinutes(4));drive.caerPrimerBloque=false;
        assertThat(worker().ejecutar()).isEqualTo(1);assertThat(estado()).isEqualTo("COPIADO");assertThat(intentos()).isEqualTo(1);
        assertThat(drive.inicios).isEqualTo(1);assertThat(drive.iniciosBloque).containsExactly(0L,4194304L);
        assertThat(Files.readAllBytes(disco.resolve("pendiente/archivo"))).isEqualTo(contenido);
    }
    @Test void cuartoIntentoInterrumpidoTambienSeRecuperaSinQuinto() throws Exception {
        preparar(1);d.sql.update("update adjunto set intentos=4,copia_en_intento=true where id=?",d.adjunto);
        worker().ejecutar();assertThat(estado()).isEqualTo("COPIADO");assertThat(intentos()).isEqualTo(4);
    }
    @Test void sesionCaducadaReiniciaContenidoEnElMismoId() throws Exception {
        preparar(4194304+1);drive.caerPrimerBloque=true;assertThatThrownBy(()->worker().ejecutar()).isInstanceOf(AssertionError.class);
        String id=drive.copia.idReservado();drive.caerPrimerBloque=false;drive.sesionCaducada=true;drive.blobIncompleto=true;
        reloj.avanzar(Duration.ofMinutes(4));worker().ejecutar();assertThat(estado()).isEqualTo("COPIADO");
        assertThat(drive.copia.idReservado()).isEqualTo(id);assertThat(drive.reservas).isEqualTo(3);assertThat(drive.reinicios).isEqualTo(1); // archivo y dos carpetas
    }
    @Test void checksumRemotoDivergenteNoSeConfirmaNiBorraElLocal() throws Exception {
        preparar(7);drive.shaDivergente=true;worker().ejecutar();
        assertThat(estado()).isEqualTo("FALLIDO");
        assertThat(d.sql.queryForObject("select drive_id from adjunto where id=?",String.class,d.adjunto)).isNull();
        assertThat(d.sql.queryForObject("select ultimo_error from adjunto where id=?",String.class,d.adjunto)).isEqualTo("METADATOS_DIVERGENTES");
        assertThat(disco.resolve("pendiente/archivo")).exists();
    }
    @Test void localCorruptoFallaAntesDeGoogleSinBorrarlo() throws Exception {
        preparar(3);Files.write(disco.resolve("pendiente/archivo"),new byte[]{8,8,8});worker().ejecutar();
        assertThat(estado()).isEqualTo("FALLIDO");assertThat(drive.reservas).isZero();assertThat(disco.resolve("pendiente/archivo")).exists();
    }
    @Test void dosReclamacionesConcurrentesSoloObtienenUnaFilaYFencingImpideConfirmacionVieja() throws Exception {
        preparar(3);var barrera=new CyclicBarrier(2);var a=UUID.randomUUID();var b=UUID.randomUUID();
        try(var pool=Executors.newFixedThreadPool(2)) {
            var f1=pool.submit(()->{barrera.await(5,TimeUnit.SECONDS);return copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),a);});
            var f2=pool.submit(()->{barrera.await(5,TimeUnit.SECONDS);return copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),b);});
            var l1=f1.get(10,TimeUnit.SECONDS);var l2=f2.get(10,TimeUnit.SECONDS);
            assertThat(l1.size()+l2.size()).isEqualTo(1);assertThat(intentos()).isEqualTo(1);
            UUID ganador=l1.isEmpty()?b:a;reloj.avanzar(Duration.ofMinutes(4));UUID nuevo=UUID.randomUUID();
            assertThat(copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),nuevo)).hasSize(1);
            assertThat(intentos()).isEqualTo(1);
            assertThat(copias.reservarId(d.adjunto,ganador,"archivo-viejo",reloj.ahora())).isFalse();
            assertThat(copias.confirmar(d.adjunto,ganador,"archivo-viejo","https://drive.google.com/file/d/ficticio",reloj.ahora())).isFalse();
        }
    }
    @Test void workersConcurrentesNoDuplicanTransferencia() throws Exception {
        preparar(3);var listo=new CountDownLatch(1);var continuar=new CountDownLatch(1);drive.listo=listo;drive.continuar=continuar;
        try(var pool=Executors.newFixedThreadPool(2)) {
            var f=pool.submit(()->worker().ejecutar());assertThat(listo.await(10,TimeUnit.SECONDS)).isTrue();
            try{assertThat(worker().ejecutar()).isZero();}finally{continuar.countDown();}
            assertThat(f.get(10,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado()).isEqualTo("COPIADO");assertThat(drive.inicios).isEqualTo(1);assertThat(drive.envios).isEqualTo(1);
    }
    @Test void cerrojoOcupadoNoHaceRedNiConsumeReintentoAdicional() throws Exception {
        preparar(1);try(var lock=new BloqueoCopiasEnDisco(disco).intentar(d.adjunto).orElseThrow()){worker().ejecutar();}
        assertThat(drive.reservas).isZero();assertThat(intentos()).isEqualTo(1);
        reloj.avanzar(Duration.ofSeconds(31));worker().ejecutar();assertThat(estado()).isEqualTo("COPIADO");assertThat(intentos()).isEqualTo(1);
    }

    String fotoFila() { return d.sql.queryForObject("select row_to_json(a)::text from adjunto a where id=?",String.class,d.adjunto); }
    UUID reclamarConSesion() {
        UUID owner=UUID.randomUUID();assertThat(copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),owner)).hasSize(1);
        assertThat(copias.reservarId(d.adjunto,owner,"id-ficticio",reloj.ahora())).isTrue();
        assertThat(copias.sesion(d.adjunto,owner,"carpeta-ficticia",new byte[]{1,2,3},1,reloj.ahora())).isTrue();
        return owner;
    }
    @Test void cederConOtroPropietarioOLeaseCaducadoNoCambiaNingunDato() throws Exception {
        preparar(3);UUID owner=reclamarConSesion();String antes=fotoFila();
        assertThat(copias.ceder(d.adjunto,UUID.randomUUID(),reloj.ahora())).isFalse();assertThat(fotoFila()).isEqualTo(antes);
        reloj.avanzar(Duration.ofMinutes(3));
        assertThat(copias.ceder(d.adjunto,owner,reloj.ahora())).isFalse();assertThat(fotoFila()).isEqualTo(antes);
    }
    @Test void cesionTardiaNoModificaUnaReclamacionNueva() throws Exception {
        preparar(3);UUID viejo=reclamarConSesion();reloj.avanzar(Duration.ofSeconds(181));UUID nuevo=UUID.randomUUID();
        assertThat(copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),nuevo)).hasSize(1);
        assertThat(copias.avanzar(d.adjunto,nuevo,2,reloj.ahora())).isTrue();String antes=fotoFila();
        assertThat(copias.ceder(d.adjunto,viejo,reloj.ahora())).isFalse();assertThat(fotoFila()).isEqualTo(antes);
        assertThat(intentos()).isEqualTo(1);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"COPIADO","FALLIDO"})
    void cesionNoReabreEstadosTerminales(String terminal) throws Exception {
        preparar(3);UUID owner=reclamarConSesion();
        if(terminal.equals("COPIADO")) assertThat(copias.confirmar(d.adjunto,owner,"id-ficticio","https://drive.google.com/file/d/ficticio",reloj.ahora())).isTrue();
        else assertThat(copias.registrarFallo(d.adjunto,owner,Causa.PERMANENTE,null,true,reloj.ahora())).isTrue();
        String antes=fotoFila();assertThat(copias.ceder(d.adjunto,owner,reloj.ahora())).isFalse();assertThat(fotoFila()).isEqualTo(antes);
    }
    @Test void cesionConservaSesionOffsetIdentidadYReferenciaYReclamaElMismoIntento() throws Exception {
        preparar(3);UUID owner=reclamarConSesion();
        assertThat(copias.ceder(d.adjunto,owner,reloj.ahora())).isTrue();
        var fila=d.sql.queryForMap("select * from adjunto where id=?",d.adjunto);
        assertThat(fila).containsEntry("reclamado_por",null).containsEntry("reclamado_hasta",null)
            .containsEntry("drive_id_reservado","id-ficticio").containsEntry("drive_offset",1L)
            .containsEntry("drive_carpeta_id","carpeta-ficticia").containsEntry("clave_local","pendiente/archivo")
            .containsEntry("copia_en_intento",true).containsEntry("intentos",1).containsEntry("estado_copia","PENDIENTE");
        assertThat((byte[])fila.get("drive_sesion_cifrada")).containsExactly(1,2,3);
        assertThat(((java.sql.Timestamp)fila.get("proximo_intento_en")).toInstant()).isEqualTo(reloj.ahora().plusSeconds(30));
        assertThat(copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),UUID.randomUUID())).isEmpty();
        reloj.avanzar(Duration.ofSeconds(30));var siguiente=copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),UUID.randomUUID());
        assertThat(siguiente).hasSize(1);assertThat(siguiente.getFirst().intentos()).isEqualTo(1);
        assertThat(Files.readAllBytes(disco.resolve("pendiente/archivo"))).isEqualTo(contenido);
    }
    @Test void renovarYCederConcurrentementeNoDuplicaNiPierdeElTrabajo() throws Exception {
        preparar(3);UUID owner=reclamarConSesion();var barrera=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var renovar=pool.submit(()->{barrera.await(5,TimeUnit.SECONDS);return copias.renovar(d.adjunto,owner,reloj.ahora(),reloj.ahora().plusSeconds(240));});
            var ceder=pool.submit(()->{barrera.await(5,TimeUnit.SECONDS);return copias.ceder(d.adjunto,owner,reloj.ahora());});
            assertThat(ceder.get(10,TimeUnit.SECONDS)).isTrue();renovar.get(10,TimeUnit.SECONDS);
        }
        assertThat(d.sql.queryForObject("select reclamado_por from adjunto where id=?",UUID.class,d.adjunto)).isNull();
        reloj.avanzar(Duration.ofSeconds(31));assertThat(copias.reclamar(1,reloj.ahora(),reloj.ahora().plusSeconds(180),UUID.randomUUID())).hasSize(1);
        assertThat(intentos()).isEqualTo(1);assertThat(d.sql.queryForObject("select count(*) from adjunto",Long.class)).isEqualTo(1);
    }

    @Test void leaseVenceConTransferenciaEnVueloYLaRecuperacionNoReenviaNiConsumeOtroIntento() throws Exception {
        preparar(3);drive.listo=new CountDownLatch(1);drive.continuar=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var viejo=pool.submit(()->worker().ejecutar());assertThat(drive.listo.await(10,TimeUnit.SECONDS)).isTrue();
            try {
                reloj.avanzar(Duration.ofMinutes(4));assertThat(worker().ejecutar()).isEqualTo(1);
                assertThat(drive.inicios).isEqualTo(1);assertThat(intentos()).isEqualTo(1);
            } finally {drive.continuar.countDown();}
            assertThat(viejo.get(10,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado()).isEqualTo("PENDIENTE");
        assertThat(d.sql.queryForObject("select drive_id from adjunto where id=?",String.class,d.adjunto)).isNull();
        reloj.avanzar(Duration.ofSeconds(31));worker().ejecutar();
        assertThat(estado()).isEqualTo("COPIADO");assertThat(intentos()).isEqualTo(1);
        assertThat(drive.inicios).isEqualTo(1);assertThat(drive.envios).isEqualTo(1);
        assertThat(Files.readAllBytes(disco.resolve("pendiente/archivo"))).isEqualTo(contenido);
    }
    @Test void carpetaCreadaConRespuestaInciertaSeReconciliaSinCrearOtra() throws Exception {
        preparar(3);drive.inciertoCarpeta=true;worker().ejecutar();
        assertThat(estado()).isEqualTo("COPIADO");assertThat(drive.carpetas).hasSize(2);
        assertThat(drive.reservas).isEqualTo(3);assertThat(drive.inicios).isEqualTo(1);
        assertThat(d.sql.queryForObject("select count(*) from carpeta_drive where confirmada",Long.class)).isEqualTo(2);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings={"pedido","entrada","padre","propiedad"})
    void reconciliacionNoConfirmaUnArchivoConIdentidadRemotaAjena(String campo) throws Exception {
        preparar(3);drive.identidadAjena=campo;worker().ejecutar();assertThat(estado()).isEqualTo("FALLIDO");
        assertThat(d.sql.queryForObject("select drive_id from adjunto where id=?",String.class,d.adjunto)).isNull();
        assertThat(d.sql.queryForObject("select ultimo_error from adjunto where id=?",String.class,d.adjunto)).isEqualTo("METADATOS_DIVERGENTES");
        assertThat(drive.inicios).isEqualTo(1);assertThat(drive.envios).isEqualTo(1);assertThat(disco.resolve("pendiente/archivo")).exists();
    }
    final class DriveFalso implements AlmacenArchivos {
        final Map<String,Remoto> carpetas=new ConcurrentHashMap<>();
        final List<Long> iniciosBloque=new ArrayList<>();
        int reservas,inicios,envios,reinicios;long offset;Copia copia;
        String identidadAjena="";boolean inciertoCarpeta,blobIncompleto,inciertoAlFinal,caerPrimerBloque,sesionCaducada,shaDivergente;FalloIntegracionGoogle falloReserva;
        CountDownLatch listo,continuar;
        void fueraTx(){assertThat(TransactionSynchronizationManager.isActualTransactionActive()).as("Google fuera de transacción").isFalse();}
        public String reservarId(){fueraTx();if(falloReserva!=null)throw falloReserva;return "ficticio_"+(++reservas);}
        public Sesion iniciar(Copia c){fueraTx();inicios++;copia=c;offset=0;return new Sesion("sesion-ficticia");}
        public Sesion reiniciar(Copia c){reinicios++;return iniciar(c);}
        public Progreso consultar(Sesion s,long total){fueraTx();if(sesionCaducada){sesionCaducada=false;throw new FalloIntegracionGoogle(Causa.SESION_CADUCADA);}return new Progreso(offset,Optional.empty());}
        public Progreso enviarFragmento(Sesion s,long inicio,long total,byte[] b) {
            fueraTx();if(listo!=null){listo.countDown();try{if(!continuar.await(10,TimeUnit.SECONDS))throw new AssertionError("Worker rival no liberó barrera");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new AssertionError(e);}}
            assertThat(inicio).isEqualTo(offset);assertThat(b).isEqualTo(Arrays.copyOfRange(contenido,(int)inicio,(int)inicio+b.length));
            envios++;iniciosBloque.add(inicio);offset+=b.length;
            if(caerPrimerBloque)throw new AssertionError("Caída de proceso simulada tras ACK remoto, antes del checkpoint");
            if(inciertoAlFinal && offset==total)throw new FalloIntegracionGoogle(Causa.RESULTADO_INCIERTO);
            return new Progreso(offset,offset==total?Optional.of(remoto()):Optional.empty());
        }
        public Optional<Remoto> metadatos(String id){fueraTx();
            if(id.equals("root"))return Optional.of(carpeta("raiz-ficticia","Mi unidad",List.of(),""));
            if(carpetas.containsKey(id))return Optional.of(carpetas.get(id));
            if(blobIncompleto && copia!=null && id.equals(copia.idReservado()) && offset<contenido.length) {
                Remoto r=remoto();return Optional.of(new Remoto(r.id(),0,r.enlace(),r.adjuntoId(),r.pedidoId(),r.entradaId(),r.padres(),r.tipo(),"","",false,true,"","",r.nombre()));
            }
            return copia!=null && id.equals(copia.idReservado()) && offset==contenido.length?Optional.of(remoto()):Optional.empty();
        }
        public Remoto crearCarpeta(String id,String nombre,String padre,String clave){fueraTx();var r=carpeta(id,nombre,List.of(padre),clave);assertThat(carpetas.putIfAbsent(id,r)).isNull();if(inciertoCarpeta){inciertoCarpeta=false;throw new FalloIntegracionGoogle(Causa.RESULTADO_INCIERTO);}return r;}
        Remoto carpeta(String id,String nombre,List<String> padres,String clave){return new Remoto(id,0,"","","","",padres,"application/vnd.google-apps.folder","","",false,true,"",clave,nombre);}
        Remoto remoto(){try{return new Remoto(copia.idReservado(),contenido.length,"https://drive.google.com/file/d/ficticio",d.adjunto.toString(),identidadAjena.equals("pedido")?UUID.randomUUID().toString():d.pedido.toString(),identidadAjena.equals("entrada")?UUID.randomUUID().toString():d.entrada.toString(),List.of(identidadAjena.equals("padre")?"carpeta-ajena":copia.carpetaId()),"application/octet-stream",shaDivergente?"0".repeat(64):HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(contenido)),"",false,!identidadAjena.equals("propiedad"),"","","ficticio.bin");}catch(Exception e){throw new AssertionError(e);}}
    }
}
