package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Carpetas de Mi unidad con clave estable (paso 3.4; ADR-0025 y ADR-0026), compartidas por la copia de adjuntos y el
 * aprovisionamiento de la carpeta de cada usuario. Cada carpeta se reserva con un ID de Drive persistido antes de crearla
 * ({@code carpeta_drive}); una reserva concurrente perdedora usa el ID ganador y, ante 409 o una respuesta incierta, se
 * consulta ese mismo ID: reintentos, concurrencia y reinicios no duplican carpetas. Antes de cada operación remota se
 * llama a {@code latido}, que renueva el lease de quien la pide (y falla si lo perdió). No comparte ni publica nada.
 * Núcleo sin Spring/JPA/SDK.
 */
final class CarpetasDrive {
    static final String CARPETA = "application/vnd.google-apps.folder";
    /** Alias de Drive para la raíz de Mi unidad. */
    static final String RAIZ_ALIAS = "root";
    static final String CLAVE_RAIZ = "d";
    static final String NOMBRE_RAIZ = "DECOKASA";
    static final String CLAVE_USUARIOS = "d/u";
    static final String NOMBRE_USUARIOS = "Usuarios";

    private final CarpetaDriveRepositorio carpetas;
    private final AlmacenArchivos drive;
    private final Reloj reloj;

    CarpetasDrive(CarpetaDriveRepositorio carpetas, AlmacenArchivos drive, Reloj reloj) {
        this.carpetas = carpetas;
        this.drive = drive;
        this.reloj = reloj;
    }

    /**
     * ID de la raíz de Mi unidad de la cuenta autorizada: propia, fuera de la papelera y sin unidad compartida. Con el
     * ámbito {@code drive.file} (ADR-0017) Drive no deja leer la raíz y responde 404 a {@code files/root} aunque la
     * conexión sea válida (comprobado contra Google el 2026-10-26): entonces se usa el alias {@link #RAIZ_ALIAS}, que sí
     * admite crear hijos, y la carpeta DECOKASA se verifica por sus propios metadatos (ver {@link #asegurar}).
     */
    String miUnidad(Runnable latido) {
        latido.run();
        var leida = drive.metadatos(RAIZ_ALIAS);
        if (leida.isEmpty()) { return RAIZ_ALIAS; }
        var raiz = leida.orElseThrow();
        if (!CARPETA.equals(raiz.tipo()) || raiz.enPapelera() || !raiz.propietario() || !raiz.unidadCompartida().isBlank()) {
            throw fallo(Causa.CONFIGURACION);
        }
        return raiz.id();
    }

    /**
     * Padre remoto esperado. Con el alias de la raíz, Drive devuelve el ID real (no legible con {@code drive.file}): se
     * exige un único padre; la propiedad, la papelera, la unidad compartida, la clave y el nombre se comprueban aparte.
     */
    private static boolean padreCoincide(List<String> padres, String padre) {
        return RAIZ_ALIAS.equals(padre) ? padres.size() == 1 : padres.equals(List.of(padre));
    }

    /**
     * {@code Mi unidad/DECOKASA/Usuarios/{usuario}} (ADR-0026). El nombre visible lo fija la primera reserva; después
     * manda la clave (UUID del usuario), no su nombre actual.
     */
    String carpetaUsuario(String miUnidad, UUID usuarioId, Supplier<String> nombreInicial, Runnable latido) {
        String app = asegurar(CLAVE_RAIZ, null, NOMBRE_RAIZ, miUnidad, latido);
        String usuarios = asegurar(CLAVE_USUARIOS, null, NOMBRE_USUARIOS, app, latido);
        return asegurar(claveUsuario(usuarioId), nombreInicial, null, usuarios, latido);
    }

    /**
     * {@code Mi unidad/DECOKASA/{número del pedido}} (ADR-0027). La identidad es la clave con el UUID completo del pedido;
     * el nombre visible es su número, que no cambia.
     */
    String carpetaPedido(String miUnidad, UUID pedidoId, String numero, Runnable latido) {
        String app = asegurar(CLAVE_RAIZ, null, NOMBRE_RAIZ, miUnidad, latido);
        return asegurar(clavePedido(pedidoId), null, numero, app, latido);
    }

    /**
     * {@code …/{pedido}/{usuario-id8}} (ADR-0027): carpeta del autor dentro del pedido, identificada por los UUID
     * completos del pedido y del autor. El nombre visible lo fija la primera reserva; renombrar al usuario no la cambia.
     */
    String carpetaAutor(String carpetaPedido, UUID pedidoId, UUID autorId, Supplier<String> nombreInicial,
            Runnable latido) {
        return asegurar(claveAutor(pedidoId, autorId), nombreInicial, null, carpetaPedido, latido);
    }

    /** {@code …/{pedido}/{autor}/{AAAA-MM-DD}} (ADR-0027): día de subida, con dígitos ASCII. */
    String carpetaDia(String carpetaAutor, UUID pedidoId, UUID autorId, String dia, Runnable latido) {
        if (dia == null || !dia.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) { throw fallo(Causa.CONFIGURACION); }
        return asegurar(claveAutor(pedidoId, autorId) + "/" + dia, null, dia, carpetaAutor, latido);
    }

