package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenCargasEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.SesionCargaDriveSql;
import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarga;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenCargas;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CuotaAlmacenamiento;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.application.usecase.CargaADriveCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.GestionarCargaCasoDeUso;
import com.decokasa.seguimiento.archivos.application.port.out.VerificadorFormato;
import com.decokasa.seguimiento.archivos.application.usecase.SubirArchivoCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Transacciones fuera del núcleo. Red/bytes grandes fuera de transacción; metadatos en transacciones cortas.
 *
 * <p>ADR-0034: con {@code ARCHIVOS_DESTINO=drive} el almacén de cargas es Drive ({@link CargaADriveCasoDeUso}): cada
 * fragmento va a la sesión reanudable de Drive y el adjunto se registra solo con el archivo íntegro confirmado en Drive.
 * Con {@code local} se conserva el flujo de la fase 3 (parciales en disco y copia diferida).
 */
@Configuration(proxyBeanMethods=false)
public class CargasConfig {
    @Bean AlmacenCargas almacenCargas(@Value("${decokasa.archivos.directorio}") String directorio,
            @Value("${decokasa.archivos.destino:local}") String destino, PlatformTransactionManager gestor,
            DataSource fuente, CargaRepositorio cargas, AlmacenArchivos drive, CarpetaDriveRepositorio carpetas,
            PedidosParaCopias pedidos, IdentificarUsuarios identidades, ProtectorSesionDrive protector, Reloj reloj) {
        if (!ArchivosConfig.destinoDrive(destino)) { return new AlmacenCargasEnDisco(Path.of(directorio)); }
        TransactionTemplate tx=new TransactionTemplate(gestor);
        return new CargaADriveCasoDeUso(enTransaccion(CargaRepositorio.class,cargas,tx),
                new SesionCargaDriveSql(fuente,gestor),drive,enTransaccion(CarpetaDriveRepositorio.class,carpetas,tx),
                enTransaccion(PedidosParaCopias.class,pedidos,tx),enTransaccion(IdentificarUsuarios.class,identidades,tx),
                protector,reloj);
    }
    @Bean GestionarCarga gestionarCarga(PlatformTransactionManager gestor, CargaRepositorio cargas,
            PedidosParaArchivos pedidos, AlmacenLocal almacen, AlmacenCargas parciales, AdjuntoRepositorio adjuntos,
            PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite,
            CuotaAlmacenamiento cuota, VerificadorFormato verificador) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        // ADR-0026: el contenido técnico se comprueba al finalizar, sobre el archivo completo.
        SubirArchivoCasoDeUso subida=new SubirArchivoCasoDeUso(pedidos,almacen,adjuntos,publicador,usuarios,reloj,limite,
                verificador);
        GestionarCargaCasoDeUso caso=new GestionarCargaCasoDeUso(cargas,pedidos,almacen,parciales,adjuntos,subida,usuarios,
                reloj,limite,parciales instanceof CargaADriveCasoDeUso directo ? directo : null);
        return new GestionarCarga() {
            @Override public Vista iniciar(Solicitante solicitante, Inicio comando) {
                long t0=System.nanoTime();
                Vista vista=tx.execute(estado -> {
                    Vista v=caso.iniciar(solicitante,comando);
                    cuota.reservar(v.carga().adjuntoReservadoId(),v.carga().usuarioId(),v.carga().tamanoBytes());
                    return v;
                });
                LOG.info("carga.inicio carga={} pedido={} etapa={} tamano={} offset={} destino={} db_ms={}",vista.carga().id(),
                        vista.carga().pedidoId(),vista.carga().etapa(),vista.carga().tamanoBytes(),
                        vista.carga().offsetConfirmado(),caso.directoADrive() ? "drive" : "local",ms(t0));
                return vista;
            }
            @Override public Vista consultar(Solicitante solicitante, UUID id) {
                return tx.execute(estado -> caso.vista(caso.autorizar(solicitante,id,false)));
            }
            @Override public Vista fragmento(Solicitante solicitante, UUID id, Bloque bloque) {
                long t0=System.nanoTime();
                long dbMs=0;
                long escrituraMs=0;
                try {
                    tx.executeWithoutResult(estado -> caso.autorizar(solicitante,id,false));
                    try (var bloqueo=parciales.bloquear(id)) {
                        Vista vista=tx.execute(estado -> {
                            CargaArchivo carga=caso.autorizar(solicitante,id,true);
                            caso.validarBloque(carga,bloque);
                            return caso.vista(carga);
                        });
                        dbMs=ms(t0);
                        if (bloque.inicio()==vista.carga().offsetConfirmado()) {
                            // Con destino Drive, escribir reenvía el fragmento a Drive (sin transacción abierta).
                            long t1=System.nanoTime();
                            parciales.reconciliar(id,vista.carga().offsetConfirmado());
                            parciales.escribir(id,bloque.inicio(),bloque.contenido());
                            escrituraMs=ms(t1);
                        }
                        // Si la autorización/commit falla, el tail se trunca al offset durable en la próxima petición.
                        long t2=System.nanoTime();
                        Vista resultado=tx.execute(estado -> caso.confirmarBloque(solicitante,id,bloque));
                        dbMs+=ms(t2);
                        registrarProgreso(resultado,bloque,dbMs,escrituraMs,caso.directoADrive());
                        return resultado;
                    }
                } catch (RuntimeException error) {
                    LOG.warn("carga.fragmento.error carga={} inicio={} bytes={} causa={} db_ms={} {}_ms={}",id,bloque.inicio(),
                            bloque.contenido()==null ? 0 : bloque.contenido().length,causa(error),dbMs,
                            caso.directoADrive() ? "drive" : "disco",escrituraMs);
                    throw error;
                }
            }
            @Override public AdjuntoVista completar(Solicitante solicitante, UUID id) {
                long t0=System.nanoTime();
                try {
                    AdjuntoVista adjunto=completarCarga(solicitante,id);
                    LOG.info("carga.finalizada carga={} pedido={} adjunto={} tamano={} estado_copia={} ms={}",id,
                            adjunto.adjunto().pedidoId(),adjunto.adjunto().id(),adjunto.adjunto().tamanoBytes(),
                            adjunto.adjunto().estadoCopia(),ms(t0));
                    return adjunto;
                } catch (RuntimeException error) {
                    LOG.warn("carga.finalizacion.error carga={} causa={} ms={}",id,causa(error),ms(t0));
                    throw error;
                }
            }
            private AdjuntoVista completarCarga(Solicitante solicitante, UUID id) {
                tx.executeWithoutResult(estado -> caso.autorizar(solicitante,id,false));
                try (var bloqueo=parciales.bloquear(id)) {
                    Vista vista=tx.execute(estado -> caso.vista(caso.autorizar(solicitante,id,true)));
                    if (vista.carga().estado()==CargaArchivo.Estado.COMPLETADA) {
                        AdjuntoVista ya=tx.execute(estado -> caso.adjuntoCompletado(caso.autorizar(solicitante,id,true)));
                        if (parciales instanceof CargaADriveCasoDeUso directo) { directo.olvidarSesion(id); }
                        return ya;
                    }
                    if (caso.directoADrive()) {
                        // ADR-0034: verificación contra Drive fuera de transacción; registro en una transacción corta.
                        long t1=System.nanoTime();
                        GestionarCargaCasoDeUso.PreparadoDrive preparado=caso.prepararFinalDrive(vista);
                        LOG.info("carga.drive_confirmado carga={} tamano={} verificacion_ms={}",id,preparado.tamanoBytes(),ms(t1));
                        long t2=System.nanoTime();
                        AdjuntoVista registrado=tx.execute(estado -> {
                            var adjunto=caso.registrarFinalDrive(solicitante,id,preparado);
                            cuota.confirmar(adjunto.adjunto().id(),solicitante.usuarioId(),adjunto.adjunto().tamanoBytes());
                            return adjunto;
                        });
                        LOG.debug("carga.registro carga={} db_ms={}",id,ms(t2));
                        ((CargaADriveCasoDeUso) parciales).olvidarSesion(id);
                        return registrado;
                    }
                    AlmacenLocal.ArchivoRecibido preparado;
                    try { preparado=caso.prepararFinal(vista); }
                    catch (UncheckedIOException error) { throw ProblemaCarga.almacenamiento(); }
                    // No borrar el final reservado ante un rollback; sigue vinculado a la sesión para el próximo intento.
                    return tx.execute(estado -> {
                        var adjunto=caso.registrarFinal(solicitante,id,preparado);
                        cuota.confirmar(adjunto.adjunto().id(),solicitante.usuarioId(),adjunto.adjunto().tamanoBytes());
                        return adjunto;
                    });
                }
            }
        };
    }

    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("decokasa.archivos.cargas");

    private static long ms(long desde) { return (System.nanoTime()-desde)/1_000_000; }

    /** Causa depurada: código del contrato, causa de Google o solo el tipo; nunca el mensaje (puede llevar datos). */
    private static String causa(RuntimeException error) {
        if (error instanceof com.decokasa.seguimiento.compartido.domain.ErrorDeDominio dominio) {
            return dominio.codigo().name();
        }
        if (error instanceof com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle google) {
            return "GOOGLE_"+google.causa().name();
        }
        return error.getClass().getSimpleName();
    }

    /** Cada fragmento en DEBUG; en INFO solo el primero y al cruzar 25, 50, 75 y 100 %. */
    private static void registrarProgreso(GestionarCarga.Vista vista, GestionarCarga.Bloque bloque, long dbMs,
            long escrituraMs, boolean drive) {
        long total=vista.carga().tamanoBytes();
        long hecho=vista.carga().offsetConfirmado();
        String destino=drive ? "drive" : "disco";
        LOG.debug("carga.fragmento carga={} inicio={} bytes={} offset={}/{} db_ms={} {}_ms={}",vista.carga().id(),
                bloque.inicio(),bloque.contenido().length,hecho,total,dbMs,destino,escrituraMs);
        long antes=bloque.inicio()*4/total;
        long ahora=hecho*4/total;
        if (bloque.inicio()==0 || ahora>antes) {
            LOG.info("carga.progreso carga={} offset={}/{} porcentaje={} {}_ms_ultimo={}",vista.carga().id(),hecho,total,
                    hecho*100/total,destino,escrituraMs);
        }
    }

    /** Cada llamada del puerto en su propia transacción corta (las excepciones salen tal cual). */
    @SuppressWarnings("unchecked")
    private static <T> T enTransaccion(Class<T> tipo, T destino, TransactionTemplate tx) {
        return (T) java.lang.reflect.Proxy.newProxyInstance(tipo.getClassLoader(), new Class<?>[] {tipo},
                (proxy, metodo, argumentos) -> {
                    if (metodo.getDeclaringClass() == Object.class) { return metodo.invoke(destino, argumentos); }
                    return tx.execute(estado -> {
                        try {
                            return metodo.invoke(destino, argumentos);
                        } catch (java.lang.reflect.InvocationTargetException error) {
                            if (error.getCause() instanceof RuntimeException fallo) { throw fallo; }
                            if (error.getCause() instanceof Error grave) { throw grave; }
                            throw new IllegalStateException(error.getCause());
                        } catch (IllegalAccessException error) {
                            throw new IllegalStateException(error);
                        }
                    });
                });
    }
}
