package com.decokasa.seguimiento.plazos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.VersionDesactualizada;
import com.decokasa.seguimiento.plazos.application.port.out.ConfiguracionPlazosRepositorio;
import com.decokasa.seguimiento.plazos.domain.ConfiguracionPlazos;
import com.decokasa.seguimiento.plazos.domain.OrigenMeta;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA de {@link ConfiguracionPlazosRepositorio} sobre la fila única de V14. Si la fila no existe, la base no
 * está migrada: es un error de despliegue, no una configuración vacía, y no se inventa una meta.
 */
@Component
public class ConfiguracionPlazosJpaRepositorio implements ConfiguracionPlazosRepositorio {

    private final EntityManager entityManager;

    public ConfiguracionPlazosJpaRepositorio(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public ConfiguracionPlazos leer() {
        return aDominio(fila());
    }

    /** {@code refresh} con bloqueo: la entidad puede estar ya cargada en esta transacción (patrón de bodegas). */
    @Override
    public ConfiguracionPlazos leerParaActualizar() {
        ConfiguracionPlazosEntity entidad = fila();
        entityManager.refresh(entidad, LockModeType.PESSIMISTIC_WRITE);
        return aDominio(entidad);
    }

    @Override
    public ConfiguracionPlazos guardar(ConfiguracionPlazos configuracion) {
        ConfiguracionPlazosEntity entidad = fila();
        entidad.actualizar(configuracion.metaDias(), configuracion.origenMeta().name(), configuracion.actualizadoEn(),
                configuracion.actualizadoPor());
        try {
            entityManager.flush();
        } catch (OptimisticLockException | ObjectOptimisticLockingFailureException error) {
            // Red de seguridad: otra escritura cambió la versión sin pasar por el bloqueo de fila.
            throw new VersionDesactualizada(
                    "Otra persona cambió la configuración de plazos. Vuelve a cargarla y repite el cambio.");
        }
        return aDominio(entidad);
    }

    private ConfiguracionPlazosEntity fila() {
        ConfiguracionPlazosEntity entidad =
                entityManager.find(ConfiguracionPlazosEntity.class, ConfiguracionPlazosEntity.FILA_UNICA);
        if (entidad == null) {
            throw new IllegalStateException("Falta la fila de configuracion_plazos (migración V14).");
        }
        return entidad;
    }

    private static ConfiguracionPlazos aDominio(ConfiguracionPlazosEntity entidad) {
        return new ConfiguracionPlazos(entidad.getMetaDias(), OrigenMeta.valueOf(entidad.getMetaOrigen()),
                entidad.getActualizadoEn(), entidad.getActualizadoPor(),
                entidad.getVersion() == null ? 0 : entidad.getVersion());
    }
}
