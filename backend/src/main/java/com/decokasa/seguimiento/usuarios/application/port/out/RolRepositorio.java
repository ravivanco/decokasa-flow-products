package com.decokasa.seguimiento.usuarios.application.port.out;

import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Roles guardados (los 7 iniciales vienen de la migración V1; el admin puede crear más). */
public interface RolRepositorio {

    Optional<Rol> buscarPorId(UUID id);

    Optional<Rol> buscarPorCodigo(CodigoRol codigo);

    List<Rol> listar();

    void guardar(Rol rol);
}