    static String clavePedido(UUID pedidoId) {
        return CLAVE_RAIZ + "/o/" + compacto(pedidoId);
    }

    static String claveAutor(UUID pedidoId, UUID autorId) {
        return clavePedido(pedidoId) + "/" + compacto(autorId);
    }

    /**
     * Nombre visible de la carpeta del autor en un pedido (ADR-0027): su nombre de usuario al crearla y los 8 últimos
     * dígitos de su UUID ({@code mfranco-c5e1defb}). La identidad la da la clave, no este nombre.
     */
    static String nombreAutor(UUID usuarioId, String usuario) {
        String base = usuario == null ? "" : usuario.codePoints().filter(c -> !Character.isISOControl(c) && c != '/')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString().strip();
        if (base.isEmpty()) { base = "usuario"; }
        if (base.codePointCount(0, base.length()) > 200) { base = base.substring(0, base.offsetByCodePoints(0, 200)).strip(); }
        String id = usuarioId.toString();
        return base + "-" + id.substring(id.length() - 8);
    }

    /** Con {@code nombreInicial}, el nombre lo fija la reserva (gane quien gane); si no, debe ser {@code nombreFijo}. */
    String asegurar(String clave, Supplier<String> nombreInicial, String nombreFijo, String padre, Runnable latido) {
        var reserva = carpetas.buscar(clave);
        if (reserva.isEmpty()) {
            String propuesto = nombreInicial == null ? nombreFijo : nombreInicial.get();
            latido.run();
            String nuevo = drive.reservarId();
            reserva = Optional.of(carpetas.reservar(clave, propuesto, padre, nuevo, reloj.ahora()));
        }
        var c = reserva.orElseThrow();
        String nombre = nombreInicial == null ? nombreFijo : c.nombre();
        if (!c.nombre().equals(nombre) || !c.padreId().equals(padre)) { throw fallo(Causa.METADATOS_DIVERGENTES); }
        latido.run();
        var remoto = drive.metadatos(c.driveId());
        if (remoto.isEmpty()) {
            if (c.confirmada()) { throw fallo(Causa.METADATOS_DIVERGENTES); }
            try {
                latido.run();
                remoto = Optional.of(drive.crearCarpeta(c.driveId(), nombre, padre, clave));
            } catch (FalloIntegracionGoogle error) {
                if (!reintentable(error.causa())) { throw error; }
                latido.run();
                remoto = drive.metadatos(c.driveId());
                if (remoto.isEmpty()) { throw error; }
            }
        }
        var r = remoto.orElseThrow();
        if (!r.id().equals(c.driveId()) || !CARPETA.equals(r.tipo()) || r.enPapelera() || !r.propietario()
                || !r.unidadCompartida().isBlank() || !padreCoincide(r.padres(), padre)
                || !clave.equals(r.claveCarpeta()) || !nombre.equals(r.nombre())) { throw fallo(Causa.METADATOS_DIVERGENTES); }
        carpetas.confirmar(clave, c.driveId());
        return c.driveId();
    }

    /** Reserva confirmada de esa clave con ese ID (para terminar una copia en una organización anterior). */
    boolean confirmadaCon(String clave, String driveId) {
        if (driveId == null) { return false; }
        var c = carpetas.buscar(clave);
        return c.isPresent() && c.orElseThrow().confirmada() && driveId.equals(c.orElseThrow().driveId());
    }

    static String claveUsuario(UUID usuarioId) {
        return CLAVE_USUARIOS + "/" + compacto(usuarioId);
    }

    /**
     * Nombre visible de la carpeta del usuario: su nombre de usuario al crearla y los 8 últimos dígitos de su UUID, para
     * distinguirlo a la vista. La identidad la da la clave, no este nombre.
     */
    static String nombreUsuario(UUID usuarioId, String usuario) {
        String base = usuario == null ? "" : usuario.codePoints().filter(c -> !Character.isISOControl(c))
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString().strip();
        if (base.isEmpty()) { base = "usuario"; }
        if (base.codePointCount(0, base.length()) > 200) { base = base.substring(0, base.offsetByCodePoints(0, 200)).strip(); }
        String id = usuarioId.toString();
        return base + " · " + id.substring(id.length() - 8);
    }

    static boolean reintentable(Causa causa) {
        return switch (causa) {
            case CUOTA, TRANSITORIA, RESULTADO_INCIERTO, RESPUESTA_INVALIDA, CONFLICTO_REMOTO, SESION_CADUCADA -> true;
            default -> false;
        };
    }

    static FalloIntegracionGoogle fallo(Causa causa) {
        return new FalloIntegracionGoogle(causa);
    }

    /** UUID en base64url sin relleno (22 caracteres): la clave más larga queda en 90 de los 100 admitidos. */
    static String compacto(UUID id) {
        ByteBuffer bytes = ByteBuffer.allocate(16).putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.array());
    }
}
