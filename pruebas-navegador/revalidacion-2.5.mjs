// Comprobación visual aislada de F-2.5-02/03. API ficticia; no ejecuta el recorrido 2.B3.
import { chromium } from '../.tmp/verificacion-ui/node_modules/playwright/index.mjs';
import assert from 'node:assert/strict';
import { mkdir, writeFile } from 'node:fs/promises';

const destino=new URL('../documentos/pruebas/evidencias-fase-2/revalidacion-2.5-2026-10-16/',import.meta.url);
await mkdir(destino,{recursive:true});
const origen='http://127.0.0.1:3017';
const id='0193f0a0-0000-7000-8000-000000000001';
const persona={id:'00000000-0000-4000-8000-000000000010',nombre:'Admin Prueba',roles:['admin']};
const usuario={...persona,correo:'admin@ejemplo.test',usuario:'adminprueba',telefono:'+593990000010',paises:['EC'],activo:true,version:0};
const nombres=['Crear solicitud de pedido','Codificación','Revisión y aprobación','Análisis y gestión','Fabricación','Embarque','Tránsito','Aduana','Bodega','Recepción','Fin'];
const roles=['admin','marketing','asistente_compras','compras_china','compras_china','compras_china','logistica','logistica','logistica','logistica',null];
const texto='Entrada anterior: Rechazada por Ana Prueba (Asistente de compras) el 15/10/2026 16:00 · vuelve a A2. Observación: «<b>Texto literal</b> y corrección ficticia». '+ 'Detalle informativo de la etapa. '.repeat(6);
const etapas=nombres.map((nombre,i)=>({numero:i+1,nombre,estado:i<2?'COMPLETADA':i===2?'ACTUAL':'PENDIENTE',rolResponsable:roles[i],
  estadoPlazo:i===0||i===10?'SIN_PLAZO':'DEFINIDO',asignadoA:null,asignacionVigente:null,
  entradaId:i<=2?`0193f0a0-0000-7000-9000-${String(i+1).padStart(12,'0')}`:null,
  entrada:i<=2?'2026-10-15T09:00:00-05:00':null,salida:i<2?'2026-10-15T10:00:00-05:00':null,
  venceEn:i===2?'2026-10-20T16:00:00-05:00':null,reentradas:i===2?1:0,resumen:i===2?texto:null}));
const pedido={id,numero:'DK-EC-2026-0001',nombre:'Pedido ficticio para comprobar el ancho de la página',pais:'EC',tipoFlujo:'A',
  etapaActual:3,nombreEtapaActual:nombres[2],responsableActual:null,venceEn:'2026-10-20T16:00:00-05:00',vencida:false,
  motivoRetraso:null,urgencia:null,finalizado:false,descripcion:'Descripción ficticia',incluyeProductosNuevos:false,
  creadoPor:persona,creadoEn:'2026-10-14T09:00:00-05:00',versionFlujo:1,lineaDeTiempo:etapas,accionesDisponibles:[]};
const flujo={pais:'EC',tipoFlujo:'A',version:1,etapas:etapas.map(e=>({numero:e.numero,nombre:e.nombre,rolResponsable:e.rolResponsable,
  estadoPlazo:e.estadoPlazo,revisaEtapa:e.numero===3?2:null,plazo:e.estadoPlazo==='DEFINIDO'?{cantidad:24,tipo:'HORAS_HABILES'}:null,
  acciones:[],automatica:e.numero===11}))};
