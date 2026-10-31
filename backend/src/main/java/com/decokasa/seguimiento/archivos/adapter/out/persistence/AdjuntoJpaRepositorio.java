package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.AdjuntoRepositorio;
import com.decokasa.seguimiento.archivos.domain.Adjunto;
import com.decokasa.seguimiento.archivos.domain.EstadoCopia;
import jakarta.persistence.EntityManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador JPA del puerto {@link AdjuntoRepositorio}. Inserta con {@code persist} y vacía la sesión enseguida: así
 * el adjunto existe antes de que el historial inserte su relación con el evento (FK de evento_adjunto).
 */
@Component
public class AdjuntoJpaRepositorio implements AdjuntoRepositorio {

    private final EntityManager entityManager;
    private final AdjuntoDatos datos;

    public AdjuntoJpaRepositorio(EntityManager entityManager, AdjuntoDatos datos) {
        this.entityManager = entityManager;
        this.datos = datos;
    }

    @Override
    public void insertar(Adjunto adjunto) {
        entityManager.persist(new AdjuntoEntity(adjunto.id(), adjunto.pedidoId(), (short) adjunto.etapa(),
                adjunto.entradaId(), adjunto.tipoDocumento(), adjunto.nombre(), adjunto.tamanoBytes(),
                adjunto.sha256(), adjunto.tipoDetectado(), adjunto.claveLocal(), adjunto.estadoCopia().name(),
                adjunto.subidoPor(), adjunto.subidoEn()));
        entityManager.flush();
    }

    /** ADR-0034: una sola sentencia que cumple las restricciones de V3/V4/V6 (COPIADO ⇔ drive_id, enlace HTTPS). */
    @Override
    public void insertarEnDrive(Adjunto adjunto, EnDrive drive) {
        if (adjunto.estadoCopia() != com.decokasa.seguimiento.archivos.domain.EstadoCopia.COPIADO
                || adjunto.claveLocal() != null) {
            throw new IllegalArgumentException("Un documento solo en Drive se registra COPIADO y sin clave local");
        }
        entityManager.flush();
        entityManager.createNativeQuery("""
                insert into adjunto (id, pedido_id, etapa, entrada_id, tipo_documento, nombre, tamano_bytes, sha256,
                    tipo_detectado, clave_local, drive_id, drive_id_reservado, drive_carpeta_id, drive_enlace,
                    estado_copia, copiado_en, subido_por, subido_en)
                values (?1, ?2, ?3, ?4, ?5, ?6, ?7, ?8, ?9, null, ?10, ?10, ?11, ?12, 'COPIADO', ?13, ?14, ?13)
                """)
                .setParameter(1, adjunto.id()).setParameter(2, adjunto.pedidoId())
                .setParameter(3, (short) adjunto.etapa()).setParameter(4, adjunto.entradaId())
                .setParameter(5, adjunto.tipoDocumento()).setParameter(6, adjunto.nombre())
                .setParameter(7, adjunto.tamanoBytes()).setParameter(8, adjunto.sha256())
                .setParameter(9, adjunto.tipoDetectado()).setParameter(10, drive.driveId())
                .setParameter(11, drive.carpetaId()).setParameter(12, drive.enlace())
                .setParameter(13, java.sql.Timestamp.from(adjunto.subidoEn())).setParameter(14, adjunto.subidoPor())
                .executeUpdate();
    }

    @Override
    public Optional<Adjunto> porId(UUID adjuntoId) {
        return Optional.ofNullable(entityManager.find(AdjuntoEntity.class, adjuntoId))
                .map(AdjuntoJpaRepositorio::aDominio);
    }

    @Override
    public List<Adjunto> porIds(Collection<UUID> adjuntoIds) {
        return adjuntoIds.isEmpty() ? List.of()
                : datos.porIds(adjuntoIds).stream().map(AdjuntoJpaRepositorio::aDominio).toList();
    }

    @Override
    public List<Adjunto> deEtapa(UUID pedidoId, int etapa) {
        return datos.deEtapa(pedidoId, (short) etapa).stream().map(AdjuntoJpaRepositorio::aDominio).toList();
    }

    @Override
    public Set<String> tiposDeEntrada(UUID entradaId) {
        return Set.copyOf(datos.tiposDeEntrada(entradaId));
    }

    @Override
    public Map<UUID, UUID> eventosDeSubida(Collection<UUID> adjuntoIds) {
        Map<UUID, UUID> eventos = new HashMap<>();
        if (adjuntoIds.isEmpty()) {
            return eventos;
        }
        for (Object[] fila : datos.eventosDeSubida(adjuntoIds)) {
            eventos.put(UUID.fromString((String) fila[0]), UUID.fromString((String) fila[1]));
        }
        return eventos;
    }

    private static Adjunto aDominio(AdjuntoEntity fila) {
        return new Adjunto(fila.getId(), fila.getPedidoId(), fila.getEtapa(), fila.getEntradaId(),
                fila.getTipoDocumento(), fila.getNombre(), fila.getTamanoBytes(), fila.getSha256(),
                fila.getTipoDetectado(), fila.getClaveLocal(), EstadoCopia.valueOf(fila.getEstadoCopia()),
                fila.getSubidoPor(), fila.getSubidoEn());
    }
}
