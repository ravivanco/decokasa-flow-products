package com.decokasa.seguimiento.archivos.config;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco;
import com.decokasa.seguimiento.archivos.application.port.in.ConsultarAdjuntosDeEntrada;
import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CuotaAlmacenamiento;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.CuotaSqlRepositorio;
import javax.sql.DataSource;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.ArchivoRecibido;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.application.usecase.DescargarArchivoCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.ListarAdjuntosDeEtapaCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.SubirArchivoCasoDeUso;
import com.decokasa.seguimiento.archivos.application.usecase.ValidarAdjuntosCasoDeUso;
import com.decokasa.seguimiento.archivos.domain.LimiteDeTamano;
import com.decokasa.seguimiento.compartido.application.port.out.PublicadorEventos;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.application.port.in.ResumirUsuarios;
import jakarta.servlet.MultipartConfigElement;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.unit.DataSize;

/**
 * Raíz de composición del módulo archivos (P-05 opción A). Las transacciones se abren aquí (ADR-0012). Dirección de
 * dependencias (R9): archivos → usuarios (puerto de entrada) y compartido; pedidos → archivos. Archivos no depende de
 * pedidos: el adaptador de {@link PedidosParaArchivos} vive en pedidos.
 */
@Configuration(proxyBeanMethods = false)
public class ArchivosConfig {

    @Bean
    CuotaAlmacenamiento cuotaAlmacenamiento(DataSource fuente,
            @Value("${decokasa.archivos.cuota.global:10GB}") DataSize global,
            @Value("${decokasa.archivos.cuota.usuario:2GB}") DataSize usuario,
            @Value("${decokasa.archivos.cuota.cargas-globales:10}") int cargasGlobales,
            @Value("${decokasa.archivos.cuota.cargas-usuario:2}") int cargasUsuario) {
        return new CuotaSqlRepositorio(fuente,global.toBytes(),usuario.toBytes(),cargasGlobales,cargasUsuario);
    }

    @Bean
    AlmacenLocal almacenLocal(@Value("${decokasa.archivos.directorio}") String directorio) {
        return new AlmacenLocalEnDisco(Path.of(directorio));
    }

    /**
     * Se une a la transacción de la acción que lo llama (propagación REQUIRED). El caso de uso no se registra como
     * bean: implementa dos puertos y Spring vería dos candidatos para cada uno.
     */
    @Bean
    ValidarAdjuntosParaAccion validarAdjuntosParaAccion(PlatformTransactionManager gestor,
            AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        ValidarAdjuntosCasoDeUso caso = new ValidarAdjuntosCasoDeUso(adjuntos, almacen);
        return (pedidoId, etapa, entradaId, adjuntoIds) -> tx.execute(
                estado -> caso.tiposValidos(pedidoId, etapa, entradaId, adjuntoIds));
    }

    @Bean
    ConsultarAdjuntosDeEntrada consultarAdjuntosDeEntrada(PlatformTransactionManager gestor,
            AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        TransactionTemplate tx = soloLectura(gestor);
        ValidarAdjuntosCasoDeUso caso = new ValidarAdjuntosCasoDeUso(adjuntos, almacen);
        return entradaId -> tx.execute(estado -> caso.tiposSubidos(entradaId));
    }

    @Bean
    ListarAdjuntosDeEtapa listarAdjuntosDeEtapa(PlatformTransactionManager gestor, PedidosParaArchivos pedidos,
            AdjuntoRepositorio adjuntos, ResumirUsuarios usuarios) {
        TransactionTemplate tx = soloLectura(gestor);
        ListarAdjuntosDeEtapa caso = new ListarAdjuntosDeEtapaCasoDeUso(pedidos, adjuntos, usuarios);
        return (solicitante, pedidoId, etapa) -> tx.execute(estado -> caso.listar(solicitante, pedidoId, etapa));
    }

    /** La autorización va en una transacción corta; la lectura y verificación del archivo, fuera de ella. */
    @Bean
    DescargarArchivo descargarArchivo(PlatformTransactionManager gestor, PedidosParaArchivos pedidos,
            AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        TransactionTemplate tx = soloLectura(gestor);
        DescargarArchivo caso = new DescargarArchivoCasoDeUso(pedidos, adjuntos, almacen);
        return new DescargarArchivo() {
            @Override
            public Autorizada autorizar(Solicitante solicitante, UUID adjuntoId) {
                return tx.execute(estado -> caso.autorizar(solicitante, adjuntoId));
            }

            @Override
            public Contenido abrir(Autorizada autorizada) {
                return caso.abrir(autorizada);
            }
        };
    }

    // --- Subida: límite provisional del equipo de 100 MiB por archivo, configurable y pendiente del cliente ---

    @Bean
    LimiteDeTamano limiteDeTamano(@Value("${decokasa.archivos.tamano-maximo}") DataSize tamanoMaximo) {
        return new LimiteDeTamano(tamanoMaximo.toBytes());
    }

    /** Reserve before receiving application bytes; finalize metadata/history/quota in one short transaction.
     * On failure, keep the durable allocation and any retained bytes for explicit recovery. */
    @Bean
    SubirArchivo subirArchivo(PlatformTransactionManager gestor, PedidosParaArchivos pedidos, AlmacenLocal almacen,
            AdjuntoRepositorio adjuntos, PublicadorEventos publicador, ResumirUsuarios usuarios, Reloj reloj,
            LimiteDeTamano limite, CuotaAlmacenamiento cuota) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        SubirArchivoCasoDeUso caso = new SubirArchivoCasoDeUso(pedidos, almacen, adjuntos, publicador, usuarios, reloj,
                limite);
        return (solicitante, comando) -> {
            UUID adjuntoId = UUID.randomUUID();
            tx.executeWithoutResult(estado -> {
                caso.prevalidar(solicitante, comando);
                // Legacy multipart has no declared length: reserve the full per-file allowance.
                cuota.reservar(adjuntoId,solicitante.usuarioId(),limite.bytes());
            });
            ArchivoRecibido recibido = caso.recibir(comando);
            // Keep the allocation/bytes on failure: no unapproved automatic retention policy.
            return tx.execute(estado -> {
                var vista=caso.registrar(solicitante,comando,recibido,adjuntoId);
                cuota.confirmar(adjuntoId,solicitante.usuarioId(),recibido.tamanoBytes());
                return vista;
            });
        };
    }

    /**
     * Multipart con el mismo límite: el servlet guarda la parte en el volumen (umbral 0, sin cargarla en memoria) y
     * rechaza lo que lo supera con 413 ARCHIVO_DEMASIADO_GRANDE. Al cuerpo completo se le suma 1 MiB para el resto
     * del formulario (separadores, cabeceras de cada parte, tipoDocumento y entradaEsperadaId). nginx usa el mismo
     * margen (frontend/limite-archivos.envsh), así que los dos rechazan en el mismo punto.
     */
    @Bean
    MultipartConfigElement configuracionMultipart(LimiteDeTamano limite,
            @Value("${decokasa.archivos.directorio}") String directorio) {
        Path temporales = Path.of(directorio).toAbsolutePath().normalize().resolve(".multipart");
        temporales.toFile().mkdirs();
        long margenFormulario = DataSize.ofMegabytes(1).toBytes();
        return new MultipartConfigElement(temporales.toString(), limite.bytes(), limite.bytes() + margenFormulario, 0);
    }

    private static TransactionTemplate soloLectura(PlatformTransactionManager gestor) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        tx.setReadOnly(true);
        return tx;
    }
}