const resultados=[];
const browser=await chromium.launch({headless:true,executablePath:'C:/Program Files/Google/Chrome/Application/chrome.exe'});
try {
  for(const ancho of [1440,390]) {
    const contexto=await browser.newContext({viewport:{width:ancho,height:ancho===1440?1000:844},isMobile:ancho===390,hasTouch:ancho===390});
    const pagina=await contexto.newPage();
    const errores=[],sinMock=[];
    pagina.on('pageerror',e=>errores.push(e.message));
    await contexto.route(/\/api\/v1\//,async ruta=>{
      const path=new URL(ruta.request().url()).pathname;
      let body;
      if(path.endsWith('/auth/renovar')||path.endsWith('/auth/login')) body={tokenAcceso:'token-ficticio-aislado',expiraEn:900,usuario};
      else if(path.endsWith('/auth/yo')) body={...usuario,permisos:[]};
      else if(path.includes('/flujos/')) body=flujo;
      else if(path.endsWith('/historial')||path.endsWith('/archivos')) body=[];
      else if(path===`/api/v1/pedidos/${id}`) body=pedido;
      else if(path==='/api/v1/pedidos') body={elementos:[pedido],pagina:0,tamano:20,total:1};
      else {sinMock.push(path); await ruta.fulfill({status:404,json:{codigo:'NO_ENCONTRADO'}}); return;}
      await ruta.fulfill({status:200,json:body});
    });
    await pagina.goto(`${origen}/pedidos/${id}`);
    const nota=pagina.getByRole('note',{name:'Lo ocurrido en la etapa',exact:true});
    try {await nota.waitFor({state:'visible'});} catch(error) {
      console.log(JSON.stringify({url:pagina.url(),texto:await pagina.locator('body').innerText(),errores,sinMock}));
      throw error;
    }
    assert.equal(await nota.getAttribute('role'),'note');
    assert.equal(await nota.locator('[role="alert"]').count(),0);
    assert.equal(await nota.locator('b,img').count(),0);
    assert.equal(await nota.locator('[data-slot="alert-description"]').textContent(),texto);
    const accesibilidad=await nota.ariaSnapshot();
    assert.match(accesibilidad,/note "Lo ocurrido en la etapa"/);
    const linea=pagina.getByRole('list',{name:`Recorrido del pedido ${pedido.numero}`});
    const medidas=await linea.evaluate(el=>{
      const contenedor=el.parentElement;
      contenedor.scrollLeft=0;
      const inicio=contenedor.scrollLeft;
      contenedor.scrollLeft=500;
      return {viewport:innerWidth,documento:document.documentElement.scrollWidth,body:document.body.scrollWidth,
        contenedor:contenedor.clientWidth,contenido:contenedor.scrollWidth,overflow:getComputedStyle(contenedor).overflowX,
        scrollInicial:inicio,scrollFinal:contenedor.scrollLeft};
    });
    assert.equal(medidas.documento,ancho,'El documento no debe desbordarse');
    assert.ok(medidas.contenido>medidas.contenedor,'Las once etapas mantienen su ancho interno');
    assert.equal(medidas.overflow,'auto');
    assert.ok(medidas.scrollFinal>medidas.scrollInicial,'La línea conserva desplazamiento interno');
    await pagina.screenshot({path:new URL(`detalle-${ancho}.png`,destino).pathname.replace(/^\/(?:([A-Za-z]):)/,'$1:'),fullPage:true});
    await pagina.goto(`${origen}/pedidos`);
    await pagina.getByRole('link',{name:pedido.numero,exact:true}).waitFor({state:'visible'});
    const lista=await pagina.evaluate(()=>({viewport:innerWidth,documento:document.documentElement.scrollWidth}));
    assert.equal(lista.documento,ancho,'La lista de pedidos no debe desbordarse');
    assert.deepEqual(errores,[]); assert.deepEqual(sinMock,[]);
    await pagina.screenshot({path:new URL(`lista-${ancho}.png`,destino).pathname.replace(/^\/(?:([A-Za-z]):)/,'$1:'),fullPage:true});
    resultados.push({ancho,detalle:medidas,lista,nota:{role:'note',nombre:'Lo ocurrido en la etapa',alertas:0,htmlInterpretado:false},errores,sinMock});
    await contexto.close();
  }
  await writeFile(new URL('navegador.json',destino),JSON.stringify(resultados,null,2));
  console.log(JSON.stringify(resultados,null,2));
} finally {await browser.close();}
