import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
import fs from 'node:fs';import path from 'node:path';import {fileURLToPath} from 'node:url';
const base=path.dirname(fileURLToPath(import.meta.url));const acceso=()=>JSON.parse(fs.readFileSync(path.join(base,'.tmp/acceso.json')));
const guardar=(n,v)=>fs.writeFileSync(path.join(base,n+'.json'),JSON.stringify(v,null,2));
const axeSource=fs.readFileSync(path.resolve(base,'../../../.tmp/f5b3-axe/node_modules/axe-core/axe.min.js'),'utf8');
async function control(p){const r=await fetch('http://127.0.0.1:43186/'+p);expect(r.ok).toBeTruthy();return r.json()}
async function login(page,rol='admin'){await page.goto('/login');await page.getByLabel('Correo, usuario o teléfono').fill('b2-'+rol);await page.getByLabel('Contraseña',{exact:true}).fill(acceso().password);await page.getByRole('button',{name:'Ingresar',exact:true}).click();await expect(page).toHaveURL(/inicio/)}
async function auth(context,rol='admin'){const r=await context.request.post('/api/v1/auth/login',{data:{identificador:'b2-'+rol,contrasena:acceso().password}});expect(r.status()).toBe(200);return (await r.json()).tokenAcceso}
async function listo(page){await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))})}
async function detalle(page,id){await page.goto('/pedidos/'+id);await expect(page.getByRole('heading',{name:'Historial',exact:true})).toBeVisible();await expect(page.getByRole('region',{name:'Cronograma frente a la meta'}).getByText('Sin estimar',{exact:true}).first()).toBeVisible();await listo(page)}
async function escanear(page,n){await listo(page);await page.evaluate(axeSource);const r=await page.evaluate(async()=>{const r=await window.axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21a','wcag21aa','wcag22aa','best-practice']}});return {url:location.pathname,version:r.testEngine.version,violations:r.violations,incomplete:r.incomplete,passes:r.passes.map(x=>({id:x.id,nodes:x.nodes.length})),inapplicable:r.inapplicable.map(x=>x.id)}});guardar(n,r);return r}
async function foco(page){return page.evaluate(()=>{const e=document.activeElement,s=getComputedStyle(e),r=e.getBoundingClientRect();return {tag:e.tagName,nombre:e.getAttribute('aria-label')||e.textContent?.trim().slice(0,100),id:e.id,rect:r.toJSON(),outline:s.outline,boxShadow:s.boxShadow,focusVisible:e.matches(':focus-visible'),viewport:{width:innerWidth,height:innerHeight},scrollX,scrollY}})}

test('01 axe-core escritorio y móvil: dashboard, cronograma, línea, etapas y meta',async({browser})=>{
 const resultados=[];
 for(const ancho of [1440,390]){
  const context=await browser.newContext({viewport:{width:ancho,height:1000}});const page=await context.newPage();
  try{
   await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();
   resultados.push({nombre:'dashboard-'+ancho,...await escanear(page,'axe-dashboard-'+ancho)});await page.screenshot({path:path.join(base,'dashboard-'+ancho+'.png'),fullPage:true});
   for(const [etapa,id] of [[6,acceso().pedido6],[8,acceso().pedido8],[10,acceso().pedido10]]){
    await detalle(page,id);await expect(page.getByLabel('Nueva observación',{exact:true})).toBeVisible();
    if([6,10].includes(etapa))await expect(page.getByText('Documentos pendientes de definición',{exact:true})).toBeVisible();
    resultados.push({nombre:'detalle-A'+etapa+'-'+ancho,...await escanear(page,'axe-detalle-A'+etapa+'-'+ancho)});
    if(etapa===8)await page.screenshot({path:path.join(base,'detalle-'+ancho+'.png'),fullPage:true});
   }
   await page.goto('/configuracion/plazos');await expect(page.getByLabel('Meta propuesta (días de pedido a llegada)',{exact:true})).toBeVisible();
   resultados.push({nombre:'meta-'+ancho,...await escanear(page,'axe-meta-'+ancho)});await page.screenshot({path:path.join(base,'meta-'+ancho+'.png'),fullPage:true});
  }finally{await context.close()}
 }
 guardar('axe-resumen',resultados.map(r=>({nombre:r.nombre,violations:r.violations.map(x=>({id:x.id,impact:x.impact,nodos:x.nodes.length})),incomplete:r.incomplete.map(x=>({id:x.id,nodos:x.nodes.length}))})));
 for(const r of resultados)expect.soft(r.violations,r.nombre).toEqual([]);
});
test('02 teclado, tabs y foco visible en escritorio y móvil',async({browser})=>{
 const muestras=[];
 for(const width of [1440,390]){
  const context=await browser.newContext({viewport:{width,height:1000}});const page=await context.newPage();
  try{
   await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();await page.goto('/inicio');await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();
   const recorrido=[];for(let i=0;i<18;i++){await page.keyboard.press('Tab');recorrido.push(await foco(page))}
   const tabs=page.getByRole('tablist',{name:'Pedidos en curso'});const primero=tabs.getByRole('tab').first();await primero.focus();await page.keyboard.press('ArrowRight');
   await expect(tabs.getByRole('tab',{name:/Plazo vencido/})).toBeFocused();await expect(tabs.getByRole('tab',{name:/Plazo vencido/})).toHaveAttribute('aria-selected','true');
   await page.keyboard.press('End');await expect(tabs.getByRole('tab',{name:/Con urgencia/})).toBeFocused();await page.keyboard.press('Home');await expect(primero).toBeFocused();
   const controles=await tabs.getByRole('tab').evaluateAll(es=>es.map(e=>({nombre:e.textContent,selected:e.getAttribute('aria-selected'),tabIndex:e.tabIndex,controls:e.getAttribute('aria-controls')})));
   const panelId=await primero.getAttribute('aria-controls');expect(await page.locator('[id="'+panelId+'"]').getAttribute('role')).toBe('tabpanel');expect(controles.filter(x=>x.tabIndex===0)).toHaveLength(1);
   await page.screenshot({path:path.join(base,'foco-tabs-'+width+'.png')});const focoTab=await foco(page);expect(focoTab.focusVisible).toBe(true);expect(focoTab.boxShadow==='none'&&focoTab.outline.includes('0px')).toBe(false);
   await detalle(page,acceso().pedido8);const actual=page.getByRole('button',{name:/^A8 .*, etapa actual$/});await expect(actual).toHaveAttribute('aria-current','step');await actual.focus();await page.keyboard.press('Shift+Tab');await page.keyboard.press('Tab');await page.keyboard.press('Enter');await expect(actual).toHaveAttribute('aria-pressed','true');
   const fin=page.getByRole('button',{name:/^A11 .*, pendiente$/});await fin.focus();await page.keyboard.press('Enter');await expect(page.getByText('Etapa automática',{exact:true})).toBeVisible();
   expect(await page.getByRole('button',{name:/^(Aprobar|Confirmar y finalizar|Enviar)$/}).count()).toBe(0);
   muestras.push({width,recorrido,controles,focoTab,linea:await foco(page),anchoPagina:await page.evaluate(()=>({viewport:innerWidth,scroll:document.documentElement.scrollWidth}))});
  }finally{await context.close()}
 }
 guardar('teclado',muestras);
});
test('03 formulario meta, errores, 409 y borrador conservado con API real',async({browser})=>{
 const a=await browser.newContext(),b=await browser.newContext();const page=await a.newPage();
 try{
  await login(page);await page.goto('/configuracion/plazos');const input=page.getByLabel('Meta propuesta (días de pedido a llegada)',{exact:true});await expect(input).toBeVisible();
  await input.fill('0');await page.getByRole('button',{name:'Guardar meta',exact:true}).click();await expect(input).toHaveAttribute('aria-invalid','true');await expect(page.getByText('Escribe la meta en días: un número entero desde 1.',{exact:true})).toBeVisible();
  const error=await escanear(page,'axe-meta-error');const vinculacion=await input.evaluate(e=>({describedby:e.getAttribute('aria-describedby'),errormessage:e.getAttribute('aria-errormessage'),active:document.activeElement===e,formText:e.closest('form').textContent}));
  await input.fill('77');const token=await auth(b);const headers={Authorization:'Bearer '+token};const cfg=await (await b.request.get('/api/v1/configuracion/plazos',{headers})).json();
  expect((await b.request.put('/api/v1/configuracion/plazos',{headers,data:{metaDias:81,version:cfg.version}})).status()).toBe(200);
  let puts=0;page.on('request',r=>{if(r.method()==='PUT'&&r.url().includes('/configuracion/plazos'))puts++});const resp=page.waitForResponse(r=>r.request().method()==='PUT'&&r.url().includes('/configuracion/plazos'));
  await page.getByRole('button',{name:'Guardar meta',exact:true}).click();expect((await resp).status()).toBe(409);await expect(page.getByText('Otra persona cambió la meta mientras editabas',{exact:true})).toBeVisible();await expect(input).toHaveValue('77');
  await expect(page.getByRole('button',{name:'Usar el valor vigente'})).toBeEnabled();await expect(page.getByRole('button',{name:'Mantener mi valor'})).toBeEnabled();await page.evaluate(async()=>{await Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})))});const conflicto=await escanear(page,'axe-meta-conflicto');await page.screenshot({path:path.join(base,'meta-conflicto.png')});guardar('colores-conflicto',await page.getByRole('alert').getByRole('button').evaluateAll(es=>es.map(e=>({texto:e.textContent,color:getComputedStyle(e).color,fondo:getComputedStyle(e).backgroundColor}))));await expect(page.getByRole('button',{name:'Mantener mi valor'})).toBeEnabled();await page.getByRole('button',{name:'Mantener mi valor'}).focus();await page.keyboard.press('Enter');await expect(input).toHaveValue('77');expect(puts).toBe(1);
  guardar('meta-formulario',{vinculacion,puts,conflicto:409,borrador:'77 conservado, nunca guardado automáticamente',propuesta:true,errorViolations:error.violations,conflictoViolations:conflicto.violations});
  expect.soft(error.violations).toEqual([]);expect.soft(conflicto.violations).toEqual([]);
 }finally{await a.close();await b.close()}
});
test('04 documentos reales bloqueados A6 A9 A10 y Fin sin acciones',async({page,context})=>{
 await login(page);const token=await auth(context);const headers={Authorization:'Bearer '+token};const r=[];
 for(const etapa of [6,9,10]){
  const lista=await (await context.request.get('/api/v1/pedidos?etapa='+etapa+'&tamano=1',{headers})).json();const id=lista.elementos[0].id;await detalle(page,id);
  await expect(page.getByText('Documentos pendientes de definición',{exact:true})).toBeVisible();const boton=page.getByRole('button',{name:/^(Aprobar|Confirmar y finalizar)$/});await expect(boton).toBeDisabled();
  const antes=await (await context.request.get('/api/v1/pedidos/'+id+'/historial',{headers})).json();const respuesta=await context.request.post('/api/v1/pedidos/'+id+'/acciones',{headers,data:{accion:'APROBAR',etapaEsperada:etapa,adjuntoIds:[]}});expect(respuesta.status()).toBe(409);const problema=await respuesta.json();expect(problema.detail).toMatch(/documentos.*no est.*definidos/i);
  const despues=await (await context.request.get('/api/v1/pedidos/'+id+'/historial',{headers})).json();expect(despues).toEqual(antes);r.push({etapa,status:409,problema,historialIntacto:true});
 }
 guardar('bloqueos',r);
});
test('05 polling y anuncios: sin foco robado, sin cambios vacíos repetidos',async({browser})=>{
 const a=await browser.newContext(),b=await browser.newContext();const page=await a.newPage();
 try{
  await login(page);await expect(page.getByRole('tab',{name:/Pendientes.*20/})).toBeVisible();await page.getByRole('link',{name:'Abrir la bandeja por etapa'}).focus();await page.keyboard.press('Shift+Tab');await page.keyboard.press('Tab');const inicial=await foco(page);
  await page.evaluate(()=>{window.__avisos=[];window.__observadores=[];for(const e of document.querySelectorAll('[aria-live],[role=status],[role=alert]')){const o=new MutationObserver(()=>window.__avisos.push({texto:e.textContent?.trim(),live:e.getAttribute('aria-live'),role:e.getAttribute('role'),time:performance.now()}));o.observe(e,{childList:true,subtree:true,characterData:true});window.__observadores.push(o)}});
  let lecturas=0;page.on('response',r=>{if(r.url().includes('/pedidos/bandeja?pagina=0&tamano=20'))lecturas++});await expect.poll(()=>lecturas,{timeout:45000,intervals:[1000]}).toBeGreaterThan(0);const avisosSinCambio=await page.evaluate(()=>window.__avisos);expect(avisosSinCambio).toEqual([]);const trasPolling=await foco(page);expect(trasPolling.nombre).toBe(inicial.nombre);
  const token=await auth(b);const headers={Authorization:'Bearer '+token};const lista=await (await b.request.get('/api/v1/pedidos?etapa=7&tamano=1',{headers})).json();const id=lista.elementos[0].id;
  expect((await b.request.post('/api/v1/pedidos/'+id+'/acciones',{headers,data:{accion:'APROBAR',etapaEsperada:7,adjuntoIds:[]}})).status()).toBe(200);
  const numero=lista.elementos[0].numero;await expect(page.getByRole('row').filter({has:page.getByRole('link',{name:numero,exact:true})}).getByText('A8',{exact:true})).toBeVisible({timeout:45000});
  const trasCambio=await foco(page);expect(trasCambio.nombre).toBe(inicial.nombre);
  await expect.poll(()=>page.evaluate(()=>window.__avisos.filter(x=>x.texto?.startsWith('Lista actualizada')).length)).toBe(1);const lecturasAntes=lecturas;await expect.poll(()=>lecturas,{timeout:45000,intervals:[1000]}).toBeGreaterThan(lecturasAntes);const avisos=await page.evaluate(()=>window.__avisos);expect(avisos.filter(x=>x.texto?.startsWith('Lista actualizada'))).toHaveLength(1);expect((await foco(page)).nombre).toBe(inicial.nombre);guardar('anuncios-polling',{inicial,trasPolling,trasCambio,avisosSinCambio,avisos,cambioReal:'A7→A8',lecturas,limitacion:'Se inspecciona DOM/árbol, no se prueba salida hablada de NVDA/VoiceOver'});
 }finally{await a.close();await b.close()}
});
test('06 conflicto de entrada: borrador recuperable y sin envío a la nueva',async({page})=>{
 await login(page);await detalle(page,acceso().pedido8);const texto='Borrador ficticio de la entrada original';await page.getByLabel('Nueva observación',{exact:true}).fill(texto);const nueva=await control('reentrada');let posts=[];
 page.on('request',r=>{if(r.method()==='POST'&&r.url().endsWith('/observaciones'))posts.push(r.postDataJSON())});
 const r=page.waitForResponse(r=>r.request().method()==='POST'&&r.url().endsWith('/observaciones'));
 await page.getByRole('button',{name:'Publicar observación',exact:true}).focus();await page.keyboard.press('Enter');expect((await r).status()).toBe(409);
 const viejo=page.getByLabel('Texto sin enviar: observación');await expect(viejo).toHaveValue(texto);await expect(viejo).toHaveAttribute('readonly','');await expect(page.getByLabel('Nueva observación',{exact:true})).toHaveValue('');expect(posts).toHaveLength(1);expect(posts[0].entradaEsperadaId).not.toBe(nueva.entradaId);
 const scan=await escanear(page,'axe-borrador-conflicto');guardar('borrador-conflicto',{status:409,posts:posts.length,entradaEnviada:posts[0].entradaEsperadaId,nuevaEntrada:nueva.entradaId,focus:await foco(page),recuperable:true,violations:scan.violations});expect.soft(scan.violations).toEqual([]);
});


