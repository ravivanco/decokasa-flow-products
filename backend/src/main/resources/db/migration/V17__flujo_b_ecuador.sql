-- V17: flujo B de Ecuador, versión 1 (fase 6, paso 6.1). Siguiente versión disponible tras V16.
--
-- Fuentes: requerimientos.md 1.5 y 4.5 (documento del cliente y su tabla de transiciones), respuestas del cliente de §5
-- (B5 devuelve a Codificación, que en B es la etapa 3; plazos "24h" de Codificación, Revisión, Autorización de salida y
-- Bodega) y la decisión de Richard del 2026-10-21 sobre los documentos de B1, B3, B7, B10 y B11 (requerimientos §5,
-- "Decisiones confirmadas por Richard"; no es una respuesta del cliente).
--
-- Solo inserta filas nuevas: no modifica la definición del flujo A (V2, V3, V11, V16) ni pedidos en curso. Las reglas
-- comunes (observación, urgencia, llegada, bodega, documentos, Fin automático) usan las mismas columnas que A; el motor
-- no se toca en este paso.
--
-- No se extiende nada por analogía. Siguen pendientes y quedan así en los datos:
--   * Plazo de B2 "Revisión de productos propuestos": la respuesta "24h" nombra "Revisión" sin distinguir B2 de B4
--     (F6-D4) -> PENDIENTE.
--   * Etapas revisoras (revisa_etapa, P-17) de B2 y B4 (F6-D4) -> sin marca. Hasta decidirlas, solo el admin y quien
--     puede actuar en la etapa del archivo lo descargan.
--   * Plazos de Fabricación, Aduana e Incidencias (como A5, A7 y A10) -> PENDIENTE.
--   * Creación de B por el admin (P-14) y habilitación de B11 (EtapasHabilitadas, F6-D3): son reglas del motor de los
--     pasos 6.2 y 6.4, no de esta migración.
--
-- Identificadores fijos, iguales en todos los entornos: UUIDv7 con marca 2026-10-21T00:00:00Z (01a15bc1-f400),
-- versión 7, variante RFC 9562 y número de orden al final (…0001 flujo, …01NN etapas, …02NN transiciones).

insert into definicion_flujo (id, pais_codigo, tipo_flujo, version, vigente, creada_en) values
    ('01a15bc1-f400-7000-8000-000000000001', 'EC', 'B', 1, true, now());

