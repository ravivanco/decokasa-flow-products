package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contactos vigentes para avisar a las personas (ADR-0032). Solo cuentas activas con al menos un rol y el país pedido;
 * el correo es el registrado en la cuenta. Lee la base en cada llamada. No decide permisos de acción.
 */
public interface ConsultarContactos {

    /** Cuentas activas con ese rol y ese país asignado hoy. */
    List<Contacto> activosConRol(CodigoRol rol, CodigoPais pais);

    /** La cuenta, si está activa, tiene algún rol y tiene ese país. */
    Optional<Contacto> activoEnPais(UUID usuarioId, CodigoPais pais);

    /** Nombre para mostrar; vacío si no existe. */
    Optional<String> nombre(UUID usuarioId);

    /** El correo pertenece hoy a una cuenta activa con algún rol y con ese país. */
    boolean correoAutorizado(CodigoPais pais, String correo);

    record Contacto(UUID usuarioId, String nombre, String correo) {
    }
}
