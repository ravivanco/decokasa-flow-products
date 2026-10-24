package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AutoresDeCopias;
import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Núcleo sin Spring/JPA/SDK. Los repositorios inyectados cierran su transacción antes de retornar.
 *
 * <p>Organización aprobada por Richard (2026-10-24): {@code Mi unidad/DECOKASA/{EC|CO}/{numeroPedido}/{rol}/{persona}/}.
 * Cada carpeta se crea al necesitarla la primera copia, con un ID reservado y persistido antes de crearla en Drive
 * ({@code carpeta_drive}); una reserva concurrente perdedora usa el ID ganador. Las claves usan identificadores internos
 * (UUID del pedido y del usuario, código del rol), no nombres: un cambio de nombre no crea otra carpeta. País, pedido,
 * rol y autor salen de datos del servidor: el pedido y el evento de subida validado (P-06), nunca del navegador.
 * No comparte carpetas ni genera enlaces públicos. Una copia que ya había empezado en la organización anterior
 * ({@code Mi unidad/{país}/{número}}) termina allí: no se mueve ni se borra nada.
 */
public final class CopiarPendientesCasoDeUso implements ReintentarCopiasPendientes {
    private static final int BLOQUE=4*1024*1024;
    private static final String CARPETA="application/vnd.google-apps.folder";
    /** Carpeta de pedido de la organización anterior (paso 3.4); solo se lee para terminar copias ya empezadas. */
    private static final String CLAVE_PEDIDO_ANTERIOR="pedido:";
    private static final String CLAVE_RAIZ="d";
    private static final String NOMBRE_RAIZ="DECOKASA";
    private final boolean habilitado;
    private final int lote;
    private final CopiaAdjuntoRepositorio copias;
    private final CarpetaDriveRepositorio carpetas;
    private final PedidosParaCopias pedidos;
    private final AlmacenArchivos drive;
    private final AlmacenLocal local;
    private final BloqueoCopiaLocal bloqueos;
    private final ProtectorSesionDrive protector;
    private final Reloj reloj;
    private final PoliticaCopia politica;
    /** Nulos solo con el constructor de compatibilidad: organización anterior {@code Mi unidad/{país}/{número}}. */
    private final AutoresDeCopias autores;
    private final ResumirUsuarios usuarios;

