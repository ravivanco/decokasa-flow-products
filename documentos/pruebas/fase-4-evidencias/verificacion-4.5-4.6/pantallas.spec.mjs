import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
import {readFileSync} from 'node:fs';
const f=JSON.parse(readFileSync(new URL('./fixtures-browser.json',import.meta.url)));
const id=f.detalle.id;
const accion=(extra={})=>({accion:'APROBAR',etapaDestino:null,requiereObservacion:false,requiereUrgencia:false,requiereBodega:false,requisitosDocumentales:'DEFINIDOS',documentosObligatorios:[],documentosFaltantes:[],puedeCompletarse:true,...extra});
// Estados explicitos para pantallas; NO un motor ni un recorrido A1-Fin aprobado.
function pedido(etapa,acciones=[],extra={}) {
 const d=structuredClone(f.detalle);d.etapaActual=etapa;d.nombreEtapaActual=f.flujo.etapas[etapa-1].nombre;
 d.finalizado=false;d.venceEn=null;d.bodega=null;d.demoraAduana=null;d.llegadaAduanaEn=null;d.accionesDisponibles=acciones;
 d.lineaDeTiempo=d.lineaDeTiempo.map(e=>({...e,nombre:f.flujo.etapas[e.numero-1].nombre,rolResponsable:f.flujo.etapas[e.numero-1].rolResponsable,estadoPlazo:f.flujo.etapas[e.numero-1].estadoPlazo,estado:e.numero===etapa?'ACTUAL':e.numero<etapa?'COMPLETADA':'PENDIENTE',resumen:null}));
 return {...d,...extra};
}
async function entorno(page,roles,detalle,handler=async()=>false) {
 const inesperadas=[],errores=[];const usuario={...f.admin,roles};
 page.on('pageerror',e=>errores.push(e.message));
 await page.route('**/*',async route=>{
  const url=new URL(route.request().url());
  if(url.origin!=='http://127.0.0.1:43184'){inesperadas.push(url.origin);return route.abort();}
  if(!url.pathname.startsWith('/api/'))return route.continue();
  const json=body=>route.fulfill({json:body});
  if(url.pathname.endsWith('/auth/renovar'))return json({tokenAcceso:'ficticio-f4b',expiraEn:900,usuario});
  if(url.pathname.endsWith('/auth/yo'))return json({...usuario,permisos:[]});
  if(await handler(route,url,json))return;
  if(url.pathname===`/api/v1/pedidos/${id}`)return json(detalle());
  if(url.pathname.endsWith('/historial'))return json([]);
  if(/\/etapas\/\d+\/archivos$/.test(url.pathname))return json([]);
  if(url.pathname==='/api/v1/flujos/EC/A')return json(f.flujo);
  inesperadas.push(url.pathname);return route.fulfill({status:500,json:{}});
 });
 return ()=>{expect(inesperadas).toEqual([]);expect(errores).toEqual([]);};
}
for(const [roles,etapas] of [[['compras_china'],[5,6,7]],[['logistica'],[8,9,10]],[['consulta','compras_china','logistica'],[5,6,7,8,9,10]]]) {
 test(`bandejas por roles ${roles.join('+')} · API simulada`,async({page})=>{
  const fin=await entorno(page,roles,()=>pedido(1),async(r,u,json)=>{
   if(u.pathname.endsWith('/pedidos/bandeja')){await json({elementos:etapas.map((e,i)=>({...pedido(e),id:`fixture-${i}`,numero:`FICTICIO-${i}`})),pagina:0,tamano:100,total:etapas.length});return true;}
  });
  await page.goto('/bandeja');for(const e of etapas)await expect(page.getByRole('heading',{level:2,name:new RegExp(`^A${e} ·`)})).toBeVisible();
  await expect(page.getByRole('heading',{level:2})).toHaveCount(etapas.length);fin();
 });
}
for(const etapa of [6,9,10])test(`bloqueo documental real visible A${etapa}`,async({page})=>{
 let envios=0;const fin=await entorno(page,['admin'],()=>pedido(etapa,[accion({etapaDestino:etapa+1,requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]),async(r,u)=>{if(u.pathname.endsWith('/acciones')){envios++;await r.fulfill({status:500,json:{}});return true;}});
 await page.goto(`/pedidos/${id}`);await expect(page.getByText('Documentos pendientes de definición',{exact:true})).toBeVisible();
 await expect(page.getByRole('button',{name:'Aprobar',exact:true})).toBeDisabled();expect(envios).toBe(0);fin();
});
test('demora envia ISO y muestra estimacion confirmada, sin avanzar localmente',async({page})=>{
 let actual=pedido(7,[accion({accion:'REPORTAR_DEMORA'})]),cuerpos=[];
 const fin=await entorno(page,['compras_china'],()=>actual,async(r,u,json)=>{
  if(u.pathname.endsWith('/acciones')){cuerpos.push(r.request().postDataJSON());actual=pedido(7,[accion({accion:'REPORTAR_DEMORA'})],{demoraAduana:{reportadaPor:f.admin,reportadaEn:'2026-10-16T22:00:00Z',motivo:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-19'}});await json(actual);return true;}
 });
 await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Reportar demora'}).click();const d=page.getByRole('dialog');
 await d.getByRole('button',{name:'Reportar demora'}).click();expect(cuerpos).toHaveLength(0);
 await d.getByLabel('Observación').fill('Demora ficticia');await d.getByLabel('Nueva fecha estimada de llegada').fill('2026-10-17');
 await d.getByRole('button',{name:'Reportar demora'}).click();await expect(d).toBeHidden();
 expect(cuerpos).toEqual([{accion:'REPORTAR_DEMORA',etapaEsperada:7,observacion:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-17',adjuntoIds:[]}]);
 await expect(page.getByText('Última demora · llegada estimada 19/10/2026')).toBeVisible();await expect(page.getByRole('button',{name:/^A7 .*, etapa actual$/})).toBeVisible();fin();
});
test('llegada usa la marca y conserva solo la fecha del servidor',async({page})=>{
 let actual=pedido(7,[accion({etapaDestino:8})]);const fin=await entorno(page,['compras_china'],()=>actual,async(r,u,json)=>{
  if(u.pathname.endsWith('/acciones')){expect(r.request().postDataJSON()).toEqual({accion:'APROBAR',etapaEsperada:7,adjuntoIds:[]});actual=pedido(8,[],{llegadaAduanaEn:'2026-10-16T22:00:00Z'});await json(actual);return true;}
 });
 await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Confirmar llegada'}).click();await page.getByRole('dialog').getByRole('button',{name:'Confirmar llegada'}).click();
 await expect(page.getByRole('dialog')).toBeHidden();await expect(page.getByRole('button',{name:/^A8 .*, etapa actual$/})).toBeVisible();await expect(page.getByText('Sin llegada confirmada')).toHaveCount(0);fin();
});
test('fixture A9 definido selecciona bodega y actualiza detalle tras API',async({page})=>{
 let actual=pedido(9,[accion({etapaDestino:10,requiereBodega:true})]),cuerpo;
 const fin=await entorno(page,['logistica'],()=>actual,async(r,u,json)=>{
  if(u.pathname==='/api/v1/bodegas'){expect(u.searchParams.get('activa')).toBe('true');await json([f.bodega]);return true;}
  if(u.pathname.endsWith('/acciones')){cuerpo=r.request().postDataJSON();actual=pedido(10,[],{bodega:f.bodega});await json(actual);return true;}
 });
 await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Aprobar'}).click();const d=page.getByRole('dialog');await expect(d.getByRole('button',{name:'Aprobar'})).toBeDisabled();
 await d.getByRole('combobox',{name:'Bodega de destino'}).click();await page.getByRole('option',{name:'Bodega ficticia F4 · Ciudad ficticia'}).click();await d.getByRole('button',{name:'Aprobar'}).click();
 await expect(d).toBeHidden();expect(cuerpo).toEqual({accion:'APROBAR',etapaEsperada:9,bodegaId:f.bodega.id,adjuntoIds:[]});await expect(page.getByText('Bodega ficticia F4 · Ciudad ficticia')).toBeVisible();fin();
});
test('conflicto de etapa no reenvia y relee el estado',async({page})=>{
 let actual=pedido(8,[accion({etapaDestino:9})]),envios=0;const fin=await entorno(page,['logistica'],()=>actual,async(r,u)=>{
  if(u.pathname.endsWith('/acciones')){envios++;actual=pedido(9);await r.fulfill({status:409,contentType:'application/problem+json',json:{status:409,title:'Cambio ficticio',codigo:'ETAPA_CAMBIO'}});return true;}
 });
 await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Aprobar'}).click();await page.getByRole('dialog').getByRole('button',{name:'Aprobar'}).click();
 await expect(page.getByText('Otro usuario actualizó este pedido')).toBeVisible();await page.getByRole('button',{name:'Ver estado actual'}).click();await expect(page.getByRole('button',{name:/^A9 .*, etapa actual$/})).toBeVisible();expect(envios).toBe(1);fin();
});
test('catalogo guarda version y conflicto conserva borrador sin reenviar',async({page})=>{
 let actual=f.bodega,envios=0,lecturas=0;const fin=await entorno(page,['consulta','logistica'],()=>pedido(1),async(r,u,json)=>{
  if(u.pathname==='/api/v1/bodegas'){lecturas++;await json([actual]);return true;}
  if(u.pathname===`/api/v1/bodegas/${actual.id}`){envios++;expect(r.request().postDataJSON()).toMatchObject({nombre:'Mi borrador',version:0});actual={...f.bodega,nombre:'Cambio ajeno',version:1};await r.fulfill({status:409,contentType:'application/problem+json',json:{status:409,title:'Cambio ficticio',codigo:'VERSION_DESACTUALIZADA'}});return true;}
 });
 await page.goto('/bodegas');await page.getByRole('button',{name:'Editar bodega Bodega ficticia F4'}).click();const d=page.getByRole('dialog');await d.getByLabel('Nombre',{exact:true}).fill('Mi borrador');await d.getByRole('button',{name:'Guardar bodega'}).click();
 await expect(d.getByText(/Otra persona modificó esta bodega/)).toBeVisible();await expect(d.getByLabel('Nombre',{exact:true})).toHaveValue('Mi borrador');await expect(d.getByRole('button',{name:'Guardar bodega'})).toBeDisabled();expect(envios).toBe(1);expect(lecturas).toBeGreaterThan(1);
 await d.getByRole('button',{name:'Cerrar y revisar versión actual'}).click();await expect(page.getByRole('cell',{name:'Cambio ajeno',exact:true})).toBeVisible();fin();
});
test('solo consulta no accede a catalogo ni dispara su API',async({page})=>{
 let lecturas=0;const fin=await entorno(page,['consulta'],()=>pedido(1),async(r,u,json)=>{if(u.pathname==='/api/v1/bodegas'){lecturas++;await json([]);return true;}});
 await page.goto('/bodegas');await expect(page.getByRole('heading',{name:'Sin permiso'})).toBeVisible();await expect(page.getByRole('link',{name:'Bodegas'})).toHaveCount(0);expect(lecturas).toBe(0);fin();
});
test('Fin confirmado por API muestra Sistema y resumen sin acciones manuales',async({page})=>{
 const actual=pedido(11,[],{finalizado:true});actual.lineaDeTiempo[10].estado='COMPLETADA';actual.lineaDeTiempo[9].resumen='Resumen ficticio del servidor';
 const fin=await entorno(page,['admin'],()=>actual,async(r,u,json)=>{if(u.pathname.endsWith('/historial')){await json([{id:'fin-ficticio',pedidoId:id,etapa:11,tipo:'PEDIDO_FINALIZADO',usuario:null,rolIntervencion:null,fecha:'2026-10-16T22:00:00Z',detalle:null,adjuntoIds:[]}]);return true;}});
 await page.goto(`/pedidos/${id}`);await expect(page.getByText('Sistema',{exact:true})).toBeVisible();await expect(page.getByText('Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.')).toBeVisible();
 await expect(page.getByRole('button',{name:/^(Aprobar|Enviar|Reasignar|Confirmar llegada|Subir archivo)$/})).toHaveCount(0);await page.getByRole('button',{name:/^A10 /}).click();await expect(page.getByText('Resumen ficticio del servidor')).toBeVisible();fin();
});
