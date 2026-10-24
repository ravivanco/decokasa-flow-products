package com.decokasa.seguimiento.archivos;

import static org.assertj.core.api.Assertions.*;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.*;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.*;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.*;
import com.decokasa.seguimiento.archivos.application.usecase.CopiarPendientesCasoDeUso;
import com.decokasa.seguimiento.archivos.config.CopiaDriveConfig;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.*;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import com.decokasa.seguimiento.pedidos.domain.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.*;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** PostgreSQL propio por clase/esquema por caso; el único Drive es este doble en memoria. */
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
class CarpetasDriveAdr25IT {
    @Container static final PostgreSQLContainer PG = new PostgreSQLContainer("postgres:17").withReuse(false);
    @TempDir Path disco;
    DatosB1 d;
    AlmacenLocal local;
    CopiaAdjuntoRepositorio copias;
    CarpetaDriveRepositorio carpetas;
    AutoresDeCopias autores;
    DriveDoble drive;
    Instant ahora = Instant.parse("2026-10-24T12:00:00Z");
    final Reloj reloj = () -> ahora;
    final Map<UUID,ResumirUsuarios.Resumen> nombres = new ConcurrentHashMap<>();
    final byte[] contenido = {11,12,13};
    final PoliticaCopia politica = new PoliticaCopia(Duration.ofMinutes(3),
            List.of(Duration.ofMinutes(1),Duration.ofMinutes(5),Duration.ofMinutes(15)));
    final ProtectorSesionDrive protector = new ProtectorSesionDrive() {
        public byte[] cifrar(UUID id,AlmacenArchivos.Sesion sesion) { return sesion.uri().getBytes(java.nio.charset.StandardCharsets.UTF_8); }
        public AlmacenArchivos.Sesion abrir(UUID id,byte[] bytes) { return new AlmacenArchivos.Sesion(new String(bytes,java.nio.charset.StandardCharsets.UTF_8)); }
    };

