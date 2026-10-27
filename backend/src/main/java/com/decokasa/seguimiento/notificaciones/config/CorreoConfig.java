package com.decokasa.seguimiento.notificaciones.config;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.notificaciones.adapter.out.persistence.NotificacionSqlRepositorio;
import com.decokasa.seguimiento.notificaciones.application.port.in.ConsultarNotificaciones;
import com.decokasa.seguimiento.notificaciones.application.port.in.EnviarNotificacionesPendientes;
import com.decokasa.seguimiento.notificaciones.application.port.out.*;
import com.decokasa.seguimiento.notificaciones.application.usecase.ConsultarNotificacionesCasoDeUso;
import com.decokasa.seguimiento.notificaciones.application.usecase.EnviarNotificacionesCasoDeUso;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarResponsables;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration(proxyBeanMethods=false)
public class CorreoConfig {
    @Bean ConsultarNotificaciones consultarNotificaciones(PlatformTransactionManager gestor,
            RegistroNotificaciones registro, ConsultarResponsables usuarios) {
        var tx=new TransactionTemplate(gestor); tx.setReadOnly(true);
        var caso=new ConsultarNotificacionesCasoDeUso(registro,usuarios);
        return (s,pais,e,p,t) -> tx.execute(estado -> caso.consultar(s,pais,e,p,t));
    }
    @Bean EnviarNotificacionesPendientes enviarNotificacionesPendientes(PlatformTransactionManager gestor,
            NotificacionSqlRepositorio repo, Notificador gmail, DirectorioDestinatarios directorio, Reloj reloj,
            @Value("${decokasa.google.oauth.habilitada:false}") boolean oauth,
            @Value("${decokasa.google.gmail.habilitada:false}") boolean habilitada,
            @Value("${decokasa.google.gmail.worker.lote:5}") int lote,
            @Value("${decokasa.google.gmail.worker.lease:PT3M}") Duration lease,
            @Value("${decokasa.notificaciones.enviar-desde:}") String enviarDesde) {
        // ADR-0032: sin instante de corte no se envía nada; con él, solo lo creado desde entonces (nunca el histórico).
        Instant desde = enviarDesde == null || enviarDesde.isBlank() ? null : Instant.parse(enviarDesde.strip());
        var tx=new TransactionTemplate(gestor);
        TrabajoCorreo trabajo=new TrabajoCorreo() {
            public Optional<Notificacion> reclamar(Instant a,Instant h,UUID p) {
                return tx.execute(e -> repo.reclamar(1,a,h,p,desde).stream().findFirst());
            }
            public boolean vigente(UUID id,UUID p,Instant a) { return Boolean.TRUE.equals(tx.execute(e -> repo.vigente(id,p,a))); }
            public boolean enviada(UUID id,UUID p,String proveedor,Instant a) {
                return Boolean.TRUE.equals(tx.execute(e -> repo.marcarEnviada(id,p,proveedor,a)));
            }
            public boolean fallo(UUID id,UUID p,FalloIntegracionGoogle.Causa causa,Instant a) {
                // Permanent/uncertain/exhausted results stop; no automatic reset.
                return Boolean.TRUE.equals(tx.execute(e -> repo.registrarFallo(id,p,causa,null,true,a)));
            }
            @Override public boolean reintentar(UUID id,UUID p,FalloIntegracionGoogle.Causa causa,
                    Instant siguiente,Instant ahora) {
                return Boolean.TRUE.equals(tx.execute(e -> repo.registrarFallo(id,p,causa,siguiente,false,ahora)));
            }
        };
        var caso=new EnviarNotificacionesCasoDeUso(trabajo,gmail,directorio,reloj,oauth && habilitada && desde != null,
                lote,lease);
        return () -> {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                throw new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.TRANSACCION_ACTIVA);
            }
            return caso.procesar();
        };
    }
}
