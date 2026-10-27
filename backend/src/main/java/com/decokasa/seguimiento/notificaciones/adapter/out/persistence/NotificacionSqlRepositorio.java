package com.decokasa.seguimiento.notificaciones.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.notificaciones.application.port.out.NotificacionRepositorio;
import com.decokasa.seguimiento.notificaciones.domain.Notificacion;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class NotificacionSqlRepositorio implements NotificacionRepositorio,
        com.decokasa.seguimiento.notificaciones.application.port.out.RegistroNotificaciones {
    private final NamedParameterJdbcTemplate sql;
    private final JsonMapper json;
    public NotificacionSqlRepositorio(DataSource fuente, JsonMapper json) {
        this.sql = new NamedParameterJdbcTemplate(fuente); this.json = json;
    }
    @Override public boolean insertarSiAusente(Notificacion n) {
        return sql.update("""
                insert into notificacion (id,pedido_id,evento_id,destinatario,plantilla,asunto,datos,creada_en,
                  proximo_intento_en,message_id) values (:id,:pedido,:evento,:destinatario,:plantilla,:asunto,
                  cast(:datos as jsonb),:creada,:creada,:message)
                on conflict on constraint notificacion_evento_destinatario do nothing
                """, new MapSqlParameterSource("id", n.id()).addValue("pedido", n.pedidoId())
                .addValue("evento", n.eventoId()).addValue("destinatario", n.destinatario())
                .addValue("plantilla", n.plantilla()).addValue("asunto", n.asunto())
                .addValue("datos", json.writeValueAsString(n.datos())).addValue("creada", Timestamp.from(n.creadaEn()))
                .addValue("message", n.messageId())) == 1;
    }
    @Override public List<Notificacion> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario) {
        return reclamar(limite, ahora, hasta, propietario, null);
    }
    /**
     * ADR-0032: con {@code desde}, solo filas creadas desde ese instante. Así activar Gmail nunca envía mensajes
     * anteriores a la activación (outbox histórico).
     */
    public List<Notificacion> reclamar(int limite, Instant ahora, Instant hasta, UUID propietario, Instant desde) {
        if (limite < 1 || limite > 100 || !hasta.isAfter(ahora) || propietario == null) {
            throw new IllegalArgumentException("Reclamación inválida");
        }
        // Se marca incertidumbre ANTES del envío. Una caída no permite reenviar ciegamente al vencer el lease.
        return sql.query("""
                with lote as (select id from notificacion where estado = 'PENDIENTE'
                  and destinatario is not null and not resultado_incierto and intentos < 3
                  and creada_en >= :desde
                  and (proximo_intento_en is null or proximo_intento_en <= :ahora)
                  and (reclamado_hasta is null or reclamado_hasta <= :ahora)
                  order by creada_en,id limit :limite for update skip locked)
                update notificacion n set reclamado_por = :propietario, reclamado_hasta = :hasta,
                  intentos = intentos + 1, estado = 'FALLIDA', resultado_incierto = true,
                  ultimo_error = 'RESULTADO_INCIERTO' from lote where n.id = lote.id returning n.*
                """, Map.of("limite", limite, "ahora", Timestamp.from(ahora), "hasta", Timestamp.from(hasta),
                "propietario", propietario,
                "desde", Timestamp.from(desde == null ? Instant.parse("1970-01-01T00:00:00Z") : desde)), this::leer);
    }
    @Override public boolean marcarEnviada(UUID id, UUID propietario, String proveedorId, Instant ahora) {
        if (proveedorId == null || !proveedorId.matches("[A-Za-z0-9_-]{1,200}")) {
            throw new IllegalArgumentException("Id Gmail inválido");
        }
        return sql.update("""
                update notificacion set estado = 'ENVIADA', enviada_en = :ahora, proveedor_id = :proveedor,
                  resultado_incierto = false, ultimo_error = null, proximo_intento_en = null,
                  reclamado_por = null, reclamado_hasta = null where id = :id and reclamado_por = :propietario
                  and reclamado_hasta > :ahora and estado = 'FALLIDA' and resultado_incierto
                """, new MapSqlParameterSource("id",id).addValue("propietario",propietario)
                .addValue("proveedor",proveedorId).addValue("ahora",Timestamp.from(ahora))) == 1;
    }
    @Override public boolean registrarFallo(UUID id, UUID propietario, Causa causa, Instant siguiente, boolean terminal, Instant ahora) {
        if (!terminal && siguiente == null) { throw new IllegalArgumentException("Falta próxima fecha"); }
        if (!terminal && (causa!=Causa.CUOTA && causa!=Causa.TRANSITORIA
                || !siguiente.isAfter(ahora))) { throw new IllegalArgumentException("Reintento invalido"); }
        boolean incierto = causa == Causa.RESULTADO_INCIERTO;
        return sql.update("""
                update notificacion set estado = case when intentos>=3 then 'FALLIDA' else :estado end,
                  resultado_incierto = :incierto, ultimo_error = :causa,
                  proximo_intento_en = case when intentos>=3 then null else cast(:siguiente as timestamptz) end,
                  reclamado_por = null, reclamado_hasta = null
                where id = :id and reclamado_por = :propietario and reclamado_hasta > :ahora
                  and estado = 'FALLIDA' and resultado_incierto
                """, new MapSqlParameterSource("id",id).addValue("propietario",propietario)
                .addValue("ahora",Timestamp.from(ahora))
                .addValue("estado",terminal || incierto ? "FALLIDA" : "PENDIENTE").addValue("incierto",incierto)
                .addValue("causa",causa.name()).addValue("siguiente",siguiente == null ? null : Timestamp.from(siguiente))) == 1;
    }
    @Override public boolean resolverDestinatario(UUID id, String destinatario) {
        return sql.update("""
                update notificacion n set destinatario = :destinatario where n.id = :id
                  and n.destinatario is null and n.estado = 'PENDIENTE' and n.intentos = 0
                  and not exists (select 1 from notificacion d where d.evento_id = n.evento_id
                    and d.destinatario_clave = :destinatario)
                """, Map.of("id",id,"destinatario",Notificacion.normalizarCorreo(destinatario))) == 1;
    }
    @Override public List<Notificacion> listar(Notificacion.Estado estado, int pagina, int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) { throw new IllegalArgumentException("Página inválida"); }
        return sql.query("select * from notificacion where (:estado is null or estado = :estado) order by creada_en desc,id limit :limite offset :inicio",
                new MapSqlParameterSource().addValue("estado",estado == null ? null : estado.name(),java.sql.Types.VARCHAR)
                    .addValue("limite",tamano).addValue("inicio",Math.multiplyExact((long)pagina,tamano)),this::leer);
    }
    // Fase 7 (ADR-0024): el país sale del pedido del correo (notificacion.pedido_id es obligatorio, V4).
    private static final String DE_PAISES = " from notificacion n join pedido p on p.id = n.pedido_id"
            + " where p.pais_codigo in (:paises) and (:estado is null or n.estado = :estado)";
    @Override public List<Notificacion> listar(Set<CodigoPais> paises, Notificacion.Estado estado, int pagina,
            int tamano) {
        if (pagina < 0 || tamano < 1 || tamano > 100) { throw new IllegalArgumentException("Página inválida"); }
        if (paises.isEmpty()) { return List.of(); }
        return sql.query("select n.*" + DE_PAISES + " order by n.creada_en desc, n.id limit :limite offset :inicio",
                new MapSqlParameterSource().addValue("paises", paises.stream().map(CodigoPais::name).toList())
                    .addValue("estado", estado == null ? null : estado.name(), java.sql.Types.VARCHAR)
                    .addValue("limite", tamano).addValue("inicio", Math.multiplyExact((long) pagina, tamano)),
                this::leer);
    }
    @Override public long contar(Set<CodigoPais> paises, Notificacion.Estado estado) {
        if (paises.isEmpty()) { return 0; }
        Long total = sql.queryForObject("select count(*)" + DE_PAISES,
                new MapSqlParameterSource().addValue("paises", paises.stream().map(CodigoPais::name).toList())
                    .addValue("estado", estado == null ? null : estado.name(), java.sql.Types.VARCHAR), Long.class);
        return total == null ? 0 : total;
    }
    @Override public long contar(Notificacion.Estado estado) {
        Long total=sql.queryForObject("select count(*) from notificacion where (:estado is null or estado = :estado)",
            new MapSqlParameterSource().addValue("estado",estado == null ? null : estado.name(),java.sql.Types.VARCHAR),Long.class);
        return total == null ? 0 : total;
    }
    public boolean vigente(UUID id, UUID propietario, Instant ahora) {
        return Boolean.TRUE.equals(sql.queryForObject("""
            select exists(select 1 from notificacion where id=:id and reclamado_por=:propietario
              and reclamado_hasta > :ahora and estado='FALLIDA' and resultado_incierto)
            """,Map.of("id",id,"propietario",propietario,"ahora",Timestamp.from(ahora)),Boolean.class));
    }
    private Notificacion leer(ResultSet r, int numero) throws SQLException {
        Map<String,String> datos = new HashMap<>();
        json.readTree(r.getString("datos")).properties().forEach(e -> datos.put(e.getKey(),e.getValue().asString()));
        return new Notificacion(r.getObject("id",UUID.class),r.getObject("pedido_id",UUID.class),
                r.getObject("evento_id",UUID.class),r.getString("destinatario"),r.getString("plantilla"),
                r.getString("asunto"),datos,Notificacion.Estado.valueOf(r.getString("estado")),r.getInt("intentos"),
                instante(r,"proximo_intento_en"),r.getString("ultimo_error"),instante(r,"creada_en"),
                instante(r,"enviada_en"),r.getString("proveedor_id"),r.getString("message_id"),r.getBoolean("resultado_incierto"));
    }
    private static Instant instante(ResultSet r, String campo) throws SQLException {
        Timestamp valor = r.getTimestamp(campo); return valor == null ? null : valor.toInstant();
    }
}
