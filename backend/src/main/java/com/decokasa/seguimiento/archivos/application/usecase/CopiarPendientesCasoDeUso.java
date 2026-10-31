package com.decokasa.seguimiento.archivos.application.usecase;

import static com.decokasa.seguimiento.archivos.application.usecase.CarpetasDrive.compacto;
import static com.decokasa.seguimiento.archivos.application.usecase.CarpetasDrive.fallo;
import static com.decokasa.seguimiento.archivos.application.usecase.CarpetasDrive.reintentable;

import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AutoresDeCopias;
import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.FechasDeSubida;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Núcleo sin Spring/JPA/SDK. Los repositorios inyectados cierran su transacción antes de retornar.
 *
 * <p>Organización vigente (ADR-0027, aprobada por Richard el 2026-10-26):
 * {@code Mi unidad/DECOKASA/{número del pedido}/{usuario-id8}/{AAAA-MM-DD}/}. El autor es el autor real e histórico de
 * la subida (evento ARCHIVO_SUBIDO validado, P-06), identificado por su UUID dentro del pedido; el día es
 * {@code adjunto.subido_en} en la zona del país del pedido, no la creación del pedido, la copia ni el reintento. Pedido,
 * autor y día se crean con la primera copia que los necesita, con un ID reservado y persistido antes de crearlos en Drive
 * ({@code carpeta_drive}, {@link CarpetasDrive}). Cada adjunto se copia una sola vez. Pedido, fecha y autor salen de
 * datos del servidor, nunca del navegador. No comparte carpetas ni genera enlaces públicos.
 *
 * <p>Compatibilidad: una copia que ya había empezado en una carpeta confirmada de una organización anterior
 * ({@code Mi unidad/{país}/{número}} del paso 3.4, {@code DECOKASA/{país}/{pedido}/{rol}/{persona}} de ADR-0025 o
 * {@code DECOKASA/Usuarios/…} de ADR-0026) termina allí; no se mueve ni se borra nada. Los constructores anteriores
 * conservan esas organizaciones para sus pruebas; la configuración de la aplicación usa el de ADR-0027.
 */
public final class CopiarPendientesCasoDeUso implements ReintentarCopiasPendientes {
    private static final int BLOQUE=4*1024*1024;
    /** Carpeta de pedido de la organización del paso 3.4; solo se lee para terminar copias ya empezadas. */
    private static final String CLAVE_PEDIDO_ANTERIOR="pedido:";
    private enum Organizacion { PAIS_PEDIDO, ROL_PERSONA, USUARIO, PEDIDO_AUTOR_DIA }
    private final boolean habilitado;
    private final int lote;
    private final CopiaAdjuntoRepositorio copias;
    private final PedidosParaCopias pedidos;
    private final AlmacenArchivos drive;
    private final AlmacenLocal local;
    private final BloqueoCopiaLocal bloqueos;
    private final ProtectorSesionDrive protector;
    private final Reloj reloj;
    private final PoliticaCopia politica;
    private final CarpetasDrive carpetasDrive;
    private final Organizacion organizacion;
    /** Nulos solo con el constructor del paso 3.4: organización {@code Mi unidad/{país}/{número}}. */
    private final AutoresDeCopias autores;
    private final ResumirUsuarios usuarios;
    /** Nulo salvo con las organizaciones de ADR-0026 y ADR-0027. */
    private final IdentificarUsuarios identidades;
    /** Nulo salvo con la organización de ADR-0027: fecha histórica de subida de cada adjunto. */
    private final FechasDeSubida fechas;

