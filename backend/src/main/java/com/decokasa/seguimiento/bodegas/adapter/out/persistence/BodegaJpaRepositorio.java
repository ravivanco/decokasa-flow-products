package com.decokasa.seguimiento.bodegas.adapter.out.persistence;

import com.decokasa.seguimiento.bodegas.application.port.out.BodegaRepositorio;
import com.decokasa.seguimiento.bodegas.domain.Bodega;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.application.port.out.Reloj;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA de {@link BodegaRepositorio}. Traduce entre la entidad y el dominio y convierte en errores del contrato
 * las violaciones que solo detecta la base: nombre repetido por un alta o edición simultánea (409 DUPLICADO) y versión
 * cambiada sin el bloqueo de fila (409 VERSION_DESACTUALIZADA). Cualquier otro error de integridad sigue su curso.
 */
@Component
public class BodegaJpaRepositorio implements BodegaRepositorio {

    private static final String VIOLACION_DE_UNICIDAD = "23505";
    private static final String NOMBRE_UNICO = "bodega_nombre_unico";
    private static final Sort POR_NOMBRE = Sort.by("nombre");
    private static final Sort POR_PAIS_Y_NOMBRE = Sort.by("paisCodigo", "nombre");

    private final BodegaDatos datos;
    private final Reloj reloj;
    private final EntityManager entityManager;

    public BodegaJpaRepositorio(BodegaDatos datos, Reloj reloj, EntityManager entityManager) {
        this.datos = datos;
        this.reloj = reloj;
        this.entityManager = entityManager;
    }

    @Override
    public List<Bodega> listar(Boolean activa) {
        List<BodegaEntity> filas = activa == null ? datos.findAll(POR_NOMBRE) : datos.findByActiva(activa, POR_NOMBRE);
        return filas.stream().map(BodegaJpaRepositorio::aDominio).toList();
    }

    /** Fase 7 (ADR-0024): el filtro por país va en la consulta. */
    @Override
    public List<Bodega> listar(Set<CodigoPais> paises, Boolean activa) {
        List<String> codigos = paises.stream().map(CodigoPais::name).toList();
        List<BodegaEntity> filas = activa == null
                ? datos.findByPaisCodigoIn(codigos, POR_PAIS_Y_NOMBRE)
                : datos.findByPaisCodigoInAndActiva(codigos, activa, POR_PAIS_Y_NOMBRE);
        return filas.stream().map(BodegaJpaRepositorio::aDominio).toList();
    }

    @Override
    public Optional<Bodega> buscarPorNombre(CodigoPais pais, String nombre) {
        return datos.findByPaisCodigoAndNombreIgnoreCase(pais.name(), nombre).map(BodegaJpaRepositorio::aDominio);
    }

    @Override
    public Optional<Bodega> buscarPorId(UUID id) {
        return datos.findById(id).map(BodegaJpaRepositorio::aDominio);
    }

    /**
     * {@code SELECT … FOR UPDATE} sobre la fila. Se usa {@code refresh} y no una consulta porque la entidad puede estar
     * ya cargada en esta transacción; {@code refresh} la recarga con lo último confirmado, ya con la fila bloqueada.
     */
    @Override
    public Optional<Bodega> buscarParaActualizar(UUID id) {
        BodegaEntity entidad = entityManager.find(BodegaEntity.class, id);
        if (entidad == null) {
            return Optional.empty();
        }
        entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
        return Optional.of(aDominio(entidad));
    }

    /**
     * Firma anterior a la fase 7. Desde V19 el nombre solo es único por país: por compatibilidad documentada busca en
     * Ecuador (ADR-0024). Ningún caso de uso la usa; usan {@link #buscarPorNombre(CodigoPais, String)}.
     */
    @Override
    public Optional<Bodega> buscarPorNombre(String nombre) {
        return buscarPorNombre(CodigoPais.EC, nombre);
    }

    @Override
    public Bodega guardar(Bodega bodega) {
        Instant ahora = reloj.ahora();
        BodegaEntity entidad = datos.findById(bodega.id())
                .orElseGet(() -> new BodegaEntity(bodega.id(), bodega.pais().name(), ahora));
        entidad.actualizar(bodega.nombre(), bodega.ciudad(), bodega.activa(), ahora);
        // Se escribe ya, y no al confirmar, para que una carrera con otra alta o edición llegue aquí como 409.
        try {
            return aDominio(datos.saveAndFlush(entidad));
        } catch (DataIntegrityViolationException error) {
            if (esNombreRepetido(error)) {
                throw new DatoDuplicado("nombre", "Ya existe una bodega con ese nombre en ese país.");
            }
            throw error;
        } catch (ObjectOptimisticLockingFailureException error) {
            // Red de seguridad: otra escritura cambió la versión sin pasar por el bloqueo de fila.
            throw new VersionDesactualizada("Otra persona modificó esta bodega. Vuelve a cargarla y repite el cambio.");
        }
    }

    private static boolean esNombreRepetido(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof ConstraintViolationException violacion) {
                return VIOLACION_DE_UNICIDAD.equals(violacion.getSQLState())
                        && NOMBRE_UNICO.equals(violacion.getConstraintName());
            }
        }
        return false;
    }

    private static Bodega aDominio(BodegaEntity entidad) {
        return new Bodega(entidad.getId(), entidad.getNombre(), entidad.getCiudad(), entidad.isActiva(),
                entidad.getVersion() == null ? 0 : entidad.getVersion(), CodigoPais.valueOf(entidad.getPaisCodigo()));
    }
}