-- Responsables (1.5 y 4.5): B1, B5–B8 Compras China (David); B2 y B4 Asistente de compras (Marcela); B3 Marketing
-- (Andrea Quishpe); B9–B11 Logística (Anderson); B12 Fin sin responsable (S-1).
-- Plazos: 3 días (B5) y 10 días (B7) del documento; 24 h hábiles en B3, B4, B9 y B10 por la respuesta "24h".
insert into definicion_etapa
    (id, flujo_id, numero, nombre, rol_codigo, plazo_estado, plazo_cantidad, plazo_tipo, automatica, revisa_etapa,
     registra_llegada) values
    ('01a15bc1-f400-7000-8000-000000000101', '01a15bc1-f400-7000-8000-000000000001', 1,
        'Crear solicitud de pedido', 'compras_china', 'SIN_PLAZO', null, null, false, null, false),
    ('01a15bc1-f400-7000-8000-000000000102', '01a15bc1-f400-7000-8000-000000000001', 2,
        'Revisión de productos propuestos', 'asistente_compras', 'PENDIENTE', null, null, false, null, false),
    ('01a15bc1-f400-7000-8000-000000000103', '01a15bc1-f400-7000-8000-000000000001', 3,
        'Codificación', 'marketing', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false),
    ('01a15bc1-f400-7000-8000-000000000104', '01a15bc1-f400-7000-8000-000000000001', 4,
        'Revisión y aprobación', 'asistente_compras', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false),
    ('01a15bc1-f400-7000-8000-000000000105', '01a15bc1-f400-7000-8000-000000000001', 5,
        'Análisis y gestión', 'compras_china', 'DEFINIDO', 3, 'DIAS_HABILES', false, null, false),
    ('01a15bc1-f400-7000-8000-000000000106', '01a15bc1-f400-7000-8000-000000000001', 6,
        'Fabricación', 'compras_china', 'PENDIENTE', null, null, false, null, false),
    ('01a15bc1-f400-7000-8000-000000000107', '01a15bc1-f400-7000-8000-000000000001', 7,
        'Embarque', 'compras_china', 'DEFINIDO', 10, 'DIAS_HABILES', false, null, false),
    -- Etapa de llegada a aduana (ADR-0018): su APROBAR es el "✓ Llegada", como A7.
    ('01a15bc1-f400-7000-8000-000000000108', '01a15bc1-f400-7000-8000-000000000001', 8,
        'Aduana Ecuador', 'compras_china', 'PENDIENTE', null, null, false, null, true),
    ('01a15bc1-f400-7000-8000-000000000109', '01a15bc1-f400-7000-8000-000000000001', 9,
        'Autorización de salida', 'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false),
    ('01a15bc1-f400-7000-8000-000000000110', '01a15bc1-f400-7000-8000-000000000001', 10,
        'Bodega', 'logistica', 'DEFINIDO', 24, 'HORAS_HABILES', false, null, false),
    ('01a15bc1-f400-7000-8000-000000000111', '01a15bc1-f400-7000-8000-000000000001', 11,
        'Incidencias', 'logistica', 'PENDIENTE', null, null, false, null, false),
    -- Fin: solo visual; se completa sola al confirmar Incidencias (respuesta del cliente sobre la etapa 12 de B).
    ('01a15bc1-f400-7000-8000-000000000112', '01a15bc1-f400-7000-8000-000000000001', 12,
        'Fin', null, 'SIN_PLAZO', null, null, true, null, false);

-- Transiciones de 4.5: B2 ✗ vuelve a B1; B4 ✗ vuelve a B3; B5 devuelve por error a B3 (nunca a B2) y por producto
-- nuevo a B3 con urgencia, que termina al volver a B5 (la etapa que admite DEVOLVER_POR_PRODUCTO_NUEVO). B6–B12 igual
-- que A5–A11. ELEGIR_BODEGA no se siembra (como en A).
insert into transicion_permitida (id, flujo_id, etapa_origen, accion, etapa_destino, requiere_observacion,
        requiere_urgencia, requiere_bodega, requisitos_documentales, exige_algun_documento) values
    ('01a15bc1-f400-7000-8000-000000000201', '01a15bc1-f400-7000-8000-000000000001', 1, 'ENVIAR', 2,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000202', '01a15bc1-f400-7000-8000-000000000001', 2, 'APROBAR', 3,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000203', '01a15bc1-f400-7000-8000-000000000001', 2, 'RECHAZAR', 1,
        true, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000204', '01a15bc1-f400-7000-8000-000000000001', 3, 'ENVIAR', 4,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000205', '01a15bc1-f400-7000-8000-000000000001', 4, 'APROBAR', 5,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000206', '01a15bc1-f400-7000-8000-000000000001', 4, 'RECHAZAR', 3,
        true, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000207', '01a15bc1-f400-7000-8000-000000000001', 5, 'APROBAR', 6,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000208', '01a15bc1-f400-7000-8000-000000000001', 5, 'DEVOLVER_POR_ERROR', 3,
        true, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000209', '01a15bc1-f400-7000-8000-000000000001', 5,
        'DEVOLVER_POR_PRODUCTO_NUEVO', 3, true, true, false, 'DEFINIDOS', false),
    -- Fabricación: "dar check y observaciones" (como A5).
    ('01a15bc1-f400-7000-8000-000000000210', '01a15bc1-f400-7000-8000-000000000001', 6, 'APROBAR', 7,
        true, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000211', '01a15bc1-f400-7000-8000-000000000001', 7, 'APROBAR', 8,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000212', '01a15bc1-f400-7000-8000-000000000001', 8, 'APROBAR', 9,
        false, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000213', '01a15bc1-f400-7000-8000-000000000001', 8, 'REPORTAR_DEMORA', null,
        true, false, false, 'DEFINIDOS', false),
    ('01a15bc1-f400-7000-8000-000000000214', '01a15bc1-f400-7000-8000-000000000001', 9, 'APROBAR', 10,
        false, false, false, 'DEFINIDOS', false),
    -- Bodega: bodega activa y al menos un documento de cualquier tipo (decisión de Richard, 2026-10-21).
    ('01a15bc1-f400-7000-8000-000000000215', '01a15bc1-f400-7000-8000-000000000001', 10, 'APROBAR', 11,
        false, false, true, 'DEFINIDOS', true),
    -- Incidencias: al menos un documento de cualquier tipo, sin excepción "sin incidencias" (Richard, 2026-10-21).
    ('01a15bc1-f400-7000-8000-000000000216', '01a15bc1-f400-7000-8000-000000000001', 11, 'APROBAR', 12,
        false, false, false, 'DEFINIDOS', true);

-- Documentos con tipo (decisión de Richard, 2026-10-21): Solicitud en B1, Documentos codificados en B3 y Packing List
-- en B7 (BL y otros adjuntos, opcionales). En todas las etapas se admiten adjuntos adicionales; solo cuentan los del
-- pedido, la etapa y la entrada actual (V3, P-05).
insert into documento_requerido (transicion_id, tipo_documento) values
    ('01a15bc1-f400-7000-8000-000000000201', 'Solicitud'),
    ('01a15bc1-f400-7000-8000-000000000204', 'Documentos codificados'),
    ('01a15bc1-f400-7000-8000-000000000211', 'Packing List');

-- Comprobación final: si algo no quedó como se espera, la migración falla y Flyway la revierte entera.
do $$
declare
    flujo_b constant uuid := '01a15bc1-f400-7000-8000-000000000001';
    flujo_a constant uuid := '01a13cdb-cc00-7000-8000-000000000001';
begin
    if (select count(*) from definicion_flujo where pais_codigo = 'EC' and tipo_flujo = 'B' and vigente) <> 1 then
        raise exception 'V17: debe haber exactamente un flujo B vigente de Ecuador.';
    end if;
    if (select count(*) from definicion_etapa where flujo_id = flujo_b) <> 12
       or (select count(*) from definicion_etapa where flujo_id = flujo_b and automatica) <> 1
       or not exists (select 1 from definicion_etapa where flujo_id = flujo_b and numero = 12 and automatica)
       or (select count(*) from definicion_etapa where flujo_id = flujo_b and registra_llegada) <> 1
       or not exists (select 1 from definicion_etapa where flujo_id = flujo_b and numero = 8 and registra_llegada) then
        raise exception 'V17: el flujo B debe tener 12 etapas, Fin automático en B12 y la llegada en B8.';
    end if;
    if (select count(*) from transicion_permitida where flujo_id = flujo_b) <> 16
       or exists (select 1 from transicion_permitida where flujo_id = flujo_b and requisitos_documentales <> 'DEFINIDOS') then
        raise exception 'V17: el flujo B debe tener 16 transiciones con documentos definidos.';
    end if;
    -- B5 nunca devuelve a B2: sus dos devoluciones van a Codificación (B3).
    if exists (select 1 from transicion_permitida where flujo_id = flujo_b and etapa_origen = 5
               and accion in ('DEVOLVER_POR_ERROR', 'DEVOLVER_POR_PRODUCTO_NUEVO') and etapa_destino <> 3) then
        raise exception 'V17: las devoluciones de B5 deben ir a B3.';
    end if;
    if (select count(*) from documento_requerido d join transicion_permitida t on t.id = d.transicion_id
            where t.flujo_id = flujo_b) <> 3
       or (select count(*) from transicion_permitida where flujo_id = flujo_b and exige_algun_documento) <> 2 then
        raise exception 'V17: documentos del flujo B distintos de los confirmados.';
    end if;
    -- La definición de A no cambia: sigue vigente con sus 11 etapas y 14 transiciones.
    if not exists (select 1 from definicion_flujo where id = flujo_a and vigente)
       or (select count(*) from definicion_etapa where flujo_id = flujo_a) <> 11
       or (select count(*) from transicion_permitida where flujo_id = flujo_a) <> 14 then
        raise exception 'V17: la definición del flujo A no debe cambiar.';
    end if;
end;
$$;
