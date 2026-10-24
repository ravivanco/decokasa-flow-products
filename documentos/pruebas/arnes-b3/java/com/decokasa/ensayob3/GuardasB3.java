package com.decokasa.ensayob3;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/** Exclusivo del arnes. No abre red, no modifica ACL y no acepta otros receptores. */
public final class GuardasB3 {
    public static final String CUENTA = "importadora.latinoamericaworld@gmail.com";
    public static final long BYTES = 104857600L;
    private GuardasB3() { }

    static void exigir(boolean condicion) {
        if (!condicion) throw new IllegalStateException("Precondicion B3 no satisfecha; operacion bloqueada");
    }
    static void receptor(String correo) { exigir(CUENTA.equals(correo)); }
    /**
     * Reutilizar la base del ensayo. Un volumen existente exige las credenciales privadas que lo inicializaron: nunca se
     * generan otras. Una base ya inicializada exige su volumen: nunca se sustituye por una base vacia.
     */
    static void reutilizacion(boolean volumenExiste, boolean privadoNuevo, boolean baseInicializada) {
        exigir(!(volumenExiste && privadoNuevo) && (volumenExiste || !baseInicializada));
    }
    static boolean confirmar(String recibido, String esperado) { return esperado.equals(recibido); }
    static Map<String, Object> modo(boolean oauth, boolean drive, boolean gmail) {
        exigir(!(drive && gmail) && (!(drive || gmail) || oauth));
        return Map.of("decokasa.google.oauth.habilitada", oauth,
                "decokasa.google.drive.habilitada", drive, "decokasa.google.gmail.habilitada", gmail,
                "decokasa.google.drive.worker.automatico", false,
                "decokasa.google.gmail.worker.automatico", false);
    }
    static void contenido(long pedidos, long archivos, long bytes, String nombre) {
        exigir(pedidos == 1 && archivos == 1 && bytes == BYTES
                && "ficticio-3b3-100MiB.bin".equals(nombre));
    }
    static void correo(long avisos, String receptor, String estado, int intentos, boolean incierto) {
        receptor(receptor);
        exigir(avisos == 1 && "PENDIENTE".equals(estado) && intentos == 0 && !incierto);
    }
    static Path externo(Path ruta, Path repo) throws IOException {
        exigir(ruta.isAbsolute() && Files.isDirectory(ruta, LinkOption.NOFOLLOW_LINKS));
        for (Path p = ruta; p != null; p = p.getParent()) {
            exigir(!Files.isSymbolicLink(p) && !Files.exists(p.resolve(".git"), LinkOption.NOFOLLOW_LINKS));
        }
        Path real = ruta.toRealPath();
        exigir(!real.startsWith(repo.toRealPath()));
        for (Path p = real; p != null; p = p.getParent()) exigir(!Files.exists(p.resolve(".git")));
        privado(real);
        return real;
    }
    static void privado(Path ruta) throws IOException {
        exigir(!Files.isSymbolicLink(ruta));
        var posix = Files.getFileAttributeView(ruta, PosixFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
        if (posix != null) {
            exigir(posix.readAttributes().permissions().stream().noneMatch(p -> p.name().startsWith("GROUP_")
                    || p.name().startsWith("OTHERS_")));
        } else {
            var acl = Files.getFileAttributeView(ruta, AclFileAttributeView.class, LinkOption.NOFOLLOW_LINKS);
            exigir(acl != null);
            for (var e : acl.getAcl()) exigir(e.type() != AclEntryType.ALLOW || e.principal().equals(acl.getOwner()));
        }
    }
    static void consumirCorreo(Path marca) throws IOException {
        // CREATE_NEW antes del POST: crash, timeout o respuesta incierta nunca autorizan otro intento.
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
            Files.createFile(marca, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        else Files.createFile(marca);
        privado(marca);
        Files.writeString(marca, "Intento 3.B3 consumido; no borrar para reenviar.\n",
                StandardOpenOption.WRITE, StandardOpenOption.SYNC);
    }
}
