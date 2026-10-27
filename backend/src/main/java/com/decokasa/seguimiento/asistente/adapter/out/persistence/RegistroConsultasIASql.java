package com.decokasa.seguimiento.asistente.adapter.out.persistence;

import com.decokasa.seguimiento.asistente.application.port.out.RegistroConsultasIA;
import java.sql.Timestamp;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** consulta_ia (V20): solo inserción y sin texto. Un trigger impide cambiarla o borrarla. */
@Repository
public class RegistroConsultasIASql implements RegistroConsultasIA {

    private final NamedParameterJdbcTemplate sql;

    public RegistroConsultasIASql(DataSource fuente) {
        this.sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    @Transactional
    public void registrar(ConsultaRegistrada consulta) {
        sql.update("""
                insert into consulta_ia (id, conversacion_id, usuario_id, pedido_id, pais_codigo, modelo,
                                         tokens_entrada, tokens_salida, tokens_cache_escritura,
                                         tokens_cache_lectura, costo_micro_usd, resultado, documentos_enviados,
                                         creada_en)
                values (:id, :conversacion, :usuario, :pedido, :pais, :modelo, :entrada, :salida, :escritura,
                        :lectura, :costo, :resultado, cast(:enviados as uuid[]), :creada)
                """, new MapSqlParameterSource()
                .addValue("id", consulta.id())
                .addValue("conversacion", consulta.conversacionId())
                .addValue("usuario", consulta.usuarioId())
                .addValue("pedido", consulta.pedidoId())
                .addValue("pais", consulta.pais().name())
                .addValue("modelo", consulta.modelo())
                .addValue("entrada", consulta.consumo().entrada())
                .addValue("salida", consulta.consumo().salida())
                .addValue("escritura", consulta.consumo().escrituraCache())
                .addValue("lectura", consulta.consumo().lecturaCache())
                .addValue("costo", consulta.costoMicroUsd())
                .addValue("resultado", consulta.resultado().name())
                .addValue("enviados", ConversacionesIASql.arregloUuid(consulta.documentosEnviados()))
                .addValue("creada", Timestamp.from(consulta.creadaEn())));
    }
}
