import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';
const base=path.dirname(fileURLToPath(import.meta.url));
const enlaces=[['Inicio','/inicio'],['Mi bandeja','/bandeja'],['Pedidos','/pedidos'],['Bodegas','/bodegas'],['Correos','/notificaciones'],['Usuarios','/usuarios'],['Roles','/roles'],['Meta de plazos','/configuracion/plazos']];
const usuario={id:'00000000-0000-4000-8000-000000000010',nombre:'Admin ficticio H05',correo:'admin@ejemplo.test',usuario:'admin-ficticio',telefono:'+593990000010',roles:['admin'],paises:['EC'],activo:true,version:0,permisos:[]};
async function medir(page){return page.evaluate(()=>{const e=document.activeElement,s=getComputedStyle(e),r=e.getBoundingClientRect();return {nombre:e.textContent.trim(),rect:r.toJSON(),sombra:s.boxShadow,focusVisible:e.matches(':focus-visible'),viewport:innerWidth,scroll:document.documentElement.scrollWidth,recortes:[...function*(n){for(let p=n.parentElement;p;p=p.parentElement)if(/hidden|clip|auto|scroll/.test(getComputedStyle(p).overflowX)){const x=p.getBoundingClientRect();yield {tag:p.tagName,left:x.left,right:x.right}}}(e)]}})}
for(const width of [390,1440])test('H05 teclado completo '+width,async({browser})=>{
 const context=await browser.newContext({viewport:{width,height:1000}}),page=await context.newPage();let activa=true;const pasos=[];
 try{
  await page.route('**/api/**',async route=>{const url=new URL(route.request().url()),p=url.pathname;let body;
   if(p.endsWith('/auth/cerrar-sesion')){activa=false;await route.fulfill({status:204});return}
   if(p.endsWith('/auth/renovar')){if(!activa){await route.fulfill({status:401,json:{status:401,title:'Sesión ficticia cerrada',codigo:'NO_AUTENTICADO'}});return}body={tokenAcceso:'acceso-ficticio-no-valido-en-servidor',expiraEn:900,usuario}}
   else if(p.endsWith('/auth/yo'))body=usuario;
   else if(p.endsWith('/configuracion/plazos'))body={metaDias:80,origenMeta:'PROPUESTA',metaPendienteDelCliente:true,feriados:[],version:0,actualizadoEn:null};
   else if(['/api/v1/roles','/api/v1/usuarios','/api/v1/bodegas'].includes(p))body=[];
   else if(p.startsWith('/api/v1/pedidos')||p.startsWith('/api/v1/notificaciones'))body={elementos:[],pagina:0,tamano:20,total:0};
   else {await route.fulfill({status:503,json:{status:503,title:'Respuesta ficticia no necesaria para menú',codigo:'ERROR_INTERNO'}});return}
   await route.fulfill({status:200,json:body});
  });
  await page.goto('/inicio');const menu=page.getByRole('navigation',{name:'Menú principal'});await expect(menu).toBeVisible();
  async function comprobar(nombre,direccion){await expect(menu.getByRole('link',{name:nombre,exact:true})).toBeFocused();const d=await medir(page);expect(d.focusVisible).toBe(true);expect(d.sombra).not.toBe('none');expect(d.rect.left-3).toBeGreaterThanOrEqual(0);expect(d.rect.right+3).toBeLessThanOrEqual(width);expect(d.rect.top-3).toBeGreaterThanOrEqual(0);expect(d.rect.bottom+3).toBeLessThanOrEqual(1000);expect(d.scroll).toBe(width);for(const x of d.recortes){expect(d.rect.left-3).toBeGreaterThanOrEqual(x.left);expect(d.rect.right+3).toBeLessThanOrEqual(x.right)}pasos.push({direccion,...d});await page.screenshot({path:path.join(base,`${width}-${direccion}-${nombre.replaceAll(' ','_')}.png`)});}
  await menu.getByRole('link',{name:'Inicio',exact:true}).focus();await page.keyboard.press('Shift+Tab');await page.keyboard.press('Tab');
  for(let i=0;i<enlaces.length;i++){await comprobar(enlaces[i][0],'Tab');if(i<enlaces.length-1)await page.keyboard.press('Tab')}
  for(let i=enlaces.length-1;i>=0;i--){await comprobar(enlaces[i][0],'ShiftTab');if(i>0)await page.keyboard.press('Shift+Tab')}
  for(const [nombre,ruta] of enlaces){await menu.getByRole('link',{name:nombre,exact:true}).focus();await page.keyboard.press('Enter');await expect(page).toHaveURL('http://127.0.0.1:43184'+ruta);await expect(menu.getByRole('link',{name:nombre,exact:true})).toHaveAttribute('aria-current','page');pasos.push({enter:nombre,ruta});}
  const cerrar=page.getByRole('button',{name:'Cerrar sesión',exact:true});await cerrar.focus();await page.keyboard.press('Tab');await page.keyboard.press('Shift+Tab');await expect(cerrar).toBeFocused();await page.evaluate(async()=>{await Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})))});const focoCerrar=await medir(page);expect(focoCerrar.focusVisible).toBe(true);expect(focoCerrar.sombra).not.toBe('none');expect(focoCerrar.rect.left-5).toBeGreaterThanOrEqual(0);expect(focoCerrar.rect.right+5).toBeLessThanOrEqual(width);await page.screenshot({path:path.join(base,'cerrar-'+width+'.png')});await page.keyboard.press('Enter');await expect(page).toHaveURL(/\/login$/);await expect.poll(()=>activa).toBe(false);
  fs.writeFileSync(path.join(base,'resultado-'+width+'.json'),JSON.stringify({width,pasos,focoCerrar,logout:true,api:'Dobles HTTP del navegador; backend no iniciado'},null,2));
 }finally{await context.close()}
});
