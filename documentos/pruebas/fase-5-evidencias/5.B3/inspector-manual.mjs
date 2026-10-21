import fs from 'node:fs';import path from 'node:path';import readline from 'node:readline';import {fileURLToPath} from 'node:url';
import {chromium} from 'file:///C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/index.mjs';
const base=path.dirname(fileURLToPath(import.meta.url));const privado=JSON.parse(fs.readFileSync(path.join(base,'.tmp/acceso.json')));
const browser=await chromium.launch({channel:'msedge',headless:true});const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();
const pasos=[];
try{
 await page.goto('http://127.0.0.1:43184/login');await page.getByLabel('Correo, usuario o teléfono').fill('b2-admin');await page.getByLabel('Contraseña',{exact:true}).fill(privado.password);await page.getByRole('button',{name:'Ingresar',exact:true}).click();await page.waitForURL('**/inicio');await page.getByRole('tab',{name:/Pendientes.*20/}).waitFor();
 console.log('INSPECTOR_LISTO. Comandos JSON: url(ruta), tecla(key), foco(role,name), llenar(label,texto), viewport(width), foto, arbol, salir.');
 for await(const line of readline.createInterface({input:process.stdin,crlfDelay:Infinity})){
  try{
   const c=JSON.parse(line);if(c.tipo==='salir')break;
   if(c.tipo==='url'){if(!c.ruta.startsWith('/')||c.ruta.startsWith('//'))throw Error();await page.goto('http://127.0.0.1:43184'+c.ruta);}
   else if(c.tipo==='tecla')await page.keyboard.press(c.key);
   else if(c.tipo==='foco')await page.getByRole(c.role,{name:c.name,exact:true}).focus();
   else if(c.tipo==='llenar')await page.getByLabel(c.label,{exact:true}).fill(c.texto);
   else if(c.tipo==='viewport')await page.setViewportSize({width:c.width,height:1000});
   else if(c.tipo==='arbol'){console.log((await page.locator('main').ariaSnapshot()).slice(0,14000));}
   else if(c.tipo!=='foto')throw Error();
   await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))});
   const foco=await page.evaluate(()=>{const e=document.activeElement,s=getComputedStyle(e);return {nombre:e.getAttribute('aria-label')||e.textContent?.trim().slice(0,120),tag:e.tagName,rect:e.getBoundingClientRect().toJSON(),outline:s.outline,boxShadow:s.boxShadow,focusVisible:e.matches(':focus-visible'),scrollWidth:document.documentElement.scrollWidth,viewport:innerWidth}});
   const imagen='manual-'+String(pasos.length+1).padStart(2,'0')+'.png';await page.screenshot({path:path.join(base,imagen)});pasos.push({accion:c,foco,imagen,url:new URL(page.url()).pathname});fs.writeFileSync(path.join(base,'manual-pasos.json'),JSON.stringify(pasos,null,2));console.log(JSON.stringify({paso:pasos.length,foco,imagen}));
  }catch{console.log('No se pudo completar el comando; no se muestran cabeceras ni detalles privados.');}
 }
}finally{await context.close();await browser.close();console.log('INSPECTOR_DETENIDO');}