    /**
     * Solo para compatibilidad con las pruebas anteriores al 2026-10-24: copia con la organización anterior. La
     * configuración de la aplicación usa el constructor con {@link AutoresDeCopias}.
     */
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,null,null,false);
    }
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,
                Objects.requireNonNull(autores,"autores"),Objects.requireNonNull(usuarios,"usuarios"),true);
    }
    private CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios,boolean organizacionPorAutor) {
        if (lote<1 || lote>100) { throw new IllegalArgumentException("Lote inválido"); }
        this.habilitado=habilitado;this.lote=lote;this.copias=copias;this.carpetas=carpetas;this.pedidos=pedidos;
        this.drive=drive;this.local=local;this.bloqueos=bloqueos;this.protector=protector;this.reloj=reloj;this.politica=politica;
        this.autores=organizacionPorAutor ? autores : null;this.usuarios=organizacionPorAutor ? usuarios : null;
    }
    @Override public int ejecutar() {
        if (!habilitado) { return 0; }
        int reclamadas=0;
        for (int i=0;i<lote && !Thread.currentThread().isInterrupted();i++) {
            UUID token=UUID.randomUUID();Instant ahora=reloj.ahora();
            List<CopiaAdjuntoRepositorio.Pendiente> filas=copias.reclamar(1,ahora,ahora.plus(politica.lease()),token);
            if (filas.isEmpty()) { break; }
            reclamadas++;
            var trabajo=new Ejecucion(filas.getFirst(),token);
            try {
                Optional<BloqueoCopiaLocal.Bloqueo> cerrojo=bloqueos.intentar(trabajo.p.id());
                if (cerrojo.isEmpty()) { copias.ceder(trabajo.p.id(),token,reloj.ahora());continue; }
                try (var bloqueo=cerrojo.orElseThrow()) { trabajo.procesar(); }
            } catch (FalloIntegracionGoogle error) {
                // No hacer red fuera del cerrojo. Si ya confirmó, la actualización condicional no modifica COPIADO.
                trabajo.registrar(error);
            }
        }
        return reclamadas;
    }
    private final class Ejecucion {
        private final CopiaAdjuntoRepositorio.Pendiente p;
        private final UUID token;
        private String id;
        private String carpeta;
        private String md5;
        /** Autor y rol de intervención del evento ARCHIVO_SUBIDO; nulo solo con el constructor de compatibilidad. */
        private AutoresDeCopias.Autor autor;
        private Instant proximoLatido=Instant.MIN;
        private Ejecucion(CopiaAdjuntoRepositorio.Pendiente p,UUID token) {
            this.p=p;this.token=token;this.id=p.driveIdReservado();this.carpeta=p.carpetaId();
        }
        private void renovar() {
            Instant ahora=reloj.ahora();
            if (!copias.renovar(p.id(),token,ahora,ahora.plus(politica.lease()))) { throw fallo(Causa.LEASE_PERDIDO); }
            proximoLatido=ahora.plus(politica.lease().dividedBy(3));
        }
        private void latido() { if (!reloj.ahora().isBefore(proximoLatido)) { renovar(); } }
        private void procesar() {
            try { copiar(); }
            catch (FalloIntegracionGoogle error) { gestionarFallo(error); }
            catch (RuntimeException error) {
                // No guardar mensaje/causa: puede contener SQL, URI privada o rutas de infraestructura.
                gestionarFallo(fallo(Causa.TRANSITORIA));
            }
        }
        private void gestionarFallo(FalloIntegracionGoogle error) {
            if (error.causa()==Causa.LEASE_PERDIDO) { return; }
            // También en el cuarto intento y ante un commit incierto: comprobar antes de darlo por fallido.
            if (id!=null && carpeta!=null && md5!=null && reintentable(error.causa())) {
                try { if (reconciliar()) { return; } }
                catch (FalloIntegracionGoogle conciliacion) {
                    if (conciliacion.causa()==Causa.LEASE_PERDIDO) { return; }
                    if (!reintentable(conciliacion.causa())) { registrar(conciliacion);return; }
                    if (conciliacion.esperaMinima().compareTo(error.esperaMinima())>0) {
                        error=new FalloIntegracionGoogle(error.causa(),conciliacion.esperaMinima());
                    }
                }
            }
            registrar(error);
        }
        private void copiar() {
            if (p.tamano()<1 || p.tamano()>104857600L) { throw fallo(Causa.CONFIGURACION); }
            // Antes de cualquier operación Drive (reservar el ID del archivo, leer la raíz, crear carpetas): sin un evento
            // ARCHIVO_SUBIDO coherente no hay rol ni persona validados y la copia no empieza (F3-ADR25-01).
            if (autores!=null) { autor=autores.autor(p.id()).orElseThrow(() -> fallo(Causa.CONFIGURACION)); }
            renovar();verificarLocal();
            if (id==null) {
                renovar();String reservado=drive.reservarId();
                exigir(copias.reservarId(p.id(),token,reservado,reloj.ahora()));id=reservado;
            }
            String destino=resolverCarpetas();
            if (carpeta!=null && !carpeta.equals(destino)) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            carpeta=destino;
            Optional<AlmacenArchivos.Remoto> existente=consultarId();
            if (existente.isPresent()) {
                verificarIdentidad(existente.orElseThrow());
                if (completo(existente.orElseThrow())) { confirmar(existente.orElseThrow());return; }
            }
            AlmacenArchivos.Sesion sesion=p.sesionCifrada()==null ? null : protector.abrir(p.id(),p.sesionCifrada());
            long offset=0;
            if (sesion!=null) {
                try {
                    renovar();var progreso=drive.consultar(sesion,p.tamano());offset=progreso.siguienteByte();
                    if (progreso.completo().isPresent()) { confirmarTrasConsulta();return; }
                    exigir(copias.avanzar(p.id(),token,offset,reloj.ahora()));
                } catch (FalloIntegracionGoogle error) {
                    if (error.causa()!=Causa.SESION_CADUCADA) { throw error; }
                    if (reconciliar()) { return; }
                    existente=consultarId();
                    if (existente.isPresent()) { verificarIdentidad(existente.orElseThrow()); }
                    exigir(copias.sesion(p.id(),token,carpeta,null,0,reloj.ahora()));sesion=null;
                }
            }
            if (sesion==null) {
                renovar();AlmacenArchivos.Copia comando=new AlmacenArchivos.Copia(id,p.id(),p.pedidoId(),p.entradaId(),
                        p.nombre(),p.tipoDetectado(),p.tamano(),carpeta);
                // Si existe un blob incompleto con el mismo ID, reinicia su contenido, no otro archivo.
                sesion=existente.isPresent() ? drive.reiniciar(comando) : drive.iniciar(comando);
                exigir(copias.sesion(p.id(),token,carpeta,protector.cifrar(p.id(),sesion),0,reloj.ahora()));offset=0;
            }
            for (int peticiones=0;offset<p.tamano();peticiones++) {
                if (peticiones>=64) { throw fallo(Causa.TRANSITORIA); }
                byte[] bytes=leer(offset,(int)Math.min(BLOQUE,p.tamano()-offset));
                renovar();var progreso=drive.enviarFragmento(sesion,offset,p.tamano(),bytes);
                if (progreso.siguienteByte()<=offset || progreso.siguienteByte()>offset+bytes.length) {
                    throw fallo(Causa.RESPUESTA_INVALIDA);
                }
                offset=progreso.siguienteByte();
                exigir(copias.avanzar(p.id(),token,offset,reloj.ahora()));
                if (progreso.completo().isPresent()) { break; }
            }
            confirmarTrasConsulta();
        }
        private void verificarLocal() {
            try (var entrada=local.abrir(p.claveLocal())) {
                MessageDigest sha=MessageDigest.getInstance("SHA-256"),md=MessageDigest.getInstance("MD5");
                byte[] buffer=new byte[65536];long total=0;int n;
                while ((n=entrada.read(buffer))!=-1) {
                    total+=n;if (total>p.tamano()) { throw fallo(Causa.ALMACEN_LOCAL); }
                    sha.update(buffer,0,n);md.update(buffer,0,n);latido();
                }
                if (total!=p.tamano() || !HexFormat.of().formatHex(sha.digest()).equals(p.sha256())) {
                    throw fallo(Causa.ALMACEN_LOCAL);
                }
                md5=HexFormat.of().formatHex(md.digest());
            } catch (IOException | UncheckedIOException error) { throw fallo(Causa.ALMACEN_LOCAL); }
            catch (NoSuchAlgorithmException error) { throw fallo(Causa.CONFIGURACION); }
        }
        private byte[] leer(long inicio,int cantidad) {
            try (var entrada=local.abrir(p.claveLocal())) {
                entrada.skipNBytes(inicio);byte[] datos=entrada.readNBytes(cantidad);
                if (datos.length!=cantidad) { throw fallo(Causa.ALMACEN_LOCAL); } return datos;
            } catch (IOException | UncheckedIOException error) { throw fallo(Causa.ALMACEN_LOCAL); }
        }
        private String resolverCarpetas() {
            var destino=pedidos.destino(p.pedidoId());
            renovar();var raiz=drive.metadatos("root").orElseThrow(() -> fallo(Causa.CONFIGURACION));
            if (!CARPETA.equals(raiz.tipo()) || raiz.enPapelera() || !raiz.propietario() || !raiz.unidadCompartida().isBlank()) {
                throw fallo(Causa.CONFIGURACION);
            }
            // Organización anterior: con el constructor de compatibilidad o para terminar, sin moverla, una copia que
            // ya había empezado en la carpeta de pedido anterior confirmada. Se comprueba en Drive como antes.
            if (autores==null || enCarpetaAnterior()) {
                String pais=asegurarCarpeta("pais:"+destino.pais().name(),destino.pais().name(),raiz.id());
                return asegurarCarpeta(CLAVE_PEDIDO_ANTERIOR+p.pedidoId(),destino.numero(),pais);
            }
            // El autor ya se validó al empezar la copia (copiar), antes de cualquier operación Drive.
            var autor=this.autor;
            if (autor==null) { throw fallo(Causa.CONFIGURACION); }
            String rol=autor.rol().valor();
            String app=asegurarCarpeta(CLAVE_RAIZ,NOMBRE_RAIZ,raiz.id());
            String pais=asegurarCarpeta(CLAVE_RAIZ+"/pais/"+destino.pais().name(),destino.pais().name(),app);
            String clavePedido=CLAVE_RAIZ+"/p/"+compacto(p.pedidoId());
            String pedido=asegurarCarpeta(clavePedido,destino.numero(),pais);
            String carpetaRol=asegurarCarpeta(clavePedido+"/"+rol,rol,pedido);
            // El nombre visible se fija en la primera reserva; después manda la clave (UUID), no el nombre actual.
            return asegurarCarpeta(clavePedido+"/"+rol+"/"+compacto(autor.usuarioId()),
                    () -> nombrePersona(autor.usuarioId()),carpetaRol);
        }
        private boolean enCarpetaAnterior() {
            if (carpeta==null) { return false; }
            var anterior=carpetas.buscar(CLAVE_PEDIDO_ANTERIOR+p.pedidoId());
            return anterior.isPresent() && anterior.orElseThrow().confirmada() && carpeta.equals(anterior.orElseThrow().driveId());
        }
        private String asegurarCarpeta(String clave,String nombre,String padre) {
            return asegurarCarpeta(clave,null,nombre,padre);
        }
        private String asegurarCarpeta(String clave,Supplier<String> nombreInicial,String padre) {
            return asegurarCarpeta(clave,nombreInicial,null,padre);
        }
        /** Con {@code nombreInicial}, el nombre lo fija la reserva (gane quien gane); si no, debe ser {@code nombreFijo}. */
        private String asegurarCarpeta(String clave,Supplier<String> nombreInicial,String nombreFijo,String padre) {
            var reserva=carpetas.buscar(clave);
            if (reserva.isEmpty()) {
                String propuesto=nombreInicial==null ? nombreFijo : nombreInicial.get();
                renovar();String nuevo=drive.reservarId();
                reserva=Optional.of(carpetas.reservar(clave,propuesto,padre,nuevo,reloj.ahora()));
            }
            var c=reserva.orElseThrow();
            String nombre=nombreInicial==null ? nombreFijo : c.nombre();
            if (!c.nombre().equals(nombre) || !c.padreId().equals(padre)) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            renovar();var remoto=drive.metadatos(c.driveId());
            if (remoto.isEmpty()) {
                if (c.confirmada()) { throw fallo(Causa.METADATOS_DIVERGENTES); }
                try { renovar();remoto=Optional.of(drive.crearCarpeta(c.driveId(),nombre,padre,clave)); }
                catch (FalloIntegracionGoogle error) {
                    if (!reintentable(error.causa())) { throw error; }
                    renovar();remoto=drive.metadatos(c.driveId());
                    if (remoto.isEmpty()) { throw error; }
                }
            }
            var r=remoto.orElseThrow();
            if (!r.id().equals(c.driveId()) || !CARPETA.equals(r.tipo()) || r.enPapelera() || !r.propietario()
                    || !r.unidadCompartida().isBlank() || !r.padres().equals(List.of(padre))
                    || !clave.equals(r.claveCarpeta()) || !nombre.equals(r.nombre())) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            carpetas.confirmar(clave,c.driveId());return c.driveId();
        }
        /**
         * Nombre visible de la carpeta de la persona: su nombre actual al crearla y los 8 últimos dígitos de su UUID, para
         * distinguir nombres iguales a la vista. La identidad la da la clave, no este nombre.
         */
        private String nombrePersona(UUID usuarioId) {
            var resumen=usuarios.resumenes(List.of(usuarioId)).get(usuarioId);
            String base=resumen==null ? "" : resumen.nombre().codePoints().filter(c -> !Character.isISOControl(c))
                    .collect(StringBuilder::new,StringBuilder::appendCodePoint,StringBuilder::append).toString().strip();
            if (base.isEmpty()) { base="Usuario"; }
            if (base.codePointCount(0,base.length())>200) { base=base.substring(0,base.offsetByCodePoints(0,200)).strip(); }
            String id=usuarioId.toString();
            return base+" · "+id.substring(id.length()-8);
        }
        private Optional<AlmacenArchivos.Remoto> consultarId() { renovar();return drive.metadatos(id); }
        private boolean reconciliar() {
            var remoto=consultarId();
            if (remoto.isEmpty()) { return false; }
            verificarIdentidad(remoto.orElseThrow());
            if (!completo(remoto.orElseThrow())) { return false; }
            confirmar(remoto.orElseThrow());return true;
        }
        private void verificarIdentidad(AlmacenArchivos.Remoto r) {
            if (!id.equals(r.id()) || !p.id().toString().equals(r.adjuntoId()) || !p.pedidoId().toString().equals(r.pedidoId())
                    || !p.entradaId().toString().equals(r.entradaId()) || !r.padres().equals(List.of(carpeta))
                    || r.enPapelera() || !r.propietario() || !r.unidadCompartida().isBlank()
                    || r.tipo().startsWith("application/vnd.google-apps.")) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            if (r.tamano()>0 && r.tamano()!=p.tamano()) { throw fallo(Causa.METADATOS_DIVERGENTES); }
        }
        private boolean completo(AlmacenArchivos.Remoto r) {
            if (r.tamano()!=p.tamano()) { return false; }
            if (!r.sha256().isBlank()) {
                if (!r.sha256().equalsIgnoreCase(p.sha256())) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            } else if (!r.md5().isBlank()) {
                if (!r.md5().equalsIgnoreCase(md5)) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            } else { throw fallo(Causa.RESPUESTA_INVALIDA); }
            if (r.enlace()==null || !r.enlace().matches("https://drive\\.google\\.com/[^\\s]{1,2000}")) {
                throw fallo(Causa.RESPUESTA_INVALIDA);
            }
            return true;
        }
        private void confirmarTrasConsulta() { if (!reconciliar()) { throw fallo(Causa.RESULTADO_INCIERTO); } }
        private void confirmar(AlmacenArchivos.Remoto r) {
            renovar();exigir(copias.confirmar(p.id(),token,id,r.enlace(),reloj.ahora()));
        }
        private void registrar(FalloIntegracionGoogle error) {
            if (error.causa()==Causa.LEASE_PERDIDO) { return; }
            boolean terminal=p.intentos()>=4 || !reintentable(error.causa());
            Instant siguiente=null;
            if (!terminal) {
                Duration espera=politica.esperaDespuesDe(p.intentos());
                if (error.esperaMinima().compareTo(espera)>0) { espera=error.esperaMinima(); }
                siguiente=reloj.ahora().plus(espera);
            }
            copias.registrarFallo(p.id(),token,error.causa(),siguiente,terminal,reloj.ahora());
        }
    }
    private static boolean reintentable(Causa causa) {
        return switch(causa) {
            case CUOTA,TRANSITORIA,RESULTADO_INCIERTO,RESPUESTA_INVALIDA,CONFLICTO_REMOTO,SESION_CADUCADA -> true;
            default -> false;
        };
    }
    private static FalloIntegracionGoogle fallo(Causa causa) { return new FalloIntegracionGoogle(causa); }
    /** UUID en base64url sin relleno (22 caracteres): la clave más larga queda en 90 de los 100 admitidos. */
    private static String compacto(UUID id) {
        ByteBuffer bytes=ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }
    private static void exigir(boolean vigente) { if (!vigente) { throw fallo(Causa.LEASE_PERDIDO); } }
}
