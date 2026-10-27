package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenCargasEnDisco;
import com.decokasa.seguimiento.archivos.application.port.in.AdjuntoVista;
import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarga;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenCargas;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CuotaAlmacenamiento;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.application.usecase.GestionarCargaCasoDeUso;
import com.decokasa.seguimiento.archivos.application.port.out.VerificadorFormato;
import com.decokasa.seguimiento.archivos.application.usecase.SubirArchivoCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Transacciones fuera del núcleo. Red/bytes grandes fuera de transacción; metadatos en transacciones cortas. */
@Configuration(proxyBeanMethods=false)
public class CargasConfig {
    @Bean AlmacenCargas almacenCargas(@Value("${decokasa.archivos.directorio}") String directorio) {
        return new AlmacenCargasEnDisco(Path.of(directorio));
    }
    @Bean GestionarCarga gestionarCarga(PlatformTransactionManager gestor, CargaRepositorio cargas,
            PedidosParaArchivos pedidos, AlmacenLocal almacen, AlmacenCargas parciales, AdjuntoRepositorio adjuntos,
            PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj, LimiteDeTamano limite,
            CuotaAlmacenamiento cuota, VerificadorFormato verificador) {
        TransactionTemplate tx=new TransactionTemplate(gestor);
        // ADR-0026: el contenido técnico se comprueba al finalizar, sobre el archivo completo.
        SubirArchivoCasoDeUso subida=new SubirArchivoCasoDeUso(pedidos,almacen,adjuntos,publicador,usuarios,reloj,limite,
                verificador);
        GestionarCargaCasoDeUso caso=new GestionarCargaCasoDeUso(cargas,pedidos,almacen,parciales,adjuntos,subida,usuarios,reloj,limite);
        return new GestionarCarga() {
            @Override public Vista iniciar(Solicitante solicitante, Inicio comando) {
                return tx.execute(estado -> {
                    Vista vista=caso.iniciar(solicitante,comando);
                    cuota.reservar(vista.carga().adjuntoReservadoId(),vista.carga().usuarioId(),vista.carga().tamanoBytes());
                    return vista;
                });
            }
            @Override public Vista consultar(Solicitante solicitante, UUID id) {
                return tx.execute(estado -> caso.vista(caso.autorizar(solicitante,id,false)));
            }
            @Override public Vista fragmento(Solicitante solicitante, UUID id, Bloque bloque) {
                tx.executeWithoutResult(estado -> caso.autorizar(solicitante,id,false));
                try (var bloqueo=parciales.bloquear(id)) {
                    Vista vista=tx.execute(estado -> {
                        CargaArchivo carga=caso.autorizar(solicitante,id,true);
                        caso.validarBloque(carga,bloque);
                        return caso.vista(carga);
                    });
                    if (bloque.inicio()==vista.carga().offsetConfirmado()) {
                        parciales.reconciliar(id,vista.carga().offsetConfirmado());
                        parciales.escribir(id,bloque.inicio(),bloque.contenido());
                    }
                    // Si la autorización/commit falla, el tail se trunca al offset durable en la próxima petición.
                    return tx.execute(estado -> caso.confirmarBloque(solicitante,id,bloque));
                }
            }
            @Override public AdjuntoVista completar(Solicitante solicitante, UUID id) {
                tx.executeWithoutResult(estado -> caso.autorizar(solicitante,id,false));
                try (var bloqueo=parciales.bloquear(id)) {
                    Vista vista=tx.execute(estado -> caso.vista(caso.autorizar(solicitante,id,true)));
                    if (vista.carga().estado()==CargaArchivo.Estado.COMPLETADA) {
                        return tx.execute(estado -> caso.adjuntoCompletado(caso.autorizar(solicitante,id,true)));
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
}
