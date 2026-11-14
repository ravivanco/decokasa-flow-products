package com.decokasa.seguimiento.notificaciones.adapter.out.persistence;

import com.decokasa.seguimiento.notificaciones.application.port.out.AlertaWhatsApp;
import com.decokasa.seguimiento.notificaciones.application.port.out.ColaWhatsApp;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Alertas de WhatsApp: una fila por evento y persona, en la transacción de la acción. Con WHATSAPP_MODO=simulado (por
 * defecto) quedan SIMULADO y nada sale del servidor; con cloud quedan PENDIENTE para el trabajador; con apagado, nada.
 */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class WhatsAppSimuladoSql implements AlertaWhatsApp, ColaWhatsApp {

    private final NamedParameterJdbcTemplate sql;
    private final String modo;

    public WhatsAppSimuladoSql(DataSource fuente, @Value("${decokasa.whatsapp.modo:simulado}") String modo) {
        sql = new NamedParameterJdbcTemplate(fuente);
        this.modo = modo == null ? "simulado" : modo.strip().toLowerCase(java.util.Locale.ROOT);
    }

    @Override
    public void simular(UUID pedidoId, UUID eventoId, UUID usuarioId, String texto, Instant ahora) {
        if ("apagado".equals(modo)) {
            return;
        }
        sql.update("""
                insert into notificacion_whatsapp (id, pedido_id, evento_id, usuario_id, texto, estado, creado_en)
                values (:id, :pedido, :evento, :usuario, :texto, :estado, :ahora)
                on conflict on constraint notificacion_whatsapp_unica do nothing
                """, new MapSqlParameterSource("id", UUID.randomUUID()).addValue("pedido", pedidoId)
                .addValue("evento", eventoId).addValue("usuario", usuarioId).addValue("texto", texto)
                .addValue("ahora", Timestamp.from(ahora)).addValue("estado", "cloud".equals(modo) ? "PENDIENTE" : "SIMULADO"));
    }

    @Override
    public List<Pendiente> reclamar(int lote) {
        return sql.query("""
                update notificacion_whatsapp w set estado = 'ENVIANDO'
                where w.id in (select id from notificacion_whatsapp where estado = 'PENDIENTE'
                  order by creado_en limit :lote for update skip locked)
                returning w.id, w.texto, (select u.telefono from usuario u where u.id = w.usuario_id) as telefono
                """, new MapSqlParameterSource("lote", lote), (r, n) -> new Pendiente(r.getObject("id", UUID.class),
                r.getString("telefono"), r.getString("texto")));
    }

    @Override
    public void enviada(UUID id, String proveedorId, Instant ahora) {
        sql.update("update notificacion_whatsapp set estado = 'ENVIADA', proveedor_id = :proveedor, enviado_en = :ahora"
                + " where id = :id", new MapSqlParameterSource("id", id).addValue("proveedor", proveedorId)
                .addValue("ahora", Timestamp.from(ahora)));
    }

    @Override
    public void fallida(UUID id, String causa, Instant ahora) {
        sql.update("update notificacion_whatsapp set estado = 'FALLIDA', error = :causa, enviado_en = :ahora"
                + " where id = :id", new MapSqlParameterSource("id", id).addValue("causa", causa)
                .addValue("ahora", Timestamp.from(ahora)));
    }
}
