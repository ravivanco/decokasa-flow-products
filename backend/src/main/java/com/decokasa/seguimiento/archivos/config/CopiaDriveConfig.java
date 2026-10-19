package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.BloqueoCopiasEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.google.ProtectorSesionDriveAes;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.application.usecase.CopiarPendientesCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Cada operación de persistencia abre/cierra su transacción antes del siguiente I/O remoto. */
@Configuration(proxyBeanMethods=false)
public class CopiaDriveConfig {
    @Bean ProtectorSesionDrive protectorSesionDrive(@Value("${decokasa.google.oauth.llave-ruta:}") String llave) {
        return new ProtectorSesionDriveAes(llave);
    }
    @Bean BloqueoCopiaLocal bloqueoCopiaLocal(@Value("${decokasa.archivos.directorio}") String directorio) {
        return new BloqueoCopiasEnDisco(Path.of(directorio));
    }
    @Bean ReintentarCopiasPendientes reintentarCopiasPendientes(PlatformTransactionManager gestor,
            CopiaAdjuntoRepositorio repositorio,CarpetaDriveRepositorio directorio,PedidosParaCopias pedidos,
            AlmacenArchivos drive,AlmacenLocal local,BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean oauth,
            @Value("${decokasa.google.drive.habilitada:false}") boolean driveHabilitado,
            @Value("${decokasa.google.drive.worker.lote:5}") int lote,
            @Value("${decokasa.google.drive.worker.lease:PT3M}") String lease,
            @Value("${decokasa.google.drive.worker.reintento-1:PT1M}") String espera1,
            @Value("${decokasa.google.drive.worker.reintento-2:PT5M}") String espera2,
            @Value("${decokasa.google.drive.worker.reintento-3:PT15M}") String espera3) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        CopiaAdjuntoRepositorio copias=new CopiaAdjuntoRepositorio() {
            @Override public List<Pendiente> reclamar(int limite,Instant ahora,Instant hasta,UUID propietario) {
                return tx.execute(s -> repositorio.reclamar(limite,ahora,hasta,propietario));
            }
            @Override public boolean reservarId(UUID id,UUID token,String remoto,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.reservarId(id,token,remoto,ahora)));
            }
            @Override public boolean confirmar(UUID id,UUID token,String remoto,String enlace,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.confirmar(id,token,remoto,enlace,ahora)));
            }
            @Override public boolean registrarFallo(UUID id,UUID token,Causa causa,Instant siguiente,boolean terminal,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.registrarFallo(id,token,causa,siguiente,terminal,ahora)));
            }
            @Override public boolean renovar(UUID id,UUID token,Instant ahora,Instant hasta) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.renovar(id,token,ahora,hasta)));
            }
            @Override public boolean sesion(UUID id,UUID token,String carpeta,byte[] cifrada,long offset,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.sesion(id,token,carpeta,cifrada,offset,ahora)));
            }
            @Override public boolean avanzar(UUID id,UUID token,long offset,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.avanzar(id,token,offset,ahora)));
            }
            @Override public boolean ceder(UUID id,UUID token,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.ceder(id,token,ahora)));
            }
        };
        CarpetaDriveRepositorio carpetas=new CarpetaDriveRepositorio() {
            @Override public Optional<Carpeta> buscar(String clave) { return tx.execute(s -> directorio.buscar(clave)); }
            @Override public Carpeta reservar(String clave,String nombre,String padre,String id,Instant ahora) {
                return tx.execute(s -> directorio.reservar(clave,nombre,padre,id,ahora));
            }
            @Override public void confirmar(String clave,String id) { tx.executeWithoutResult(s -> directorio.confirmar(clave,id)); }
        };
        PedidosParaCopias datos=id -> tx.execute(s -> pedidos.destino(id));
        var politica=new PoliticaCopia(Duration.parse(lease),List.of(Duration.parse(espera1),Duration.parse(espera2),Duration.parse(espera3)));
        var caso=new CopiarPendientesCasoDeUso(oauth && driveHabilitado,lote,copias,carpetas,datos,drive,local,bloqueos,protector,reloj,politica);
        return () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new FalloIntegracionGoogle(Causa.TRANSACCION_ACTIVA); }
            return caso.ejecutar();
        };
    }
}
