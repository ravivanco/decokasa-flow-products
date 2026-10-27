package com.decokasa.seguimiento.compartido.domain;

import java.util.Set;
import java.util.UUID;

/**
 * Quién pide una operación: el usuario autenticado y sus roles (uno o varios, requerimientos 4.7). Lo arma el
 * adaptador web a partir del token; los casos de uso revisan los permisos (R6), no solo la pantalla. Los roles son
 * los del token, que pueden estar desactualizados hasta 15 minutos: una acción sensible se decide con el estado
 * actual del usuario en la base (hallazgo A1).
 */
public record Solicitante(UUID usuarioId, Set<CodigoRol> roles) {
}
