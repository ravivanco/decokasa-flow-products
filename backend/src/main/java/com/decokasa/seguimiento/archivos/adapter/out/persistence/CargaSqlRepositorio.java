package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CargaRepositorio;
import com.decokasa.seguimiento.archivos.domain.CargaArchivo;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Misma DataSource/transacción que el adjunto/historial; todos los parámetros están enlazados. */
@Repository
@Transactional(propagation=Propagation.MANDATORY)
public class CargaSqlRepositorio implements CargaRepositorio {
    private final NamedParameterJdbcTemplate sql;
    public CargaSqlRepositorio(DataSource fuente) { sql=new NamedParameterJdbcTemplate(fuente); }
    @Override public CargaArchivo crearSiAusente(CargaArchivo c) {
        sql.update("""
                insert into carga_archivo (id,usuario_id,clave_idempotencia,pedido_id,etapa,entrada_id,nombre,
                  tipo_documento,tamano_bytes,adjunto_reservado_id,creada_en,actualizada_en)
                values (:id,:usuario,:clave,:pedido,:etapa,:entrada,:nombre,:tipo,:total,:adjunto,:ahora,:ahora)
                on conflict on constraint carga_inicio_idempotente do nothing
                """,new MapSqlParameterSource("id",c.id()).addValue("usuario",c.usuarioId())
                .addValue("clave",c.claveIdempotencia()).addValue("pedido",c.pedidoId()).addValue("etapa",c.etapa())
                .addValue("entrada",c.entradaId()).addValue("nombre",c.nombre()).addValue("tipo",c.tipoDocumento())
                .addValue("total",c.tamanoBytes()).addValue("adjunto",c.adjuntoReservadoId())
                .addValue("ahora",Timestamp.from(c.creadaEn())));
        return sql.query("select * from carga_archivo where usuario_id=:usuario and clave_idempotencia=:clave",
                Map.of("usuario",c.usuarioId(),"clave",c.claveIdempotencia()),this::leer).stream()
                .findFirst().orElseThrow(ProblemaCarga::conflicto);
    }
    @Override public Optional<CargaArchivo> porId(UUID id, boolean bloquear) {
        String consulta=bloquear ? "select * from carga_archivo where id=:id for update"
                : "select * from carga_archivo where id=:id";
        return sql.query(consulta,Map.of("id",id),this::leer).stream().findFirst();
    }
    @Override public List<CargaArchivo.Fragmento> fragmentos(UUID id) {
        return sql.query("select inicio,tamano,sha256 from carga_fragmento where carga_id=:id order by inicio limit 25",
                Map.of("id",id),(r,n) -> new CargaArchivo.Fragmento(r.getLong("inicio"),r.getInt("tamano"),r.getString("sha256")));
    }
    @Override public void avanzar(UUID id, long esperado, CargaArchivo.Fragmento f, Instant ahora) {
        if (sql.update("""
                update carga_archivo set offset_confirmado=:siguiente,actualizada_en=:ahora
                where id=:id and estado='RECIBIENDO' and offset_confirmado=:esperado
                  and :siguiente<=tamano_bytes
                """,Map.of("id",id,"esperado",esperado,"siguiente",esperado+f.tamano(),"ahora",Timestamp.from(ahora)))!=1) {
            throw ProblemaCarga.conflicto();
        }
        sql.update("insert into carga_fragmento(carga_id,inicio,tamano,sha256) values (:id,:inicio,:tamano,:sha)",
                Map.of("id",id,"inicio",f.inicio(),"tamano",f.tamano(),"sha",f.sha256()));
    }
    @Override public void completar(UUID id, UUID adjuntoId, UUID eventoId, Instant ahora) {
        if (sql.update("""
                update carga_archivo set estado='COMPLETADA',adjunto_id=:adjunto,evento_id=:evento,actualizada_en=:ahora
                where id=:id and estado='RECIBIENDO' and offset_confirmado=tamano_bytes and adjunto_reservado_id=:adjunto
                """,Map.of("id",id,"adjunto",adjuntoId,"evento",eventoId,"ahora",Timestamp.from(ahora)))!=1) {
            throw ProblemaCarga.conflicto();
        }
    }
    private CargaArchivo leer(ResultSet r,int numero) throws SQLException {
        return new CargaArchivo(r.getObject("id",UUID.class),r.getObject("usuario_id",UUID.class),
                r.getObject("clave_idempotencia",UUID.class),r.getObject("pedido_id",UUID.class),r.getInt("etapa"),
                r.getObject("entrada_id",UUID.class),r.getString("nombre"),r.getString("tipo_documento"),
                r.getLong("tamano_bytes"),r.getLong("offset_confirmado"),CargaArchivo.Estado.valueOf(r.getString("estado")),
                r.getObject("adjunto_reservado_id",UUID.class),r.getObject("adjunto_id",UUID.class),
                r.getObject("evento_id",UUID.class),r.getTimestamp("creada_en").toInstant(),r.getTimestamp("actualizada_en").toInstant());
    }
}
