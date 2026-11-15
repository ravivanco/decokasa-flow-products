-- V36: Admin puede restablecer la contraseña de un usuario con una temporal (plan B cuando el correo de recuperación no
-- llega; Richard, 2026-11-15). Esa cuenta debe crear una contraseña propia al iniciar sesión: la marca se borra en
-- cuanto la persona cambia su contraseña (por sí misma o con un enlace de recuperación). Las cuentas existentes no la
-- tienen.
alter table usuario add column debe_cambiar_contrasena boolean not null default false;
