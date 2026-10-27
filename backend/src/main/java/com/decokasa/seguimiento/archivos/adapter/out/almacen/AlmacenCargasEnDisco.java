package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenCargas;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Solo bytes técnicos en .cargas del volumen existente. Nunca usa nombres del usuario como rutas. */
public final class AlmacenCargasEnDisco implements AlmacenCargas {
    private final Path base;
    public AlmacenCargasEnDisco(Path directorio) {
        base=directorio.toAbsolutePath().normalize().resolve(".cargas");
    }
    private Path ruta(UUID id, String sufijo) throws IOException {
        Files.createDirectories(base);
        if (Files.isSymbolicLink(base) || Files.isSymbolicLink(base.getParent())) {
            throw new IOException("Almacenamiento no disponible");
        }
        Path ruta=base.resolve(id.toString()+sufijo);
        if (Files.isSymbolicLink(ruta)) { throw new IOException("Almacenamiento no disponible"); }
        return ruta;
    }
    @Override public Bloqueo bloquear(UUID id) {
        FileChannel canal=null;
        try {
            canal=FileChannel.open(ruta(id,".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
            FileLock lock=canal.tryLock();
            if (lock==null) { cerrar(canal); throw ProblemaCarga.ocupada(); }
            FileChannel abierto=canal;
            return () -> {
                try { lock.release(); }
                catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
                finally { cerrar(abierto); }
            };
        } catch (OverlappingFileLockException error) {
            if (canal!=null) { cerrar(canal); }
            throw ProblemaCarga.ocupada();
        } catch (IOException error) {
            if (canal!=null) { cerrar(canal); }
            throw ProblemaCarga.almacenamiento();
        }
    }
    private static void cerrar(FileChannel canal) {
        try { canal.close(); } catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
    }
    @Override public void reconciliar(UUID id, long offset) {
        try (FileChannel canal=FileChannel.open(ruta(id,".parte"),StandardOpenOption.CREATE,
                StandardOpenOption.READ,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) {
            if (canal.size()<offset) { throw ProblemaCarga.almacenamiento(); }
            if (canal.size()>offset) { canal.truncate(offset); canal.force(true); }
        } catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
    }
    @Override public void escribir(UUID id, long inicio, byte[] contenido) {
        try (FileChannel canal=FileChannel.open(ruta(id,".parte"),StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) {
            if (canal.size()!=inicio) { throw ProblemaCarga.conflicto(); }
            canal.position(inicio);
            ByteBuffer buffer=ByteBuffer.wrap(contenido);
            while (buffer.hasRemaining()) { canal.write(buffer); }
            canal.force(true);
        } catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
    }
    @Override public void validar(UUID id, List<CargaArchivo.Fragmento> fragmentos, long total) {
        try (InputStream entrada=abrir(id)) {
            long offset=0;
            for (CargaArchivo.Fragmento fragmento:fragmentos) {
                if (fragmento.inicio()!=offset || fragmento.tamano()!=Math.min(CargaArchivo.FRAGMENTO_BYTES,total-offset)) {
                    throw ProblemaCarga.conflicto();
                }
                byte[] bytes=entrada.readNBytes(fragmento.tamano());
                if (bytes.length!=fragmento.tamano() || !HexFormat.of().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(bytes)).equals(fragmento.sha256())) {
                    throw ProblemaCarga.almacenamiento();
                }
                offset+=bytes.length;
            }
            if (offset!=total || entrada.read()!=-1) { throw ProblemaCarga.incompleta(); }
        } catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException("SHA-256 no disponible"); }
    }
    @Override public InputStream abrir(UUID id) {
        try { return Files.newInputStream(ruta(id,".parte"),LinkOption.NOFOLLOW_LINKS); }
        catch (IOException error) { throw ProblemaCarga.almacenamiento(); }
    }
}
