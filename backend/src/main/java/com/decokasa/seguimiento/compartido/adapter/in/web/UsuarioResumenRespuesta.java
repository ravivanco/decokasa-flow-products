package com.decokasa.seguimiento.compartido.adapter.in.web;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Esquema UsuarioResumen del contrato: id, nombre y roles; sin correo, teléfono ni credenciales. */
public record UsuarioResumenRespuesta(UUID id, String nombre, List<String> roles) {

    public static UsuarioResumenRespuesta de(UUID id, String nombre, Collection<CodigoRol> roles) {
        return new UsuarioResumenRespuesta(id, nombre, roles.stream().map(CodigoRol::valor).sorted().toList());
    }
}
