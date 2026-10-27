package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lectura acotada del directorio de un contenedor OLE/CFB (MS-CFB): solo los nombres de los streams que cuelgan de la
 * raíz (los almacenes no cuentan: un almacén llamado {@code Workbook} no es un libro). No lee el contenido de ningún stream. Un contenedor dañado, con cadenas en bucle o fuera de los límites no
 * tiene directorio legible y devuelve un conjunto vacío.
 */
final class DirectorioOle {

    private static final long FIN_CADENA = 0xFFFFFFFEL;
    private static final long MAXIMO_SECTOR = 0xFFFFFFFAL;
    private static final int SIN_ENTRADA = -1;
    private static final int ENTRADA = 128;
    private static final int STREAM = 2;
    private static final int DIFAT_CABECERA = 109;
    /** Presupuesto del directorio: 4 MiB son decenas de miles de entradas, muy por encima de un libro real. */
    private static final long MAXIMO_DIRECTORIO = 4L * 1024 * 1024;

    private DirectorioOle() {
    }

    /** Nombres (en mayúsculas, como los compara CFB) de los streams hijos de la raíz. */
    static Set<String> streamsRaiz(Path archivo) {
        try (FileChannel canal = FileChannel.open(archivo, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            return leer(canal);
        } catch (IOException | RuntimeException error) {
            return Set.of();
        }
    }

    private static Set<String> leer(FileChannel canal) throws IOException {
        long tamano = canal.size();
        ByteBuffer cabecera = leer(canal, 0, 512);
        int version = cabecera.getShort(0x1A) & 0xFFFF;
        int desplazamiento = cabecera.getShort(0x1E) & 0xFFFF;
        if ((cabecera.getShort(0x1C) & 0xFFFF) != 0xFFFE
                || !((version == 3 && desplazamiento == 9) || (version == 4 && desplazamiento == 12))) {
            return Set.of();
        }
        int sector = 1 << desplazamiento;
        long sectores = (tamano - sector) / sector;
        long sectoresFat = cabecera.getInt(0x2C) & 0xFFFFFFFFL;
        if (sectores <= 0 || sectoresFat == 0 || sectoresFat > sectores) {
            return Set.of();
        }
        List<Long> fat = sectoresFat(canal, cabecera, sector, sectores, sectoresFat);
        List<ByteBuffer> entradas = directorio(canal, cabecera.getInt(0x30) & 0xFFFFFFFFL, fat, sector, sectores);
        if (entradas.isEmpty() || tipo(entradas.get(0)) != 5) {
            return Set.of();
        }
        return hijosDeRaiz(entradas);
    }

    /** Sectores de la FAT: los 109 de la cabecera y, si hace falta, la cadena DIFAT. */
    private static List<Long> sectoresFat(FileChannel canal, ByteBuffer cabecera, int sector, long sectores,
            long sectoresFat) throws IOException {
        List<Long> fat = new ArrayList<>();
        for (int i = 0; i < DIFAT_CABECERA && fat.size() < sectoresFat; i++) {
            fat.add(valido(cabecera.getInt(0x4C + i * 4) & 0xFFFFFFFFL, sectores));
        }
        long difat = cabecera.getInt(0x44) & 0xFFFFFFFFL;
        int porSector = sector / 4 - 1;
        Set<Long> vistos = new HashSet<>();
        while (fat.size() < sectoresFat) {
            if (!vistos.add(valido(difat, sectores))) {
                throw new IllegalStateException("Cadena DIFAT en bucle.");
            }
            ByteBuffer bloque = leer(canal, (difat + 1) * sector, sector);
            for (int i = 0; i < porSector && fat.size() < sectoresFat; i++) {
                fat.add(valido(bloque.getInt(i * 4) & 0xFFFFFFFFL, sectores));
            }
            difat = bloque.getInt(porSector * 4) & 0xFFFFFFFFL;
        }
        return fat;
    }

    /** Entradas del directorio siguiendo su cadena en la FAT, con detección de bucles y presupuesto. */
    private static List<ByteBuffer> directorio(FileChannel canal, long inicio, List<Long> fat, int sector,
            long sectores) throws IOException {
        List<ByteBuffer> entradas = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        int porFat = sector / 4;
        for (long actual = inicio; actual != FIN_CADENA; ) {
            if (!vistos.add(valido(actual, sectores)) || (long) vistos.size() * sector > MAXIMO_DIRECTORIO) {
                throw new IllegalStateException("Directorio en bucle o demasiado grande.");
            }
            ByteBuffer bloque = leer(canal, (actual + 1) * sector, sector);
            for (int desde = 0; desde + ENTRADA <= sector; desde += ENTRADA) {
                entradas.add(bloque.slice(desde, ENTRADA).order(ByteOrder.LITTLE_ENDIAN));
            }
            int indice = (int) (actual / porFat);
            if (indice >= fat.size()) {
                throw new IllegalStateException("Sector fuera de la FAT.");
            }
            actual = leer(canal, (fat.get(indice) + 1) * sector + (actual % porFat) * 4, 4).getInt(0) & 0xFFFFFFFFL;
        }
        return entradas;
    }

    /** Recorre el árbol de hermanos que cuelga del hijo de la raíz (sin bajar a los almacenes); solo guarda streams. */
    private static Set<String> hijosDeRaiz(List<ByteBuffer> entradas) {
        Set<String> nombres = new HashSet<>();
        Set<Integer> vistos = new HashSet<>();
        Deque<Integer> pendientes = new ArrayDeque<>();
        pendientes.push(entradas.get(0).getInt(0x4C));
        while (!pendientes.isEmpty()) {
            int id = pendientes.pop();
            if (id == SIN_ENTRADA) {
                continue;
            }
            if (id < 0 || id >= entradas.size() || !vistos.add(id)) {
                throw new IllegalStateException("Árbol del directorio inválido.");
            }
            ByteBuffer entrada = entradas.get(id);
            if (tipo(entrada) == STREAM) {
                nombres.add(nombre(entrada));
            }
            pendientes.push(entrada.getInt(0x44));
            pendientes.push(entrada.getInt(0x48));
        }
        return nombres;
    }

    private static int tipo(ByteBuffer entrada) {
        return entrada.get(0x42) & 0xFF;
    }

    private static String nombre(ByteBuffer entrada) {
        int longitud = entrada.getShort(0x40) & 0xFFFF;
        if (longitud < 2 || longitud > 64 || longitud % 2 != 0) {
            throw new IllegalStateException("Nombre de entrada inválido.");
        }
        byte[] bytes = new byte[longitud - 2];
        entrada.get(0, bytes);
        return new String(bytes, StandardCharsets.UTF_16LE).toUpperCase(Locale.ROOT);
    }

    private static long valido(long numero, long sectores) {
        if (numero > MAXIMO_SECTOR || numero >= sectores) {
            throw new IllegalStateException("Sector fuera del archivo.");
        }
        return numero;
    }

    private static ByteBuffer leer(FileChannel canal, long posicion, int longitud) throws IOException {
        ByteBuffer bloque = ByteBuffer.allocate(longitud).order(ByteOrder.LITTLE_ENDIAN);
        while (bloque.hasRemaining()) {
            if (canal.read(bloque, posicion + bloque.position()) < 0) {
                throw new IOException("Contenedor truncado.");
            }
        }
        return bloque.flip();
    }
}
