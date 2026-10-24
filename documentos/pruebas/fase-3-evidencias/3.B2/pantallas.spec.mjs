import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
import {readFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
const f=JSON.parse(readFileSync(new URL('./fixtures.json',import.meta.url)));
const pedido=f.detalle.id, id=f.adjunto.id;
async function entorno(page,usuario,handler) {
 const inesperadas=[];
 await page.route('**/*',async route=>{
  const url=new URL(route.request().url());
  if(url.origin!=='http://127.0.0.1:43182') {inesperadas.push(url.origin);return route.abort();}
  if(!url.pathname.startsWith('/api/'))return route.continue();
  const json=body=>route.fulfill({json:body});
  if(url.pathname.endsWith('/auth/renovar'))return json({tokenAcceso:'ficticio-b2',expiraEn:900,usuario});
  if(url.pathname.endsWith('/auth/yo'))return json({...usuario,permisos:[]});
  if(await handler(route,url,json))return;
  if(url.pathname.endsWith('/historial'))return json([]);
  if(/\/etapas\/\d+\/archivos$/.test(url.pathname))return json([]);
  if(url.pathname===`/api/v1/pedidos/${pedido}`)return json(f.detalle);
  if(url.pathname==='/api/v1/flujos/EC/A')return json(f.flujo);
  if(url.pathname.startsWith('/api/v1/')) {inesperadas.push(url.pathname);return route.fulfill({status:500,json:{}});}
 });
 return inesperadas;
}
test('admin ve registro persistido simulado sin envio ni exito inventado',async({page})=>{
 const i=await entorno(page,f.admin,async(r,u,json)=>{
  if(u.pathname.endsWith('/notificaciones')) {await json({elementos:[{id,pedidoId:pedido,eventoId:id,destinatario:'admin@ejemplo.test',asunto:'Aviso ficticio B2',estado:'FALLIDA',creadaEn:'2026-10-19T15:00:00Z',enviadaEn:null,intentos:1,ultimoError:'RESULTADO_INCIERTO',resultadoIncierto:true,bloqueo:null}],pagina:0,tamano:20,total:1});return true;}
  if(u.pathname.endsWith('/copias-fallidas')) {await json([]);return true;}
 });
 await page.goto('/notificaciones');
 await expect(page.getByRole('heading',{name:'Registro de correos'})).toBeVisible();
 await expect(page.getByRole('cell',{name:'Resultado incierto RESULTADO_INCIERTO'})).toBeVisible();
 await expect(page.getByRole('cell',{name:'admin@ejemplo.test'})).toBeVisible();
 await expect(page.getByRole('button',{name:/^(Enviar|Reenviar|Reintentar)/})).toHaveCount(0);
 expect(i).toEqual([]);
});
test('no admin no consulta el registro ni ve el menu de correos',async({page})=>{
 let consultas=0;
 await entorno(page,f.ana,async(r,u,json)=>{if(u.pathname.endsWith('/notificaciones')){consultas++;await json({});return true;}});
 await page.goto('/notificaciones');
 await expect(page.getByRole('heading',{name:'Sin permiso'})).toBeVisible();
 await expect(page.getByRole('link',{name:'Correos',exact:true})).toHaveCount(0);
 expect(consultas).toBe(0);
});
test('enlaces solo para copia confirmada; descarga se conserva en backend',async({page})=>{
 let enlaces=0,descargas=0;
 const filas=['PENDIENTE','COPIADO','FALLIDO'].map((estadoCopia,k)=>({...f.adjunto,id:id.slice(0,-1)+k,nombre:`copia-${k}.bin`,estadoCopia}));
 const i=await entorno(page,f.mario,async(r,u,json)=>{
  if(u.pathname.endsWith('/etapas/2/archivos')){await json(filas);return true;}
  if(u.pathname.endsWith('/enlace')){enlaces++;await json({url:'https://drive.google.com/file/d/id-ficticio-b2/view'});return true;}
  if(u.pathname.endsWith('/contenido')){descargas++;await r.fulfill({body:'bytes-ficticios-b2',contentType:'application/octet-stream'});return true;}
 });
 await page.goto(`/pedidos/${pedido}`);
 await expect(page.getByText('Copia a Drive pendiente',{exact:true})).toBeVisible();
 await expect(page.getByText('Falló la copia',{exact:true})).toBeVisible();
 await expect(page.getByRole('button',{name:'Consultar enlace de Drive'})).toHaveCount(1);
 await expect(page.getByRole('link',{name:'Abrir en Drive'})).toHaveCount(0);
 expect(enlaces).toBe(0);
 await page.getByRole('button',{name:'Consultar enlace de Drive'}).click();
 await expect(page.getByRole('link',{name:'Abrir en Drive'})).toHaveAttribute('href','https://drive.google.com/file/d/id-ficticio-b2/view');
 const d=page.waitForEvent('download');await page.getByRole('button',{name:'Descargar copia-0.bin'}).click();
 expect((await d).suggestedFilename()).toBe('copia-0.bin');expect(descargas).toBe(1);expect(enlaces).toBe(1);expect(i).toEqual([]);
});
test('progreso de bloques pausa y reanuda desde el prefijo confirmado sin exito anticipado',async({page})=>{
 const total=4194305,cargaId='00000000-0000-4000-8000-000000000095';let offset=0;const fragmentos=[];let finales=0;
 let liberar;const gate=new Promise(r=>liberar=r);let recibidos=0;let reanudaciones=0;
 let segundo;const gate2=new Promise(r=>segundo=r);
 const carga=()=>({id:cargaId,pedidoId:pedido,etapa:2,entradaId:f.adjunto.entradaId,nombre:'ficticio.bin',tipoDocumento:'Documentos codificados',tamanoBytes:total,offsetConfirmado:offset,fragmentoBytes:4194304,estado:'RECIBIENDO',adjuntoId:null,eventoId:null,creadaEn:'2026-10-19T15:00:00Z',actualizadaEn:'2026-10-19T15:00:00Z',fragmentos});
 const i=await entorno(page,f.mario,async(r,u,json)=>{
  if(u.pathname.endsWith('/archivos/cargas')){await json(carga());return true;}
  if(u.pathname===`/api/v1/archivos/cargas/${cargaId}`){reanudaciones++;await json(carga());return true;}
  if(u.pathname.endsWith('/fragmentos')){
   const req=r.request(),b=req.postDataBuffer(),start=offset;recibidos++;
   expect(req.headers()['content-range']).toBe(`bytes ${start}-${start+b.length-1}/${total}`);
   expect(req.headers()['x-fragmento-sha256']).toBe(createHash('sha256').update(b).digest('hex'));
   if(recibidos===1)await gate;
   if(recibidos===2){await gate2;await r.abort();return true;}
   offset+=b.length;fragmentos.push({inicio:start,tamano:b.length,sha256:req.headers()['x-fragmento-sha256']});await json(carga());return true;
  }
  if(u.pathname.endsWith('/finalizacion')){finales++;await json(f.adjunto);return true;}
 });
 await page.goto(`/pedidos/${pedido}`);await page.getByLabel('Archivo',{exact:true}).setInputFiles({name:'ficticio.bin',mimeType:'application/octet-stream',buffer:Buffer.alloc(total,120)});
 await page.getByRole('button',{name:'Subir o reanudar'}).click();
 const progress=page.getByRole('progressbar',{name:'Bytes confirmados de la carga'});
 await expect.poll(()=>recibidos).toBe(1);await expect(progress).toHaveAttribute('value','0');expect(finales).toBe(0);
 await expect(page.getByText(/ficticio\.bin recibido por el servidor\. Copia a Drive pendiente\./)).toHaveCount(0);liberar();
 await expect.poll(()=>recibidos).toBe(2);await expect(progress).toHaveAttribute('value','4194304');expect(finales).toBe(0);
 await page.getByRole('button',{name:/Pausar/}).click();
 await expect(page.getByText(/Carga pausada/)).toBeVisible();segundo();
 await expect(page.getByText(/ficticio\.bin recibido por el servidor/)).toHaveCount(0);
 await page.getByRole('button',{name:'Subir o reanudar'}).click();
 await expect.poll(()=>finales).toBe(1);expect(recibidos).toBe(3);expect(reanudaciones).toBe(1);expect(offset).toBe(total);
 await expect(page.getByText(/ficticio\.bin recibido por el servidor\. Copia a Drive pendiente\./)).toBeVisible();expect(i).toEqual([]);
});
