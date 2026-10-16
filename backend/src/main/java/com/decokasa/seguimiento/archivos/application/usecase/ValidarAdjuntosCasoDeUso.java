package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.in.ConsultarAdjuntosDeEntrada;
import com.decokasa.seguimiento.archivos.application.port.in.ValidarAdjuntosParaAccion;
import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.compartido.domain.DatoInvalido;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Valida los archivos que acompañan una acción (P-05 opción A, P-18). Un identificador inválido nunca se ignora, y una
 * fila sin su archivo en el almacenamiento no cuenta como documento. Sin Spring (R2).
 */
public final class ValidarAdjuntosCasoDeUso implements ValidarAdjuntosParaAccion, ConsultarAdjuntosDeEntrada {

    private static final String CAMPO = "adjuntoIds";

    private final AdjuntoRepositorio adjuntos;
    private final AlmacenLocal almacen;

    public ValidarAdjuntosCasoDeUso(AdjuntoRepositorio adjuntos, AlmacenLocal almacen) {
        this.adjuntos = adjuntos;
        this.almacen = almacen;
    }

    @Override
    public Set<String> tiposValidos(UUID pedidoId, int etapa, UUID entradaId, List<UUID> adjuntoIds) {
        if (adjuntoIds == null || adjuntoIds.isEmpty()) {
            return Set.of();
        }
        if (adjuntoIds.stream().anyMatch(Objects::isNull) || new HashSet<>(adjuntoIds).size() != adjuntoIds.size()) {
            throw new DatoInvalido(CAMPO, "Hay archivos vacíos o repetidos en la lista.");
        }
        if (entradaId == null) {
            throw new DatoInvalido(CAMPO, "La etapa actual no tiene una entrada a la que asociar archivos.");
        }
        Map<UUID, Adjunto> encontrados = adjuntos.porIds(adjuntoIds).stream()
                .collect(Collectors.toMap(Adjunto::id, Function.identity()));
        Set<String> tipos = new HashSet<>();
        for (UUID id : adjuntoIds) {
            Adjunto adjunto = encontrados.get(id);
            if (adjunto == null || !adjunto.pedidoId().equals(pedidoId) || adjunto.etapa() != etapa
                    || !adjunto.entradaId().equals(entradaId)) {
                throw new DatoInvalido(CAMPO,
                        "Un archivo no existe o no es de la entrada actual de esta etapa: súbelo de nuevo.");
            }
            OptionalLong tamano = almacen.tamano(adjunto.claveLocal());
            if (tamano.isEmpty() || tamano.getAsLong() != adjunto.tamanoBytes() || !contenidoIntegro(adjunto)) {
                throw new DatoInvalido(CAMPO, "Un archivo no está disponible en el almacenamiento: súbelo de nuevo.");
            }
            tipos.add(adjunto.tipoDocumento());
        }
        return Set.copyOf(tipos);
    }

    /** Verifica los bytes antes de que el consumidor modifique el pedido o publique su evento. */
    private boolean contenidoIntegro(Adjunto adjunto) {
        try (InputStream contenido = almacen.abrir(adjunto.claveLocal())) {
            MessageDigest resumen = MessageDigest.getInstance("SHA-256");
            byte[] bloque = new byte[64 * 1024];
            long total = 0;
            int leidos;
            while ((leidos = contenido.read(bloque)) != -1) {
                total += leidos;
                if (total > adjunto.tamanoBytes()) {
                    return false;
                }
                resumen.update(bloque, 0, leidos);
            }
            return total == adjunto.tamanoBytes()
                    && adjunto.sha256().equals(HexFormat.of().formatHex(resumen.digest()));
        } catch (IOException | UncheckedIOException error) {
            // Un fallo de lectura tampoco acredita un documento disponible.
            return false;
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no está disponible.", error);
        }
    }

    @Override
    public Set<String> tiposSubidos(UUID entradaId) {
        return entradaId == null ? Set.of() : adjuntos.tiposDeEntrada(entradaId);
    }
}
