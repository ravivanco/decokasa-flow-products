package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle.Inicio;
import java.time.Instant;
import java.util.UUID;

/** Frontera técnica OAuth; ningún token Google sale por el puerto de entrada o por HTTP. */
public interface ConexionOAuthGoogle {
    Inicio iniciar(UUID adminId, String vinculoNavegador);

    /**
     * Retorno público HTTPS (ADR-0034): canjea el ticket de un solo uso de {@link #iniciar}, vincula la solicitud a la
     * cookie nueva del dominio del callback y devuelve la URL de consentimiento de Google.
     */
    String salida(String ticket, String vinculoNavegador);

    Pendiente consumir(String estado, String vinculoNavegador);

    void conectar(Pendiente pendiente, String codigo);

    record Pendiente(UUID adminId, String verificador, String nonce, Instant caducaEn) {
        @Override
        public String toString() { return "PendienteGoogle[PROTEGIDO]"; }
    }
}