test('07 nombres, cronograma y menú con teclado sin overflow',async({browser})=>{
 const datos=[];
 for(const width of [1440,390]){
  const c=await browser.newContext({viewport:{width,height:1000}});const page=await c.newPage();
  try{
   await login(page);await detalle(page,acceso().pedido8);
   const linea=page.getByRole('list',{name:/Recorrido del pedido/});const nombres=await linea.ariaSnapshot();
   expect(nombres).not.toMatch(/etapa actual.*etapa actual|completada.*completada|pendiente.*pendiente/);expect(await linea.getByRole('button').count()).toBe(11);
   expect(await linea.getByRole('button').evaluateAll(es=>es.every(e=>!e.hasAttribute('aria-label')))).toBe(true);
   const region=page.getByRole('region',{name:'Fechas de cada etapa (desplazable)',exact:true});
   await region.focus();await page.keyboard.press('Shift+Tab');await page.keyboard.press('Tab');await expect(region).toBeFocused();const antes=await region.evaluate(e=>({left:e.scrollLeft,width:e.clientWidth,scroll:e.scrollWidth}));
   await page.keyboard.press('ArrowRight');if(antes.scroll>antes.width)await expect.poll(()=>region.evaluate(e=>e.scrollLeft)).toBeGreaterThan(antes.left);
   const focus=await foco(page);expect(focus.focusVisible).toBe(true);await page.screenshot({path:path.join(base,'cronograma-teclado-'+width+'.png')});
   const menu=page.getByRole('navigation',{name:'Menú principal'});const inicio=menu.getByRole('link',{name:'Inicio',exact:true});await inicio.focus();
   const pasos=[];for(let i=0;i<8;i++){const actual=await foco(page);expect.soft(actual.rect.x).toBeGreaterThanOrEqual(-1);expect.soft(actual.rect.right).toBeLessThanOrEqual(width+1);pasos.push(actual);guardar("menu-parcial-"+width,pasos);if(i<7)await page.keyboard.press('Tab')}
   await page.keyboard.press('Home');const cerrar=page.getByRole('button',{name:'Cerrar sesión',exact:true});await cerrar.focus();await page.keyboard.press('Tab');await page.keyboard.press('Shift+Tab');await expect(cerrar).toBeFocused();const cierre=await foco(page);expect(cierre.rect.x).toBeGreaterThanOrEqual(0);expect(cierre.rect.right).toBeLessThanOrEqual(width);
   const ancho=await page.evaluate(()=>({viewport:innerWidth,scroll:document.documentElement.scrollWidth}));expect(ancho.scroll).toBeLessThanOrEqual(width);await page.screenshot({path:path.join(base,'shell-teclado-'+width+'.png')});
   datos.push({width,nombres,antes,despues:await region.evaluate(e=>e.scrollLeft),focus,pasos,cierre,ancho});
  }finally{await c.close()}
 }
 guardar('nombres-scroll-menu',datos);
});

