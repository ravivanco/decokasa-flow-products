package com.decokasa.seguimiento.asistente.adapter.out.persistence;

import com.decokasa.seguimiento.asistente.application.port.out.TarifasIA;
import com.decokasa.seguimiento.asistente.domain.Tarifa;
import com.decokasa.seguimiento.compartido.domain.DatoDuplicado;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** precio_modelo_ia (V21). Un trigger impide modificar una tarifa salvo marcarla como validada una vez. */
@Repository
public class TarifasIASql implements TarifasIA {

    static final String COLUMNAS = "id, modelo, vigente_desde, entrada_micro_usd_mtok, salida_micro_usd_mtok, "
            + "escritura_cache_micro_usd_mtok, lectura_cache_micro_usd_mtok, umbral_tokens_entrada, "
            + "entrada_superior_micro_usd_mtok, salida_superior_micro_usd_mtok, "
            + "escritura_cache_superior_micro_usd_mtok, lectura_cache_superior_micro_usd_mtok, fuente, consultado_en, "
            + "registrado_por, registrado_en, validado_por, validado_en";

    private final NamedParameterJdbcTemplate sql;

    public TarifasIASql(DataSource fuente) {
        this.sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TarifaRegistrada> listar() {
        return sql.query("select " + COLUMNAS + " from precio_modelo_ia order by modelo, vigente_desde desc",
                Map.of(), (fila, n) -> registrada(fila));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TarifaRegistrada> porId(UUID tarifaId) {
        return sql.query("select " + COLUMNAS + " from precio_modelo_ia where id = :id", Map.of("id", tarifaId),
                (fila, n) -> registrada(fila)).stream().findFirst();
    }

    @Override
    @Transactional
    public void registrar(Tarifa tarifa, UUID registradaPor, Instant ahora) {
        Tarifa.Precios superior = tarifa.superior();
        try {
            sql.update("""
                    insert into precio_modelo_ia (id, modelo, vigente_desde, entrada_micro_usd_mtok,
                        salida_micro_usd_mtok, escritura_cache_micro_usd_mtok, lectura_cache_micro_usd_mtok,
                        umbral_tokens_entrada, entrada_superior_micro_usd_mtok, salida_superior_micro_usd_mtok,
                        escritura_cache_superior_micro_usd_mtok, lectura_cache_superior_micro_usd_mtok, fuente,
                        consultado_en, registrado_por, registrado_en)
                    values (:id, :modelo, :vigente, :entrada, :salida, :escritura, :lectura, :umbral, :entradaSup,
                            :salidaSup, :escrituraSup, :lecturaSup, :fuente, :consultado, :por, :en)
                    """, new MapSqlParameterSource()
                    .addValue("id", tarifa.id())
                    .addValue("modelo", tarifa.modelo())
                    .addValue("vigente", tarifa.vigenteDesde())
                    .addValue("entrada", tarifa.base().entrada())
                    .addValue("salida", tarifa.base().salida())
                    .addValue("escritura", tarifa.base().escrituraCache())
                    .addValue("lectura", tarifa.base().lecturaCache())
                    .addValue("umbral", tarifa.umbralTokensEntrada())
                    .addValue("entradaSup", superior == null ? null : superior.entrada())
                    .addValue("salidaSup", superior == null ? null : superior.salida())
                    .addValue("escrituraSup", superior == null ? null : superior.escrituraCache())
                    .addValue("lecturaSup", superior == null ? null : superior.lecturaCache())
                    .addValue("fuente", tarifa.fuente())
                    .addValue("consultado", tarifa.consultadoEn())
                    .addValue("por", registradaPor)
                    .addValue("en", Timestamp.from(ahora)));
        } catch (DuplicateKeyException duplicada) {
            throw new DatoDuplicado("vigenteDesde", "Ya hay una tarifa de ese modelo para esa fecha.");
        }
    }

    @Override
    @Transactional
    public boolean validar(UUID tarifaId, UUID validadaPor, Instant ahora) {
        return sql.update("""
                update precio_modelo_ia set validado_por = :por, validado_en = :en
                 where id = :id and validado_en is null
                """, new MapSqlParameterSource()
                .addValue("id", tarifaId)
                .addValue("por", validadaPor)
                .addValue("en", Timestamp.from(ahora))) == 1;
    }

    static Tarifa tarifa(ResultSet fila) throws SQLException {
        Integer umbral = fila.getObject("umbral_tokens_entrada", Integer.class);
        Tarifa.Precios superior = umbral == null ? null : new Tarifa.Precios(
                fila.getLong("entrada_superior_micro_usd_mtok"), fila.getLong("salida_superior_micro_usd_mtok"),
                fila.getLong("escritura_cache_superior_micro_usd_mtok"),
                fila.getLong("lectura_cache_superior_micro_usd_mtok"));
        return new Tarifa(fila.getObject("id", UUID.class), fila.getString("modelo"),
                fila.getObject("vigente_desde", LocalDate.class),
                new Tarifa.Precios(fila.getLong("entrada_micro_usd_mtok"), fila.getLong("salida_micro_usd_mtok"),
                        fila.getLong("escritura_cache_micro_usd_mtok"), fila.getLong("lectura_cache_micro_usd_mtok")),
                umbral, superior, fila.getString("fuente"), fila.getObject("consultado_en", LocalDate.class),
                fila.getTimestamp("validado_en") != null);
    }

    private static TarifaRegistrada registrada(ResultSet fila) throws SQLException {
        Timestamp validada = fila.getTimestamp("validado_en");
        return new TarifaRegistrada(tarifa(fila), fila.getObject("registrado_por", UUID.class),
                fila.getTimestamp("registrado_en").toInstant(), fila.getObject("validado_por", UUID.class),
                validada == null ? null : validada.toInstant());
    }
}
