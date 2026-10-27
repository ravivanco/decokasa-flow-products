-- Forward-only data migration. Run offline with writers stopped, after V7.
-- Preserve historical owners, sizes and UUIDs; do not reject/delete grandfathered data.
insert into archivo_cuota_cerrojo(id) values(1);
insert into archivo_cuota(adjunto_id,propietario_id,bytes_comprometidos,estado,creada_en)
    select id,subido_por,tamano_bytes,'CONFIRMADA',subido_en from adjunto;
insert into archivo_cuota(adjunto_id,propietario_id,bytes_comprometidos,estado,creada_en)
    select adjunto_reservado_id,usuario_id,tamano_bytes,'RESERVADA',creada_en
    from carga_archivo where estado='RECIBIENDO';
-- Completed uploads already have the same adjunto UUID: no second allocation.
-- Paused/failed/uncommitted uploads stay reserved; no TTL, cleanup or ownership reassignment.