test('08 contraste Reintentar en alertas de error inducido',async({browser})=>{
 const resultados=[];
 for(const caso of [{id:'meta',ruta:'/configuracion/plazos',patron:'**/api/v1/configuracion/plazos'},{id:'cronograma',ruta:null,patron:'**/api/v1/pedidos/*/cronograma'},{id:'bandeja',ruta:'/inicio',patron:'**/api/v1/pedidos/bandeja?*'}]){
  const c=await browser.newContext();const page=await c.newPage();
  try{
   await login(page);await page.route(caso.patron,r=>r.fulfill({status:500,contentType:'application/problem+json',body:JSON.stringify({status:500,title:'Fallo ficticio controlado',detail:'Error ficticio para comprobar accesibilidad',codigo:'ERROR_INTERNO'})}));
   await page.goto(caso.ruta||'/pedidos/'+acceso().pedido8);const boton=page.getByRole('button',{name:'Reintentar',exact:true}).first();await expect(boton).toBeVisible();await page.evaluate(async()=>{await Promise.all(document.getAnimations().filter(a=>a.effect?.getTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{})))});const normal=await escanear(page,'axe-reintentar-normal-'+caso.id);expect.soft(normal.violations).toEqual([]);await boton.focus();await page.keyboard.press('Tab');await page.keyboard.press('Shift+Tab');await expect(boton).toBeFocused();
   const scan=await escanear(page,'axe-reintentar-'+caso.id);const colores=await boton.evaluate(e=>{const s=getComputedStyle(e);return {color:s.color,fondo:s.backgroundColor,texto:e.textContent}});await page.screenshot({path:path.join(base,'reintentar-'+caso.id+'.png')});resultados.push({caso:caso.id,colores,violations:scan.violations,origen:'Fallo HTTP sintético; no integración fallida real del servidor'});expect.soft(scan.violations).toEqual([]);
  }finally{await c.close()}
 }
 guardar('reintentar',resultados);
});
