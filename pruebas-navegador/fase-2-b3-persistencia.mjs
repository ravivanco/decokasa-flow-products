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
async function check(name,fn){try{const detail=await fn();results.push({name,status:'PASS',detail});console.log('PASS',name);}catch(e){results.push({name,status:'FAIL',error:e.stack});console.log('FAIL',name,e.message);for(const [n,s]of Object.entries(sessions)){await s.page.screenshot({path:`${out}/fallo-${results.length}-${n}.png`,fullPage:true}).catch(()=>{});await fs.writeFile(`${out}/fallo-${results.length}-${n}.txt`,await s.page.locator('body').innerText()).catch(()=>{});}}await fs.writeFile(`${out}/persistencia.json`,JSON.stringify({results,traffic,errors},null,2));}
async function open(s,id){await s.page.goto(base+'/pedidos/'+id);await s.page.getByRole('heading',{level:1}).waitFor();await s.page.getByRole('button',{name:/^A1 /}).waitFor();}
async function upload(s,id,stage,name){await open(s,id);await s.page.getByLabel('Archivo',{exact:true}).setInputFiles({name,mimeType:'text/plain',buffer:Buffer.from('Documento ficticio real '+name+'\nSin datos personales.\n')});const rp=s.page.waitForResponse(r=>r.url().endsWith(`/etapas/${stage}/archivos`)&&r.request().method()==='POST');await s.page.getByRole('button',{name:'Subir archivo',exact:true}).click();const r=await rp;assert.equal(r.status(),201);return r.json();}
async function action(s,id,name,stage,observation='Verificación real 2.B3',urgent=false){await open(s,id);await s.page.getByRole('button',{name,exact:true}).click();const d=s.page.getByRole('dialog');await d.getByLabel(/^Observación/).fill(observation);if(urgent)await d.getByRole('radio',{name:/Urgente/}).check();const rp=s.page.waitForResponse(r=>r.url().endsWith('/acciones')&&r.request().method()==='POST');await d.getByRole('button',{name,exact:true}).click();const r=await rp;assert.equal(r.status(),200,await r.text());const b=await r.json();assert.equal(b.etapaActual,stage);return b;}
try{const a=await user('admin');
 await check('estado historial y bytes sobreviven reinicio de API',async()=>{const p=JSON.parse(await fs.readFile(out+'/pedido.json','utf8'));await open(a,p.id);const b=await api(a,`/pedidos/${p.id}`);assert.equal(b.body.etapaActual,5);const h=(await api(a,`/pedidos/${p.id}/historial`)).body;assert.deepEqual(h.slice(1),JSON.parse(await fs.readFile(out+'/historial.json','utf8')));assert.equal(h.length,19);const files=(await api(a,`/pedidos/${p.id}/etapas/2/archivos`)).body;assert.equal(files.length,4);const file=files.find(f=>f.nombre==='codificados-entrada4.txt');const r=await a.context.request.get(base+`/api/v1/archivos/${file.id}/contenido`,{headers:{Authorization:'Bearer '+a.token}});assert.equal(r.status(),200);assert.equal((await r.body()).toString(),'Documento ficticio real codificados-entrada4.txt\nSin datos personales.\n');return{etapa:b.body.etapaActual,eventos:h.length,archivos:files.length};});
 await check('413 no deja adjuntos eventos ni avances parciales',async()=>{const previous=JSON.parse(await fs.readFile(out+'/resultados.json','utf8'));const paths=[...new Set(previous.results.find(x=>x.name.startsWith('413 Spring')).detail.map(x=>x.problem.instance).filter(Boolean))];const data=[];for(const path of paths){const id=path.split('/')[4];const p=(await api(a,`/pedidos/${id}`)).body;assert.equal(p.etapaActual,1);const h=(await api(a,`/pedidos/${id}/historial`)).body;assert.deepEqual(h.map(x=>x.tipo),['PEDIDO_CREADO']);const files=(await api(a,`/pedidos/${id}/etapas/1/archivos`)).body;assert.deepEqual(files,[]);data.push({id,etapa:p.etapaActual,eventos:h.length,archivos:files.length});}assert.ok(data.length);return data;});
}finally{await fs.writeFile(out+'/persistencia.json',JSON.stringify({results,traffic,errors},null,2));await browser.close();}
