-- Experimento adicional de escalado: solo el contenedor aislado f5b2; 500 pedidos sin alterar.
begin;
insert into usuario(id,nombre,correo,usuario,telefono,contrasena_hash,activo,creado_en,actualizado_en,version)
select gen_random_uuid(),'Inactivo ficticio B2 '||i,'inactivo-b2-'||i||'@ejemplo.test','b2-inactivo-'||i,
       '+593990008'||lpad(i::text,3,'0'),(select contrasena_hash from usuario where usuario='b2-admin'),false,now(),now(),0
from generate_series(1,16) i;
insert into usuario_rol select u.id,r.id,now() from usuario u cross join rol r where u.usuario like 'b2-inactivo-%' and r.codigo='logistica';
insert into usuario_pais select id,'EC' from usuario where usuario like 'b2-inactivo-%';
commit;
select count(*) as usuarios_ficticios from usuario;
