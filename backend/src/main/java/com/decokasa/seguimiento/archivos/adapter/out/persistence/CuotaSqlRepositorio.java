package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.CuotaAlmacenamiento;
import com.decokasa.seguimiento.archivos.domain.CuotaAlmacenamientoExcedida;
import com.decokasa.seguimiento.archivos.domain.ProblemaCarga;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Serializes admissions across JVMs. No HTTP or filesystem operations under this lock. */
@Transactional(propagation=Propagation.MANDATORY)
public class CuotaSqlRepositorio implements CuotaAlmacenamiento {
    private final NamedParameterJdbcTemplate sql;
    private final long globalBytes;
    private final long usuarioBytes;
    private final int globalCargas;
    private final int usuarioCargas;

    public CuotaSqlRepositorio(DataSource fuente, long globalBytes, long usuarioBytes,
            int globalCargas, int usuarioCargas) {
        if (globalBytes<1 || usuarioBytes<1 || usuarioBytes>globalBytes
                || globalCargas<1 || usuarioCargas<1 || usuarioCargas>globalCargas) {
            throw new IllegalArgumentException("Cuotas de almacenamiento invalidas");
        }
        this.sql=new NamedParameterJdbcTemplate(fuente);
        this.globalBytes=globalBytes; this.usuarioBytes=usuarioBytes;
        this.globalCargas=globalCargas; this.usuarioCargas=usuarioCargas;
    }
    @Override public void reservar(UUID id, UUID propietario, long bytes) {
        validar(id,propietario,bytes);
        bloquear();
        var anteriores=sql.query("""
            select propietario_id,bytes_comprometidos from archivo_cuota where adjunto_id=:id
            """,Map.of("id",id),(r,n) -> new Reserva(r.getObject(1,UUID.class),r.getLong(2)));
        if (!anteriores.isEmpty()) {
            var anterior=anteriores.getFirst();
            if (!anterior.propietario().equals(propietario) || anterior.bytes()!=bytes) {
                throw ProblemaCarga.conflicto();
            }
            return; // Same upload/resume: no second reservation, including completed uploads.
        }
        var uso=sql.queryForObject("""
            select coalesce(sum(bytes_comprometidos),0) as global_bytes,
              coalesce(sum(bytes_comprometidos) filter(where propietario_id=:usuario),0) as usuario_bytes,
              count(*) filter(where estado='RESERVADA') as global_cargas,
              count(*) filter(where estado='RESERVADA' and propietario_id=:usuario) as usuario_cargas
            from archivo_cuota
            """,Map.of("usuario",propietario),(r,n) -> new Uso(r.getLong(1),r.getLong(2),r.getLong(3),r.getLong(4)));
        if (uso==null || bytes>globalBytes || bytes>usuarioBytes
                || uso.globalBytes()>globalBytes-bytes || uso.usuarioBytes()>usuarioBytes-bytes
                || uso.globalCargas()>=globalCargas || uso.usuarioCargas()>=usuarioCargas) {
            throw new CuotaAlmacenamientoExcedida();
        }
        sql.update("""
            insert into archivo_cuota(adjunto_id,propietario_id,bytes_comprometidos,estado)
            values(:id,:usuario,:bytes,'RESERVADA')
            """,Map.of("id",id,"usuario",propietario,"bytes",bytes));
    }
    @Override public void confirmar(UUID id, UUID propietario, long bytes) {
        validar(id,propietario,bytes);
        bloquear();
        if (sql.update("""
            update archivo_cuota set bytes_comprometidos=:bytes,estado='CONFIRMADA'
            where adjunto_id=:id and propietario_id=:usuario and bytes_comprometidos>=:bytes
              and (estado='RESERVADA' or bytes_comprometidos=:bytes)
            """,Map.of("id",id,"usuario",propietario,"bytes",bytes))!=1) {
            throw ProblemaCarga.conflicto();
        }
    }
    private void bloquear() {
        // UPDATE, rather than a read-only lock: concurrent REPEATABLE READ transactions
        // serialize/fail instead of checking an obsolete snapshot and overbooking.
        if (sql.update("update archivo_cuota_cerrojo set version=version+1 where id=1",Map.of())!=1) {
            throw ProblemaCarga.almacenamiento();
        }
    }
    private static void validar(UUID id, UUID propietario, long bytes) {
        if (id==null || propietario==null || bytes<1) { throw new IllegalArgumentException("Reserva invalida"); }
    }
    private record Reserva(UUID propietario,long bytes) {}
    private record Uso(long globalBytes,long usuarioBytes,long globalCargas,long usuarioCargas) {}
}
