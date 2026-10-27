package com.decokasa.seguimiento.notificaciones.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.notificaciones.application.port.out.AvisoRepositorio;
import com.decokasa.seguimiento.notificaciones.domain.AvisoEtapa;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** aviso_etapa (V27, solo inserción) y aviso_lectura (V26). Corre en la transacción del llamador. */
@Repository
@Transactional(propagation = Propagation.MANDATORY)
public class AvisoEtapaSql implements AvisoRepositorio {

    private static final String DE_LA_PERSONA = " from aviso_etapa a where a.usuario_id = :usuario"
            + " and a.pais_codigo in (:paises)";

    private final EntityManager entityManager;
    private final NamedParameterJdbcTemplate sql;

    public AvisoEtapaSql(EntityManager entityManager, DataSource fuente) {
        this.entityManager = entityManager;
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public boolean insertarSiAusente(AvisoEtapa a) {
        // El pedido (en la creación) y el evento se agregaron con JPA en esta transacción: se escriben antes, también
        // para la fila del outbox que se inserta a continuación.
        entityManager.flush();
        return sql.update("insert into aviso_etapa (id, evento_id, pedido_id, usuario_id, tipo, pais_codigo, tipo_flujo,"
                        + " numero_pedido, etapa_origen, nombre_origen, etapa_destino, nombre_destino, actor_id,"
                        + " actor_nombre, motivo, como_admin, como_responsable, ocurrido_en) values (:id, :evento,"
                        + " :pedido, :usuario, :tipo, :pais, :flujo, :numero, :origen, :nombreOrigen, :destino,"
                        + " :nombreDestino, :actor, :actorNombre, :motivo, :admin, :responsable, :ocurrido)"
                        + " on conflict (evento_id, usuario_id) do nothing",
                new MapSqlParameterSource("id", a.id())
                        .addValue("evento", a.eventoId())
                        .addValue("pedido", a.pedidoId())
                        .addValue("usuario", a.usuarioId())
                        .addValue("tipo", a.tipo().name())
                        .addValue("pais", a.pais())
                        .addValue("flujo", a.tipoFlujo())
                        .addValue("numero", a.numeroPedido())
                        .addValue("origen", a.etapaOrigen() == null ? null : a.etapaOrigen().shortValue(),
                                Types.SMALLINT)
                        .addValue("nombreOrigen", a.nombreOrigen(), Types.VARCHAR)
                        .addValue("destino", (short) a.etapaDestino())
                        .addValue("nombreDestino", a.nombreDestino())
                        .addValue("actor", a.actorId(), Types.OTHER)
                        .addValue("actorNombre", a.actorNombre(), Types.VARCHAR)
                        .addValue("motivo", a.motivo(), Types.VARCHAR)
                        .addValue("admin", a.comoAdmin())
                        .addValue("responsable", a.comoResponsable())
                        .addValue("ocurrido", OffsetDateTime.ofInstant(a.ocurridoEn(), ZoneOffset.UTC))) == 1;
    }

    @Override
    public List<AvisoEtapa> recientes(UUID usuarioId, Set<CodigoPais> paises, Instant desde, int limite) {
        return sql.query("select a.*" + DE_LA_PERSONA + " and a.ocurrido_en >= :desde"
                        + " order by a.ocurrido_en desc, a.id limit :limite",
                parametros(usuarioId, paises).addValue("desde", Timestamp.from(desde)).addValue("limite", limite),
                (f, n) -> new AvisoEtapa(f.getObject("id", UUID.class), f.getObject("evento_id", UUID.class),
                        f.getObject("pedido_id", UUID.class), f.getObject("usuario_id", UUID.class),
                        AvisoEtapa.Tipo.valueOf(f.getString("tipo")), f.getString("pais_codigo"),
                        f.getString("tipo_flujo"), f.getString("numero_pedido"),
                        f.getObject("etapa_origen", Integer.class), f.getString("nombre_origen"),
                        f.getInt("etapa_destino"), f.getString("nombre_destino"),
                        f.getObject("actor_id", UUID.class), f.getString("actor_nombre"), f.getString("motivo"),
                        f.getBoolean("como_admin"), f.getBoolean("como_responsable"),
                        f.getTimestamp("ocurrido_en").toInstant()));
    }

    @Override
    public int contarDespuesDe(UUID usuarioId, Set<CodigoPais> paises, Instant despuesDe) {
        Integer total = sql.queryForObject("select count(*)" + DE_LA_PERSONA + " and a.ocurrido_en > :despues",
                parametros(usuarioId, paises).addValue("despues", Timestamp.from(despuesDe)), Integer.class);
        return total == null ? 0 : total;
    }

    @Override
    public Instant leidasHasta(UUID usuarioId) {
        return sql.queryForObject("select coalesce(l.leidas_hasta, u.creado_en) from usuario u"
                        + " left join aviso_lectura l on l.usuario_id = u.id where u.id = :usuario",
                new MapSqlParameterSource("usuario", usuarioId), Timestamp.class).toInstant();
    }

    @Override
    public void marcarLeidas(UUID usuarioId, Instant hasta) {
        // Nunca retrocede: con dos pestañas, gana la marca más reciente.
        sql.update("insert into aviso_lectura (usuario_id, leidas_hasta) values (:usuario, :hasta)"
                        + " on conflict (usuario_id) do update set leidas_hasta"
                        + " = greatest(aviso_lectura.leidas_hasta, excluded.leidas_hasta)",
                new MapSqlParameterSource("usuario", usuarioId)
                        .addValue("hasta", OffsetDateTime.ofInstant(hasta, ZoneOffset.UTC)));
    }

    private static MapSqlParameterSource parametros(UUID usuarioId, Set<CodigoPais> paises) {
        if (paises.isEmpty()) {
            throw new IllegalArgumentException("Sin países en el alcance");
        }
        return new MapSqlParameterSource("usuario", usuarioId)
                .addValue("paises", paises.stream().map(Enum::name).toList());
    }
}
