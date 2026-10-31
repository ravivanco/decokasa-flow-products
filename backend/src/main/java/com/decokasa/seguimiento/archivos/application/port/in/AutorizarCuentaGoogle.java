package com.decokasa.seguimiento.archivos.application.port.in;

import com.decokasa.seguimiento.compartido.domain.Solicitante;
import java.time.Instant;

/** Conexión administrativa de la cuenta Google de empresa; no envía correos ni copia archivos. */
public interface AutorizarCuentaGoogle {
    Inicio iniciar(Solicitante solicitante, String vinculoNavegador);

    /** ADR-0034: paso intermedio en el dominio del callback público; devuelve la URL de Google. */
    String salida(String ticket, String vinculoNavegador);

    void completar(String estado, String vinculoNavegador, String codigo, boolean denegado);

    record Inicio(String urlAutorizacion, Instant caducaEn) {
        @Override
        public String toString() { return "InicioGoogle[PROTEGIDO]"; }
    }
}
