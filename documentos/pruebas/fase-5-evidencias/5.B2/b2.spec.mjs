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
async function detalle(page,id=pedido8()){
 await page.goto('/pedidos/'+id);await expect(page.getByLabel('Nueva observación',{exact:true})).toBeVisible();
}
test('01 entorno real y paginación global frente a grupos de página',async({page,context,browserName})=>{
 await login(page);await expect(page.getByRole('heading',{name:'Pedidos en curso'})).toBeVisible();
 await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();
 await expect(page.getByText('Total global de vencidos:')).toBeVisible();
 await expect(page.getByText('Total global de vencidos:',{exact:true}).locator('..').getByText('350',{exact:true})).toBeVisible();
 await expect(page.getByText('Resultados cargados:',{exact:true}).locator('..').getByText('1–50',{exact:true})).toBeVisible();
 await expect(page.getByText(/Los grupos por etapa y por responsable cuentan solo/)).toBeVisible();
 const token=await auth(context);const pages=[];
 for(let pagina=0;pagina<25;pagina++){
  const r=await context.request.get('/api/v1/pedidos?pagina='+pagina+'&tamano=20',{headers:{Authorization:'Bearer '+token}});expect(r.status()).toBe(200);
  const data=await r.json();expect(data.total).toBe(500);expect(data.elementos.length).toBe(20);pages.push(...data.elementos.map(x=>x.id));
 }
 expect(new Set(pages).size).toBe(500);
 guardar('paginacion',{total:500,paginas:25,tamano:20,idsUnicos:new Set(pages).size,vencidosGlobal:350,vencidosCargados:50,plazosPendientesSinFecha:[5,7,10],estado:await control('estado')});
 guardar('equipo',{cpu:os.cpus()[0].model,hilos:os.cpus().length,ramBytes:os.totalmem(),os:os.version(),release:os.release(),node:process.version,browserName,conexion:'loopback IPv4 sin limitación de red ni CPU',viewport:{width:1440,height:1000},pollingMs:30000});
});
test('10 diagnóstico de estabilidad visual con traza CDP, sin cambiar producto',async({page,context})=>{
 await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();
 await page.addInitScript(()=>{
  window.__shifts=[];
  new PerformanceObserver(l=>{for(const e of l.getEntries())if(!e.hadRecentInput)window.__shifts.push({time:e.startTime,value:e.value,sources:e.sources.map(s=>({tag:s.node?.tagName,id:s.node?.id,clases:s.node?.className,previous:s.previousRect.toJSON(),current:s.currentRect.toJSON()}))})}).observe({type:'layout-shift',buffered:true});
 });
 const cdp=await context.newCDPSession(page);await cdp.send('Network.enable');await cdp.send('Network.clearBrowserCache');
 await cdp.send('Tracing.start',{categories:'devtools.timeline,loading,blink.user_timing',transferMode:'ReturnAsStream'});
 await page.goto('/inicio');await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();await expect(page.getByText('Total global de vencidos:')).toBeVisible();await page.evaluate(()=>document.fonts.ready);
 await page.screenshot({path:path.join(base,'dashboard-ficticio.png'),fullPage:true});
 const terminado=new Promise(r=>cdp.once('Tracing.tracingComplete',r));await cdp.send('Tracing.end');const {stream}=await terminado;let raw='';
 while(true){const data=await cdp.send('IO.read',{handle:stream});raw+=data.base64Encoded?Buffer.from(data.data,'base64').toString():data.data;if(data.eof)break}await cdp.send('IO.close',{handle:stream});
 raw=raw.replace(/eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g,'[TOKEN_FICTICIO_RETIRADO]');fs.writeFileSync(path.join(base,'dashboard.trace.json.gz'),gzipSync(raw));
 guardar('estabilidad-visual',{shifts:await page.evaluate(()=>window.__shifts),traceBytes:Buffer.byteLength(raw),nota:'Diagnóstico, no aceptación de CLS; únicamente datos ficticios'});
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
 await page.clock.install();await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();const llamadas=[];
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
test('04 borrador y respuesta tardía real; conflicto de entrada sin reenviar',async({page})=>{
 await login(page);await detalle(page);const texto='Borrador privado de la entrada antigua';
 let liberar;let capturada=false;const pid=pedido8();let unica=true;
 await page.route('**/api/v1/pedidos/'+pid,async route=>{
  const response=await route.fetch();if(unica){unica=false;capturada=true;await new Promise(r=>liberar=r)}await route.fulfill({response});
 });
 await page.evaluate(()=>{Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'hidden'});window.dispatchEvent(new Event('visibilitychange'));Object.defineProperty(document,'visibilityState',{configurable:true,get:()=> 'visible'});window.dispatchEvent(new Event('visibilitychange'))});
 await expect.poll(()=>capturada).toBe(true);
 await page.getByLabel('Nueva observación',{exact:true}).fill(texto);liberar();
 await expect(page.getByLabel('Nueva observación',{exact:true})).toHaveValue(texto);
 await page.unrouteAll({behavior:'wait'});
 const nueva=await control('reentrada');
 let posts=[];page.on('request',r=>{if(r.method()==='POST'&&r.url().endsWith('/observaciones'))posts.push(r.postDataJSON())});
 const response=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/observaciones'));
 await page.getByRole('button',{name:/Publicar observación/}).click();expect((await response).status()).toBe(409);
 await expect(page.getByLabel('Texto sin enviar: observación')).toHaveValue(texto);
 await expect(page.getByLabel('Nueva observación',{exact:true})).toHaveValue('');expect(posts.length).toBe(1);
 expect(posts[0].entradaEsperadaId).not.toBe(nueva.entradaId);
 guardar('borrador',{conservadoTrasRespuestaTardia:true,conflictoHttp:409,posts:posts.length,entradaEnviada:posts[0].entradaEsperadaId,nuevaEntrada:nueva.entradaId,textoNuncaTransferido:true});
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
test('07 latencias HTTP, consultas por página, concurrencia y planes',async({context})=>{
 const token=await auth(context);const headers={Authorization:'Bearer '+token};const endpoints={dashboard:'/api/v1/pedidos/bandeja?pagina=0&tamano=20',busqueda:'/api/v1/pedidos/buscar?numero=DK-EC-2026-0008'};
 const resultado={};
 for(const [nombre,url] of Object.entries(endpoints)){
  const ms=[];let bytes=0;
  for(let n=0;n<100;n++){const t=performance.now(),r=await context.request.get(url,{headers});expect(r.status()).toBe(200);const b=await r.body();ms.push(performance.now()-t);bytes=b.length;}
  const t=performance.now();await Promise.all(Array.from({length:10},async()=>expect((await context.request.get(url,{headers})).status()).toBe(200)));
  resultado[nombre]={repeticiones:100,ms,bytes,concurrentes:10,loteConcurrenteMs:performance.now()-t};
 }
 const consultas={};
 for(const tamano of [1,20,100]){
  await control('reset');expect((await context.request.get('/api/v1/pedidos/bandeja?tamano='+tamano,{headers})).status()).toBe(200);
  consultas[tamano]=await control('stats');
 }
 guardar('api-metricas',resultado);guardar('consultas',consultas);guardar('explain',await control('explain'));
});
test('08 pantalla y búsqueda: caché cliente fría/caliente y métricas de laboratorio',async({browser})=>{
 const mediciones=[];
 for(let repeticion=1;repeticion<=5;repeticion++){
  const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();
  try{
   await page.addInitScript(()=>{
    window.__perf={lcp:null,cls:0,shifts:[],eventos:[],longTasks:[]};
    new PerformanceObserver(l=>{for(const e of l.getEntries())window.__perf.lcp=e.startTime}).observe({type:'largest-contentful-paint',buffered:true});
    new PerformanceObserver(l=>{for(const e of l.getEntries())if(!e.hadRecentInput)window.__perf.shifts.push({time:e.startTime,value:e.value});let first=0,last=0,sum=0,max=0;for(const s of window.__perf.shifts){if(s.time-last>1000||s.time-first>5000){first=s.time;sum=0}sum+=s.value;last=s.time;max=Math.max(max,sum)}window.__perf.cls=max}).observe({type:'layout-shift',buffered:true});
    new PerformanceObserver(l=>{for(const e of l.getEntries())if(e.interactionId)window.__perf.eventos.push({duration:e.duration,interactionId:e.interactionId,name:e.name})}).observe({type:'event',buffered:true,durationThreshold:16});
    new PerformanceObserver(l=>{for(const e of l.getEntries())window.__perf.longTasks.push(e.duration)}).observe({type:'longtask',buffered:true});
   });
   // Cookie ficticia conservada; caché HTTP vaciada antes de cada navegación fría. No se vacía PostgreSQL.
   const pendientes=new Set();page.on('request',r=>pendientes.add(r));page.on('requestfinished',r=>pendientes.delete(r));page.on('requestfailed',r=>pendientes.delete(r));
   await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();await expect.poll(()=>pendientes.size).toBe(0);
   const cdp=await context.newCDPSession(page);await cdp.send('Network.enable');
   for(const cache of ['fria','caliente','caliente-query']){
    if(cache==='fria')await cdp.send('Network.clearBrowserCache');
    const reqs=[];const listener=r=>{if(r.url().includes('/api/v1/'))reqs.push(new URL(r.url()).pathname+new URL(r.url()).search)};page.on('request',listener);
    let t=performance.now();if(cache==='caliente-query')await page.getByRole('link',{name:'Inicio',exact:true}).click();else await page.goto('/inicio');await expect(page.getByRole('tab',{name:/Pendientes.*500/})).toBeVisible();
    await expect(page.getByText('Total global de vencidos:')).toBeVisible();await expect(page.getByText('DK-EC-2026-0500',{exact:true}).first()).toBeVisible();
    const dashboardMs=performance.now()-t;const solicitudesDashboard=[...reqs];
    await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))});
    const perf=await page.evaluate(()=>({vitals:window.__perf,navegacion:performance.getEntriesByType('navigation').map(e=>e.toJSON()),recursos:performance.getEntriesByType('resource').map(e=>({name:new URL(e.name).pathname,duration:e.duration,transferSize:e.transferSize,encodedBodySize:e.encodedBodySize}))}));
    await page.getByRole('searchbox',{name:'Buscar pedido por número',exact:true}).fill('DK-EC-2026-0008');t=performance.now();
    await page.getByRole('search').getByRole('button',{name:'Buscar',exact:true}).click();await expect(page.getByLabel('Nueva observación',{exact:true})).toBeVisible();
    const busquedaMs=performance.now()-t;const eventos=await page.evaluate(()=>window.__perf.eventos);page.off('request',listener);
    mediciones.push({repeticion,cache,dashboardMs,busquedaMs,solicitudesDashboard,solicitudes:reqs,perf,eventos});
   }
  }finally{await context.close()}
 }
 guardar('pantalla-metricas',{browserVersion:browser.version(),repeticiones:5,mediciones,definicionFria:'HTTP cliente vacía y QueryClient recreado por navegación; JVM/PG precalentados, no frío de base',definicionCaliente:'HTTP cliente conservada; navegación completa recrea QueryClient'});
});
test('09 cambio real de A7 a A8 conserva borrador y bloqueos documentales',async({browser})=>{
 const a=await browser.newContext(),b=await browser.newContext();const p=await a.newPage(),q=await b.newPage();
 try{
  await login(p);await login(q,'compras_china');const token=await auth(a);
  const headers={Authorization:'Bearer '+token};
  const candidatos=await (await a.request.get('/api/v1/pedidos?etapa=7&tamano=1',{headers})).json();const id=candidatos.elementos[0].id;expect(candidatos.elementos[0].etapaActual).toBe(7);await detalle(p,id);await detalle(q,id);
  const texto='Conservar texto de A7 sin trasladarlo a A8';await q.getByLabel('Nueva observación',{exact:true}).fill(texto);
  await p.getByRole('button',{name:'Confirmar llegada',exact:true}).click();
  const accion=p.waitForResponse(r=>r.url().endsWith('/acciones')&&r.request().method()==='POST');
  await p.getByRole('dialog').getByRole('button',{name:'Confirmar llegada',exact:true}).click();expect((await accion).status()).toBe(200);
  const peticiones=[];q.on('request',r=>{if(r.url().endsWith('/observaciones')&&r.method()==='POST')peticiones.push(r.postDataJSON())});
  const conflicto=q.waitForResponse(r=>r.url().endsWith('/observaciones')&&r.request().method()==='POST');
  await q.getByRole('button',{name:'Publicar observación'}).click();expect((await conflicto).status()).toBe(409);
  await expect(q.getByLabel('Texto sin enviar: observación')).toHaveValue(texto);
  await expect(q.getByLabel('Nueva observación',{exact:true})).toHaveCount(0);expect(peticiones).toHaveLength(1);expect(peticiones[0].etapaEsperada).toBe(7);
  for(const etapa of [6,9,10]){
   const candidatos=await (await a.request.get('/api/v1/pedidos?etapa='+etapa+'&tamano=1',{headers})).json();
   await detalle(p,candidatos.elementos[0].id);await expect(p.getByText('Documentos pendientes de definición',{exact:true})).toBeVisible();
   await expect(p.getByRole('button',{name:/^(Aprobar|Confirmar y finalizar)$/})).toBeDisabled();
  }
  guardar('cambio-etapa',{pedido:id,transicionReal:'A7 a A8, APROBAR registra llegada',resultado:200,observacionObsoleta:409,envios:peticiones.length,borradorRecuperable:true,bloqueosReales:'A6/A9/A10 visibles, confirmación deshabilitada, definición sin modificar'});
 }finally{await a.close();await b.close()}
});