    @SuppressWarnings("unchecked")
    <T> T tx(Class<T> tipo,T repo) {
        var plantilla = new TransactionTemplate(new DataSourceTransactionManager(d.ds));
        return (T) Proxy.newProxyInstance(tipo.getClassLoader(),new Class<?>[]{tipo},(p,m,a) -> plantilla.execute(s -> {
            try { return m.invoke(repo,a); }
            catch (InvocationTargetException e) { throw (RuntimeException)e.getCause(); }
            catch (IllegalAccessException e) { throw new IllegalStateException(e); }
        }));
    }
    @BeforeEach void preparar() throws Exception {
        local = new AlmacenLocalEnDisco(disco);
        var recibido = local.recibir(new ByteArrayInputStream(contenido),100);
        local.confirmar(recibido,"fixture/archivo");
        d = new DatosB1(PG,null);
        d.poblar(contenido.length,recibido.sha256(),"fixture/archivo");
        copias = tx(CopiaAdjuntoRepositorio.class,new CopiaAdjuntoSqlRepositorio(d.ds));
        carpetas = tx(CarpetaDriveRepositorio.class,new CarpetaDriveSqlRepositorio(d.ds));
        autores = tx(AutoresDeCopias.class,new AutoresDeCopiasSql(d.ds));
        nombre(d.usuario,"Persona ficticia ADR25",Set.of(new CodigoRol("admin")));
        drive = new DriveDoble();
    }
    void nombre(UUID usuario,String nombre,Set<CodigoRol> roles) {
        nombres.put(usuario,new ResumirUsuarios.Resumen(usuario,nombre,roles,!roles.isEmpty()));
    }
    CopiarPendientesCasoDeUso worker() {
        return new CopiarPendientesCasoDeUso(true,1,copias,carpetas,pedidos(),drive,local,
                new BloqueoCopiasEnDisco(disco),protector,reloj,politica,autores,ids -> Map.copyOf(nombres));
    }
    PedidosParaCopias pedidos() {
        return id -> d.sql.queryForObject("select pais_codigo,numero from pedido where id=?",
                (r,n) -> new PedidosParaCopias.Destino(CodigoPais.valueOf(r.getString(1)),r.getString(2)),id);
    }
    String estado(UUID adjunto) { return d.sql.queryForObject("select estado_copia from adjunto where id=?",String.class,adjunto); }
    String error(UUID adjunto) { return d.sql.queryForObject("select ultimo_error from adjunto where id=?",String.class,adjunto); }
    String destino(UUID adjunto) { return drive.copias.values().stream().filter(c -> c.adjuntoId().equals(adjunto)).findFirst().orElseThrow().carpetaId(); }
    String clavePedido(UUID pedido) { return "d/p/"+compacto(pedido); }
    static String compacto(UUID id) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(16)
                .putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).array());
    }
    UUID usuario(String nombre) {
        UUID id=UUID.randomUUID();
        d.sql.update("insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,creado_en,actualizado_en,version) values (?,?,?,?,?,'hash-ficticio',now(),now(),0)",
                id,nombre,id+"@ejemplo.test",id.toString(),"+593990000002");
        nombre(id,nombre,Set.of(new CodigoRol("admin"))); return id;
    }
    UUID pedido(String pais) {
        UUID id=UUID.randomUUID();
        UUID flujo=d.sql.queryForObject("select id from definicion_flujo where pais_codigo=? and tipo_flujo='A' order by version desc limit 1",UUID.class,pais);
        d.sql.update("insert into pedido(id,numero,pais_codigo,tipo_flujo,flujo_id,nombre,creado_por,creado_en,etapa_actual,actualizado_en,version) values (?,?,?,'A',?,'Pedido ficticio ADR25',?,now(),1,now(),0)",
                id,"DK-"+pais+"-"+id.toString().substring(0,8),pais,flujo,d.usuario); return id;
    }
    UUID archivo(UUID pedido,UUID usuario,String rol) { return archivo(pedido,usuario,usuario,pedido,rol,"ARCHIVO_SUBIDO",true); }
    UUID archivo(UUID pedido,UUID autor,UUID autorEvento,UUID pedidoEvento,String rol,String tipo,boolean enlazado) {
        UUID entrada=UUID.randomUUID(),id=UUID.randomUUID(),evento=UUID.randomUUID();
        d.sql.update("insert into entrada_etapa(id,pedido_id,etapa,entrada_en) values (?,?,1,now())",entrada,pedido);
        d.sql.update("insert into adjunto(id,pedido_id,etapa,entrada_id,tipo_documento,nombre,tamano_bytes,sha256,clave_local,estado_copia,subido_por,subido_en) select ?,?,1,?,'Ficha','ficticio.bin',tamano_bytes,sha256,clave_local,'PENDIENTE',?,now() from adjunto where id=?",id,pedido,entrada,autor,d.adjunto);
        if (enlazado) {
            d.sql.update("insert into evento_pedido(id,pedido_id,etapa,tipo,usuario_id,rol_codigo,ocurrido_en,datos) values (?,?,1,?,?,?,now(),'{}')",evento,pedidoEvento,tipo,autorEvento,rol);
            d.sql.update("insert into evento_adjunto(evento_id,adjunto_id,relacion) values (?,?,'SUBIDO')",evento,id);
        }
        return id;
    }
    void copiar(UUID id) {
        assertThat(worker().ejecutar()).isEqualTo(1);
        assertThat(estado(id)).isEqualTo("COPIADO");
    }
    @Test void creaCincoNivelesConClavesPadresYDestinoConfirmados(CapturedOutput salida) {
        copiar(d.adjunto);
        assertThat(drive.creaciones).extracting(AlmacenArchivos.Remoto::nombre)
                .containsExactly("DECOKASA","EC","DK-EC-2026-0001","admin","Persona ficticia ADR25 · "+d.usuario.toString().substring(28));
        String padre="root-real";
        for (var carpeta:drive.creaciones) {
            assertThat(carpeta.padres()).containsExactly(padre);
            var fila=carpetas.buscar(carpeta.claveCarpeta()).orElseThrow();
            assertThat(fila.driveId()).isEqualTo(carpeta.id());
            assertThat(fila.padreId()).isEqualTo(padre);
            assertThat(fila.nombre()).isEqualTo(carpeta.nombre());
            assertThat(fila.confirmada()).isTrue();
            assertThat(fila.clave().length()).isLessThanOrEqualTo(100);
            padre=carpeta.id();
        }
        assertThat(destino(d.adjunto)).isEqualTo(padre);
        assertThat(drive.creaciones).extracting(AlmacenArchivos.Remoto::claveCarpeta).containsExactly(
                "d","d/pais/EC",clavePedido(d.pedido),clavePedido(d.pedido)+"/admin",clavePedido(d.pedido)+"/admin/"+compacto(d.usuario));
        assertThat(salida.getAll()).doesNotContain("Persona ficticia ADR25",compacto(d.usuario),clavePedido(d.pedido));
    }
    @Test void reutilizaMismaPersonaYRolSinNuevasReservasDeCarpetas() {
        copiar(d.adjunto); int reservas=drive.reservas.get();
        UUID nuevo=archivo(d.pedido,d.usuario,"admin"); copiar(nuevo);
        assertThat(drive.creaciones).hasSize(5); assertThat(drive.reservas.get()).isEqualTo(reservas+1);
        assertThat(destino(nuevo)).isEqualTo(destino(d.adjunto));
    }
    @Test void otraPersonaMismoRolCreaSoloPersonaYSeparaHomonimos() {
        copiar(d.adjunto); UUID otro=usuario("Persona ficticia ADR25"); UUID nuevo=archivo(d.pedido,otro,"admin"); copiar(nuevo);
        assertThat(drive.creaciones).hasSize(6); assertThat(destino(nuevo)).isNotEqualTo(destino(d.adjunto));
        assertThat(drive.creaciones.getLast().nombre()).endsWith(otro.toString().substring(28));
    }
    @Test void renombrarPersonaNoRenombraNiDuplicaCarpeta() {
        copiar(d.adjunto); String anterior=drive.remotos.get(destino(d.adjunto)).nombre();
        nombre(d.usuario,"Nombre posterior",Set.of()); UUID nuevo=archivo(d.pedido,d.usuario,"admin"); copiar(nuevo);
        assertThat(drive.creaciones).hasSize(5); assertThat(drive.remotos.get(destino(nuevo)).nombre()).isEqualTo(anterior);
    }
    @Test void rolDelEventoGanaSobreRolesActualesYAdminTieneCarpetaPropia() {
        copiar(d.adjunto);
        nombre(d.usuario,"Persona ficticia ADR25",Set.of(new CodigoRol("marketing"),new CodigoRol("asistente_compras")));
        UUID nuevo=archivo(d.pedido,d.usuario,"asistente_compras");
        nombre(d.usuario,"Persona ficticia ADR25",Set.of()); copiar(nuevo);
        assertThat(drive.creaciones).hasSize(7);
        assertThat(drive.remotos.get(destino(nuevo)).claveCarpeta()).contains("/asistente_compras/");
        assertThat(drive.remotos.get(destino(d.adjunto)).claveCarpeta()).contains("/admin/");
    }
    @ParameterizedTest @ValueSource(strings={"multirrol","admin","admin-y-responsable","asignada-a-otra"})
    void p06DecideRolAlSubirYElEventoLoConserva(String caso) {
        copiar(d.adjunto);
        var responsable=new CodigoRol("asistente_compras"); var admin=new CodigoRol("admin");
        Set<CodigoRol> roles=caso.equals("multirrol")?Set.of(responsable,new CodigoRol("marketing")):
                caso.equals("admin")?Set.of(admin):Set.of(admin,responsable);
        var etapa=new EtapaDePedido(UUID.randomUUID(),2,EstadoEtapa.ACTUAL,
                caso.equals("asignada-a-otra")?UUID.randomUUID():null,ahora,null,null,0,UUID.randomUUID());
        var actor=new ActorVigente(d.usuario,true,roles,Set.of(CodigoPais.EC));
        var intervencion=PermisosEtapa.paraActuar(etapa,responsable,actor);
        String esperado=caso.equals("admin")||caso.equals("asignada-a-otra")?"admin":"asistente_compras";
        assertThat(intervencion.rol().valor()).isEqualTo(esperado);
        UUID nuevo=archivo(d.pedido,d.usuario,intervencion.rol().valor());
        nombre(d.usuario,"Persona ficticia ADR25",Set.of()); copiar(nuevo);
        assertThat(drive.remotos.get(destino(nuevo)).claveCarpeta()).contains("/"+esperado+"/");
    }
    @Test void aislamientoEcCoYPedidosDelMismoPais() {
        copiar(d.adjunto); UUID co=pedido("CO"),ec=pedido("EC");
        UUID coArchivo=archivo(co,d.usuario,"admin"); copiar(coArchivo);
        UUID ecArchivo=archivo(ec,d.usuario,"admin"); copiar(ecArchivo);
        assertThat(Set.of(destino(d.adjunto),destino(coArchivo),destino(ecArchivo))).hasSize(3);
        assertThat(carpetas.buscar("d/pais/EC")).isPresent(); assertThat(carpetas.buscar("d/pais/CO")).isPresent();
        String coPedido=carpetas.buscar(clavePedido(co)).orElseThrow().padreId();
        assertThat(coPedido).isEqualTo(carpetas.buscar("d/pais/CO").orElseThrow().driveId());
        assertThat(carpetas.buscar(clavePedido(ec)).orElseThrow().padreId()).isEqualTo(carpetas.buscar("d/pais/EC").orElseThrow().driveId());
    }
    @ParameterizedTest @ValueSource(strings={"ausente","otro-usuario","otro-pedido","otro-tipo"})
    void eventoIncoherenteFallaSinCrearCarpetas(String caso) {
        // Se conserva el fixture ya COPIADO para que solo se reclame el adjunto inválido.
        copiar(d.adjunto); drive.creaciones.clear();
        UUID otro=usuario("Otra persona ficticia"),otroPedido=pedido("EC");
        UUID nuevo=archivo(d.pedido,d.usuario,caso.equals("otro-usuario")?otro:d.usuario,
                caso.equals("otro-pedido")?otroPedido:d.pedido,"admin",
                caso.equals("otro-tipo")?"ETAPA_ENVIADA":"ARCHIVO_SUBIDO",!caso.equals("ausente"));
        worker().ejecutar();
        assertThat(estado(nuevo)).isEqualTo("FALLIDO"); assertThat(error(nuevo)).isEqualTo("CONFIGURACION");
        assertThat(drive.creaciones).isEmpty();
    }
    @ParameterizedTest @ValueSource(strings={"RESULTADO_INCIERTO","CONFLICTO_REMOTO"})
    void crearConTimeoutO409ConsultaMismoIdSinDuplicar(String causa) {
        drive.falloCrear=Causa.valueOf(causa); copiar(d.adjunto);
        assertThat(drive.creaciones).hasSize(5);
        assertThat(drive.llamadasCrear.values()).allSatisfy(n -> assertThat(n.get()).isEqualTo(1));
        assertThat(d.sql.queryForObject("select count(*) from carpeta_drive where confirmada",Long.class)).isEqualTo(5);
    }
    @Test void respuestaInciertaSinObjetoVisibleRetomaMismoIdReservado() {
        drive.fallarAntesDeCrear=true; worker().ejecutar();
        assertThat(estado(d.adjunto)).isEqualTo("PENDIENTE");
        assertThat(error(d.adjunto)).isEqualTo("RESULTADO_INCIERTO");
        var reserva=carpetas.buscar("d").orElseThrow(); assertThat(reserva.confirmada()).isFalse();
        ahora=ahora.plusSeconds(61); drive.fallarAntesDeCrear=false; copiar(d.adjunto);
        assertThat(carpetas.buscar("d").orElseThrow().driveId()).isEqualTo(reserva.driveId());
        assertThat(drive.creaciones).hasSize(5);
        assertThat(drive.creaciones.getFirst().id()).isEqualTo(reserva.driveId());
    }
    @ParameterizedTest @ValueSource(strings={"ausente","nombre","padre","papelera","propietario","compartida","clave"})
    void divergenciaDeCarpetaConfirmadaBloqueaSinReemplazar(String campo) {
        copiar(d.adjunto); String id=destino(d.adjunto); var r=drive.remotos.get(id);
        if(campo.equals("ausente")) drive.remotos.remove(id);
        else drive.remotos.put(id,new AlmacenArchivos.Remoto(r.id(),r.tamano(),r.enlace(),r.adjuntoId(),r.pedidoId(),r.entradaId(),
                campo.equals("padre")?List.of("otro-padre"):r.padres(),r.tipo(),r.sha256(),r.md5(),campo.equals("papelera"),
                !campo.equals("propietario"),campo.equals("compartida")?"unidad-ajena":"",campo.equals("clave")?"otra":r.claveCarpeta(),campo.equals("nombre")?"Cambiado":r.nombre()));
        UUID nuevo=archivo(d.pedido,d.usuario,"admin"); worker().ejecutar();
        assertThat(estado(nuevo)).isEqualTo("FALLIDO"); assertThat(error(nuevo)).isEqualTo("METADATOS_DIVERGENTES");
        assertThat(drive.creaciones).hasSize(5);
    }
    @Test void copiaEmpezadaAnteriorTerminaAlliYCopiadosPermanecenIguales() {
        var pais=carpetas.reservar("pais:EC","EC","root-real","pais-anterior",ahora);
        carpetas.confirmar(pais.clave(),pais.driveId());
        var pedido=carpetas.reservar("pedido:"+d.pedido,"DK-EC-2026-0001",pais.driveId(),"pedido-anterior",ahora);
        carpetas.confirmar(pedido.clave(),pedido.driveId());
        drive.remotos.put(pais.driveId(),drive.carpeta(pais.driveId(),pais.nombre(),pais.padreId(),pais.clave()));
        drive.remotos.put(pedido.driveId(),drive.carpeta(pedido.driveId(),pedido.nombre(),pedido.padreId(),pedido.clave()));
        d.sql.update("update adjunto set drive_id_reservado='archivo-anterior',drive_carpeta_id=?,intentos=1,copia_en_intento=true where id=?",pedido.driveId(),d.adjunto);
        copiar(d.adjunto); assertThat(destino(d.adjunto)).isEqualTo("pedido-anterior"); assertThat(drive.creaciones).isEmpty();
        String antes=d.sql.queryForObject("select row_to_json(a)::text from adjunto a where id=?",String.class,d.adjunto);
        assertThat(worker().ejecutar()).isZero();
        assertThat(d.sql.queryForObject("select row_to_json(a)::text from adjunto a where id=?",String.class,d.adjunto)).isEqualTo(antes);
        UUID nuevo=archivo(d.pedido,d.usuario,"admin"); copiar(nuevo);
        assertThat(destino(nuevo)).isNotEqualTo("pedido-anterior"); assertThat(drive.creaciones).hasSize(5);
    }
    @Test void constructorNuevoNoAdmitePuertosNulosNiCaeAlAnterior() {
        assertThatThrownBy(() -> new CopiarPendientesCasoDeUso(true,1,copias,carpetas,pedidos(),drive,local,
                new BloqueoCopiasEnDisco(disco),protector,reloj,politica,null,ids -> Map.of())).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CopiarPendientesCasoDeUso(true,1,copias,carpetas,pedidos(),drive,local,
                new BloqueoCopiasEnDisco(disco),protector,reloj,politica,autores,null)).isInstanceOf(NullPointerException.class);
    }
    @Test void composicionRealUsaConstructorNuevoYNoHaceNadaConFlagsDesactivados() throws Exception {
        var metodo=Arrays.stream(CopiaDriveConfig.class.getDeclaredMethods()).filter(m -> m.getName().equals("reintentarCopiasPendientes")).findFirst().orElseThrow();
        metodo.setAccessible(true);
        Object[] argumentos={new DataSourceTransactionManager(d.ds),new CopiaAdjuntoSqlRepositorio(d.ds),
                new CarpetaDriveSqlRepositorio(d.ds),pedidos(),new AutoresDeCopiasSql(d.ds),
                (ResumirUsuarios)ids -> Map.copyOf(nombres),drive,local,new BloqueoCopiasEnDisco(disco),protector,reloj,
                false,false,1,"PT3M","PT1M","PT5M","PT15M"};
        var apagado=(ReintentarCopiasPendientes)metodo.invoke(new CopiaDriveConfig(),argumentos);
        assertThat(apagado.ejecutar()).isZero(); assertThat(drive.reservas.get()).isZero();
        // Solo se habilita el caso de uso contra el doble; no flags de entorno ni integración Google.
        argumentos[11]=true; argumentos[12]=true;
        var simulado=(ReintentarCopiasPendientes)metodo.invoke(new CopiaDriveConfig(),argumentos);
        assertThat(simulado.ejecutar()).isEqualTo(1); assertThat(estado(d.adjunto)).isEqualTo("COPIADO");
        assertThat(drive.creaciones).hasSize(5); assertThat(carpetas.buscar("pais:EC")).isEmpty();
    }
    @Test void composicionRealSinAutorFallaCerradaSinDestinoAnterior() throws Exception {
        var metodo=Arrays.stream(CopiaDriveConfig.class.getDeclaredMethods()).filter(m -> m.getName().equals("reintentarCopiasPendientes")).findFirst().orElseThrow();
        metodo.setAccessible(true);
        Object[] argumentos={new DataSourceTransactionManager(d.ds),new CopiaAdjuntoSqlRepositorio(d.ds),
                new CarpetaDriveSqlRepositorio(d.ds),pedidos(),(AutoresDeCopias)id -> Optional.empty(),
                (ResumirUsuarios)ids -> Map.copyOf(nombres),drive,local,new BloqueoCopiasEnDisco(disco),protector,reloj,
                true,true,1,"PT3M","PT1M","PT5M","PT15M"};
        var simulado=(ReintentarCopiasPendientes)metodo.invoke(new CopiaDriveConfig(),argumentos);
        assertThat(simulado.ejecutar()).isEqualTo(1); assertThat(estado(d.adjunto)).isEqualTo("FALLIDO");
        assertThat(error(d.adjunto)).isEqualTo("CONFIGURACION"); assertThat(drive.creaciones).isEmpty();
        assertThat(carpetas.buscar("pais:EC")).isEmpty();
    }
    @Test void reservaSqlConcurrenteDevuelveIdGanadorInclusoAlPerdedor() throws Exception {
        var barrera=new CyclicBarrier(2);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> {barrera.await(10,TimeUnit.SECONDS);return carpetas.reservar("d","DECOKASA","root-real","candidato-a",ahora);});
            var b=pool.submit(() -> {barrera.await(10,TimeUnit.SECONDS);return carpetas.reservar("d","DECOKASA","root-real","candidato-b",ahora);});
            var primero=a.get(20,TimeUnit.SECONDS); var segundo=b.get(20,TimeUnit.SECONDS);
            assertThat(primero.driveId()).isEqualTo(segundo.driveId()).isIn("candidato-a","candidato-b");
            assertThat(d.sql.queryForObject("select count(*) from carpeta_drive where clave='d'",Long.class)).isEqualTo(1);
        }
        assertThat(drive.creaciones).isEmpty();
    }
    @Test void dosWorkersConPostgresConcilianMismoIdYCreanUnSoloObjetoPorClave() throws Exception {
        UUID segundo=archivo(d.pedido,d.usuario,"admin");
        drive.carrera=true;
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(() -> worker().ejecutar()); var b=pool.submit(() -> worker().ejecutar());
            assertThat(a.get(30,TimeUnit.SECONDS)).isEqualTo(1); assertThat(b.get(30,TimeUnit.SECONDS)).isEqualTo(1);
        }
        assertThat(estado(d.adjunto)).isEqualTo("COPIADO"); assertThat(estado(segundo)).isEqualTo("COPIADO");
        assertThat(destino(segundo)).isEqualTo(destino(d.adjunto));
        assertThat(d.sql.queryForObject("select count(*) from carpeta_drive",Long.class)).isEqualTo(5);
        assertThat(drive.creaciones).hasSize(5);
        // La idempotencia promete un objeto por ID; dos POST concurrentes pueden conciliarse por 409.
        assertThat(drive.llamadasCrear.get("d").get()).isEqualTo(2);
        assertThat(drive.idsCrear.get("d")).hasSize(2).containsOnly(carpetas.buscar("d").orElseThrow().driveId());
        assertThat(drive.creaciones.stream().filter(c -> c.claveCarpeta().equals("d")).count()).isEqualTo(1);
    }
    final class DriveDoble implements AlmacenArchivos {
        final AtomicInteger reservas=new AtomicInteger();
        final Map<String,Remoto> remotos=new ConcurrentHashMap<>();
        final Map<String,Copia> copias=new ConcurrentHashMap<>();
        final Map<String,AtomicInteger> llamadasCrear=new ConcurrentHashMap<>();
        final Map<String,List<String>> idsCrear=new ConcurrentHashMap<>();
        final List<Remoto> creaciones=new CopyOnWriteArrayList<>();
        final Set<String> completos=ConcurrentHashMap.newKeySet();
        final CyclicBarrier ambosSinRaiz=new CyclicBarrier(2),ambosCreando=new CyclicBarrier(2);
        volatile boolean carrera;
        volatile Causa falloCrear;
        volatile boolean fallarAntesDeCrear;
        void fueraTx() { assertThat(TransactionSynchronizationManager.isActualTransactionActive()).as("Drive siempre fuera de tx").isFalse(); }
        public String reservarId() { fueraTx(); return "ficticio_"+reservas.incrementAndGet(); }
        public Sesion iniciar(Copia c) { fueraTx(); copias.put(c.idReservado(),c); return new Sesion(c.idReservado()); }
        public Sesion reiniciar(Copia c) { return iniciar(c); }
        public Progreso consultar(Sesion s,long total) { fueraTx(); return new Progreso(0,Optional.empty()); }
        public Progreso enviarFragmento(Sesion s,long inicio,long total,byte[] bytes) {
            fueraTx(); assertThat(bytes).isEqualTo(contenido); completos.add(s.uri()); return new Progreso(total,Optional.of(remotosArchivo(s.uri())));
        }
        Remoto remotosArchivo(String id) {
            Copia c=copias.get(id); String sha=d.sql.queryForObject("select sha256 from adjunto where id=?",String.class,c.adjuntoId());
            return new Remoto(id,contenido.length,"https://drive.google.com/file/d/"+id,c.adjuntoId().toString(),c.pedidoId().toString(),c.entradaId().toString(),List.of(c.carpetaId()),"application/octet-stream",sha,"",false,true,"","",c.nombre());
        }
        public Optional<Remoto> metadatos(String id) {
            fueraTx();
            if(id.equals("root")) return Optional.of(new Remoto("root-real",0,"","","","",List.of(),"application/vnd.google-apps.folder","","",false,true,"","","Mi unidad"));
            if(completos.contains(id)) return Optional.of(remotosArchivo(id));
            Optional<Remoto> resultado=Optional.ofNullable(remotos.get(id));
            if(carrera && resultado.isEmpty() && carpetas.buscar("d").map(c -> c.driveId().equals(id)).orElse(false)) esperar(ambosSinRaiz);
            return resultado;
        }
        public Remoto crearCarpeta(String id,String nombre,String padre,String clave) {
            fueraTx(); llamadasCrear.computeIfAbsent(clave,k -> new AtomicInteger()).incrementAndGet();
            idsCrear.computeIfAbsent(clave,k -> new CopyOnWriteArrayList<>()).add(id);
            if(fallarAntesDeCrear) throw new FalloIntegracionGoogle(Causa.RESULTADO_INCIERTO);
            if(carrera && clave.equals("d")) esperar(ambosCreando);
            Remoto r=carpeta(id,nombre,padre,clave);
            if(remotos.putIfAbsent(id,r)!=null) throw new FalloIntegracionGoogle(Causa.CONFLICTO_REMOTO);
            creaciones.add(r);
            if(falloCrear!=null) throw new FalloIntegracionGoogle(falloCrear);
            return r;
        }
        Remoto carpeta(String id,String nombre,String padre,String clave) {
            return new Remoto(id,0,"","","","",List.of(padre),"application/vnd.google-apps.folder","","",false,true,"",clave,nombre);
        }
        void esperar(CyclicBarrier barrera) {
            try { barrera.await(15,TimeUnit.SECONDS); }
            catch(Exception e) { throw new AssertionError("No se alcanzo la carrera coordinada",e); }
        }
    }
}
