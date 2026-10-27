package com.decokasa.seguimiento.usuarios.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Lectura de contactos vigentes (ADR-0032). */
public interface Contactos {

    List<ConsultarContactos.Contacto> activosConRol(CodigoRol rol, CodigoPais pais);

    Optional<ConsultarContactos.Contacto> activoEnPais(UUID usuarioId, CodigoPais pais);

    Optional<String> nombre(UUID usuarioId);

    boolean correoAutorizado(CodigoPais pais, String correo);
}
