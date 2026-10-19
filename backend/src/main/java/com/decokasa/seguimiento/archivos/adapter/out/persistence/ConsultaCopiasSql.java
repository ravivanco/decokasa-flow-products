package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.ConsultaCopias;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public List<UUID> fallidos(int pagina,int tamano) {
        return sql.query("""
            select id from adjunto where estado_copia='FALLIDO' order by subido_en desc,id
              limit :limite offset :inicio
            """,Map.of("limite",tamano,"inicio",Math.multiplyExact((long)pagina,tamano)),
            (r,n)->r.getObject(1,UUID.class));
    }
}
