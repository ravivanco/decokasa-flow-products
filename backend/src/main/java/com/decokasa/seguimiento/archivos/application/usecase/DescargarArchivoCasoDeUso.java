package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.DocumentosDePedido;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.archivos.domain.DriveNoDisponible;
import com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.compartido.domain.SinPermiso;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.OptionalLong;
import java.util.UUID;

/**
 * Descarga a través del servidor (P-17, ampliada por ADR-0028). Autoriza en cada llamada con el estado vigente: P-17
 * sobre la etapa del archivo o, para quien puede actuar en la etapa actual, cualquier documento del pedido. Sirve el archivo
 * local si existe con su tamaño y huella; si no, y la copia está confirmada, lo transmite desde Drive con la conexión
 * central (metadatos comprobados antes y tamaño/huella comprobados mientras se transmite, {@link FlujoVerificado}). Un
 * archivo ausente, incompleto o dañado nunca se entrega completo. Nunca cambia el estado de la copia. Sin Spring (R2).
 */
public final class DescargarArchivoCasoDeUso implements DescargarArchivo {

    private static final String NO_DISPONIBLE = "El archivo no está disponible. Avísale al admin.";
    private static final String SIN_ARCHIVO = "Este documento no tiene archivo en el servidor ni copia en Drive "
            + "(por ejemplo, un registro recuperado sin su archivo). El registro se conserva; avísale al admin.";
    private static final String COPIA_DIVERGENTE = "La copia de Drive de este documento ya no coincide con la "
            + "registrada o no existe. Avísale al admin.";

    private final PedidosParaArchivos pedidos;
    private final AdjuntoRepositorio adjuntos;
    private final AlmacenLocal almacen;
    /** Nulos con el constructor anterior a ADR-0028: solo archivo local y P-17. */
    private final DocumentosDePedido documentos;
    private final AlmacenArchivos drive;

    public DescargarArchivoCasoDeUso(PedidosParaArchivos pedidos, AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        this(pedidos, adjuntos, almacen, null, null);
    }

    /** ADR-0028: documentos del pedido desde la etapa actual y lectura desde Drive para las copias confirmadas. */
    public DescargarArchivoCasoDeUso(PedidosParaArchivos pedidos, AdjuntoRepositorio adjuntos, AlmacenLocal almacen,
            DocumentosDePedido documentos, AlmacenArchivos drive) {
        this.pedidos = pedidos;
        this.adjuntos = adjuntos;
        this.almacen = almacen;
        this.documentos = documentos;
        this.drive = drive;
    }

    @Override
    public Autorizada autorizar(Solicitante solicitante, UUID adjuntoId) {
        Adjunto adjunto = adjuntos.porId(adjuntoId).orElseThrow(() -> new NoEncontrado("El archivo no existe."));
        try {
            pedidos.autorizarDescarga(solicitante, adjunto.pedidoId(), adjunto.etapa());
        } catch (NoEncontrado deOtroPais) {
            // ADR-0024: un archivo de un pedido de otro país responde igual que uno inexistente.
            throw new NoEncontrado("El archivo no existe.");
        } catch (SinPermiso sinP17) {
            // ADR-0028: todo documento del pedido (presentado o solo subido) lo abre quien actúa en la etapa actual.
            if (documentos == null) { throw sinP17; }
            try {
                pedidos.autorizarAperturaDesdeEtapaActual(solicitante, adjunto.pedidoId());
            } catch (NoEncontrado deOtroPais) {
                throw new NoEncontrado("El archivo no existe.");
            }
        }
        String driveId = documentos == null ? null : documentos.driveIdCopiado(adjunto.id()).orElse(null);
        return new Autorizada(adjunto.id(), adjunto.nombre(), adjunto.tamanoBytes(), adjunto.sha256(),
                adjunto.claveLocal(), driveId);
    }

    @Override
    public Contenido abrir(Autorizada autorizada) {
        OptionalLong tamano = almacen.tamano(autorizada.claveLocal());
        boolean localIntegro = tamano.isPresent() && tamano.getAsLong() == autorizada.tamanoBytes()
                && autorizada.sha256().equals(huellaOpcional(autorizada.claveLocal()));
        if (localIntegro) {
            return new Contenido(autorizada.nombre(), autorizada.tamanoBytes(), almacen.abrir(autorizada.claveLocal()));
        }
        if (autorizada.driveId() == null || drive == null) {
            // Sin archivo local íntegro ni copia confirmada (por ejemplo, un registro recuperado sin su archivo).
            throw new ArchivoNoDisponible(tamano.isEmpty() ? SIN_ARCHIVO : NO_DISPONIBLE);
        }
        return desdeDrive(autorizada);
    }

    /** ADR-0028: metadatos comprobados antes de transmitir; tamaño y huella comprobados mientras se transmite. */
    private Contenido desdeDrive(Autorizada autorizada) {
        try {
            var remoto = drive.metadatos(autorizada.driveId())
                    .orElseThrow(() -> new ArchivoNoDisponible(COPIA_DIVERGENTE));
            if (!autorizada.driveId().equals(remoto.id()) || remoto.enPapelera() || !remoto.propietario()
                    || !remoto.unidadCompartida().isBlank() || remoto.tamano() != autorizada.tamanoBytes()
                    || (!remoto.sha256().isBlank() && !remoto.sha256().equalsIgnoreCase(autorizada.sha256()))) {
                throw new ArchivoNoDisponible(COPIA_DIVERGENTE);
            }
            InputStream flujo = drive.descargar(autorizada.driveId());
            return new Contenido(autorizada.nombre(), autorizada.tamanoBytes(),
                    new FlujoVerificado(flujo, autorizada.tamanoBytes(), autorizada.sha256()));
        } catch (FalloIntegracionGoogle error) {
            if (error.causa() == FalloIntegracionGoogle.Causa.METADATOS_DIVERGENTES) {
                throw new ArchivoNoDisponible(COPIA_DIVERGENTE);
            }
            throw new DriveNoDisponible();
        }
    }

    /** Huella del archivo local, o nula si no se puede leer (entonces se intenta la copia de Drive). */
    private String huellaOpcional(String clave) {
        try {
            return huella(clave);
        } catch (ArchivoNoDisponible ilegible) {
            return null;
        }
    }

    private String huella(String clave) {
        try (InputStream flujo = almacen.abrir(clave)) {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bloque = new byte[64 * 1024];
            int leidos;
            while ((leidos = flujo.read(bloque)) != -1) {
                resumen.update(bloque, 0, leidos);
            }
            return HexFormat.of().formatHex(resumen.digest());
        } catch (IOException | UncheckedIOException error) {
            throw new ArchivoNoDisponible(NO_DISPONIBLE);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible.", error);
        }
    }
}
