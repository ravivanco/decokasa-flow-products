package com.decokasa.seguimiento.usuarios.application.usecase;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import com.decokasa.seguimiento.usuarios.application.port.out.Contactos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Contactos vigentes (ADR-0032). Sin Spring (R2). */
public final class ConsultarContactosCasoDeUso implements ConsultarContactos {

    private final Contactos contactos;

    public ConsultarContactosCasoDeUso(Contactos contactos) {
        this.contactos = contactos;
    }

    @Override
    public List<Contacto> activosConRol(CodigoRol rol, CodigoPais pais) {
        return rol == null || pais == null ? List.of() : contactos.activosConRol(rol, pais);
    }

    @Override
    public Optional<Contacto> activoEnPais(UUID usuarioId, CodigoPais pais) {
        return usuarioId == null || pais == null ? Optional.empty() : contactos.activoEnPais(usuarioId, pais);
    }

    @Override
    public Optional<String> nombre(UUID usuarioId) {
        return usuarioId == null ? Optional.empty() : contactos.nombre(usuarioId);
    }

    @Override
    public boolean correoAutorizado(CodigoPais pais, String correo) {
        return pais != null && correo != null && !correo.isBlank() && contactos.correoAutorizado(pais, correo);
    }
}
