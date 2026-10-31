package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Operaciones remotas pasivas; el llamador conserva id/sesión y coordina las transacciones cortas. */
public interface AlmacenArchivos {
    String reservarId();
    Sesion iniciar(Copia copia);
    Sesion reiniciar(Copia copia);
    Progreso consultar(Sesion sesion, long total);
    Progreso enviarFragmento(Sesion sesion, long inicio, long total, byte[] fragmento);
    Optional<Remoto> metadatos(String id);
    Remoto crearCarpeta(String idReservado, String nombre, String padreId, String clave);

    /**
     * Contenido de un archivo ya copiado, por su ID persistido (ADR-0028), como flujo que el llamador debe cerrar; nunca
     * se carga entero en memoria. Fuera de transacciones. Los dobles anteriores no lo implementan: Drive no disponible.
     *
     * @throws com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle si Google falla o la integración está
     *         apagada
     */
    default InputStream descargar(String id) {
        throw new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.DESACTIVADA);
    }

    /**
     * ADR-0034: borra un archivo que esta aplicación acaba de subir y que NO llegó a registrarse (por ejemplo, rechazado
     * por la verificación técnica). Nunca se usa con un documento registrado. Idempotente: si ya no existe, no falla.
     */
    default void eliminar(String id) {
        throw new FalloIntegracionGoogle(FalloIntegracionGoogle.Causa.DESACTIVADA);
    }

    record Copia(String idReservado, UUID adjuntoId, UUID pedidoId, UUID entradaId,
            String nombre, String tipoDetectado, long tamano, String carpetaId) {
        public Copia {
            tipoDetectado = tipoDetectado == null ? "application/octet-stream" : tipoDetectado;
            if (idReservado == null || !idReservado.matches("[A-Za-z0-9_-]{1,200}")
                    || adjuntoId == null || pedidoId == null || entradaId == null
                    || nombre == null || nombre.isBlank() || nombre.codePointCount(0,nombre.length()) > 255
                    || tipoDetectado == null || !tipoDetectado.matches("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+")
                    || tamano < 1 || tamano > 104857600L
                    || carpetaId == null || !carpetaId.matches("[A-Za-z0-9_-]{1,200}")) {
                throw new IllegalArgumentException("Copia inválida");
            }
        }
    }
    /** La URI es una credencial de sesión: nunca exponer por HTTP, logs o toString. */
    record Sesion(String uri) {
        public Sesion {
            if (uri == null || uri.isBlank() || uri.length() > 4096) {
                throw new IllegalArgumentException("Sesión remota inválida");
            }
        }
        @Override public String toString() { return "SesionDrive[PROTEGIDA]"; }
    }
    record Progreso(long siguienteByte, Optional<Remoto> completo) { }
    record Remoto(String id, long tamano, String enlace, String adjuntoId, String pedidoId, String entradaId,
            List<String> padres, String tipo, String sha256, String md5, boolean enPapelera,
            boolean propietario, String unidadCompartida, String claveCarpeta, String nombre) {
        public Remoto { padres=List.copyOf(padres); }
    }
}
