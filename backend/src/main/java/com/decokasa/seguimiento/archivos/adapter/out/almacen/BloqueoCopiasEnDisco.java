package com.decokasa.seguimiento.archivos.adapter.out.almacen;

import com.decokasa.seguimiento.archivos.application.port.out.BloqueoCopiaLocal;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.UUID;

/** Segundo cerrojo junto al lease: un worker pausado no se solapa con otro al caducar el lease. */
public final class BloqueoCopiasEnDisco implements BloqueoCopiaLocal {
    private final Path base;
    public BloqueoCopiasEnDisco(Path directorio) { base=directorio.toAbsolutePath().normalize().resolve(".copias-locks"); }
    @Override public Optional<Bloqueo> intentar(UUID id) {
        FileChannel canal=null;
        try {
            Files.createDirectories(base);
            Path ruta=base.resolve(id+".lock");
            if (Files.isSymbolicLink(base) || Files.isSymbolicLink(base.getParent()) || Files.isSymbolicLink(ruta)) {
                throw new FalloIntegracionGoogle(Causa.ALMACEN_LOCAL);
            }
            canal=FileChannel.open(ruta,StandardOpenOption.CREATE,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS);
            FileLock lock=canal.tryLock();
            if (lock==null) { cerrar(canal);return Optional.empty(); }
            FileChannel abierto=canal;
            return Optional.of(() -> {
                try { lock.release(); }
                catch (IOException error) { throw new FalloIntegracionGoogle(Causa.ALMACEN_LOCAL); }
                finally { cerrar(abierto); }
            });
        } catch (OverlappingFileLockException error) {
            if (canal!=null) { cerrar(canal); } return Optional.empty();
        } catch (IOException error) {
            if (canal!=null) { cerrar(canal); } throw new FalloIntegracionGoogle(Causa.ALMACEN_LOCAL);
        }
    }
    private static void cerrar(FileChannel canal) {
        try { canal.close(); } catch (IOException error) { throw new FalloIntegracionGoogle(Causa.ALMACEN_LOCAL); }
    }
}
