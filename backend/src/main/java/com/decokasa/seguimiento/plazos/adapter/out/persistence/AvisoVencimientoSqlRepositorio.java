package com.decokasa.seguimiento.plazos.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.ClaseVencimiento;
import com.decokasa.seguimiento.plazos.application.port.out.AvisosVencimientoRepositorio;
import com.decokasa.seguimiento.plazos.domain.AvisoVencimiento;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Adaptador SQL de {@link AvisosVencimientoRepositorio} sobre aviso_vencimiento (V15, solo inserción). La columna de
 * referencia sale del enum, nunca de datos de entrada. Cada clase se inserta con su propia columna para no enlazar
 * parámetros nulos sin tipo.
 */
@Component
public class AvisoVencimientoSqlRepositorio implements AvisosVencimientoRepositorio {

    private final EntityManager entityManager;

    public AvisoVencimientoSqlRepositorio(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public boolean existe(ClaseVencimiento clase, UUID referenciaId, Instant venceEn) {
        Number total = (Number) entityManager.createNativeQuery("select count(*) from aviso_vencimiento"
                        + " where clase = :clase and " + columna(clase) + " = :referencia and vence_en = :vence")
                .setParameter("clase", clase.name())
                .setParameter("referencia", referenciaId)
                .setParameter("vence", venceEn)
                .getSingleResult();
        return total.longValue() > 0;
    }

    @Override
    public void registrar(AvisoVencimiento aviso) {
        // El evento del historial se agregó con persist en esta transacción: se escribe antes (FK aviso → evento).
        entityManager.flush();
        Query insercion = entityManager.createNativeQuery("insert into aviso_vencimiento"
                        + " (id, pedido_id, clase, etapa, " + columna(aviso.clase())
                        + ", vence_en, evento_id, registrado_en)"
                        + " values (:id, :pedido, :clase, :etapa, :referencia, :vence, :evento, :registrado)")
                .setParameter("id", aviso.id())
                .setParameter("pedido", aviso.pedidoId())
                .setParameter("clase", aviso.clase().name())
                .setParameter("etapa", (short) aviso.etapa())
                .setParameter("referencia", aviso.referenciaId())
                .setParameter("vence", aviso.venceEn())
                .setParameter("evento", aviso.eventoId())
                .setParameter("registrado", aviso.registradoEn());
        insercion.executeUpdate();
    }

    private static String columna(ClaseVencimiento clase) {
        return clase == ClaseVencimiento.ETAPA ? "entrada_id" : "urgencia_activacion_id";
    }
}
