package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.out.RolRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Rol;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Adaptador JPA del puerto {@link RolRepositorio}; los permisos se guardan en rol_permiso. */
@Component
public class RolJpaRepositorio implements RolRepositorio {

    private final RolDatos datos;
    private final Reloj reloj;

    public RolJpaRepositorio(RolDatos datos, Reloj reloj) {
        this.datos = datos;
        this.reloj = reloj;
    }

    @Override
    public Optional<Rol> buscarPorId(UUID id) {
        return datos.findById(id).map(RolJpaRepositorio::aDominio);
    }

    @Override
    public Optional<Rol> buscarPorCodigo(CodigoRol codigo) {
        return datos.findByCodigo(codigo.valor()).map(RolJpaRepositorio::aDominio);
    }

    @Override
    public List<Rol> listar() {
        return datos.findAll(Sort.by("codigo")).stream().map(RolJpaRepositorio::aDominio).toList();
    }

    @Override
    public void guardar(Rol rol) {
        RolEntity entidad = datos.findById(rol.id()).orElseGet(() -> new RolEntity(rol.id(), reloj.ahora()));
        entidad.actualizar(rol.codigo().valor(), rol.nombre(), rol.permisos());
        datos.save(entidad);
    }

    private static Rol aDominio(RolEntity entidad) {
        return new Rol(entidad.getId(), new CodigoRol(entidad.getCodigo()), entidad.getNombre(),
                Set.copyOf(entidad.getPermisos()));
    }
}
