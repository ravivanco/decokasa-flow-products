package com.decokasa.seguimiento.archivos.application.usecase;

import static com.decokasa.seguimiento.archivos.application.usecase.CarpetasDrive.fallo;
import static com.decokasa.seguimiento.archivos.application.usecase.CarpetasDrive.reintentable;

import com.decokasa.seguimiento.archivos.application.port.in.GestionarCarpetasUsuarios;
import com.decokasa.seguimiento.archivos.application.port.out.AlmacenArchivos;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.archivos.application.port.out.CarpetaUsuarioRepositorio;
import com.decokasa.seguimiento.archivos.domain.PoliticaCopia;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.usuarios.application.port.in.IdentificarUsuarios;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Procesa las intenciones de {@code carpeta_usuario_drive} (ADR-0026): crea {@code Mi unidad/DECOKASA/Usuarios/{usuario}}
 * con {@link CarpetasDrive} (ID reservado y persistido antes de crear, sin duplicados ante reintentos, concurrencia o
 * reinicios) y la marca CREADA solo después de comprobar sus metadatos en Drive. Mismos intentos (inicial y tres
 * reintentos), esperas y causas reintentables que la copia; después, FALLIDA con su causa. Apagado, no reclama nada y
 * las intenciones quedan PENDIENTE. No crea país, año, mes ni pedido: esos los crea la primera copia que los necesita.
 * Núcleo sin Spring/JPA/SDK; los repositorios inyectados cierran su transacción antes de retornar.
 */
public final class AprovisionarCarpetasUsuariosCasoDeUso implements GestionarCarpetasUsuarios.Procesar {
    private final boolean habilitado;
    private final int lote;
    private final CarpetaUsuarioRepositorio intenciones;
    private final CarpetasDrive carpetasDrive;
    private final IdentificarUsuarios identidades;
    private final Reloj reloj;
    private final PoliticaCopia politica;

    public AprovisionarCarpetasUsuariosCasoDeUso(boolean habilitado, int lote, CarpetaUsuarioRepositorio intenciones,
            CarpetaDriveRepositorio carpetas, AlmacenArchivos drive, IdentificarUsuarios identidades, Reloj reloj,
            PoliticaCopia politica) {
        if (lote < 1 || lote > 100) { throw new IllegalArgumentException("Lote inválido"); }
        this.habilitado = habilitado;
        this.lote = lote;
        this.intenciones = intenciones;
        this.carpetasDrive = new CarpetasDrive(carpetas, drive, reloj);
        this.identidades = identidades;
        this.reloj = reloj;
        this.politica = politica;
    }

    @Override public int ejecutar() {
        if (!habilitado) { return 0; }
        int reclamadas = 0;
        for (int i = 0; i < lote && !Thread.currentThread().isInterrupted(); i++) {
            UUID token = UUID.randomUUID();
            Instant ahora = reloj.ahora();
            var filas = intenciones.reclamar(1, ahora, ahora.plus(politica.lease()), token);
            if (filas.isEmpty()) { break; }
            reclamadas++;
            procesar(filas.getFirst(), token);
        }
        return reclamadas;
    }

    private void procesar(CarpetaUsuarioRepositorio.Pendiente pendiente, UUID token) {
        UUID usuarioId = pendiente.usuarioId();
        Runnable latido = () -> {
            Instant ahora = reloj.ahora();
            if (!intenciones.renovar(usuarioId, token, ahora, ahora.plus(politica.lease()))) {
                throw fallo(Causa.LEASE_PERDIDO);
            }
        };
        try {
            String raiz = carpetasDrive.miUnidad(latido);
            String carpeta = carpetasDrive.carpetaUsuario(raiz, usuarioId,
                    () -> CarpetasDrive.nombreUsuario(usuarioId, identidades.nombreDeUsuario(usuarioId).orElse(null)),
                    latido);
            // Sin lease vigente no cambia nada: otro trabajador la retomará y encontrará la misma carpeta.
            intenciones.confirmar(usuarioId, token, carpeta, reloj.ahora());
        } catch (FalloIntegracionGoogle error) {
            registrar(pendiente, token, error);
        } catch (RuntimeException error) {
            // No guardar mensaje/causa: puede contener SQL, URI privada o rutas de infraestructura.
            registrar(pendiente, token, fallo(Causa.TRANSITORIA));
        }
    }

    private void registrar(CarpetaUsuarioRepositorio.Pendiente pendiente, UUID token, FalloIntegracionGoogle error) {
        if (error.causa() == Causa.LEASE_PERDIDO) { return; }
        boolean terminal = pendiente.intentos() >= 4 || !reintentable(error.causa());
        Instant siguiente = null;
        if (!terminal) {
            Duration espera = politica.esperaDespuesDe(pendiente.intentos());
            if (error.esperaMinima().compareTo(espera) > 0) { espera = error.esperaMinima(); }
            siguiente = reloj.ahora().plus(espera);
        }
        intenciones.registrarFallo(pendiente.usuarioId(), token, error.causa(), siguiente, terminal, reloj.ahora());
    }
}
