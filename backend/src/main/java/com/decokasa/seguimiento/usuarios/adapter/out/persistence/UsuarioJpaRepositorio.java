package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.usuarios.application.port.out.UsuarioRepositorio;
import com.decokasa.seguimiento.usuarios.domain.Telefono;
import com.decokasa.seguimiento.usuarios.domain.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA del puerto {@link UsuarioRepositorio}. Traduce entre la entidad y el dominio; los roles se guardan
 * en usuario_rol por id y conservan su fecha de asignación. Se usa dentro de la transacción del caso de uso.
 */
@Component
public class UsuarioJpaRepositorio implements UsuarioRepositorio {

    static final String USUARIO_CAMBIO = "Otra persona modificó este usuario. Vuelve a cargarlo y repite el cambio.";

    private final UsuarioDatos datos;
    private final RolDatos roles;
    private final Reloj reloj;
    private final EntityManager entityManager;

    public UsuarioJpaRepositorio(UsuarioDatos datos, RolDatos roles, Reloj reloj, EntityManager entityManager) {
        this.datos = datos;
        this.roles = roles;
        this.reloj = reloj;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Usuario> buscarPorId(UUID id) {
        return datos.findById(id).map(this::aDominio);
    }

    /**
     * {@code SELECT … FOR UPDATE} sobre la fila del usuario. Se usa {@code refresh} y no una consulta porque la
     * entidad puede estar ya cargada en esta transacción (la búsqueda por correo, usuario o teléfono): una consulta
     * devolvería esa copia anterior; {@code refresh} la recarga con lo último confirmado, ya con la fila bloqueada.
     */
    @Override
    public Optional<Usuario> buscarParaActualizar(UUID id) {
        UsuarioEntity entidad = entityManager.find(UsuarioEntity.class, id);
        if (entidad == null) {
            return Optional.empty();
        }
        entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(aDominio(entidad));
    }

    @Override
    public Optional<Usuario> buscarPorCorreo(String correo) {
        return datos.findByCorreoIgnoreCase(correo).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorNombreDeUsuario(String usuario) {
        return datos.findByUsuarioIgnoreCase(usuario).map(this::aDominio);
    }

    @Override
    public Optional<Usuario> buscarPorTelefono(Telefono telefono) {
        return datos.findByTelefono(telefono.valor()).map(this::aDominio);
    }

    @Override
    public List<Usuario> listar() {
        return datos.findAll(Sort.by("nombre")).stream().map(this::aDominio).toList();
    }

    @Override
    public void guardar(Usuario usuario) {
        Instant ahora = reloj.ahora();
        UsuarioEntity entidad = datos.findById(usuario.id()).orElseGet(() -> new UsuarioEntity(usuario.id(), ahora));
        // Los contadores del login no se escriben aquí (D10): un usuario nuevo empieza en 0 y sin bloqueo, y luego
        // solo los cambia guardarIntentos.
        entidad.actualizar(usuario.nombre(), usuario.correo(), usuario.usuario(), usuario.telefono().valor(),
                usuario.contrasenaHash(), usuario.activo(), ahora);

        Map<UUID, Instant> asignadosAntes = entidad.getRoles().stream()
                .collect(Collectors.toMap(AsignacionRolEntity::rolId, AsignacionRolEntity::asignadoEn));
        Set<AsignacionRolEntity> asignaciones = usuario.roles().stream()
                .map(this::idDeRol)
                .map(rolId -> new AsignacionRolEntity(rolId, asignadosAntes.getOrDefault(rolId, ahora)))
                .collect(Collectors.toSet());
        entidad.getRoles().retainAll(asignaciones);
        entidad.getRoles().addAll(asignaciones);

        Set<String> paises = usuario.paises().stream().map(CodigoPais::name).collect(Collectors.toSet());
        entidad.getPaises().retainAll(paises);
        entidad.getPaises().addAll(paises);
        // Se escribe ya, y no al confirmar, para que una carrera con otra alta o edición llegue aquí y se responda
        // 409 con el campo repetido (hallazgo M2). Cualquier otro error de integridad sigue su curso.
        try {
            datos.saveAndFlush(entidad);
        } catch (DataIntegrityViolationException error) {
            Optional<DatoDuplicado> duplicado = RestriccionesUnicasUsuario.comoDuplicado(error);
            if (duplicado.isPresent()) {
                throw duplicado.get();
            }
            throw error;
        } catch (ObjectOptimisticLockingFailureException error) {
            // Red de seguridad de D10: otra escritura cambió la versión sin pasar por el bloqueo de fila.
            throw new VersionDesactualizada(USUARIO_CAMBIO);
        }
    }

    /** Solo el contador y el bloqueo, sin cambiar la versión administrativa (D10). */
    @Override
    public void guardarIntentos(Usuario usuario) {
        datos.guardarIntentos(usuario.id(), usuario.intentosFallidos(), usuario.bloqueadoHasta());
    }

    private UUID idDeRol(CodigoRol codigo) {
        return roles.findByCodigo(codigo.valor())
                .orElseThrow(() -> new IllegalStateException("El rol " + codigo.valor() + " no existe."))
                .getId();
    }

    private Usuario aDominio(UsuarioEntity entidad) {
        List<UUID> idsDeRoles = entidad.getRoles().stream().map(AsignacionRolEntity::rolId).toList();
        Set<CodigoRol> rolesDelUsuario = roles.findAllById(idsDeRoles).stream()
                .map(rol -> new CodigoRol(rol.getCodigo()))
                .collect(Collectors.toSet());
        Set<CodigoPais> paises = entidad.getPaises().stream().map(CodigoPais::valueOf).collect(Collectors.toSet());
        return new Usuario(entidad.getId(), entidad.getNombre(), entidad.getCorreo(), entidad.getUsuario(),
                new Telefono(entidad.getTelefono()), entidad.getContrasenaHash(), rolesDelUsuario, paises,
                entidad.isActivo(), entidad.getIntentosFallidos(), entidad.getBloqueadoHasta(),
                entidad.getVersion() == null ? 0 : entidad.getVersion());
    }
}
