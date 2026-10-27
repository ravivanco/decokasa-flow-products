package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.time.Instant;

/**
 * Resultado de iniciar o renovar la sesión (contrato: RespuestaSesion). El token de renovación viaja aparte del
 * cuerpo; cómo llega al navegador lo decide ADR-0002 (cookie HttpOnly, propuesta D3b pendiente de aprobación).
 *
 * @param renovableHasta vencimiento absoluto de la sesión: inicio de sesión + 8 horas (D3)
 */
public record SesionIniciada(String tokenAcceso, long expiraEnSegundos, String tokenRenovacion,
        Instant renovableHasta, Usuario usuario) {
}
