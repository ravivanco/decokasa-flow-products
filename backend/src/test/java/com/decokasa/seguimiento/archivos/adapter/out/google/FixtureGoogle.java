package com.decokasa.seguimiento.archivos.adapter.out.google;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
final class FixtureGoogle {
    private FixtureGoogle() {}
    static void privado(Path ruta) throws IOException {
        var posix=Files.getFileAttributeView(ruta,PosixFileAttributeView.class);
        if(posix!=null) {posix.setPermissions(EnumSet.of(PosixFilePermission.OWNER_READ,PosixFilePermission.OWNER_WRITE,PosixFilePermission.OWNER_EXECUTE));return;}
        var acl=Files.getFileAttributeView(ruta,AclFileAttributeView.class);
        acl.setAcl(List.of(AclEntry.newBuilder().setType(AclEntryType.ALLOW).setPrincipal(acl.getOwner()).setPermissions(EnumSet.allOf(AclEntryPermission.class)).build()));
    }
}
