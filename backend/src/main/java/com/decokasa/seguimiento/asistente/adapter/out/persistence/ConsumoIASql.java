package com.decokasa.seguimiento.asistente.adapter.out.persistence;

import com.decokasa.seguimiento.asistente.application.port.out.ConsultaConsumoIA;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Consumo para el admin desde consulta_ia (sin textos) y presupuesto_ia. */
@Repository
public class ConsumoIASql implements ConsultaConsumoIA {

    private final NamedParameterJdbcTemplate sql;

    public ConsumoIASql(DataSource fuente) {
        this.sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilaConsumo> porDiaYUsuario(Set<CodigoPais> paises, Instant desde, Instant hasta) {
        if (paises.isEmpty()) {
            return List.of();
        }
        return sql.query("""
                select (creada_en at time zone 'America/Guayaquil')::date as fecha, usuario_id,
                       count(*) as consultas,
                       coalesce(sum(tokens_entrada + tokens_cache_escritura + tokens_cache_lectura), 0) as entrada,
                       coalesce(sum(tokens_salida), 0) as salida,
                       coalesce(sum(costo_micro_usd), 0) as costo
                  from consulta_ia
                 where pais_codigo in (:paises) and creada_en >= :desde and creada_en < :hasta
                   and resultado <> 'LIMITE'
                 group by 1, 2
                 order by 1, 2
                """, new MapSqlParameterSource()
                .addValue("paises", paises.stream().map(CodigoPais::name).toList())
                .addValue("desde", Timestamp.from(desde))
                .addValue("hasta", Timestamp.from(hasta)),
                (fila, n) -> new FilaConsumo(fila.getObject("fecha", LocalDate.class),
                        fila.getObject("usuario_id", UUID.class), fila.getLong("consultas"), fila.getLong("entrada"),
                        fila.getLong("salida"), fila.getLong("costo")));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstadoPresupuesto> presupuesto(LocalDate periodo) {
        return sql.query("select limite_micro_usd, consumido_micro_usd, reservado_micro_usd from presupuesto_ia "
                + "where periodo = :periodo", Map.of("periodo", periodo),
                (fila, n) -> new EstadoPresupuesto(fila.getLong("limite_micro_usd"),
                        fila.getLong("consumido_micro_usd"), fila.getLong("reservado_micro_usd")))
                .stream().findFirst();
    }
}
