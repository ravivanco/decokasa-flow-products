-- V26: lectura de los avisos de etapa (ADR-0031, decisión del equipo del 2026-10-26; no es una respuesta del
-- cliente). Siguiente versión disponible tras V25; V1–V25 no cambian.
--
-- Los avisos no se guardan: se calculan al consultar desde el historial (evento_pedido), el pedido y la definición de
-- sus etapas. Aquí solo se guarda, por persona, hasta qué momento los leyó. Las cuentas existentes empiezan con la
-- hora de esta migración, para no presentar como nuevo lo que ya pasó; una cuenta nueva usa su hora de creación.

create table aviso_lectura (
    usuario_id uuid primary key references usuario (id),
    leidas_hasta timestamptz not null
);

insert into aviso_lectura (usuario_id, leidas_hasta)
select id, now() from usuario;
