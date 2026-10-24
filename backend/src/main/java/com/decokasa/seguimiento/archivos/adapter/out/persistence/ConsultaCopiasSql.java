package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.ConsultaCopias;
import com.decokasa.seguimiento.compartido.domain.CodigoPais;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class ConsultaCopiasSql implements ConsultaCopias {
    private final NamedParameterJdbcTemplate sql;
    public ConsultaCopiasSql(DataSource fuente) { sql=new NamedParameterJdbcTemplate(fuente); }
    public Optional<String> enlaceConfirmado(UUID id) {
        return sql.query("""
            select drive_enlace from adjunto where id=:id and estado_copia='COPIADO'
              and drive_id is not null and drive_enlace is not null and copiado_en is not null
            """,Map.of("id",id),(r,n)->r.getString(1)).stream().findFirst();
    }
    // Fase 7 (ADR-0024): el país sale del pedido del archivo.
    public List<UUID> fallidos(Set<CodigoPais> paises,int pagina,int tamano) {
        if (paises.isEmpty()) { return List.of(); }
        return sql.query("""
            select a.id from adjunto a join pedido p on p.id=a.pedido_id
             where a.estado_copia='FALLIDO' and p.pais_codigo in (:paises)
             order by a.subido_en desc,a.id
             limit :limite offset :inicio
            """,Map.of("paises",paises.stream().map(CodigoPais::name).toList(),
                "limite",tamano,"inicio",Math.multiplyExact((long)pagina,tamano)),
            (r,n)->r.getObject(1,UUID.class));
    }
}
