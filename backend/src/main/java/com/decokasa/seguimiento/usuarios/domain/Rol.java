package com.decokasa.seguimiento.usuarios.domain;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Set;
import java.util.UUID;

/**
 * Rol como dato: lo crea el admin (requisito 4.6). La lista de permisos sigue pendiente; se define con los casos
 * de uso de la fase 1 (modelo-datos.md 3.1).
 */
public record Rol(UUID id, CodigoRol codigo, String nombre, Set<String> permisos) {
}
