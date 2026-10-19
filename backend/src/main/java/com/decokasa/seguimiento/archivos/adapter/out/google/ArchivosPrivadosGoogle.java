package com.decokasa.seguimiento.archivos.adapter.out.google;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Archivos fuera del árbol de trabajo, sin symlinks y con permisos privados. Falla de forma cerrada. */
final class ArchivosPrivadosGoogle {
    private ArchivosPrivadosGoogle() { }

    static Path externo(String configurado) throws IOException {
        Path ruta = Path.of(configurado);
        if (!ruta.isAbsolute()) { throw new IOException("Ruta absoluta requerida"); }
        ruta = ruta.normalize();
        for (Path actual = ruta; actual != null; actual = actual.getParent()) {
            if (Files.isSymbolicLink(actual) || Files.exists(actual.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("Almacenamiento externo requerido");
            }
        }
        // Resuelve también junctions/reparse points de Windows y vuelve a excluir árboles Git por su ruta real.
        Path existente = ruta;
        while (existente != null && !Files.exists(existente, LinkOption.NOFOLLOW_LINKS)) {
            existente = existente.getParent();
        }
        if (existente == null) { throw new IOException("Directorio externo no disponible"); }
        Path real = existente.toRealPath().resolve(existente.relativize(ruta)).normalize();
        for (Path actual = real; actual != null; actual = actual.getParent()) {
            if (Files.exists(actual.resolve(".git"), LinkOption.NOFOLLOW_LINKS)) {
                throw new IOException("El destino real está dentro de Git");
            }
        }
        ruta = real;
        return ruta;
    }

    static byte[] leer(Path ruta, int maximo) throws IOException {
        comprobar(ruta.getParent());
        comprobar(ruta);
        if (!Files.isRegularFile(ruta, LinkOption.NOFOLLOW_LINKS) || Files.size(ruta) > maximo) {
            throw new IOException("Archivo privado inválido");
        }
        return Files.readAllBytes(ruta);
    }

    static void prepararDirectorio(Path directorio) throws IOException {
        if (!Files.isDirectory(directorio, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Crea el directorio privado antes de conectar Google");
        }
        comprobar(directorio);
    }

    static void comprobar(Path ruta) throws IOException {
        if (Files.isSymbolicLink(ruta)) { throw new IOException("No se admiten enlaces"); }
        PosixFileAttributeView posix = Files.getFileAttributeView(ruta, PosixFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            Set<PosixFilePermission> permisos = posix.readAttributes().permissions();
            Set<PosixFilePermission> ajenos = EnumSet.of(PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE, PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ, PosixFilePermission.OTHERS_WRITE,
                    PosixFilePermission.OTHERS_EXECUTE);
            if (permisos.stream().anyMatch(ajenos::contains)) { throw new IOException("Permisos privados requeridos"); }
            return;
        }
        AclFileAttributeView acl = Files.getFileAttributeView(ruta, AclFileAttributeView.class,
                LinkOption.NOFOLLOW_LINKS);
        if (acl == null) { throw new IOException("Sistema de permisos no admitido"); }
        UserPrincipal propietario = acl.getOwner();
        for (AclEntry entrada : acl.getAcl()) {
            if (entrada.type() == AclEntryType.ALLOW && !entrada.principal().equals(propietario)) {
                throw new IOException("Solo el propietario debe tener acceso al secreto");
            }
        }
    }

    static void escribirAtomico(Path destino, byte[] datos) throws IOException {
        prepararDirectorio(destino.getParent());
        if (Files.exists(destino, LinkOption.NOFOLLOW_LINKS)) { comprobar(destino); }
        Path temporal = Files.createTempFile(destino.getParent(), ".google-", ".privado");
        try {
            PosixFileAttributeView posix = Files.getFileAttributeView(temporal, PosixFileAttributeView.class);
            if (posix != null) {
                posix.setPermissions(EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
            } else {
                AclFileAttributeView acl = Files.getFileAttributeView(temporal, AclFileAttributeView.class);
                if (acl == null) { throw new IOException("ACL no disponible"); }
                acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(acl.getOwner())
                        .setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
            }
            Files.write(temporal, datos, StandardOpenOption.TRUNCATE_EXISTING);
            try (FileChannel canal = FileChannel.open(temporal, StandardOpenOption.WRITE)) { canal.force(true); }
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporal); }
    }
}
