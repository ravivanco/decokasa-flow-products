package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import com.decokasa.seguimiento.usuarios.application.port.in.EnviarCorreosRecuperacion;
import com.decokasa.seguimiento.usuarios.application.port.out.CorreoRecuperacion;
import com.decokasa.seguimiento.usuarios.application.port.out.GeneradorTokenRenovacion;
import com.decokasa.seguimiento.usuarios.application.port.out.RecuperacionRepositorio;
import java.util.regex.Pattern;

/**
 * Trabajador de los enlaces de recuperación (ADR-0033). El token se crea aquí y en la base solo queda su hash. Cada
 * solicitud se intenta una sola vez: se reclama (ENVIANDO) antes de llamar a Gmail y nunca se reenvía. Las pendientes
 * caducadas se anulan sin enviar (no hay correos históricos). El repositorio abre una transacción por operación; el
 * envío ocurre fuera de ellas.
 */
public final class EnviarCorreosRecuperacionCasoDeUso implements EnviarCorreosRecuperacion {

    /** Ruta del frontend; el token va en el fragmento para no llegar a registros ni al Referer. */
    public static final String RUTA = "/restablecer-contrasena#token=";

    private static final Pattern URL_BASE = Pattern.compile("https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[A-Za-z0-9._~/-]*)?");

    private final RecuperacionRepositorio recuperaciones;
    private final CorreoRecuperacion correo;
    private final GeneradorTokenRenovacion generador;
    private final Reloj reloj;
    private final boolean habilitado;
    private final String urlBase;
    private final int lote;

    public EnviarCorreosRecuperacionCasoDeUso(RecuperacionRepositorio recuperaciones, CorreoRecuperacion correo,
            GeneradorTokenRenovacion generador, Reloj reloj, boolean habilitado, String urlBase, int lote) {
        if (lote < 1 || lote > 25) {
            throw new IllegalArgumentException("Lote de recuperación inválido");
        }
        String base = urlBase == null ? "" : urlBase.strip();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (habilitado && !URL_BASE.matcher(base).matches()) {
            throw new IllegalStateException(
                    "Define RECUPERACION_URL_BASE (http://localhost:3000 o el dominio público) para enviar enlaces.");
        }
        this.recuperaciones = recuperaciones;
        this.correo = correo;
        this.generador = generador;
        this.reloj = reloj;
        this.habilitado = habilitado;
        this.urlBase = base;
        this.lote = lote;
    }

    @Override
    public int procesar() {
        if (!habilitado) {
            return 0;
        }
        recuperaciones.anularCaducadas(reloj.ahora());
        int tratadas = 0;
        for (int i = 0; i < lote && !Thread.currentThread().isInterrupted(); i++) {
            String token = generador.nuevo();
            var reclamada = recuperaciones.reclamar(generador.hash(token), reloj.ahora());
            if (reclamada.isEmpty()) {
                break;
            }
            var solicitud = reclamada.get();
            tratadas++;
            if (!solicitud.activa()) {
                recuperaciones.anular(solicitud.id(), reloj.ahora());
                continue;
            }
            try {
                correo.enviar(solicitud.id(), solicitud.correo(), solicitud.nombre(), urlBase + RUTA + token,
                        solicitud.expiraEn());
            } catch (RuntimeException error) {
                // Sin reintento: si Gmail llegó a entregarlo, el enlace sigue valiendo; si no, se pide otro.
                recuperaciones.marcarFallida(solicitud.id(), codigo(error), reloj.ahora());
                continue;
            }
            recuperaciones.marcarEnviada(solicitud.id(), reloj.ahora());
        }
        return tratadas;
    }

    /** Código técnico corto, sin datos personales ni mensajes del proveedor. */
    private static String codigo(RuntimeException error) {
        String texto = error instanceof FalloIntegracionGoogle google ? "GOOGLE_" + google.causa().name() : "ERROR_ENVIO";
        return texto.length() > 40 ? texto.substring(0, 40) : texto;
    }
}
