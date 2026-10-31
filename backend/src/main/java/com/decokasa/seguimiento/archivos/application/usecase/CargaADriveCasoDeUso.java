package com.decokasa.seguimiento.archivos.application.usecase;

import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio.EnDrive;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenCargas;
import com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.PedidosParaCopias;
import com.decokasa.seguimiento.archivos.application.port.out.ProtectorSesionDrive;
import com.decokasa.seguimiento.archivos.application.port.out.SesionCargaDrive;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.DriveNoDisponible;
import com.decokasa.seguimiento.archivos.domain.FormatoArchivo;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.NoEncontrado;
import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * ADR-0034 (aclaración aprobada el 2026-10-31): los documentos se guardan únicamente en Drive. Es el almacén de cargas
 * reanudables cuando {@code ARCHIVOS_DESTINO=drive}: cada fragmento, ya verificado con su SHA-256, se reenvía a una
 * sesión reanudable de Drive y no se guarda en el disco ni en PostgreSQL. Solo se recuerdan el ID reservado, la carpeta
 * y la URI de sesión cifrada ({@link SesionCargaDrive}). Si Render se reinicia entre fragmentos, la carga continúa en la
 * misma sesión; un reintento usa el mismo ID reservado, así que nunca crea otro archivo.
 *
 * <p>Carpeta vigente (ADR-0027): {@code DECOKASA/{pedido}/{autor}/{AAAA-MM-DD}}, creada al primer fragmento con el autor
 * de la carga y el día en la zona del país del pedido. Fuera de transacciones (Google exige no tener una abierta).
 */
public final class CargaADriveCasoDeUso implements AlmacenCargas {
    private static final Runnable SIN_LATIDO = () -> { };
    private final CargaRepositorio cargas;
    private final SesionCargaDrive sesiones;
    private final AlmacenArchivos drive;
    private final CarpetasDrive carpetas;
    private final PedidosParaCopias pedidos;
    private final IdentificarUsuarios identidades;
    private final ProtectorSesionDrive protector;
    private final Reloj reloj;
    private final Map<UUID, ReentrantLock> bloqueos = new ConcurrentHashMap<>();

    public CargaADriveCasoDeUso(CargaRepositorio cargas, SesionCargaDrive sesiones, AlmacenArchivos drive,
            CarpetaDriveRepositorio carpetas, PedidosParaCopias pedidos, IdentificarUsuarios identidades,
            ProtectorSesionDrive protector, Reloj reloj) {
        this.cargas = cargas;
        this.sesiones = sesiones;
        this.drive = drive;
        this.carpetas = new CarpetasDrive(carpetas, drive, reloj);
        this.pedidos = pedidos;
        this.identidades = identidades;
        this.protector = protector;
        this.reloj = reloj;
    }

    /** Una petición por carga a la vez en este proceso (Render Free corre una sola instancia). */
    @Override
    public Bloqueo bloquear(UUID cargaId) {
        ReentrantLock cerrojo = bloqueos.computeIfAbsent(cargaId, id -> new ReentrantLock());
        if (!cerrojo.tryLock()) { throw ProblemaCarga.ocupada(); }
        return cerrojo::unlock;
    }

    /** Sin parciales locales: Drive es quien conserva los bytes; la coherencia se comprueba al escribir. */
    @Override
    public void reconciliar(UUID cargaId, long offsetConfirmado) {
    }

