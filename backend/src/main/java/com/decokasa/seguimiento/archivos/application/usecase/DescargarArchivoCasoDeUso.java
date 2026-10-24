package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.DescargarArchivo;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaArchivos;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.compartido.domain.ArchivoNoDisponible;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
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
 * Descarga a través del servidor (P-17). Antes de entregar, comprueba que el archivo exista, tenga el tamaño
 * registrado y la misma huella SHA-256: un archivo ausente, incompleto o dañado nunca se entrega. Sin Spring (R2).
 */
public final class DescargarArchivoCasoDeUso implements DescargarArchivo {

    private static final String NO_DISPONIBLE = "El archivo no está disponible. Avísale al admin.";

    private final PedidosParaArchivos pedidos;
    private final AdjuntoRepositorio adjuntos;
    private final AlmacenLocal almacen;

    public DescargarArchivoCasoDeUso(PedidosParaArchivos pedidos, AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        this.pedidos = pedidos;
        this.adjuntos = adjuntos;
        this.almacen = almacen;
    }

    @Override
    public Autorizada autorizar(Solicitante solicitante, UUID adjuntoId) {
        Adjunto adjunto = adjuntos.porId(adjuntoId).orElseThrow(() -> new NoEncontrado("El archivo no existe."));
        try {
            pedidos.autorizarDescarga(solicitante, adjunto.pedidoId(), adjunto.etapa());
        } catch (NoEncontrado deOtroPais) {
            // ADR-0024: un archivo de un pedido de otro país responde igual que uno inexistente.
            throw new NoEncontrado("El archivo no existe.");
        }
        return new Autorizada(adjunto.id(), adjunto.nombre(), adjunto.tamanoBytes(), adjunto.sha256(),
                adjunto.claveLocal());
    }

    @Override
    public Contenido abrir(Autorizada autorizada) {
        OptionalLong tamano = almacen.tamano(autorizada.claveLocal());
        if (tamano.isEmpty() || tamano.getAsLong() != autorizada.tamanoBytes()) {
            throw new ArchivoNoDisponible(NO_DISPONIBLE);
        }
        if (!autorizada.sha256().equals(huella(autorizada.claveLocal()))) {
            throw new ArchivoNoDisponible(NO_DISPONIBLE);
        }
        return new Contenido(autorizada.nombre(), autorizada.tamanoBytes(), almacen.abrir(autorizada.claveLocal()));
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
