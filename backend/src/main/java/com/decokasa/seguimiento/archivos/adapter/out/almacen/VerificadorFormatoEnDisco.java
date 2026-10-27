package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import com.decokasa.seguimiento.archivos.application.port.out.VerificadorFormato;
import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;
import com.decokasa.seguimiento.archivos.domain.TipoArchivoNoAceptado;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Comprobación técnica del contenido por formato (ADR-0026), sobre el archivo completo del almacenamiento local:
 * <ul>
 *   <li>PDF: {@code %PDF-} en los primeros 1024 bytes (la tolerancia habitual de los lectores).</li>
 *   <li>PNG y JPEG: su firma.</li>
 *   <li>XLS: contenedor OLE con un libro BIFF ({@code Workbook} o {@code Book}) en la raíz de su directorio. Otro OLE
 *       (por ejemplo, un documento de Word) no es un XLS.</li>
 *   <li>XLSX: ZIP con la estructura OOXML de un libro sin macros (tipos de contenido, relación principal y parte
 *       {@code <workbook>}), analizada como XML bien formado y acotado. Un XLSX cifrado con contraseña es un OLE con
 *       {@code EncryptionInfo} y {@code EncryptedPackage} y también se admite; un XLS no es un XLSX.</li>
 *   <li>CSV: texto de 8 bits sin bytes NUL (UTF-8, Windows-1252…) o UTF-16 con BOM, y sin la firma de otro formato
 *       (la de PDF, con su misma tolerancia de 1024 bytes).</li>
 * </ul>
 * No interpreta datos ni valida el contenido comercial. No sigue enlaces simbólicos ni sale del directorio base.
 */
public final class VerificadorFormatoEnDisco implements VerificadorFormato {

    private static final int BLOQUE = 64 * 1024;
    private static final int CABECERA_PDF = 1024;
    private static final int MAXIMO_TIPOS = 1024 * 1024;
    private static final int MAXIMO_LIBRO = 4 * 1024 * 1024;
    private static final int MAXIMO_ENTRADAS_ZIP = 10_000;
    private static final String LIBRO_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml";
    private static final String TIPOS_OPC = "http://schemas.openxmlformats.org/package/2006/content-types";
    private static final String RELACIONES_OPC = "http://schemas.openxmlformats.org/package/2006/relationships";
    private static final Set<String> RELACION_DOCUMENTO = Set.of(
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument",
            "http://purl.oclc.org/ooxml/officeDocument/relationships/officeDocument");
    private static final Set<String> LIBRO_SML = Set.of(
            "http://schemas.openxmlformats.org/spreadsheetml/2006/main",
            "http://purl.oclc.org/ooxml/spreadsheetml/main");
    private static final int[] PDF = {0x25, 0x50, 0x44, 0x46, 0x2D};
    private static final int[] ZIP = {0x50, 0x4B, 0x03, 0x04};
    private static final int[] OLE = {0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1};
    private static final int[] PNG = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int[] JPEG = {0xFF, 0xD8, 0xFF};
    private static final int[] GIF = {0x47, 0x49, 0x46, 0x38};
    private static final int[] UTF16_LE = {0xFF, 0xFE};
    private static final int[] UTF16_BE = {0xFE, 0xFF};

    private final Path base;

    public VerificadorFormatoEnDisco(Path directorio) {
        this.base = directorio.toAbsolutePath().normalize();
    }

