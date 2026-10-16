import { chromium } from '../.tmp/verificacion-ui/node_modules/playwright/index.mjs';
import assert from 'node:assert/strict';
import fs from 'node:fs/promises';
import { createHash } from 'node:crypto';
const out='documentos/pruebas/evidencias-fase-2/2.B3-2026-10-16';
const base='http://localhost:13023';
const results=[]; const sessions={}; const traffic=[]; const errors=[];
const browser=await chromium.launch({executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe',headless:true});
async function user(name){
 const context=await browser.newContext({viewport:{width:1440,height:1000},acceptDownloads:true});
 await context.addInitScript(()=>{window.csp=[];document.addEventListener('securitypolicyviolation',e=>window.csp.push({directive:e.violatedDirective,uri:e.blockedURI}));});
 const page=await context.newPage(); page.setDefaultTimeout(12000); let token;
 page.on('pageerror',e=>errors.push({user:name,message:e.message}));
 page.on('response',async r=>{if(r.url().includes('/api/v1/'))traffic.push({user:name,method:r.request().method(),url:r.url(),status:r.status()});});
 await page.goto(base+'/login');
 await page.getByLabel('Correo, usuario o teléfono').fill(name+'prueba');
 await page.getByLabel('Contraseña',{exact:true}).fill('Clave-Prueba-1');
 const response=page.waitForResponse(r=>r.url().endsWith('/auth/login')&&r.request().method()==='POST');
 await page.getByRole('button',{name:'Ingresar',exact:true}).click();
 const login=await response; assert.equal(login.status(),200); const body=await login.json(); token=body.tokenAcceso;
 await page.getByRole('heading',{name:/^Hola,/}).waitFor();
 const s={context,page,token}; sessions[name]=s;return s;
}
async function api(s,path,method='GET',data){const r=await s.context.request.fetch(base+'/api/v1'+path,{method,headers:{Authorization:'Bearer '+s.token},...(data?{data}:{} )});const text=await r.text();let body;try{body=JSON.parse(text);}catch{body=text;} return {status:r.status(),body};}
async function check(name,fn){try{const detail=await fn();results.push({name,status:'PASS',detail});console.log('PASS',name);}catch(e){results.push({name,status:'FAIL',error:e.stack});console.log('FAIL',name,e.message);for(const [n,s]of Object.entries(sessions)){await s.page.screenshot({path:`${out}/suplemento-fallo-${results.length}-${n}.png`,fullPage:true}).catch(()=>{});await fs.writeFile(`${out}/suplemento-fallo-${results.length}-${n}.txt`,await s.page.locator('body').innerText()).catch(()=>{});}}await fs.writeFile(`${out}/suplemento.json`,JSON.stringify({results,traffic,errors},null,2));}
async function open(s,id){await s.page.goto(base+'/pedidos/'+id);await s.page.getByRole('heading',{level:1}).waitFor();await s.page.getByRole('button',{name:/^A1 /}).waitFor();}
async function upload(s,id,stage,name){await open(s,id);await s.page.getByLabel('Archivo',{exact:true}).setInputFiles({name,mimeType:'text/plain',buffer:Buffer.from('Documento ficticio real '+name+'\nSin datos personales.\n')});const rp=s.page.waitForResponse(r=>r.url().endsWith(`/etapas/${stage}/archivos`)&&r.request().method()==='POST');await s.page.getByRole('button',{name:'Subir archivo',exact:true}).click();const r=await rp;assert.equal(r.status(),201);return r.json();}
async function action(s,id,name,stage,observation='Verificación real 2.B3',urgent=false){await open(s,id);await s.page.getByRole('button',{name,exact:true}).click();const d=s.page.getByRole('dialog');await d.getByLabel(/^Observación/).fill(observation);if(urgent)await d.getByRole('radio',{name:/Urgente/}).check();const rp=s.page.waitForResponse(r=>r.url().endsWith('/acciones')&&r.request().method()==='POST');await d.getByRole('button',{name,exact:true}).click();const r=await rp;assert.equal(r.status(),200,await r.text());const b=await r.json();assert.equal(b.etapaActual,stage);return b;}

try {
 const a=await user('admin'),m=await user('marketing'),multi=await user('multi');
 const p=(await api(a,'/pedidos','POST',{tipoFlujo:'A',nombre:'Conflicto y reasignaci?n ficticios',incluyeProductosNuevos:false})).body;const id=p.id;
 await upload(a,id,1,'solicitud-conflicto.txt');await action(a,id,'Enviar',2);
 async function assign(name){await open(a,id);await a.page.getByRole('button',{name:'Reasignar',exact:true}).click();const d=a.page.getByRole('dialog');await d.getByRole('combobox',{name:'Persona',exact:true}).click();await a.page.getByRole('option',{name,exact:true}).click();await d.getByLabel('Motivo (opcional)').fill('Prueba aislada de asignaci?n');const rp=a.page.waitForResponse(r=>r.url().endsWith('/asignacion')&&r.request().method()==='PUT');await d.getByRole('button',{name:'Reasignar',exact:true}).click();const r=await rp;assert.equal(r.status(),200);return r.json();}
 await check('reasignaci?n desde UI a usuario multirrol y permiso vigente',async()=>{const assigned=await assign('Multi Prueba');await open(m,id);assert.equal(await m.page.getByRole('button',{name:'Enviar',exact:true}).count(),0);const before=await api(a,`/pedidos/${id}/historial`);const denied=await api(m,`/pedidos/${id}/acciones`,'POST',{accion:'ENVIAR',etapaEsperada:2,adjuntoIds:[]});assert.equal(denied.status,403);const after=await api(a,`/pedidos/${id}/historial`);assert.deepEqual(after,before);await upload(multi,id,2,'multi-codificacion.txt');await action(multi,id,'Enviar',3);await action(multi,id,'Aprobar',4);return {assigned,denied};});
 const c=await user('china');
 await check('conflicto entre dos sesiones reales: una acci?n y un 409 sin duplicado',async()=>{await open(a,id);await open(c,id);for(const s of [a,c]){await s.page.getByRole('button',{name:'Aprobar',exact:true}).click();await s.page.getByRole('dialog').getByLabel(/^Observaci/).fill('Carrera real 2.B3');}const before=(await api(a,`/pedidos/${id}/historial`)).body;const responses=[a,c].map(s=>s.page.waitForResponse(r=>r.url().endsWith('/acciones')&&r.request().method()==='POST'));await Promise.all([a,c].map(s=>s.page.getByRole('dialog').getByRole('button',{name:'Aprobar',exact:true}).click()));const rr=await Promise.all(responses);assert.deepEqual(rr.map(r=>r.status()).sort(),[200,409]);const conflict=await rr.find(r=>r.status()===409).json();assert.equal(conflict.codigo,'ETAPA_CAMBIO');const loser=rr[0].status()===409?a:c;await loser.page.getByRole('button',{name:'Ver estado actual',exact:true}).waitFor();await loser.page.screenshot({path:out+'/conflicto-sesiones.png',fullPage:true});const after=(await api(a,`/pedidos/${id}/historial`)).body;assert.equal(after.length,before.length+1);assert.equal((await api(a,`/pedidos/${id}`)).body.etapaActual,5);return {statuses:rr.map(r=>r.status()),conflict,before:before.length,after:after.length};});
 await check('bandeja real de usuario multirrol y cierre de sesi?n',async()=>{await multi.page.goto(base+'/bandeja');await multi.page.getByRole('heading',{level:1,name:'Mi bandeja'}).waitFor();await multi.page.screenshot({path:out+'/bandeja-multirrol.png',fullPage:true});assert.deepEqual(await multi.page.evaluate(()=>window.csp),[]);await multi.page.getByRole('button',{name:/^Cerrar sesi/}).click();await multi.page.getByLabel('Correo, usuario o tel?fono').waitFor();await multi.page.reload();await multi.page.getByLabel('Correo, usuario o tel?fono').waitFor();return {javascriptErrors:errors};});
}finally{await fs.writeFile(out+'/suplemento.json',JSON.stringify({results,traffic,errors},null,2));await browser.close();}
