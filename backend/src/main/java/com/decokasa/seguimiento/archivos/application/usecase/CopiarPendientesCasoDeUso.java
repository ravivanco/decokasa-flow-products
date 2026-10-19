package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Núcleo sin Spring/JPA/SDK. Los repositorios inyectados cierran su transacción antes de retornar. */
public final class CopiarPendientesCasoDeUso implements ReintentarCopiasPendientes {
    private static final int BLOQUE=4*1024*1024;
    private static final String CARPETA="application/vnd.google-apps.folder";
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
    public CopiarPendientesCasoDeUso(boolean habilitado,int lote,CopiaAdjuntoRepositorio copias,
            CarpetaDriveRepositorio carpetas,PedidosParaCopias pedidos,AlmacenArchivos drive,AlmacenLocal local,
            BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,PoliticaCopia politica) {
        if (lote<1 || lote>100) { throw new IllegalArgumentException("Lote inválido"); }
        this.habilitado=habilitado;this.lote=lote;this.copias=copias;this.carpetas=carpetas;this.pedidos=pedidos;
        this.drive=drive;this.local=local;this.bloqueos=bloqueos;this.protector=protector;this.reloj=reloj;this.politica=politica;
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
            String pais=asegurarCarpeta("pais:"+destino.pais().name(),destino.pais().name(),raiz.id());
            return asegurarCarpeta("pedido:"+p.pedidoId(),destino.numero(),pais);
        }
        private String asegurarCarpeta(String clave,String nombre,String padre) {
            var reserva=carpetas.buscar(clave);
            if (reserva.isEmpty()) {
                renovar();String nuevo=drive.reservarId();
                reserva=Optional.of(carpetas.reservar(clave,nombre,padre,nuevo,reloj.ahora()));
            }
            var c=reserva.orElseThrow();
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
    private static void exigir(boolean vigente) { if (!vigente) { throw fallo(Causa.LEASE_PERDIDO); } }
}
