-- V37: documentos confidenciales de Aduana (pedido del cliente, 2026-11-15). Los archivos de los ítems del checklist de
-- documentos (facturas, liquidaciones, FOB...) los ven solo Admin, Compras China y Asistente de Compras. Admin puede dar
-- acceso a otra persona en un pedido concreto y quitárselo después. Quitar no borra: queda quién y cuándo.
create table acceso_confidencial (
    id           uuid primary key,
    pedido_id    uuid        not null references pedido (id),
    usuario_id   uuid        not null references usuario (id),
    otorgado_por uuid        not null references usuario (id),
    otorgado_en  timestamptz not null,
    revocado_por uuid references usuario (id),
    revocado_en  timestamptz,
    constraint acceso_confidencial_revocado_completo check ((revocado_en is null) = (revocado_por is null))
);
-- Un solo acceso vigente por persona y pedido.
create unique index acceso_confidencial_vigente on acceso_confidencial (pedido_id, usuario_id) where revocado_en is null;
