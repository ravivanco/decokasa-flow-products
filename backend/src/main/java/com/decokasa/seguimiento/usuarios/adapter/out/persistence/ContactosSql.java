package com.decokasa.seguimiento.usuarios.adapter.out.persistence;

import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import com.decokasa.seguimiento.compartido.domain.CodigoRol;
import com.decokasa.seguimiento.usuarios.application.port.in.ConsultarContactos;
import com.decokasa.seguimiento.usuarios.application.port.out.Contactos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Contactos vigentes en SQL (ADR-0032). Participa en la transacción del llamador si la hay; la revalidación previa a un
 * envío de correo corre sin transacción de negocio. Activa = cuenta activa con al menos un rol.
 */
@Repository
public class ContactosSql implements Contactos {

    private static final String ACTIVA = " u.activo and exists (select 1 from usuario_rol ur where ur.usuario_id = u.id)";
    private static final String CON_PAIS =
            " and exists (select 1 from usuario_pais up where up.usuario_id = u.id and up.pais_codigo = :pais)";
    private static final RowMapper<ConsultarContactos.Contacto> CONTACTO = (f, n) -> new ConsultarContactos.Contacto(
            f.getObject("id", UUID.class), f.getString("nombre"), f.getString("correo"));

    private final NamedParameterJdbcTemplate sql;

    public ContactosSql(DataSource fuente) {
        sql = new NamedParameterJdbcTemplate(fuente);
    }

    @Override
    public List<ConsultarContactos.Contacto> activosConRol(CodigoRol rol, CodigoPais pais) {
        return sql.query("select u.id, u.nombre, u.correo from usuario u where" + ACTIVA + CON_PAIS
                        + " and exists (select 1 from usuario_rol ur join rol r on r.id = ur.rol_id"
                        + " where ur.usuario_id = u.id and r.codigo = :rol) order by u.nombre, u.id",
                new MapSqlParameterSource("pais", pais.name()).addValue("rol", rol.valor()), CONTACTO);
    }

    @Override
    public Optional<ConsultarContactos.Contacto> activoEnPais(UUID usuarioId, CodigoPais pais) {
        return sql.query("select u.id, u.nombre, u.correo from usuario u where u.id = :usuario and" + ACTIVA + CON_PAIS,
                new MapSqlParameterSource("usuario", usuarioId).addValue("pais", pais.name()), CONTACTO)
                .stream().findFirst();
    }

    @Override
    public Optional<String> nombre(UUID usuarioId) {
        return sql.queryForList("select nombre from usuario where id = :usuario",
                new MapSqlParameterSource("usuario", usuarioId), String.class).stream().findFirst();
    }

    @Override
    public boolean correoAutorizado(CodigoPais pais, String correo) {
        Integer total = sql.queryForObject("select count(*) from usuario u where lower(btrim(u.correo)) ="
                        + " lower(btrim(:correo)) and" + ACTIVA + CON_PAIS,
                new MapSqlParameterSource("correo", correo).addValue("pais", pais.name()), Integer.class);
        return total != null && total > 0;
    }
}
