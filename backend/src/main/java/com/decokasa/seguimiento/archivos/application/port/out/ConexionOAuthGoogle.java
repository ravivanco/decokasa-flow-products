package com.decokasa.seguimiento.archivos.application.port.out;

import com.decokasa.seguimiento.archivos.application.port.in.AutorizarCuentaGoogle.Inicio;
import java.time.Instant;
import java.util.UUID;

/** Frontera técnica OAuth; ningún token Google sale por el puerto de entrada o por HTTP. */
public interface ConexionOAuthGoogle {
    Inicio iniciar(UUID adminId, String vinculoNavegador);

    Pendiente consumir(String estado, String vinculoNavegador);

    void conectar(Pendiente pendiente, String codigo);

    record Pendiente(UUID adminId, String verificador, String nonce, Instant caducaEn) {
        @Override
        public String toString() { return "PendienteGoogle[PROTEGIDO]"; }
    }
}