    @Override
    public void verificar(String clave, FormatoArchivo formato) {
        Path archivo = ruta(clave);
        if (!Files.isRegularFile(archivo, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalStateException("El archivo recibido no está disponible.");
        }
        try {
            byte[] inicio = inicio(archivo);
            boolean valido = switch (formato) {
                case PDF -> contiene(inicio, PDF);
                case PNG -> empieza(inicio, PNG);
                case JPEG -> empieza(inicio, JPEG);
                case XLS -> empieza(inicio, OLE) && esLibroXls(DirectorioOle.streamsRaiz(archivo));
                case XLSX -> empieza(inicio, OLE)
                        ? esXlsxCifrado(DirectorioOle.streamsRaiz(archivo))
                        : empieza(inicio, ZIP) && esLibroXlsx(archivo);
                case CSV -> esTexto(archivo, inicio);
            };
            if (!valido) {
                throw new TipoArchivoNoAceptado();
            }
        } catch (IOException error) {
            throw new UncheckedIOException("No se pudo comprobar el archivo recibido.", error);
        }
    }

    private static byte[] inicio(Path archivo) throws IOException {
        try (InputStream entrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            return entrada.readNBytes(CABECERA_PDF);
        }
    }

    /** XLS: libro BIFF ({@code Workbook}, o {@code Book} en versiones antiguas) en la raíz del contenedor OLE. */
    private static boolean esLibroXls(Set<String> raiz) {
        return (raiz.contains("WORKBOOK") || raiz.contains("BOOK")) && !esXlsxCifrado(raiz);
    }

    /**
     * XLSX cifrado con contraseña (MS-OFFCRYPTO): OLE con {@code EncryptionInfo} y {@code EncryptedPackage} y sin libro
     * BIFF. El paquete cifrado no se puede abrir sin la contraseña; se admite por la política aprobada (ADR-0026).
     */
    private static boolean esXlsxCifrado(Set<String> raiz) {
        return raiz.contains("ENCRYPTIONINFO") && raiz.contains("ENCRYPTEDPACKAGE")
                && !raiz.contains("WORKBOOK") && !raiz.contains("BOOK");
    }

    /**
     * Estructura OOXML de un libro sin macros: {@code [Content_Types].xml} declara exactamente una parte principal de
     * libro, {@code _rels/.rels} la señala como documento principal y esa parte es un {@code <workbook>} de
     * SpreadsheetML. Las tres partes se leen con límite y se analizan como XML bien formado, sin DTD ni entidades. Un
     * tipo de contenido con macros o proyecto VBA rechaza el archivo. No se descomprime el resto del ZIP.
     */
    private static boolean esLibroXlsx(Path archivo) {
        try (ZipFile zip = new ZipFile(archivo.toFile(), StandardCharsets.UTF_8)) {
            // Los nombres de partes de OOXML no distinguen mayúsculas.
            Map<String, ZipEntry> partes = new HashMap<>();
            int cuenta = 0;
            for (Enumeration<? extends ZipEntry> entradas = zip.entries(); entradas.hasMoreElements(); ) {
                ZipEntry entrada = entradas.nextElement();
                if (++cuenta > MAXIMO_ENTRADAS_ZIP) {
                    return false;
                }
                String nombre = entrada.getName().toLowerCase(Locale.ROOT);
                // Proyecto VBA (macros) en cualquier ruta, aunque los tipos de contenido no lo declaren.
                if (nombre.endsWith("vbaproject.bin")) {
                    return false;
                }
                if (!entrada.isDirectory() && partes.putIfAbsent(nombre, entrada) != null) {
                    return false;
                }
            }
            String libro = analizar(xml(zip, partes.get("[content_types].xml"), MAXIMO_TIPOS),
                    VerificadorFormatoEnDisco::parteDelLibro);
            return libro != null
                    && libro.equals(analizar(xml(zip, partes.get("_rels/.rels"), MAXIMO_TIPOS),
                            VerificadorFormatoEnDisco::documentoPrincipal))
                    && Boolean.TRUE.equals(analizar(xml(zip, partes.get(libro), MAXIMO_LIBRO),
                            VerificadorFormatoEnDisco::esWorkbook));
        } catch (IOException | XMLStreamException | RuntimeException error) {
            // ZIP dañado, XML mal formado, DTD, partes ausentes o demasiado grandes: no es un libro XLSX legible.
            return false;
        }
    }

    /** Nombre de la única parte con el tipo de libro sin macros; nulo si falta, se repite o hay macros. */
    private static String parteDelLibro(XMLStreamReader tipos) throws XMLStreamException {
        String parte = null;
        int libros = 0;
        boolean raiz = true;
        while (tipos.hasNext()) {
            if (siguiente(tipos) != XMLStreamConstants.START_ELEMENT) {
                continue;
            }
            if (raiz && !(TIPOS_OPC.equals(tipos.getNamespaceURI()) && "Types".equals(tipos.getLocalName()))) {
                return null;
            }
            raiz = false;
            String tipo = tipos.getAttributeValue(null, "ContentType");
            if (tipo != null) {
                String minusculas = tipo.toLowerCase(Locale.ROOT);
                if (minusculas.contains("macroenabled") || minusculas.contains("vbaproject")) {
                    return null;
                }
                if ("Override".equals(tipos.getLocalName()) && LIBRO_XLSX.equals(tipo.strip())) {
                    libros++;
                    parte = nombreDeParte(tipos.getAttributeValue(null, "PartName"));
                }
            }
        }
        return libros == 1 ? parte : null;
    }

    /** Destino de la relación {@code officeDocument} de la raíz del paquete. */
    private static String documentoPrincipal(XMLStreamReader relaciones) throws XMLStreamException {
        String destino = null;
        boolean raiz = true;
        while (relaciones.hasNext()) {
            if (siguiente(relaciones) != XMLStreamConstants.START_ELEMENT) {
                continue;
            }
            if (raiz && !(RELACIONES_OPC.equals(relaciones.getNamespaceURI())
                    && "Relationships".equals(relaciones.getLocalName()))) {
                return null;
            }
            raiz = false;
            String tipo = relaciones.getAttributeValue(null, "Type");
            if ("Relationship".equals(relaciones.getLocalName()) && tipo != null
                    && RELACION_DOCUMENTO.contains(tipo.strip())
                    && !"External".equals(relaciones.getAttributeValue(null, "TargetMode"))) {
                if (destino != null) {
                    return null;
                }
                destino = nombreDeParte(relaciones.getAttributeValue(null, "Target"));
            }
        }
        return destino;
    }

    /** La parte entera debe ser XML bien formado con raíz {@code <workbook>} de SpreadsheetML. */
    private static boolean esWorkbook(XMLStreamReader libro) throws XMLStreamException {
        boolean workbook = false;
        boolean raiz = true;
        while (libro.hasNext()) {
            if (siguiente(libro) == XMLStreamConstants.START_ELEMENT && raiz) {
                raiz = false;
                workbook = "workbook".equals(libro.getLocalName()) && LIBRO_SML.contains(libro.getNamespaceURI());
            }
        }
        return workbook;
    }

    /** Analiza una parte y cierra su lector en cualquier caso. */
    private static <T> T analizar(XMLStreamReader lector, Analisis<T> analisis) throws XMLStreamException {
        try {
            return analisis.aplicar(lector);
        } finally {
            lector.close();
        }
    }

    @FunctionalInterface
    private interface Analisis<T> {
        T aplicar(XMLStreamReader lector) throws XMLStreamException;
    }

    /** Siguiente evento; una DTD o una referencia a entidad hacen que el XML no se acepte. */
    private static int siguiente(XMLStreamReader lector) throws XMLStreamException {
        int evento = lector.next();
        if (evento == XMLStreamConstants.DTD || evento == XMLStreamConstants.ENTITY_REFERENCE) {
            throw new XMLStreamException("DTD o entidad no permitida.");
        }
        return evento;
    }

    /** Lector StAX de una parte leída con límite; sin DTD, sin entidades externas ni acceso a archivos o red. */
    private static XMLStreamReader xml(ZipFile zip, ZipEntry parte, int maximo) throws IOException, XMLStreamException {
        if (parte == null) {
            throw new IOException("Parte OOXML ausente.");
        }
        byte[] datos;
        try (InputStream entrada = zip.getInputStream(parte)) {
            datos = entrada.readNBytes(maximo + 1);
        }
        if (datos.length > maximo) {
            throw new IOException("Parte OOXML demasiado grande.");
        }
        XMLInputFactory fabrica = XMLInputFactory.newDefaultFactory();
        fabrica.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        fabrica.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        fabrica.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        fabrica.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return fabrica.createXMLStreamReader(new ByteArrayInputStream(datos));
    }

    /** Nombre de parte OPC sin la barra inicial y en minúsculas; nulo si no es una ruta interna simple. */
    private static String nombreDeParte(String nombre) {
        if (nombre == null) {
            return null;
        }
        String parte = nombre.strip();
        parte = parte.startsWith("/") ? parte.substring(1) : parte;
        if (parte.isEmpty() || parte.contains("..") || parte.contains("\\") || parte.contains(":")) {
            return null;
        }
        return parte.toLowerCase(Locale.ROOT);
    }

    /**
     * CSV: sin firma binaria conocida y sin NUL, salvo UTF-16 con BOM (longitud par). La firma PDF se busca con la
     * misma tolerancia de 1024 bytes que acepta un PDF: un PDF con prefijo no pasa como CSV.
     */
    private static boolean esTexto(Path archivo, byte[] inicio) throws IOException {
        if (contiene(inicio, PDF) || empieza(inicio, ZIP) || empieza(inicio, OLE) || empieza(inicio, PNG)
                || empieza(inicio, JPEG) || empieza(inicio, GIF)) {
            return false;
        }
        if (empieza(inicio, UTF16_LE) || empieza(inicio, UTF16_BE)) {
            return Files.size(archivo) % 2 == 0;
        }
        try (InputStream entrada = Files.newInputStream(archivo, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bloque = new byte[BLOQUE];
            int leidos;
            while ((leidos = entrada.read(bloque)) != -1) {
                for (int i = 0; i < leidos; i++) {
                    if (bloque[i] == 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean empieza(byte[] datos, int[] firma) {
        if (datos.length < firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((datos[i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }

    /** {@code firma} en cualquier posición de {@code datos} (cabecera PDF dentro del primer KiB). */
    private static boolean contiene(byte[] datos, int[] firma) {
        for (int desde = 0; desde + firma.length <= datos.length; desde++) {
            boolean igual = true;
            for (int i = 0; i < firma.length && igual; i++) {
                igual = (datos[desde + i] & 0xFF) == firma[i];
            }
            if (igual) {
                return true;
            }
        }
        return false;
    }

    /** Las claves las genera el servidor; aun así, nunca se sale del directorio base. */
    private Path ruta(String clave) {
        Path ruta = base.resolve(clave).normalize();
        if (!ruta.startsWith(base) || ruta.equals(base)) {
            throw new IllegalArgumentException("Clave de almacenamiento inválida.");
        }
        return ruta;
    }
}