    /** Reenvía a Drive el fragmento que empieza en el offset confirmado; vuelve solo si Drive ya lo tiene completo. */
    @Override
    public void escribir(UUID cargaId, long inicio, byte[] contenido) {
        CargaArchivo carga = carga(cargaId);
        try {
            SesionCargaDrive.Estado estado = preparar(carga);
            AlmacenArchivos.Sesion sesion;
            if (estado.sesionCifrada() == null) {
                // Sin sesión no hay bytes en Drive: solo se puede empezar por el principio.
                if (inicio != 0) { reiniciar(carga); }
                sesion = drive.iniciar(copia(carga, estado));
                sesiones.guardarSesion(cargaId, protector.cifrar(carga.adjuntoReservadoId(), sesion));
            } else {
                sesion = protector.abrir(carga.adjuntoReservadoId(), estado.sesionCifrada());
            }
            long esperado = inicio + contenido.length;
            AlmacenArchivos.Progreso progreso;
            try {
                progreso = drive.enviarFragmento(sesion, inicio, carga.tamanoBytes(), contenido);
            } catch (FalloIntegracionGoogle fallo) {
                if (fallo.causa() == Causa.SESION_CADUCADA) { reiniciar(carga); }
                if (sinAutorizacion(fallo)) { throw fallo; }
                // Resultado incierto (o Drive ya tenía estos bytes de un intento anterior): preguntar a Drive.
                progreso = drive.consultar(sesion, carga.tamanoBytes());
            }
            if (progreso.siguienteByte() == esperado) { return; }
            if (progreso.siguienteByte() > esperado || progreso.siguienteByte() < inicio) {
                // Drive y PostgreSQL ya no coinciden: no se adivina; la carga empieza de nuevo sin duplicar.
                reiniciar(carga);
            }
            throw DriveNoDisponible.alSubir();
        } catch (FalloIntegracionGoogle fallo) {
            if (fallo.causa() == Causa.SESION_CADUCADA) { reiniciar(carga); }
            throw traducir(fallo);
        }
    }

    /** No hay parciales que validar fuera de Drive: la verificación completa se hace al finalizar. */
    @Override
    public void validar(UUID cargaId, List<CargaArchivo.Fragmento> fragmentos, long total) {
    }

    /** Contenido completo desde Drive, en flujo (para la verificación técnica y la huella al finalizar). */
    @Override
    public InputStream abrir(UUID cargaId) {
        SesionCargaDrive.Estado estado = sesiones.leer(cargaId).orElseThrow(ProblemaCarga::incompleta);
        if (estado.driveId() == null) { throw ProblemaCarga.incompleta(); }
        try {
            return drive.descargar(estado.driveId());
        } catch (FalloIntegracionGoogle fallo) {
            throw traducir(fallo);
        }
    }

    /**
     * Drive confirmó el archivo completo: mismo ID reservado, mismo tamaño, en su carpeta, propiedad de la cuenta y con
     * las marcas del adjunto reservado. Devuelve sus identificadores y las huellas que calculó Google.
     */
    public Confirmado confirmado(CargaArchivo carga) {
        SesionCargaDrive.Estado estado = sesiones.leer(carga.id()).orElseThrow(ProblemaCarga::incompleta);
        if (estado.driveId() == null || estado.carpetaId() == null) { throw ProblemaCarga.incompleta(); }
        try {
            AlmacenArchivos.Remoto r = drive.metadatos(estado.driveId()).orElseThrow(DriveNoDisponible::alSubir);
            if (!estado.driveId().equals(r.id()) || r.tamano() != carga.tamanoBytes()
                    || !carga.adjuntoReservadoId().toString().equals(r.adjuntoId())
                    || !carga.pedidoId().toString().equals(r.pedidoId())
                    || !carga.entradaId().toString().equals(r.entradaId())
                    || !r.padres().equals(List.of(estado.carpetaId())) || r.enPapelera() || !r.propietario()
                    || !r.unidadCompartida().isBlank() || r.tipo().startsWith("application/vnd.google-apps.")
                    || r.enlace() == null || !r.enlace().matches("https://drive\\.google\\.com/[^\\s]{1,2000}")) {
                throw ProblemaCarga.conflicto();
            }
            return new Confirmado(new EnDrive(r.id(), r.enlace(), estado.carpetaId()), r.sha256(), r.md5());
        } catch (FalloIntegracionGoogle fallo) {
            throw traducir(fallo);
        }
    }

