package com.decokasa.seguimiento.archivos.config;

import java.util.Set;
import com.decokasa.seguimiento.archivos.application.usecase.AccesoConfidencialCasoDeUso;
import com.decokasa.seguimiento.archivos.application.port.out.ConfidencialesRepositorio;
import com.decokasa.seguimiento.archivos.application.port.in.AccesoConfidencial;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.AlmacenLocalEnDisco;
import com.decokasa.seguimiento.archivos.adapter.out.almacen.VerificadorFormatoEnDisco;
import com.decokasa.seguimiento.archivos.application.port.in.ConsultarAdjuntosDeEntrada;
import com.decokasa.seguimiento.archivos.application.port.in.ConsultarDocumentosDelPedido;
import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ListarAdjuntosDeEtapa;
import com.decokasa.seguimiento.archivos.application.port.in.SubirArchivo;
import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import com.decokasa.seguimiento.archivos.application.port.out.CuotaAlmacenamiento;
import com.decokasa.seguimiento.archivos.adapter.out.persistence.CuotaSqlRepositorio;
import javax.sql.DataSource;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal.ArchivoRecibido;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.VerificadorFormato;
import com.decokasa.seguimiento.archivos.application.usecase.ConsultarDocumentosDelPedidoCasoDeUso;
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

    /** Contenido técnico de los formatos admitidos (ADR-0026), sobre el mismo directorio del almacenamiento local. */
    @Bean
    VerificadorFormato verificadorFormato(@Value("${decokasa.archivos.directorio}") String directorio) {
        return new VerificadorFormatoEnDisco(Path.of(directorio));
    }

    /**
     * Con {@code ARCHIVOS_DESTINO=drive} (ADR-0034) este directorio solo guarda temporales de una petición (verificación
     * técnica antes de registrar); el documento vive únicamente en Drive. Con {@code local} sigue el flujo de la fase 3.
     */
    @Bean
    AlmacenLocal almacenLocal(@Value("${decokasa.archivos.directorio}") String directorio) {
        return new AlmacenLocalEnDisco(Path.of(directorio));
    }

    /** {@code local} (desarrollo, copia diferida a Drive) o {@code drive} (ADR-0034: el documento solo en Drive). */
    static boolean destinoDrive(String destino) {
        return switch (destino.strip().toLowerCase(java.util.Locale.ROOT)) {
            case "local", "" -> false;
            case "drive" -> true;
            default -> throw new IllegalArgumentException("ARCHIVOS_DESTINO debe ser 'local' o 'drive'.");
        };
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
            AdjuntoRepositorio adjuntos, ResumirUsuarios usuarios, AccesoConfidencial confidencial) {
        TransactionTemplate tx = soloLectura(gestor);
        ListarAdjuntosDeEtapa caso = new ListarAdjuntosDeEtapaCasoDeUso(pedidos, adjuntos, usuarios);
        // V37: sin acceso, los documentos confidenciales no aparecen.
        return (solicitante, pedidoId, etapa) -> tx.execute(estado -> {
            var lista = caso.listar(solicitante, pedidoId, etapa);
            if (confidencial.puedeVer(pedidoId, solicitante.usuarioId())) { return lista; }
            Set<UUID> ocultos = confidencial.confidenciales(pedidoId);
            return lista.stream().filter(vista -> !ocultos.contains(vista.adjunto().id())).toList();
        });
    }

    /** La autorización va en una transacción corta; la lectura y verificación del archivo, fuera de ella. */
    @Bean
    DescargarArchivo descargarArchivo(PlatformTransactionManager gestor, PedidosParaArchivos pedidos,
            AdjuntoRepositorio adjuntos, AlmacenLocal almacen, DocumentosDePedido documentos, AlmacenArchivos drive,
            AccesoConfidencial confidencial) {
        TransactionTemplate tx = soloLectura(gestor);
        // ADR-0028: sin transacción externa. La autorización prueba P-17 y, si la niega, la regla de la etapa actual;
        // una negativa esperada dentro de una transacción compartida la marcaría para deshacer y el commit fallaría (500).
        // Cada lectura va en su propia transacción corta y cada comprobación de pedidos en la suya. Drive, en abrir().
        DescargarArchivo caso = new DescargarArchivoCasoDeUso(pedidos, enTransaccion(AdjuntoRepositorio.class, adjuntos, tx),
                almacen, enTransaccion(DocumentosDePedido.class, documentos, tx), drive);
        return new DescargarArchivo() {
            @Override
            public Autorizada autorizar(Solicitante solicitante, UUID adjuntoId) {
                // V37: un documento confidencial lo abre quien tiene acceso (por rol o dado por Admin en su pedido),
                // dentro de su país, sin importar la etapa; nadie más, aunque actúe en esa etapa.
                UUID pedidoId = tx.execute(estado -> adjuntos.porId(adjuntoId).map(a -> a.pedidoId()).orElse(null));
                if (pedidoId == null) { return caso.autorizar(solicitante, adjuntoId); }
                if (!confidencial.confidenciales(pedidoId).contains(adjuntoId)) {
                    // Pedido del cliente (2026-11-15): todo documento no confidencial lo ve y descarga cualquiera que vea
                    // el pedido, en cualquier etapa (P-13 y país); P-17 ya no limita estos documentos.
                    try {
                        return caso.autorizar(solicitante, adjuntoId);
                    } catch (com.decokasa.seguimiento.compartido.domain.SinPermiso porEtapa) {
                        try {
                            tx.executeWithoutResult(estado -> pedidos.autorizarConsulta(solicitante, pedidoId));
                        } catch (com.decokasa.seguimiento.compartido.domain.NoEncontrado deOtroPais) {
                            throw new com.decokasa.seguimiento.compartido.domain.NoEncontrado("El archivo no existe.");
                        }
                        return caso.delAdjunto(adjuntoId);
                    }
                }
                try {
                    tx.executeWithoutResult(estado -> pedidos.autorizarConsulta(solicitante, pedidoId));
                } catch (com.decokasa.seguimiento.compartido.domain.NoEncontrado deOtroPais) {
                    throw new com.decokasa.seguimiento.compartido.domain.NoEncontrado("El archivo no existe.");
                }
                if (!confidencial.puedeVer(pedidoId, solicitante.usuarioId())) {
                    throw new com.decokasa.seguimiento.compartido.domain.SinPermiso(
                            "Documento confidencial: pide acceso a Admin.");
                }
                return caso.delAdjunto(adjuntoId);
            }

            @Override
            public Autorizada delAdjunto(UUID adjuntoId) {
                return caso.delAdjunto(adjuntoId);
            }

            @Override
            public Contenido abrir(Autorizada autorizada) {
                return caso.abrir(autorizada);
            }
        };
    }

    /**
     * ADR-0028: documentos del pedido para consultarlos desde DECOKASA. Sin transacción externa, por la misma razón que
     * la descarga: las negativas esperadas de P-17 no deben invalidar el listado.
     */
    @Bean
    ConsultarDocumentosDelPedido consultarDocumentosDelPedido(PlatformTransactionManager gestor,
            PedidosParaArchivos pedidos, DocumentosDePedido documentos, AlmacenLocal almacen, ResumirUsuarios usuarios,
            AccesoConfidencial confidencial) {
        TransactionTemplate tx = soloLectura(gestor);
        var caso = new ConsultarDocumentosDelPedidoCasoDeUso(pedidos,
                enTransaccion(DocumentosDePedido.class, documentos, tx), almacen,
                enTransaccion(ResumirUsuarios.class, usuarios, tx));
        // V37: sin acceso, los documentos confidenciales no aparecen en la lista.
        return (solicitante, pedidoId) -> {
            var lista = caso.listar(solicitante, pedidoId);
            Set<UUID> confidenciales = confidencial.confidenciales(pedidoId);
            boolean ve = confidencial.puedeVer(pedidoId, solicitante.usuarioId());
            // Todos abren lo no confidencial; lo confidencial, solo quien tiene acceso (y a los demás no les llega).
            return lista.stream()
                    .filter(v -> ve || !confidenciales.contains(v.documento().id()))
                    .map(v -> v.puedeAbrir() ? v
                            : new ConsultarDocumentosDelPedido.Vista(v.documento(), v.autor(), v.disponibilidad(), true, v.vigente()))
                    .toList();
        };
    }

    /** V37: cada operación en su propia transacción corta. */
    @Bean
    AccesoConfidencial accesoConfidencial(PlatformTransactionManager gestor, ConfidencialesRepositorio repositorio,
            PedidosParaArchivos pedidos, com.decokasa.seguimiento.compartido.application.port.out.Reloj reloj) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        AccesoConfidencial caso = new AccesoConfidencialCasoDeUso(repositorio, pedidos, reloj);
        return new AccesoConfidencial() {
            @Override public Set<UUID> confidenciales(UUID pedidoId) { return tx.execute(e -> caso.confidenciales(pedidoId)); }
            @Override public boolean puedeVer(UUID pedidoId, UUID usuarioId) {
                return Boolean.TRUE.equals(tx.execute(e -> caso.puedeVer(pedidoId, usuarioId)));
            }
            @Override public Resumen resumen(Solicitante s, UUID pedidoId) { return tx.execute(e -> caso.resumen(s, pedidoId)); }
            @Override public java.util.List<ConAcceso> conAcceso(Solicitante s, UUID pedidoId) {
                return tx.execute(e -> caso.conAcceso(s, pedidoId));
            }
            @Override public void otorgar(Solicitante s, UUID pedidoId, UUID usuarioId) {
                tx.executeWithoutResult(e -> caso.otorgar(s, pedidoId, usuarioId));
            }
            @Override public void revocar(Solicitante s, UUID pedidoId, UUID usuarioId) {
                tx.executeWithoutResult(e -> caso.revocar(s, pedidoId, usuarioId));
            }
        };
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
            LimiteDeTamano limite, CuotaAlmacenamiento cuota, VerificadorFormato verificador,
            @Value("${decokasa.archivos.destino:local}") String destino) {
        TransactionTemplate tx = new TransactionTemplate(gestor);
        SubirArchivoCasoDeUso caso = new SubirArchivoCasoDeUso(pedidos, almacen, adjuntos, publicador, usuarios, reloj,
                limite, verificador);
        boolean soloDrive = destinoDrive(destino);
        return (solicitante, comando) -> {
            if (soloDrive) {
                // ADR-0034: sin clave de idempotencia este camino podría duplicar en Drive; se usa la carga reanudable.
                throw new com.decokasa.seguimiento.compartido.domain.DatoInvalido("archivo",
                        "En este servidor los documentos se suben por carga reanudable directamente a Drive. "
                                + "Actualiza la página y vuelve a subirlo.");
            }
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
