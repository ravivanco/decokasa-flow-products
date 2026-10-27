package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.BloqueoCopiasEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.google.ProtectorSesionDriveAes;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasPedidos;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasUsuarios;
import com.decokasa.seguimiento.archivos.application.port.in.ReintentarCopiasPendientes;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AutoresDeCopias;
import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaPedidoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaUsuarioRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CopiaAdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.FechasDeSubida;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.application.usecase.AprovisionarCarpetasPedidosCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.AprovisionarCarpetasUsuariosCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.CopiarPendientesCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.GestionarCarpetasPedidosCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.GestionarCarpetasUsuariosCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
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
            AutoresDeCopias autoresDeCopias,ResumirUsuarios usuarios,IdentificarUsuarios identificarUsuarios,
            FechasDeSubida fechasDeSubida,AlmacenArchivos drive,AlmacenLocal local,BloqueoCopiaLocal bloqueos,ProtectorSesionDrive protector,Reloj reloj,
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
        CarpetaDriveRepositorio carpetas=carpetasEnTransaccion(tx,directorio);
        PedidosParaCopias datos=id -> tx.execute(s -> pedidos.destino(id));
        AutoresDeCopias autores=id -> tx.execute(s -> autoresDeCopias.autor(id));
        FechasDeSubida fechas=id -> tx.execute(s -> fechasDeSubida.subidoEn(id));
        var politica=politica(lease,espera1,espera2,espera3);
        // ADR-0027: Mi unidad/DECOKASA/{pedido}/{usuario-id8}/{AAAA-MM-DD} (Richard, 2026-10-26). Las copias ya
        // empezadas en una organización anterior (paso 3.4, ADR-0025, ADR-0026) terminan allí.
        var caso=new CopiarPendientesCasoDeUso(oauth && driveHabilitado,lote,copias,carpetas,datos,drive,local,bloqueos,
                protector,reloj,politica,autores,usuarios,identificarUsuarios,fechas);
        return () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new FalloIntegracionGoogle(Causa.TRANSACCION_ACTIVA); }
            return caso.ejecutar();
        };
    }

    // --- Carpeta de Drive de cada pedido (ADR-0027) ---

    /** Proceso de las intenciones pendientes de pedidos: mismos flags, lote, lease y reintentos que la copia. */
    @Bean GestionarCarpetasPedidos.Procesar procesarCarpetasPedidos(PlatformTransactionManager gestor,
            CarpetaPedidoRepositorio repositorio,CarpetaDriveRepositorio directorio,AlmacenArchivos drive,
            PedidosParaCopias pedidos,Reloj reloj,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean oauth,
            @Value("${decokasa.google.drive.habilitada:false}") boolean driveHabilitado,
            @Value("${decokasa.google.drive.worker.lote:5}") int lote,
            @Value("${decokasa.google.drive.worker.lease:PT3M}") String lease,
            @Value("${decokasa.google.drive.worker.reintento-1:PT1M}") String espera1,
            @Value("${decokasa.google.drive.worker.reintento-2:PT5M}") String espera2,
            @Value("${decokasa.google.drive.worker.reintento-3:PT15M}") String espera3) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        CarpetaPedidoRepositorio intenciones=new CarpetaPedidoRepositorio() {
            @Override public void programar(UUID id,Origen origen,boolean reabrir,Instant ahora) {
                tx.executeWithoutResult(s -> repositorio.programar(id,origen,reabrir,ahora));
            }
            @Override public List<Pendiente> reclamar(int limite,Instant ahora,Instant hasta,UUID propietario) {
                return tx.execute(s -> repositorio.reclamar(limite,ahora,hasta,propietario));
            }
            @Override public boolean renovar(UUID id,UUID token,Instant ahora,Instant hasta) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.renovar(id,token,ahora,hasta)));
            }
            @Override public boolean confirmar(UUID id,UUID token,String driveId,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.confirmar(id,token,driveId,ahora)));
            }
            @Override public boolean registrarFallo(UUID id,UUID token,Causa causa,Instant siguiente,boolean terminal,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.registrarFallo(id,token,causa,siguiente,terminal,ahora)));
            }
            @Override public Optional<Estado> estado(UUID id) { return tx.execute(s -> repositorio.estado(id)); }
        };
        PedidosParaCopias datos=id -> tx.execute(s -> pedidos.destino(id));
        var caso=new AprovisionarCarpetasPedidosCasoDeUso(oauth && driveHabilitado,lote,intenciones,
                carpetasEnTransaccion(tx,directorio),drive,datos,reloj,politica(lease,espera1,espera2,espera3));
        return () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new FalloIntegracionGoogle(Causa.TRANSACCION_ACTIVA); }
            return caso.ejecutar();
        };
    }

    /** Estado y programación para el admin con el país del pedido; cada llamada en una transacción corta, sin Google. */
    @Bean GestionarCarpetasPedidos gestionarCarpetasPedidos(PlatformTransactionManager gestor,
            CarpetaPedidoRepositorio repositorio,ConsultarResponsables responsables,Reloj reloj) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        var caso=new GestionarCarpetasPedidosCasoDeUso(repositorio,responsables,reloj);
        return new GestionarCarpetasPedidos() {
            @Override public Estado estado(Solicitante s,UUID id) { return tx.execute(e -> caso.estado(s,id)); }
            @Override public Estado programar(Solicitante s,UUID id) { return tx.execute(e -> caso.programar(s,id)); }
        };
    }

    /** Alta del pedido: se une a la transacción del alta (el repositorio exige una activa). */
    @Bean GestionarCarpetasPedidos.ProgramarAlta programarAltaCarpetaPedido(CarpetaPedidoRepositorio repositorio,
            ConsultarResponsables responsables,Reloj reloj) {
        var caso=new GestionarCarpetasPedidosCasoDeUso(repositorio,responsables,reloj);
        return caso::programarAlta;
    }

    // --- Carpeta global de cada usuario (ADR-0026): solo historial desde ADR-0027 ---

    /** Proceso de las intenciones pendientes: mismos flags, lote, lease y reintentos que la copia. */
    @Bean GestionarCarpetasUsuarios.Procesar procesarCarpetasUsuarios(PlatformTransactionManager gestor,
            CarpetaUsuarioRepositorio repositorio,CarpetaDriveRepositorio directorio,AlmacenArchivos drive,
            IdentificarUsuarios identificarUsuarios,Reloj reloj,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean oauth,
            @Value("${decokasa.google.drive.habilitada:false}") boolean driveHabilitado,
            @Value("${decokasa.google.drive.worker.lote:5}") int lote,
            @Value("${decokasa.google.drive.worker.lease:PT3M}") String lease,
            @Value("${decokasa.google.drive.worker.reintento-1:PT1M}") String espera1,
            @Value("${decokasa.google.drive.worker.reintento-2:PT5M}") String espera2,
            @Value("${decokasa.google.drive.worker.reintento-3:PT15M}") String espera3) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        CarpetaUsuarioRepositorio intenciones=new CarpetaUsuarioRepositorio() {
            @Override public void programar(UUID id,Origen origen,boolean reabrir,Instant ahora) {
                tx.executeWithoutResult(s -> repositorio.programar(id,origen,reabrir,ahora));
            }
            @Override public List<Pendiente> reclamar(int limite,Instant ahora,Instant hasta,UUID propietario) {
                return tx.execute(s -> repositorio.reclamar(limite,ahora,hasta,propietario));
            }
            @Override public boolean renovar(UUID id,UUID token,Instant ahora,Instant hasta) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.renovar(id,token,ahora,hasta)));
            }
            @Override public boolean confirmar(UUID id,UUID token,String driveId,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.confirmar(id,token,driveId,ahora)));
            }
            @Override public boolean registrarFallo(UUID id,UUID token,Causa causa,Instant siguiente,boolean terminal,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(s -> repositorio.registrarFallo(id,token,causa,siguiente,terminal,ahora)));
            }
            @Override public Optional<Estado> estado(UUID id) { return tx.execute(s -> repositorio.estado(id)); }
            @Override public List<Estado> estados() { return tx.execute(s -> repositorio.estados()); }
        };
        var caso=new AprovisionarCarpetasUsuariosCasoDeUso(oauth && driveHabilitado,lote,intenciones,
                carpetasEnTransaccion(tx,directorio),drive,identificarUsuarios,reloj,politica(lease,espera1,espera2,espera3));
        return () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) { throw new FalloIntegracionGoogle(Causa.TRANSACCION_ACTIVA); }
            return caso.ejecutar();
        };
    }

    /** Estado y programación para el admin; cada llamada en una transacción corta, sin Google. */
    @Bean GestionarCarpetasUsuarios gestionarCarpetasUsuarios(PlatformTransactionManager gestor,
            CarpetaUsuarioRepositorio repositorio,ConsultarResponsables responsables,Reloj reloj) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        var caso=new GestionarCarpetasUsuariosCasoDeUso(repositorio,responsables,reloj);
        return new GestionarCarpetasUsuarios() {
            @Override public List<Estado> listar(Solicitante s) { return tx.execute(e -> caso.listar(s)); }
            @Override public Estado estado(Solicitante s,UUID id) { return tx.execute(e -> caso.estado(s,id)); }
            @Override public Estado programar(Solicitante s,UUID id) { return tx.execute(e -> caso.programar(s,id)); }
        };
    }

    private static CarpetaDriveRepositorio carpetasEnTransaccion(TransactionTemplate tx,CarpetaDriveRepositorio directorio) {
        return new CarpetaDriveRepositorio() {
            @Override public Optional<Carpeta> buscar(String clave) { return tx.execute(s -> directorio.buscar(clave)); }
            @Override public Carpeta reservar(String clave,String nombre,String padre,String id,Instant ahora) {
                return tx.execute(s -> directorio.reservar(clave,nombre,padre,id,ahora));
            }
            @Override public void confirmar(String clave,String id) { tx.executeWithoutResult(s -> directorio.confirmar(clave,id)); }
        };
    }

    private static PoliticaCopia politica(String lease,String espera1,String espera2,String espera3) {
        return new PoliticaCopia(Duration.parse(lease),List.of(Duration.parse(espera1),Duration.parse(espera2),Duration.parse(espera3)));
    }
}