    /**
     * El archivo subido no se registrará (rechazado por la verificación técnica o distinto de lo declarado): se borra
     * de Drive y la carga vuelve a empezar. Nunca se usa con un adjunto registrado.
     */
    public void descartar(CargaArchivo carga) {
        sesiones.leer(carga.id()).map(SesionCargaDrive.Estado::driveId).ifPresent(id -> {
            try { drive.eliminar(id); } catch (FalloIntegracionGoogle fallo) { throw traducir(fallo); }
        });
        sesiones.reiniciar(carga.id());
    }

    /** Tras el commit del registro: borra la URI de sesión (credencial que ya no hace falta). Nunca lanza. */
    public void olvidarSesion(UUID cargaId) {
        try { sesiones.olvidarSesion(cargaId); } catch (RuntimeException ignorado) { }
    }

    /** ID reservado y carpeta de la carga; se crean una sola vez y se reutilizan en cada reintento. */
    private SesionCargaDrive.Estado preparar(CargaArchivo carga) {
        SesionCargaDrive.Estado estado = sesiones.leer(carga.id()).orElseThrow(() -> new NoEncontrado("No existe esa carga."));
        if (estado.driveId() != null && estado.carpetaId() != null) { return estado; }
        String carpeta = estado.carpetaId() != null ? estado.carpetaId() : carpeta(carga);
        String id = estado.driveId() != null ? estado.driveId() : drive.reservarId();
        sesiones.reservar(carga.id(), id, carpeta);
        return sesiones.leer(carga.id()).orElseThrow(() -> new NoEncontrado("No existe esa carga."));
    }

    /** ADR-0027: DECOKASA/{número del pedido}/{autor}/{AAAA-MM-DD}, con los datos del servidor. */
    private String carpeta(CargaArchivo carga) {
        PedidosParaCopias.Destino destino = pedidos.destino(carga.pedidoId());
        UUID autor = carga.usuarioId();
        String raiz = carpetas.miUnidad(SIN_LATIDO);
        String pedido = carpetas.carpetaPedido(raiz, carga.pedidoId(), destino.numero(), SIN_LATIDO);
        String carpetaAutor = carpetas.carpetaAutor(pedido, carga.pedidoId(), autor,
                () -> CarpetasDrive.nombreAutor(autor, identidades.nombreDeUsuario(autor).orElse(null)), SIN_LATIDO);
        String dia = reloj.ahora().atZone(destino.pais().zona()).toLocalDate().toString();
        return carpetas.carpetaDia(carpetaAutor, carga.pedidoId(), autor, dia, SIN_LATIDO);
    }

    private static AlmacenArchivos.Copia copia(CargaArchivo carga, SesionCargaDrive.Estado estado) {
        return new AlmacenArchivos.Copia(estado.driveId(), carga.adjuntoReservadoId(), carga.pedidoId(),
                carga.entradaId(), carga.nombre(), FormatoArchivo.desdeNombre(carga.nombre()).mime(),
                carga.tamanoBytes(), estado.carpetaId());
    }

    private void reiniciar(CargaArchivo carga) {
        sesiones.reiniciar(carga.id());
        throw ProblemaCarga.reiniciada();
    }

    private CargaArchivo carga(UUID cargaId) {
        return cargas.porId(cargaId, false).orElseThrow(() -> new NoEncontrado("No existe esa carga."));
    }

    private static boolean sinAutorizacion(FalloIntegracionGoogle fallo) {
        return fallo.causa() == Causa.AUTORIZACION_REQUERIDA || fallo.causa() == Causa.DESACTIVADA;
    }

    private static RuntimeException traducir(FalloIntegracionGoogle fallo) {
        return sinAutorizacion(fallo) ? DriveNoDisponible.sinAutorizacion() : DriveNoDisponible.alSubir();
    }

    /** Identificadores del documento en Drive y huellas calculadas por Google (pueden venir vacías). */
    public record Confirmado(EnDrive drive, String sha256, String md5) { }
}
