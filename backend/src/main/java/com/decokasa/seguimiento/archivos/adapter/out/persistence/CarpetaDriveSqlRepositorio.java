package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CarpetaDriveRepositorio;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle.Causa;
import com.decokasa.seguimiento.compartido.domain.FalloIntegracionGoogle;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class CarpetaDriveSqlRepositorio implements CarpetaDriveRepositorio {
    private final NamedParameterJdbcTemplate sql;
    public CarpetaDriveSqlRepositorio(DataSource fuente) { sql=new NamedParameterJdbcTemplate(fuente); }
    @Override public Optional<Carpeta> buscar(String clave) {
        return sql.query("select * from carpeta_drive where clave=:clave",Map.of("clave",clave),
                (r,n) -> new Carpeta(r.getString("clave"),r.getString("nombre"),r.getString("padre_id"),
                        r.getString("drive_id_reservado"),r.getBoolean("confirmada"))).stream().findFirst();
    }
    @Override public Carpeta reservar(String clave,String nombre,String padre,String driveId,Instant ahora) {
        sql.update("""
                insert into carpeta_drive(clave,nombre,padre_id,drive_id_reservado,creada_en)
                values (:clave,:nombre,:padre,:id,:ahora) on conflict (clave) do nothing
                """,Map.of("clave",clave,"nombre",nombre,"padre",padre,"id",driveId,"ahora",Timestamp.from(ahora)));
        return buscar(clave).orElseThrow(() -> new FalloIntegracionGoogle(Causa.CONFIGURACION));
    }
    @Override public void confirmar(String clave,String driveId) {
        if (sql.update("update carpeta_drive set confirmada=true where clave=:clave and drive_id_reservado=:id",
                Map.of("clave",clave,"id",driveId))!=1) { throw new FalloIntegracionGoogle(Causa.METADATOS_DIVERGENTES); }
    }
}
