package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenLocal;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Disco efímero (Render Free) + bucket privado de Supabase como almacenamiento temporal duradero (ADR-0034).
 *
 * <p>Supabase Free limita cada objeto a 50 MB y el requisito es 100 MiB por documento. Cada adjunto se guarda como
 * fragmentos independientes ({@code {clave}/parte-00000}, …) de {@code fragmento} bytes como máximo, y al final un
 * manifiesto ({@code {clave}/manifiesto.json}) con el tamaño y el SHA-256 del archivo completo y de cada fragmento. El
 * manifiesto se escribe el último: si existe, el respaldo está completo. El usuario sube un solo documento; al
 * recuperarlo, los fragmentos se concatenan en el disco en un único archivo verificado, que es el que se copia a Drive.
 *
 * <ul>
 *   <li>Antes de registrar el adjunto, fuera de la transacción ({@link #respaldar}): reserva de cuota, fragmentos y
 *       manifiesto. Si algo falla, el adjunto no se registra y lo subido se marca para limpieza.</li>
 *   <li>Escritura idempotente ({@code x-upsert}) y tamaño comprobado tras cada objeto.</li>
 *   <li>Si el disco perdió el archivo (reinicio, suspensión, despliegue), {@link #abrir} y {@link #tamano} lo
 *       reconstruyen desde el bucket, verificando cada fragmento y el archivo completo.</li>
 *   <li>Los objetos de un adjunto registrado solo se borran cuando su copia de Drive está COPIADO
 *       ({@link #liberarRespaldo} y la limpieza periódica, {@link RegistroRespaldo#paraLimpiar}).</li>
 * </ul>
 */
public final class AlmacenLocalConRespaldo implements AlmacenLocal {
    private static final Logger LOG = LoggerFactory.getLogger(AlmacenLocalConRespaldo.class);
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    /** Solo claves definitivas generadas por el servidor: {pedidoId}/{etapa}/{adjuntoId}. */
    private static final Pattern DEFINITIVA = Pattern.compile(UUID + "/[0-9]{1,3}/" + UUID);
    private static final Pattern SHA256 = Pattern.compile("[0-9a-f]{64}");
    private static final String MANIFIESTO = "/manifiesto.json";
    private static final int VERSION = 1;
    /** Margen reservado para el manifiesto en la cuota. */
    private static final long RESERVA_MANIFIESTO = 64 * 1024;
    /** Máximo por objeto de Supabase Free (50 MB) menos margen. */
    public static final long FRAGMENTO_MAXIMO = 45L * 1024 * 1024;
    private static final Duration AUSENCIA = Duration.ofMinutes(10);
    private static final int MAXIMO_AUSENTES = 10000;
    private static final int BLOQUE = 64 * 1024;

    private final AlmacenLocal disco;
    private final RespaldoSupabaseStorage remoto;
    private final RegistroRespaldo registro;
    private final JsonMapper json;
    private final long limiteBytes;
    private final long fragmento;
    private final long cuota;
    /** Claves ya respaldadas por este proceso: confirmar dentro de la transacción no repite la red. */
    private final Set<String> respaldadas = ConcurrentHashMap.newKeySet();
    /** Claves que el bucket no tiene (liberadas o inexistentes), para no consultar en cada listado. */
    private final Map<String, Instant> ausentes = new ConcurrentHashMap<>();

    public AlmacenLocalConRespaldo(AlmacenLocal disco, RespaldoSupabaseStorage remoto, RegistroRespaldo registro,
            JsonMapper json, long limiteBytes, long fragmento, long cuota) {
        if (fragmento < 1024 * 1024 || fragmento > FRAGMENTO_MAXIMO) {
            throw new IllegalArgumentException("SUPABASE_FRAGMENTO debe estar entre 1MB y 45MB.");
        }
        if (cuota < limiteBytes + RESERVA_MANIFIESTO) {
            throw new IllegalArgumentException("SUPABASE_CUOTA debe admitir al menos un archivo del tamaño máximo.");
        }
        this.disco = disco;
        this.remoto = remoto;
        this.registro = registro;
        this.json = json;
        this.limiteBytes = limiteBytes;
        this.fragmento = fragmento;
        this.cuota = cuota;
    }

    @Override
    public ArchivoRecibido recibir(InputStream contenido, long limite) {
        return disco.recibir(contenido, limite);
    }

    @Override
    public void respaldar(ArchivoRecibido recibido, String claveFinal) {
        exigirDefinitiva(claveFinal);
        limpiar(5);
        long tamano = recibido.tamanoBytes();
        int partes = (int) ((tamano + fragmento - 1) / fragmento);
        registro.reservar(claveFinal, tamano + RESERVA_MANIFIESTO, partes, cuota);
        try {
            List<String> huellas = huellas(recibido, partes);
            List<Map<String, Object>> descripcion = new ArrayList<>();
            for (int i = 0; i < partes; i++) {
                long inicio = i * fragmento;
                long largo = Math.min(fragmento, tamano - inicio);
                remoto.subir(parte(claveFinal, i), () -> rango(recibido.claveTemporal(), inicio, largo), largo);
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("tamano", largo);
                d.put("sha256", huellas.get(i));
                descripcion.add(d);
            }
            Map<String, Object> manifiesto = new LinkedHashMap<>();
            manifiesto.put("version", VERSION);
            manifiesto.put("tamano", tamano);
            manifiesto.put("sha256", recibido.sha256());
            manifiesto.put("fragmento", fragmento);
            manifiesto.put("partes", descripcion);
            byte[] bytes = json.writeValueAsBytes(manifiesto);
            remoto.subir(claveFinal + MANIFIESTO, () -> new java.io.ByteArrayInputStream(bytes), bytes.length);
            registro.completar(claveFinal);
        } catch (RuntimeException error) {
            // Aún no hay adjunto registrado con esta clave: lo subido puede borrarse.
            registro.marcarLiberando(claveFinal);
            borrarObjetos(claveFinal, partes);
            throw error instanceof UncheckedIOException ? error : new UncheckedIOException(new IOException(
                    "No se pudo respaldar el archivo"));
        }
        respaldadas.add(claveFinal);
        ausentes.remove(claveFinal);
    }

    @Override
    public void confirmar(ArchivoRecibido recibido, String claveFinal) {
        // Carga reanudable (fuera de transacción) o un respaldo que no hizo este proceso: respaldar antes de mover.
        if (!respaldadas.contains(claveFinal)) { respaldar(recibido, claveFinal); }
        disco.confirmar(recibido, claveFinal);
    }

    /** Solo el disco: los objetos del bucket se borran con COPIADO o, si nunca hubo adjunto, por la limpieza. */
    @Override
    public void descartar(String clave) {
        disco.descartar(clave);
        respaldadas.remove(clave);
    }

    /** Llamado solo tras confirmar COPIADO (CopiarPendientesCasoDeUso). Nunca lanza. */
    @Override
    public void liberarRespaldo(String clave) {
        if (!DEFINITIVA.matcher(clave).matches()) { return; }
        respaldadas.remove(clave);
        try {
            var partes = registro.partes(clave);
            if (partes.isEmpty()) { return; }
            registro.marcarLiberando(clave);
            borrarObjetos(clave, partes.getAsInt());
            limpiar(5);
        } catch (RuntimeException error) {
            LOG.warn("No se pudo liberar un respaldo temporal; la limpieza periódica lo reintentará");
        }
    }

    @Override
    public OptionalLong tamano(String clave) {
        OptionalLong local = disco.tamano(clave);
        if (local.isPresent() || !DEFINITIVA.matcher(clave).matches() || ausenteReciente(clave)) { return local; }
        try {
            Manifiesto m = manifiesto(clave);
            if (m == null) { marcarAusente(clave); return OptionalLong.empty(); }
            return OptionalLong.of(m.tamano());
        } catch (UncheckedIOException error) {
            LOG.warn("No se pudo consultar el almacenamiento temporal; el archivo se trata como no disponible");
            return OptionalLong.empty();
        }
    }

    @Override
    public InputStream abrir(String clave) {
        if (disco.tamano(clave).isEmpty() && DEFINITIVA.matcher(clave).matches()) { rehidratar(clave); }
        return disco.abrir(clave);
    }

    /** Une los fragmentos en un único archivo del disco, verificando cada uno y el total; nunca sobrescribe otro. */
    private synchronized void rehidratar(String clave) {
        if (disco.tamano(clave).isPresent()) { return; }
        Manifiesto m = manifiesto(clave);
        if (m == null) { throw new UncheckedIOException(new java.nio.file.NoSuchFileException("respaldo")); }
        Iterator<Integer> indices = java.util.stream.IntStream.range(0, m.partes().size()).iterator();
        java.util.Enumeration<InputStream> flujos = new java.util.Enumeration<>() {
            @Override public boolean hasMoreElements() { return indices.hasNext(); }
            @Override public InputStream nextElement() {
                int i = indices.next();
                Parte p = m.partes().get(i);
                return new Verificado(remoto.descargar(parte(clave, i)), p.tamano(), p.sha256());
            }
        };
        ArchivoRecibido recibido;
        try (InputStream unido = new SequenceInputStream(flujos)) {
            recibido = disco.recibir(unido, limiteBytes);
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        try {
            if (recibido.tamanoBytes() != m.tamano() || !recibido.sha256().equals(m.sha256())) {
                throw new UncheckedIOException(new IOException("El respaldo reconstruido no coincide"));
            }
            disco.confirmar(recibido, clave);
        } finally {
            disco.descartar(recibido.claveTemporal());
        }
        ausentes.remove(clave);
    }

    /** Manifiesto validado, o null si no existe (respaldo inexistente, incompleto o ya liberado). */
    private Manifiesto manifiesto(String clave) {
        byte[] bytes;
        try (InputStream flujo = remoto.descargar(clave + MANIFIESTO)) {
            bytes = flujo.readNBytes(1024 * 1024 + 1);
        } catch (UncheckedIOException error) {
            if (error.getCause() instanceof java.nio.file.NoSuchFileException) { return null; }
            throw error;
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        }
        try {
            if (bytes.length > 1024 * 1024) { throw new IOException("Manifiesto demasiado grande"); }
            JsonNode raiz = json.readTree(bytes);
            long tamano = raiz.path("tamano").asLong(-1);
            String sha = raiz.path("sha256").asString("");
            JsonNode partes = raiz.path("partes");
            if (raiz.path("version").asInt(0) != VERSION || tamano < 1 || tamano > limiteBytes
                    || !SHA256.matcher(sha).matches() || !partes.isArray() || partes.isEmpty()
                    || partes.size() > 10000) {
                throw new IOException("Manifiesto inválido");
            }
            List<Parte> lista = new ArrayList<>();
            long suma = 0;
            for (JsonNode p : partes) {
                long t = p.path("tamano").asLong(-1);
                String h = p.path("sha256").asString("");
                if (t < 1 || t > FRAGMENTO_MAXIMO || !SHA256.matcher(h).matches()) {
                    throw new IOException("Manifiesto inválido");
                }
                suma += t;
                lista.add(new Parte(t, h));
            }
            if (suma != tamano) { throw new IOException("Manifiesto inválido"); }
            return new Manifiesto(tamano, sha, List.copyOf(lista));
        } catch (IOException | RuntimeException error) {
            throw new UncheckedIOException(new IOException("Manifiesto de respaldo inválido"));
        }
    }

    /** Borra las claves que ya no necesitan respaldo (COPIADO, liberadas o subidas nunca registradas). Nunca lanza. */
    private void limpiar(int limite) {
        try {
            for (RegistroRespaldo.Pendiente p : registro.paraLimpiar(limite)) {
                registro.marcarLiberando(p.clave());
                borrarObjetos(p.clave(), p.partes());
            }
        } catch (RuntimeException error) {
            LOG.warn("No se pudo completar la limpieza del almacenamiento temporal");
        }
    }

    /** Primero el manifiesto (deja de ser recuperable) y después los fragmentos; luego la fila de cuota. */
    private void borrarObjetos(String clave, int partes) {
        try {
            remoto.borrar(List.of(clave + MANIFIESTO));
            List<String> objetos = new ArrayList<>();
            for (int i = 0; i < partes; i++) { objetos.add(parte(clave, i)); }
            for (int i = 0; i < objetos.size(); i += 100) {
                remoto.borrar(objetos.subList(i, Math.min(objetos.size(), i + 100)));
            }
            registro.borrar(clave);
            marcarAusente(clave);
        } catch (RuntimeException error) {
            LOG.warn("No se pudieron borrar objetos del almacenamiento temporal; se reintentará");
        }
    }

    /** SHA-256 de cada fragmento y del total, leyendo el temporal una vez; el total debe ser el ya calculado. */
    private List<String> huellas(ArchivoRecibido recibido, int partes) {
        try (InputStream entrada = disco.abrir(recibido.claveTemporal())) {
            MessageDigest total = MessageDigest.getInstance("SHA-256");
            List<String> resultado = new ArrayList<>();
            byte[] bloque = new byte[BLOQUE];
            for (int i = 0; i < partes; i++) {
                MessageDigest parcial = MessageDigest.getInstance("SHA-256");
                long faltan = Math.min(fragmento, recibido.tamanoBytes() - i * fragmento);
                while (faltan > 0) {
                    int n = entrada.read(bloque, 0, (int) Math.min(bloque.length, faltan));
                    if (n < 0) { throw new IOException("Archivo temporal incompleto"); }
                    parcial.update(bloque, 0, n);
                    total.update(bloque, 0, n);
                    faltan -= n;
                }
                resultado.add(HexFormat.of().formatHex(parcial.digest()));
            }
            if (entrada.read() != -1
                    || !HexFormat.of().formatHex(total.digest()).equals(recibido.sha256().toLowerCase(Locale.ROOT))) {
                throw new IOException("El archivo temporal cambió");
            }
            return resultado;
        } catch (IOException error) {
            throw new UncheckedIOException(error);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 no disponible", error);
        }
    }

    private InputStream rango(String claveTemporal, long inicio, long largo) {
        InputStream entrada = disco.abrir(claveTemporal);
        try {
            entrada.skipNBytes(inicio);
        } catch (IOException error) {
            try { entrada.close(); } catch (IOException ignorado) { }
            throw new UncheckedIOException(error);
        }
        return new Limitado(entrada, largo);
    }

    private static String parte(String clave, int indice) {
        return clave + String.format(Locale.ROOT, "/parte-%05d", indice);
    }

    private boolean ausenteReciente(String clave) {
        Instant desde = ausentes.get(clave);
        return desde != null && Instant.now().isBefore(desde.plus(AUSENCIA));
    }

    private void marcarAusente(String clave) {
        if (ausentes.size() >= MAXIMO_AUSENTES) { ausentes.clear(); }
        ausentes.put(clave, Instant.now());
    }

    private static void exigirDefinitiva(String clave) {
        if (clave == null || !DEFINITIVA.matcher(clave).matches()) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida.");
        }
    }

    private record Parte(long tamano, String sha256) { }

    private record Manifiesto(long tamano, String sha256, List<Parte> partes) { }

    /** Entrega como máximo {@code restante} bytes del flujo subyacente. */
    private static final class Limitado extends FilterInputStream {
        private long restante;
        Limitado(InputStream entrada, long restante) { super(entrada); this.restante = restante; }
        @Override public int read() throws IOException {
            if (restante <= 0) { return -1; }
            int b = super.read();
            if (b >= 0) { restante--; }
            return b;
        }
        @Override public int read(byte[] b, int desde, int largo) throws IOException {
            if (restante <= 0) { return -1; }
            int n = super.read(b, desde, (int) Math.min(largo, restante));
            if (n > 0) { restante -= n; }
            return n;
        }
    }

    /** Comprueba tamaño y SHA-256 de un fragmento al terminar de leerlo; si no coincide, la lectura falla. */
    private static final class Verificado extends FilterInputStream {
        private final long esperado;
        private final String sha;
        private final MessageDigest resumen;
        private long leidos;
        private boolean comprobado;

        Verificado(InputStream entrada, long esperado, String sha) {
            super(entrada);
            this.esperado = esperado;
            this.sha = sha;
            try { this.resumen = MessageDigest.getInstance("SHA-256"); }
            catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
        }
        @Override public int read() throws IOException {
            byte[] uno = new byte[1];
            int n = read(uno, 0, 1);
            return n < 0 ? -1 : uno[0] & 0xff;
        }
        @Override public int read(byte[] b, int desde, int largo) throws IOException {
            int n = super.read(b, desde, largo);
            if (n > 0) {
                leidos += n;
                if (leidos > esperado) { throw new IOException("Fragmento más largo de lo esperado"); }
                resumen.update(b, desde, n);
            } else if (n < 0 && !comprobado) {
                comprobado = true;
                if (leidos != esperado || !HexFormat.of().formatHex(resumen.digest()).equals(sha)) {
                    throw new IOException("Fragmento dañado");
                }
            }
            return n;
        }
    }
}
