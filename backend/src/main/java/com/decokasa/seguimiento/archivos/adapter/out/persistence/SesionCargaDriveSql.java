package com.decokasa.seguimiento.archivos.adapter.out.persistence;

import com.decokasa.seguimiento.archivos.application.port.out.SesionCargaDrive;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Columnas de V31 en carga_archivo. Sentencias cortas; el reinicio va en una transacción propia. */
public final class SesionCargaDriveSql implements SesionCargaDrive {
    private final JdbcTemplate sql;
    private final TransactionTemplate tx;

    public SesionCargaDriveSql(DataSource fuente, PlatformTransactionManager gestor) {
        this.sql = new JdbcTemplate(fuente);
        this.tx = new TransactionTemplate(gestor);
    }

    @Override
    public Optional<Estado> leer(UUID cargaId) {
        List<Estado> filas = sql.query("""
                select drive_id_reservado, drive_carpeta_id, drive_sesion_cifrada from carga_archivo where id = ?
                """, (r, n) -> new Estado(r.getString(1), r.getString(2), r.getBytes(3)), cargaId);
        return filas.isEmpty() ? Optional.empty() : Optional.of(filas.getFirst());
    }

    @Override
    public void reservar(UUID cargaId, String driveId, String carpetaId) {
        sql.update("""
                update carga_archivo set drive_id_reservado = coalesce(drive_id_reservado, ?),
                    drive_carpeta_id = coalesce(drive_carpeta_id, ?), actualizada_en = now()
                where id = ? and estado = 'RECIBIENDO'
                """, driveId, carpetaId, cargaId);
    }

    @Override
    public void guardarSesion(UUID cargaId, byte[] sesionCifrada) {
        sql.update("""
                update carga_archivo set drive_sesion_cifrada = ?, actualizada_en = now()
                where id = ? and estado = 'RECIBIENDO' and drive_id_reservado is not null and drive_carpeta_id is not null
                """, sesionCifrada, cargaId);
    }

    @Override
    public void olvidarSesion(UUID cargaId) {
        sql.update("update carga_archivo set drive_sesion_cifrada = null where id = ? and estado = 'COMPLETADA'", cargaId);
    }

    @Override
    public void reiniciar(UUID cargaId) {
        tx.executeWithoutResult(estado -> {
            int filas = sql.update("""
                    update carga_archivo set offset_confirmado = 0, drive_sesion_cifrada = null,
                        drive_id_reservado = null, drive_carpeta_id = null, actualizada_en = now()
                    where id = ? and estado = 'RECIBIENDO'
                    """, cargaId);
            if (filas == 1) { sql.update("delete from carga_fragmento where carga_id = ?", cargaId); }
        });
    }
}
