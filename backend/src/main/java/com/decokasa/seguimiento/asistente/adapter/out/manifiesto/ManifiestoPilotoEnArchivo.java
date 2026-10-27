package com.decokasa.seguimiento.asistente.adapter.out.manifiesto;

import com.decokasa.seguimiento.asistente.application.port.out.ManifiestoPiloto;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lee el manifiesto local del piloto (F7-D3) en cada consulta, para que una revisión de Richard se aplique sin
 * reiniciar. Formato: un arreglo, o un objeto con {@code archivos}, de entradas {@code nombre}, {@code sha256},
 * {@code paginas}, {@code origen} (GENERADO o PLANTILLA), {@code revisado_por} y {@code revisado_en} (fecha). Una entrada
 * incompleta no autoriza nada. Si falta el archivo o no se puede leer, ningún documento queda autorizado (falla
 * cerrada). No registra su contenido.
 */
public final class ManifiestoPilotoEnArchivo implements ManifiestoPiloto {

    private static final Logger LOG = LoggerFactory.getLogger(ManifiestoPilotoEnArchivo.class);
    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");
    private static final Set<String> ORIGENES = Set.of("GENERADO", "PLANTILLA");
    /** Límite de páginas de un PDF en una petición (Messages API). */
    private static final int MAX_PAGINAS = 600;

    private final Path ruta;
    private final JsonMapper json;

    public ManifiestoPilotoEnArchivo(Path ruta, JsonMapper json) {
        this.ruta = ruta;
        this.json = json;
    }

    @Override
    public Optional<Entrada> porSha256(String sha256) {
        if (sha256 == null || !SHA256.matcher(sha256).matches() || !Files.isRegularFile(ruta)) {
            return Optional.empty();
        }
        JsonNode raiz;
        try {
            raiz = json.readTree(Files.readString(ruta));
        } catch (IOException | RuntimeException error) {
            LOG.warn("No se pudo leer el manifiesto del piloto de IA; ningún documento queda autorizado.");
            return Optional.empty();
        }
        JsonNode entradas = raiz.isArray() ? raiz : raiz.path("archivos");
        for (JsonNode nodo : entradas) {
            Optional<Entrada> entrada = entrada(nodo);
            if (entrada.isPresent() && entrada.get().sha256().equals(sha256)) {
                return entrada;
            }
        }
        return Optional.empty();
    }

    private static Optional<Entrada> entrada(JsonNode nodo) {
        String nombre = nodo.path("nombre").asString();
        String sha256 = nodo.path("sha256").asString();
        int paginas = nodo.path("paginas").asInt(0);
        String origen = nodo.path("origen").asString();
        String revisor = nodo.path("revisado_por").asString();
        String revisado = nodo.path("revisado_en").asString();
        if (nombre == null || nombre.isBlank() || sha256 == null || !SHA256.matcher(sha256).matches()
                || paginas < 1 || paginas > MAX_PAGINAS || origen == null || !ORIGENES.contains(origen)
                || revisor == null || revisor.isBlank() || !esFecha(revisado)) {
            return Optional.empty();
        }
        return Optional.of(new Entrada(nombre, sha256, paginas));
    }

    private static boolean esFecha(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        try {
            LocalDate.parse(texto.length() > 10 ? texto.substring(0, 10) : texto);
            return true;
        } catch (DateTimeParseException error) {
            return false;
        }
    }
}
