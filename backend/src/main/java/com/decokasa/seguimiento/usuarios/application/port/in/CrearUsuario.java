package com.decokasa.seguimiento.usuarios.application.port.in;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.Solicitante;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import java.util.Set;

/**
 * Alta de un usuario con uno o varios roles; solo el admin (requisito 4.2 #3, requerimientos 4.7).
 *
 * @throws com.decokasa.seguimiento.compartido.domain.SinPermiso si el solicitante no es admin
 * @throws com.decokasa.seguimiento.compartido.domain.DatoInvalido sin roles, rol inexistente, teléfono inválido o
 *         contraseña fuera de límites (8 caracteres a 72 bytes)
 * @throws com.decokasa.seguimiento.compartido.domain.DatoDuplicado correo, usuario o teléfono ya registrados
 */
public interface CrearUsuario {

    Usuario crear(Solicitante solicitante, Comando comando);

    record Comando(String nombre, String correo, String usuario, String telefono, Set<CodigoRol> roles,
            Set<CodigoPais> paises, String contrasenaInicial) {
    }
}
