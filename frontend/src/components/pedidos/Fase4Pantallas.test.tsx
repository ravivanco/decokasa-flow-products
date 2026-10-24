import '@/test/radix';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, problema, type UsuarioApi } from '@/test/apiFicticia';
import { accion, apiPedido, definicionFlujoA, evento, pagina, PEDIDO_ID, pedidoDetalle, resumenDe } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

// HTTP simulado, componentes/rutas/sesion/Query reales. NO acredita recorrido A1-Fin ni Google real.
const ruta=`/pedidos/${PEDIDO_ID}`;
const bodega={id:'00000000-0000-4000-8000-000000000077',nombre:'Almacén ficticio',ciudad:'Ciudad ficticia',activa:true,version:0};
const usuario=(roles:string[]):UsuarioApi=>({...ana,roles});
async function abrir(etapa:number, acciones:ReturnType<typeof accion>[], persona=admin) {
  const api=apiPedido(pedidoDetalle({etapaActual:etapa,acciones}));
  servidor.use(...api.manejadores);
  const app=abrirConSesion(persona,ruta);
  await screen.findByRole('heading',{level:2,name:new RegExp(`^A${etapa} ·`)});
  return {api,app};
}

describe('Fase 4.6 · pantallas conectadas',()=>{
  it.each([[['compras_china'],[5,6,7]],[['logistica'],[8,9,10]],[['consulta','compras_china','logistica'],[5,6,7,8,9,10]]] as const)
  ('la bandeja agrupa los resultados autorizados para %j',async(roles,etapas)=>{
    const datos=etapas.map(e=>resumenDe(pedidoDetalle({etapaActual:e})));
    // ID y numero distintos: no se fabrican pedidos adicionales en el componente.
    datos.forEach((p,i)=>{p.id=`00000000-0000-4000-8000-${String(i+1).padStart(12,'0')}`;p.numero=`FICTICIO-${i}`;});
    let lecturas=0;
    servidor.use(http.get('*/api/v1/pedidos/bandeja',()=>{lecturas++;return HttpResponse.json(pagina(datos));}),
      http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json(definicionFlujoA)));
    abrirConSesion(usuario([...roles]),'/bandeja');
    for(const etapa of etapas)expect(await screen.findByRole('heading',{level:2,name:new RegExp(`^A${etapa} ·`)})).toBeInTheDocument();
    expect(screen.getAllByRole('heading',{level:2})).toHaveLength(etapas.length);
    expect(lecturas).toBe(1);
  });

  it.each([6,9,10])('muestra y conserva bloqueo documental de A%i incluso con Enter',async(etapa)=>{
    const {api,app}=await abrir(etapa,[accion('APROBAR',etapa+1,{requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]);
    expect(screen.getByText('Documentos pendientes de definición')).toBeInTheDocument();
    const boton=screen.getByRole('button',{name:'Aprobar'});expect(boton).toBeDisabled();
    await app.usuario.click(boton);await app.usuario.keyboard('{Enter}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();expect(api.cuenta('POST','/acciones')).toBe(0);
  });

  it('demora exige observación y fecha, envía cadena y muestra fecha confirmada por API e historial',async()=>{
    const {api,app}=await abrir(7,[accion('REPORTAR_DEMORA',null)]);
    let cuerpo:Record<string,unknown>|undefined;
    const lecturas=api.cuenta('GET','/historial');
    servidor.use(http.post(`${api.base}/acciones`,async({request})=>{
      cuerpo=await request.json() as Record<string,unknown>;
      api.estado.detalle=pedidoDetalle({etapaActual:7,acciones:[accion('REPORTAR_DEMORA',null)],
        demoraAduana:{reportadaPor:admin,reportadaEn:'2026-10-16T17:00:00-05:00',motivo:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-19'}});
      api.estado.historial=[evento('demora-ficticia','DEMORA_REPORTADA',7,{observacion:'Demora ficticia',detalle:{fechaEstimadaSolicitada:'2026-10-17',nuevaFechaEstimadaLlegada:'2026-10-19'}})];
      return HttpResponse.json(api.estado.detalle);
    }));
    await app.usuario.click(screen.getByRole('button',{name:'Reportar demora'}));
    const dialogo=await screen.findByRole('dialog',{name:'Reportar demora'});
    await app.usuario.click(within(dialogo).getByRole('button',{name:'Reportar demora'}));
    expect(cuerpo).toBeUndefined();expect(within(dialogo).getByText('Indica la fecha estimada de llegada.')).toBeInTheDocument();
    await app.usuario.type(within(dialogo).getByLabelText('Observación'),'Demora ficticia');
    fireEvent.change(within(dialogo).getByLabelText('Nueva fecha estimada de llegada'),{target:{value:'2026-10-17'}});
    await app.usuario.click(within(dialogo).getByRole('button',{name:'Reportar demora'}));
    await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(cuerpo).toEqual({accion:'REPORTAR_DEMORA',etapaEsperada:7,observacion:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-17',adjuntoIds:[]});
    expect(await screen.findByText('Última demora · llegada estimada 19/10/2026')).toBeInTheDocument();
    expect(await screen.findByText('17/10/2026')).toBeInTheDocument();
    await waitFor(()=>expect(api.cuenta('GET','/historial')).toBeGreaterThan(lecturas));
    expect(screen.getByRole('button',{name:/^A7 .*, etapa actual$/})).toHaveAttribute('aria-current','step');
    expect(screen.getAllByText('Plazo pendiente del cliente').length).toBeGreaterThan(0);
  });

  it('llegada usa la marca de la definición y presenta solo la hora confirmada',async()=>{
    const {api,app}=await abrir(7,[accion('APROBAR',8)]);
    servidor.use(http.post(`${api.base}/acciones`,()=>{
      api.estado.detalle=pedidoDetalle({etapaActual:8,llegadaAduanaEn:'2026-10-16T17:00:00-05:00'});
      return HttpResponse.json(api.estado.detalle);
    }));
    await app.usuario.click(await screen.findByRole('button',{name:'Confirmar llegada'}));
    const d=await screen.findByRole('dialog',{name:'Confirmar llegada'});
    expect(within(d).queryByLabelText('Nueva fecha estimada de llegada')).not.toBeInTheDocument();
    await app.usuario.click(within(d).getByRole('button',{name:'Confirmar llegada'}));
    await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button',{name:/^A8 .*, etapa actual$/})).toBeInTheDocument();
    expect(screen.queryByText('Sin llegada confirmada')).not.toBeInTheDocument();
  });

  it('selecciona bodega activa de API y solo muestra la asignación confirmada',async()=>{
    const {api,app}=await abrir(9,[accion('APROBAR',10,{requiereBodega:true})]);
    let filtro:string|null=null,cuerpo:Record<string,unknown>|undefined;
    servidor.use(http.get('*/api/v1/bodegas',({request})=>{filtro=new URL(request.url).searchParams.get('activa');return HttpResponse.json([bodega]);}),
      http.post(`${api.base}/acciones`,async({request})=>{cuerpo=await request.json() as Record<string,unknown>;
        api.estado.detalle=pedidoDetalle({etapaActual:10,bodega});return HttpResponse.json(api.estado.detalle);}));
    await app.usuario.click(screen.getByRole('button',{name:'Aprobar'}));
    const d=await screen.findByRole('dialog',{name:'Aprobar'});
    expect(within(d).getByRole('button',{name:'Aprobar'})).toBeDisabled();
    await app.usuario.click(await within(d).findByRole('combobox',{name:'Bodega de destino'}));
    await app.usuario.click(await screen.findByRole('option',{name:'Almacén ficticio · Ciudad ficticia'}));
    await app.usuario.click(within(d).getByRole('button',{name:'Aprobar'}));
    await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(filtro).toBe('true');expect(cuerpo).toEqual({accion:'APROBAR',etapaEsperada:9,bodegaId:bodega.id,adjuntoIds:[]});
    expect(screen.getByText('Almacén ficticio · Ciudad ficticia')).toBeInTheDocument();
  });

  it.each([[],null])('sin bodegas o con error no permite asignar (%j)',async(datos)=>{
    const {app}=await abrir(9,[accion('APROBAR',10,{requiereBodega:true})]);let escrituras=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>datos?HttpResponse.json(datos):problema(503,'ERROR_INTERNO','Ficticio')),
      http.post('*/api/v1/pedidos/:id/acciones',()=>{escrituras++;return HttpResponse.json({});}));
    await app.usuario.click(screen.getByRole('button',{name:'Aprobar'}));
    const d=await screen.findByRole('dialog');
    if(datos)await within(d).findByText('No hay bodegas activas');else await within(d).findByText(/No se pudieron cargar las bodegas/);
    expect(within(d).getByRole('button',{name:'Aprobar'})).toBeDisabled();
    fireEvent.submit(within(d).getByRole('button',{name:'Aprobar'}).closest('form')!);expect(escrituras).toBe(0);
  });

  it('conflicto de etapa recarga detalle e historial sin reenviar',async()=>{
    const {api,app}=await abrir(8,[accion('APROBAR',9)]);let envios=0;
    const antes=api.cuenta('GET','/detalle');
    servidor.use(http.post(`${api.base}/acciones`,()=>{envios++;api.estado.detalle=pedidoDetalle({etapaActual:9});return problema(409,'ETAPA_CAMBIO','Ficticio');}));
    await app.usuario.click(screen.getByRole('button',{name:'Aprobar'}));const d=await screen.findByRole('dialog');
    await app.usuario.click(within(d).getByRole('button',{name:'Aprobar'}));
    await within(d).findByText('Otro usuario actualizó este pedido');
    expect(envios).toBe(1);await waitFor(()=>expect(api.cuenta('GET','/detalle')).toBeGreaterThan(antes));
    expect(within(d).queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
    await app.usuario.click(within(d).getByRole('button',{name:'Ver estado actual'}));
    expect(screen.getByRole('button',{name:/^A9 .*, etapa actual$/})).toBeInTheDocument();
  });

  it('Fin y resumen proceden de API, con evento del sistema y sin acciones manuales',async()=>{
    const fin=pedidoDetalle({etapaActual:11,finalizado:true,resumenes:{10:'Resumen ficticio enviado por servidor'}});
    const api=apiPedido(fin,{historial:[evento('fin-sistema','PEDIDO_FINALIZADO',11,{usuario:null,rolIntervencion:null})]});
    servidor.use(...api.manejadores);const app=abrirConSesion(admin,ruta);
    await screen.findByText('Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.');
    expect(await screen.findByText('Sistema')).toBeInTheDocument();
    expect(screen.queryByRole('button',{name:/^(Aprobar|Confirmar|Enviar|Reasignar|Subir archivo)$/})).not.toBeInTheDocument();
    await app.usuario.click(screen.getByRole('button',{name:/^A10 /}));
    expect(await screen.findByText('Resumen ficticio enviado por servidor')).toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
  });
});

describe('Catálogo real · roles y control de versión',()=>{
  it.each([['admin'],['logistica'],['consulta','logistica']])('permite crear para roles %j e invalida consultas',async(...roles)=>{
    let lista=[bodega], lecturas=0;const cuerpos:unknown[]=[];
    servidor.use(http.get('*/api/v1/bodegas',()=>{lecturas++;return HttpResponse.json(lista);}),
      http.post('*/api/v1/bodegas',async({request})=>{cuerpos.push(await request.json());const nueva={...bodega,id:'nueva-ficticia',nombre:'Nueva ficticia'};lista=[...lista,nueva];return HttpResponse.json(nueva,{status:201});}));
    const app=abrirConSesion(usuario(roles),'/bodegas');
    await screen.findByRole('cell',{name:bodega.nombre});const antes=lecturas;
    await app.usuario.click(screen.getByRole('button',{name:'Nueva bodega'}));const d=await screen.findByRole('dialog');
    await app.usuario.type(within(d).getByLabelText('Nombre'),'Nueva ficticia');await app.usuario.type(within(d).getByLabelText('Ciudad'),'Ciudad ficticia');
    await app.usuario.click(within(d).getByRole('button',{name:'Guardar bodega'}));
    expect(await screen.findByRole('cell',{name:'Nueva ficticia'})).toBeInTheDocument();expect(cuerpos).toEqual([{nombre:'Nueva ficticia',ciudad:'Ciudad ficticia',activa:true,pais:'EC'}]);expect(lecturas).toBeGreaterThan(antes);
  });

  it.each([['consulta'],['compras_china']])('no concede mantenimiento para %j',async(...roles)=>{
    let lecturas=0;servidor.use(http.get('*/api/v1/bodegas',()=>{lecturas++;return HttpResponse.json([]);}));
    abrirConSesion(usuario(roles),'/bodegas');await screen.findByRole('heading',{name:'Sin permiso'});
    expect(screen.queryByRole('link',{name:'Bodegas'})).not.toBeInTheDocument();expect(lecturas).toBe(0);
  });

  it('409 conserva borrador, recarga versión y no reenvía hasta revisar',async()=>{
    let actual=bodega, envios=0,lecturas=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>{lecturas++;return HttpResponse.json([actual]);}),
      http.patch('*/api/v1/bodegas/:id',async({request})=>{envios++;expect(await request.json()).toMatchObject({version:0,nombre:'Mi borrador'});actual={...bodega,nombre:'Cambio ajeno',version:1};return problema(409,'VERSION_DESACTUALIZADA','Ficticio');}));
    const app=abrirConSesion(admin,'/bodegas');await screen.findByRole('cell',{name:bodega.nombre});
    await app.usuario.click(screen.getByRole('button',{name:`Editar bodega ${bodega.nombre}`}));const d=await screen.findByRole('dialog');
    await app.usuario.clear(within(d).getByLabelText('Nombre'));await app.usuario.type(within(d).getByLabelText('Nombre'),'Mi borrador');
    await app.usuario.click(within(d).getByRole('button',{name:'Guardar bodega'}));
    await within(d).findByText(/Otra persona modificó esta bodega/);
    expect(within(d).getByLabelText('Nombre')).toHaveValue('Mi borrador');expect(within(d).getByRole('button',{name:'Guardar bodega'})).toBeDisabled();
    fireEvent.submit(within(d).getByRole('button',{name:'Guardar bodega'}).closest('form')!);expect(envios).toBe(1);expect(lecturas).toBeGreaterThan(1);
    await app.usuario.click(within(d).getByRole('button',{name:'Cerrar y revisar versión actual'}));
    expect(await screen.findByRole('cell',{name:'Cambio ajeno'})).toBeInTheDocument();
  });
});
