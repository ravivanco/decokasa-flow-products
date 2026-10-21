import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
import fs from 'node:fs';import path from 'node:path';import os from 'node:os';import {fileURLToPath} from 'node:url';
import {gzipSync} from 'node:zlib';
const base=path.dirname(fileURLToPath(import.meta.url));
const acceso=()=>JSON.parse(fs.readFileSync(path.join(base,'.tmp/acceso.json')));
const guardar=(name,obj)=>fs.writeFileSync(path.join(base,name+'.json'),JSON.stringify(obj,null,2));
const control=async(p)=>{const r=await fetch('http://127.0.0.1:43186/'+p);expect(r.ok).toBeTruthy();return r.json()};
async function login(page,role='admin'){
 await page.goto('/login');await page.getByLabel('Correo, usuario o teléfono').fill('b2-'+role);
 await page.getByLabel('Contraseña',{exact:true}).fill(acceso().password);
 await page.getByRole('button',{name:'Ingresar',exact:true}).click();await expect(page).toHaveURL(/inicio/);
}
async function auth(context,role='admin'){
 const r=await context.request.post('/api/v1/auth/login',{data:{identificador:'b2-'+role,contrasena:acceso().password}});
 expect(r.status()).toBe(200);return (await r.json()).tokenAcceso;
}
const pedido8=()=>acceso().pedido8;
function cls(entradas){let first=0,last=0,sum=0,max=0;for(const e of entradas){if(e.time-last>1000||e.time-first>5000){sum=0;first=e.time}sum+=e.value;last=e.time;max=Math.max(max,sum)}return max}
async function observar(page){await page.addInitScript(()=>{
 window.__shifts=[];
 new PerformanceObserver(l=>{for(const e of l.getEntries())if(!e.hadRecentInput)window.__shifts.push({time:e.startTime,value:e.value,sources:e.sources.map(s=>({tag:s.node?.tagName,clases:s.node?.className,previous:s.previousRect.toJSON(),current:s.currentRect.toJSON()}))})}).observe({type:'layout-shift',buffered:true});
})}
async function estabilizar(page){await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))})}
async function detalle(page,id=pedido8()){
 await page.goto('/pedidos/'+id);await expect(page.getByLabel('Nueva observación',{exact:true})).toBeVisible();
}
test('01 entorno real y paginación global frente a grupos de página',async({page,context,browserName})=>{
 await login(page);await expect(page.getByRole('heading',{name:'Pedidos en curso'})).toBeVisible();
 await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();
 await expect(page.getByText('Total global de vencidos:')).toBeVisible();
 await expect(page.getByText('Total global de vencidos:',{exact:true}).locator('..').getByText('14',{exact:true})).toBeVisible();
 await expect(page.getByText('Resultados cargados:',{exact:true}).locator('..').getByText('1–14',{exact:true})).toBeVisible();
 await expect(page.getByText(/Los grupos por etapa y por responsable cuentan solo/)).toHaveCount(0);
 await expect(page.getByRole('navigation',{name:'Paginación'})).toHaveCount(0);
 const token=await auth(context);const pages=[];
 const parcial=await (await context.request.get('/api/v1/pedidos?vencido=true&pagina=0&tamano=5',{headers:{Authorization:'Bearer '+token}})).json();
 expect(parcial.total).toBe(14);expect(parcial.elementos).toHaveLength(5);
 for(let pagina=0;pagina<4;pagina++){
  const r=await context.request.get('/api/v1/pedidos?pagina='+pagina+'&tamano=5',{headers:{Authorization:'Bearer '+token}});expect(r.status()).toBe(200);
  const data=await r.json();expect(data.total).toBe(20);expect(data.elementos.length).toBe(5);pages.push(...data.elementos.map(x=>x.id));
 }
 expect(new Set(pages).size).toBe(20);
 guardar('paginacion',{total:20,paginas:4,tamano:5,idsUnicos:new Set(pages).size,vencidosGlobal:14,vencidosCargados:14,plazosPendientesSinFecha:[5,7,10],estado:await control('estado')});
 guardar('equipo',{cpu:os.cpus()[0].model,hilos:os.cpus().length,ramBytes:os.totalmem(),os:os.version(),release:os.release(),node:process.version,browserName,conexion:'loopback IPv4 sin limitación de red ni CPU',viewport:{width:1440,height:1000},pollingMs:30000});
});
test('02 dos sesiones: observación confirmada aparece por polling real de 30 s',async({browser})=>{
 const a=await browser.newContext(),b=await browser.newContext();const p=await a.newPage(),q=await b.newPage();
 try{
  await login(p);await login(q,'logistica');await detalle(p);await detalle(q);
  const texto='Aviso ficticio de sesión A '+Date.now();let solicitudes=[];
  q.on('request',r=>{if(r.url().includes('/historial'))solicitudes.push(Date.now())});
  const inicio=Date.now();await p.getByLabel('Nueva observación',{exact:true}).fill(texto);
  const respuesta=p.waitForResponse(r=>r.url().endsWith('/observaciones')&&r.request().method()==='POST');
  await p.getByRole('button',{name:/Publicar observación/}).click();expect((await respuesta).status()).toBe(201);
  const confirmado=Date.now();await expect(q.getByText(texto,{exact:true})).toBeVisible({timeout:45000});
  guardar('dos-sesiones',{accionMs:confirmado-inicio,propagacionDesdeCommitMs:Date.now()-confirmado,solicitudesHistorial:solicitudes,transporte:'polling real, 30000 ms; sin API simulada'});
 }finally{await a.close();await b.close()}
});
test('03 intervalo sin solapamiento; pestaña oculta, foco y reconexión',async({page,context})=>{
 await page.clock.install();await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();const llamadas=[];
 const patron='**/api/v1/pedidos/bandeja?pagina=0&tamano=20';let activas=0,maximas=0,retener=false;
 const pendientes=[];
 await page.route(patron,async route=>{
  activas++;maximas=Math.max(maximas,activas);llamadas.push(Date.now());
  try{const response=await route.fetch();if(retener)await new Promise(r=>pendientes.push(r));await route.fulfill({response});}finally{activas--}
 });
 await page.clock.runFor(31000);await expect.poll(()=>llamadas.length).toBeGreaterThan(0);await expect.poll(()=>activas).toBe(0);
 retener=true;const n=llamadas.length;await page.clock.runFor(31000);
 await expect.poll(()=>activas).toBe(1);await page.clock.runFor(95000);expect(maximas).toBe(1);expect(llamadas.length).toBe(n+1);
 retener=false;pendientes.splice(0).forEach(r=>r());await expect.poll(()=>activas).toBe(0);
 // Emulación del estado de Page Visibility, sin sustituir respuestas de API.
 await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'hidden'});window.dispatchEvent(new Event('visibilitychange'))});
 const antes=llamadas.length;await page.clock.runFor(95000);expect(llamadas.length).toBe(antes);
 await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});window.dispatchEvent(new Event('visibilitychange'))});
 await expect.poll(()=>llamadas.length).toBeGreaterThan(antes);
 await expect.poll(()=>activas).toBe(0);await page.clock.runFor(50);
 await context.setOffline(true);await page.evaluate(()=>window.dispatchEvent(new Event('offline')));await page.clock.runFor(65000);
 const offline=llamadas.length;await context.setOffline(false);await page.evaluate(()=>window.dispatchEvent(new Event('online')));
 await expect.poll(()=>llamadas.length).toBeGreaterThan(offline);
 await expect.poll(()=>activas).toBe(0);await page.unrouteAll({behavior:'wait'});
 guardar('polling',{intervaloMs:30000,maximasSolapadas:maximas,ocultaSinSolicitudes:true,foco:true,reconexion:true,reloj:'Playwright controlado; HTTP real; retardo de una respuesta real',visibilidad:'Page Visibility emulada mediante evento y propiedad; no prueba de pestaña física'});
});
test('05 permisos vigentes, 403 sin cerrar sesión y recuperación por foco',async({page,context})=>{
 await login(page,'logistica');await detalle(page);
 const token=await auth(context,'logistica');
 const prohibida=await context.request.put('/api/v1/configuracion/plazos',{headers:{Authorization:'Bearer '+token},data:{metaDias:91,version:0}});
 expect(prohibida.status()).toBe(403);
 const propia=await context.request.get('/api/v1/pedidos/bandeja?tamano=100',{headers:{Authorization:'Bearer '+token}});
 expect(propia.status()).toBe(200);const trabajo=await propia.json();expect(trabajo.elementos.every(p=>[8,9,10].includes(p.etapaActual))).toBeTruthy();
 try{
  await control('quitar-permisos');const denied=page.waitForResponse(r=>r.url().endsWith('/pedidos/'+pedido8())&&r.status()===403);
  await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'hidden'});window.dispatchEvent(new Event('visibilitychange'));Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});window.dispatchEvent(new Event('visibilitychange'))});await denied;
  await expect(page.getByRole('button',{name:'Cerrar sesión'})).toBeVisible();expect(page.url()).not.toContain('/login');
 }finally{await control('restaurar-permisos')}
 const ok=page.waitForResponse(r=>r.url().endsWith('/pedidos/'+pedido8())&&r.status()===200);
 await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'hidden'});window.dispatchEvent(new Event('visibilitychange'));Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});window.dispatchEvent(new Event('visibilitychange'))});await ok;
 await expect(page.getByLabel('Nueva observación',{exact:true})).toBeVisible();
 guardar('permisos',{putAdminPorLogistica:403,trabajoTotal:trabajo.total,etapas:[...new Set(trabajo.elementos.map(p=>p.etapaActual))],sesionConservadaTras403:true,recuperacionTrasRestaurar:true,paises:'Solo EC v1; no se habilitan otros países para el ensayo'});
});
test('06 cambio de cuenta cancela lecturas y descarta borradores anteriores',async({page})=>{
 await login(page);await detalle(page);await page.getByLabel('Nueva observación',{exact:true}).fill('NO TRANSFERIR ENTRE CUENTAS');
 let liberar;let retenida=false;let abortadas=0;
 page.on('requestfailed',r=>{if(r.url().includes('/api/v1/'))abortadas++});
 await page.route('**/api/v1/pedidos/bandeja?pagina=0&tamano=20',async route=>{
  const response=await route.fetch();retenida=true;await new Promise(r=>liberar=r);await route.fulfill({response}).catch(()=>{});
 });
 await page.getByRole('link',{name:'Inicio',exact:true}).click();await expect.poll(()=>retenida).toBe(true);
 await page.getByRole('button',{name:'Cerrar sesión'}).click();await expect(page).toHaveURL(/login/);
 liberar();await page.unroute('**/api/v1/pedidos/bandeja?pagina=0&tamano=20');
 await login(page,'consulta');await expect(page.getByText('NO TRANSFERIR ENTRE CUENTAS')).toHaveCount(0);
 await expect(page.getByRole('heading',{name:'Pedidos en curso'})).toHaveCount(0);
 await page.goto('/pedidos/'+pedido8());await expect(page.getByRole('heading',{name:'Historial',exact:true})).toBeVisible();
 await expect(page.getByLabel('Nueva observación',{exact:true})).toHaveCount(0);expect(abortadas).toBeGreaterThan(0);
 guardar('cambio-sesion',{cancelacionesApi:abortadas,cacheAdminAusente:true,borradorDescartado:true,consultaSinFormulario:true});
});
test('11 CLS de carga fría y caliente con respuesta real tardía y viewport pequeño',async({browser})=>{
 const muestras=[];
 for(const viewport of [{width:1440,height:1000},{width:390,height:844}]){
  const context=await browser.newContext({viewport});const page=await context.newPage();
  try{
   await observar(page);await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();
   const cdp=await context.newCDPSession(page);await cdp.send('Network.enable');
   await cdp.send('Network.emulateNetworkConditions',{offline:false,latency:300,downloadThroughput:-1,uploadThroughput:-1});
   const enCurso=new Set();page.on('request',r=>enCurso.add(r));page.on('requestfinished',r=>enCurso.delete(r));page.on('requestfailed',r=>enCurso.delete(r));
   for(let repeticion=1;repeticion<=5;repeticion++)for(const cache of ['fria','caliente']){
    if(cache==='fria')await cdp.send('Network.clearBrowserCache');
    await page.goto('/inicio');await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();
    await expect(page.getByText('Total global de vencidos:',{exact:true})).toBeVisible();
    await expect.poll(()=>enCurso.size).toBe(0);await estabilizar(page);
    const entries=await page.evaluate(()=>window.__shifts);const recursos=await page.evaluate(()=>performance.getEntriesByType('resource').map(e=>({ruta:new URL(e.name).pathname,transferSize:e.transferSize,encodedBodySize:e.encodedBodySize})));muestras.push({viewport,repeticion,cache,cls:cls(entries),entries,recursos});
   }
   await page.unrouteAll({behavior:'wait'});
   await page.screenshot({path:path.join(base,'dashboard-'+viewport.width+'.png'),fullPage:true});
  }finally{await context.close()}
 }
 guardar('cls-carga',{muestras,umbral:0.1,latenciaInducidaMs:300,api:'real sin routing; CDP añade 300 ms a la red, no limita caudal; caché HTTP habilitada',observacion:'No es comparación causal directa con 500: cambió el volumen'});
 for(const m of muestras)expect(m.cls,JSON.stringify({viewport:m.viewport,cache:m.cache,repeticion:m.repeticion})).toBeLessThanOrEqual(0.1);
});
test('12 CLS durante polling real y actualización de motivo desde otra sesión',async({browser})=>{
 const a=await browser.newContext(),b=await browser.newContext();const page=await a.newPage();
 try{
  await observar(page);await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();await estabilizar(page);
  const token=await auth(b);const headers={Authorization:'Bearer '+token};
  const lista=await (await b.request.get('/api/v1/pedidos?etapa=8&tamano=1',{headers})).json();const id=lista.elementos[0].id;
  const detalle=await (await b.request.get('/api/v1/pedidos/'+id,{headers})).json();const entrada=detalle.lineaDeTiempo.find(e=>e.numero===8).entradaId;
  const inicio=await page.evaluate(()=>performance.now());const t=Date.now();const texto='Motivo ficticio visible durante polling '+t;
  const r=await b.request.put('/api/v1/pedidos/'+id+'/etapas/8/motivo-retraso',{headers,data:{entradaEsperadaId:entrada,motivo:texto}});expect(r.status()).toBe(200);
  await expect(page.getByText(texto,{exact:false}).first()).toBeVisible({timeout:45000});await estabilizar(page);
  const todas=await page.evaluate(()=>window.__shifts);const entries=todas.filter(e=>e.time>=inicio);
  const valor=cls(entries);guardar('cls-polling',{cls:valor,entries,propagacionMs:Date.now()-t,pedido:id,pedidos:20,intervaloMs:30000,transporte:'polling real, sesiones separadas'});
  expect(valor).toBeLessThanOrEqual(0.1);
 }finally{await a.close();await b.close()}
});
test('13 SQL constante al variar usuarios ajenos y tamaño de página; proyección mínima',async({context})=>{
 const token=await auth(context);const headers={Authorization:'Bearer '+token};const muestras=[];
 for(const usuarios of [4,20,60]){
  const fixture=await control('usuarios-'+usuarios);expect(fixture.usuarios).toBe(usuarios);expect(fixture.pedidos).toBe(20);
  for(const tamano of [1,5,20]){
   await control('reset');const r=await context.request.get('/api/v1/pedidos/bandeja?tamano='+tamano,{headers});expect(r.status()).toBe(200);
   const data=await r.json();expect(data.total).toBe(20);expect(data.elementos.length).toBe(tamano);
   const stats=await control('stats');muestras.push({usuarios,tamano,...stats});
  }
 }
 await control('fixture-resumen');await control('reset');const vacio=await control('resumen-vacio');expect(vacio).toEqual({});expect((await control('stats')).count).toBe(0);
 await control('reset');const resumenes=await control('resumenes');const stats=await control('stats');const valores=Object.values(resumenes);
 expect(valores.length).toBe(4);expect(valores.find(v=>v.nombre==='Extra ficticio 0')).toMatchObject({activo:false,roles:[]});
 expect(valores.find(v=>v.nombre==='Extra ficticio 1').activo).toBe(false);
 const multi=valores.find(v=>v.nombre==='Extra ficticio 2');expect(multi.activo).toBe(true);expect(multi.roles.map(r=>r.valor).sort()).toEqual(['consulta','logistica']);
 expect(stats.count).toBe(1);expect(stats.sql[0]).not.toMatch(/contrasena_hash|correo|telefono|usuario_pais/);
 guardar('sql-revalidacion',{muestras,resumen:{sql:stats.sql,count:stats.count,casos:'sin roles, inactivo, múltiples roles, duplicado, ausente',filas:valores.length},vacio:0});
 for(const tamano of [1,5,20])expect(new Set(muestras.filter(m=>m.tamano===tamano).map(m=>m.count)).size).toBe(1);
 expect(new Set(muestras.map(m=>m.count)).size).toBe(1);
});
test('14 área de resultados estable en vacío, error y recuperación con permisos reales',async({page})=>{
 await login(page,'logistica');await expect(page.getByRole('tab',{name:/Pendientes.*6/})).toBeVisible();
 const panel=page.getByRole('tabpanel');const datos=await panel.boundingBox();
 await page.getByRole('tab',{name:/Con urgencia/}).click();await expect(page.getByText('Ningún pedido tiene una urgencia vigente.')).toBeVisible();
 const vacio=await panel.boundingBox();expect(Math.abs(vacio.height-datos.height)).toBeLessThanOrEqual(1);
 await page.getByRole('tab',{name:/Pendientes/}).click();await expect(page.getByRole('region',{name:'Pendientes: pedidos',exact:true})).toBeVisible();
 try{
  await control('quitar-permisos');const denegado=page.waitForResponse(r=>r.url().includes('/pedidos/bandeja?pagina=0&tamano=20')&&r.status()===403);
  await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'hidden'});window.dispatchEvent(new Event('visibilitychange'));Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});window.dispatchEvent(new Event('visibilitychange'))});await denegado;
  await expect(panel.getByRole('button',{name:'Reintentar'})).toBeVisible();const error=await panel.boundingBox();expect(Math.abs(error.height-datos.height)).toBeLessThanOrEqual(1);
  await expect(page.getByRole('button',{name:'Cerrar sesión'})).toBeVisible();
  await control('restaurar-permisos');await panel.getByRole('button',{name:'Reintentar'}).click();await expect(panel.getByRole('region',{name:'Pendientes: pedidos',exact:true})).toBeVisible();
  const recuperado=await panel.boundingBox();expect(Math.abs(recuperado.height-datos.height)).toBeLessThanOrEqual(1);
  guardar('estados-resultados',{datos,vacio,error,recuperado,sesionConservada:true});
 }finally{await control('restaurar-permisos')}
});