    /** Solo para las pruebas del paso 3.4: organización {@code Mi unidad/{país}/{número}}. */
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,null,null,null,null,
                Organizacion.PAIS_PEDIDO);
    }
    /** Solo para las pruebas de ADR-0025: organización {@code DECOKASA/{país}/{pedido}/{rol}/{persona}}. */
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,
                Objects.requireNonNull(autores,"autores"),Objects.requireNonNull(usuarios,"usuarios"),null,null,
                Organizacion.ROL_PERSONA);
    }
    /** Organización de ADR-0026 (carpeta del autor por país, año, mes y pedido): pruebas anteriores al 2026-10-26. */
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios,IdentificarUsuarios identidades) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,
                Objects.requireNonNull(autores,"autores"),Objects.requireNonNull(usuarios,"usuarios"),
                Objects.requireNonNull(identidades,"identidades"),null,Organizacion.USUARIO);
    }
    /** Organización vigente de ADR-0027: DECOKASA/{pedido}/{autor}/{día de subida}. */
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios,IdentificarUsuarios identidades,FechasDeSubida fechas) {
        this(habilitado,lote,copias,carpetas,pedidos,drive,local,bloqueos,protector,reloj,politica,
                Objects.requireNonNull(autores,"autores"),Objects.requireNonNull(usuarios,"usuarios"),
                Objects.requireNonNull(identidades,"identidades"),Objects.requireNonNull(fechas,"fechas"),
                Organizacion.PEDIDO_AUTOR_DIA);
    }
    private CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica,
            AutoresDeCopias autores,ResumirUsuarios usuarios,IdentificarUsuarios identidades,FechasDeSubida fechas,
            Organizacion organizacion) {
        if (lote<1 || lote>100) { throw new IllegalArgumentException("Lote inválido"); }
        this.habilitado=habilitado;this.lote=lote;this.copias=copias;this.pedidos=pedidos;
        this.drive=drive;this.local=local;this.bloqueos=bloqueos;this.protector=protector;this.reloj=reloj;this.politica=politica;
        this.carpetasDrive=new CarpetasDrive(carpetas,drive,reloj);
        this.autores=autores;this.usuarios=usuarios;this.identidades=identidades;this.fechas=fechas;this.organizacion=organizacion;
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
        /** Autor y rol de intervención del evento ARCHIVO_SUBIDO; nulo solo con el constructor del paso 3.4. */
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
            // ARCHIVO_SUBIDO coherente no hay autor validado y la copia no empieza (F3-ADR25-01).
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
            Runnable latido=this::renovar;
            String raiz=carpetasDrive.miUnidad(latido);
            // Paso 3.4: con su constructor o para terminar, sin moverla, una copia que ya había empezado en su carpeta de
            // pedido confirmada. Se comprueba en Drive como antes.
            if (organizacion==Organizacion.PAIS_PEDIDO || carpetasDrive.confirmadaCon(CLAVE_PEDIDO_ANTERIOR+p.pedidoId(),carpeta)) {
                String pais=carpetasDrive.asegurar("pais:"+destino.pais().name(),null,destino.pais().name(),raiz,latido);
                return carpetasDrive.asegurar(CLAVE_PEDIDO_ANTERIOR+p.pedidoId(),null,destino.numero(),pais,latido);
            }
            // El autor ya se validó al empezar la copia (copiar), antes de cualquier operación Drive.
            var autor=this.autor;
            if (autor==null) { throw fallo(Causa.CONFIGURACION); }
            String rol=autor.rol().valor();
            String clavePedido25=CarpetasDrive.CLAVE_RAIZ+"/p/"+compacto(p.pedidoId());
            String clavePersona25=clavePedido25+"/"+rol+"/"+compacto(autor.usuarioId());
            // ADR-0025: con su constructor o para terminar una copia que ya había empezado en esa carpeta de persona.
            if (organizacion==Organizacion.ROL_PERSONA || carpetasDrive.confirmadaCon(clavePersona25,carpeta)) {
                String app=carpetasDrive.asegurar(CarpetasDrive.CLAVE_RAIZ,null,CarpetasDrive.NOMBRE_RAIZ,raiz,latido);
                String pais=carpetasDrive.asegurar(CarpetasDrive.CLAVE_RAIZ+"/pais/"+destino.pais().name(),null,
                        destino.pais().name(),app,latido);
                String pedido=carpetasDrive.asegurar(clavePedido25,null,destino.numero(),pais,latido);
                String carpetaRol=carpetasDrive.asegurar(clavePedido25+"/"+rol,null,rol,pedido,latido);
                // El nombre visible se fija en la primera reserva; después manda la clave (UUID), no el nombre actual.
                return carpetasDrive.asegurar(clavePersona25,() -> nombrePersona(autor.usuarioId()),null,carpetaRol,latido);
            }
            UUID autorId=autor.usuarioId();
            String claveUsuario=CarpetasDrive.claveUsuario(autorId);
            // ADR-0026: con su constructor o para terminar una copia que ya había empezado en esa carpeta de pedido.
            if (organizacion==Organizacion.USUARIO
                    || carpetasDrive.confirmadaCon(claveUsuario+"/p/"+compacto(p.pedidoId()),carpeta)) {
                return carpetaAdr26(raiz,destino,autorId,claveUsuario,latido);
            }
            // ADR-0027: DECOKASA/{pedido}/{autor}/{AAAA-MM-DD}. Día de la subida histórica, en la zona del país del
            // pedido (no la creación del pedido, la copia ni el reintento). LocalDate.toString usa dígitos ASCII.
            String dia=fechas.subidoEn(p.id()).orElseThrow(() -> fallo(Causa.CONFIGURACION))
                    .atZone(destino.pais().zona()).toLocalDate().toString();
            String pedido=carpetasDrive.carpetaPedido(raiz,p.pedidoId(),destino.numero(),latido);
            String carpetaAutor=carpetasDrive.carpetaAutor(pedido,p.pedidoId(),autorId,
                    () -> CarpetasDrive.nombreAutor(autorId,identidades.nombreDeUsuario(autorId).orElse(null)),latido);
            return carpetasDrive.carpetaDia(carpetaAutor,p.pedidoId(),autorId,dia,latido);
        }
        /** ADR-0026: Usuarios/{autor}/{país}/{AAAA}/{MM}/{AAAA-MM-DD}_{número}. Fecha de creación del pedido, en su país. */
        private String carpetaAdr26(String raiz,PedidosParaCopias.Destino destino,UUID autorId,String claveUsuario,
                Runnable latido) {
            if (destino.creadoEn()==null) { throw fallo(Causa.CONFIGURACION); }
            LocalDate fecha=destino.creadoEn().atZone(destino.pais().zona()).toLocalDate();
            String anio=String.format(Locale.ROOT,"%04d",fecha.getYear());
            String mes=String.format(Locale.ROOT,"%02d",fecha.getMonthValue());
            String usuario=carpetasDrive.carpetaUsuario(raiz,autorId,
                    () -> CarpetasDrive.nombreUsuario(autorId,identidades.nombreDeUsuario(autorId).orElse(null)),latido);
            String clavePais=claveUsuario+"/"+destino.pais().name();
            String pais=carpetasDrive.asegurar(clavePais,null,destino.pais().name(),usuario,latido);
            String carpetaAnio=carpetasDrive.asegurar(clavePais+"/"+anio,null,anio,pais,latido);
            String carpetaMes=carpetasDrive.asegurar(clavePais+"/"+anio+"/"+mes,null,mes,carpetaAnio,latido);
            return carpetasDrive.asegurar(claveUsuario+"/p/"+compacto(p.pedidoId()),null,fecha+"_"+destino.numero(),
                    carpetaMes,latido);
        }
        /**
         * Nombre visible de la carpeta de la persona de ADR-0025: su nombre actual al crearla y los 8 últimos dígitos de
         * su UUID, para distinguir nombres iguales a la vista. La identidad la da la clave, no este nombre.
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
            // ADR-0034: COPIADO ya es definitivo; liberar el almacenamiento temporal nunca cambia ese estado.
            try { local.liberarRespaldo(p.claveLocal()); } catch (RuntimeException ignorado) { }
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
    private static void exigir(boolean vigente) { if (!vigente) { throw fallo(Causa.LEASE_PERDIDO); } }
}
