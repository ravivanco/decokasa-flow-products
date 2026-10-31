package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.adapter.out.almacen.RegistroRespaldo;
import com.decokasa.seguimiento.archivos.domain.CuotaAlmacenamientoExcedida;
import java.util.List;
import java.util.OptionalInt;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Cuota del bucket temporal (V30). La reserva serializa a todos los procesos con un bloqueo consultivo de transacción,
 * así dos subidas simultáneas no superan juntas la cuota. Transacciones cortas, sin red.
 */
public final class RespaldoTemporalSql implements RegistroRespaldo {
    /** Identificador fijo del bloqueo consultivo de la cuota de respaldo. */
    private static final long BLOQUEO = 0x0DEC0CA5A0034L;
    private final JdbcTemplate sql;
    private final TransactionTemplate tx;

    public RespaldoTemporalSql(DataSource fuente, PlatformTransactionManager gestor) {
        this.sql = new JdbcTemplate(fuente);
        this.tx = new TransactionTemplate(gestor);
    }

    @Override
    public void reservar(String clave, long bytes, int partes, long cuota) {
        tx.executeWithoutResult(estado -> {
            sql.queryForList("select pg_advisory_xact_lock(?)", BLOQUEO);
            List<Long> previa = sql.queryForList("select bytes from respaldo_temporal where clave = ?", Long.class, clave);
            long usados = sql.queryForObject("select coalesce(sum(bytes), 0) from respaldo_temporal", Long.class);
            if (!previa.isEmpty()) {
                // Reintento de la misma clave (carga reanudable): ajusta la reserva sin contarla dos veces.
                if (usados - previa.getFirst() + bytes > cuota) { throw new CuotaAlmacenamientoExcedida(); }
                sql.update("update respaldo_temporal set bytes = ?, partes = ?, estado = 'SUBIENDO', "
                        + "actualizado_en = now() where clave = ?", bytes, partes, clave);
                return;
            }
            if (bytes > cuota || usados > cuota - bytes) { throw new CuotaAlmacenamientoExcedida(); }
            sql.update("insert into respaldo_temporal (clave, bytes, partes, estado) values (?, ?, ?, 'SUBIENDO')",
                    clave, bytes, partes);
        });
    }

    @Override
    public void completar(String clave) {
        sql.update("update respaldo_temporal set estado = 'COMPLETO', actualizado_en = now() where clave = ?", clave);
    }

    @Override
    public void marcarLiberando(String clave) {
        sql.update("update respaldo_temporal set estado = 'LIBERANDO', actualizado_en = now() where clave = ?", clave);
    }

    @Override
    public void borrar(String clave) {
        sql.update("delete from respaldo_temporal where clave = ?", clave);
    }

    @Override
    public OptionalInt partes(String clave) {
        List<Integer> filas = sql.queryForList("select partes from respaldo_temporal where clave = ?", Integer.class,
                clave);
        return filas.isEmpty() ? OptionalInt.empty() : OptionalInt.of(filas.getFirst());
    }

    @Override
    public List<Pendiente> paraLimpiar(int limite) {
        // Nunca una clave cuyo adjunto exista sin estar COPIADO: ese respaldo es la única copia duradera.
        return sql.query("""
                select r.clave, r.partes from respaldo_temporal r
                where not exists (select 1 from adjunto a where a.clave_local = r.clave and a.estado_copia <> 'COPIADO')
                  and (r.estado = 'LIBERANDO'
                       or exists (select 1 from adjunto a where a.clave_local = r.clave and a.estado_copia = 'COPIADO')
                       or (r.creado_en < now() - interval '24 hours'
                           and not exists (select 1 from adjunto a where a.clave_local = r.clave)))
                order by r.actualizado_en
                limit ?
                """, (fila, n) -> new Pendiente(fila.getString(1), fila.getInt(2)), limite);
    }

    @Override
    public long usados() {
        return sql.queryForObject("select coalesce(sum(bytes), 0) from respaldo_temporal", Long.class);
    }
}
