import '@/test/radix';
import { act, fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, problema } from '@/test/apiFicticia';
import { accion, adjunto, apiPedido, definicionFlujoA, entradaDe, evento, pagina, PEDIDO_ID, pedidoDetalle, resumenDe } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';

const ruta=`/pedidos/${PEDIDO_ID}`;
const bodega={id:'00000000-0000-4000-8000-000000000088',nombre:'Destino ficticio',ciudad:'Ciudad ficticia',activa:true,version:0};
function flujoPendiente() {
  return {...definicionFlujoA,etapas:definicionFlujoA.etapas.map(e=>({...e,acciones:[6,9,10].includes(e.numero)
    ? [accion('APROBAR',e.numero+1,{requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]:[]}))};
}
async function abrir(etapa:number, acciones:ReturnType<typeof accion>[], usuario=admin) {
  const api=apiPedido(pedidoDetalle({etapaActual:etapa,acciones}));servidor.use(...api.manejadores);
  const app=abrirConSesion(usuario,ruta);
  await screen.findByRole('heading',{level:2,name:new RegExp(`^A${etapa} ·`)});
  return {api,app};
}
async function abrirEdicion() {
  const app=abrirConSesion({...ana,roles:['consulta','logistica']},'/bodegas');
  await app.usuario.click(await screen.findByRole('button',{name:`Editar bodega ${bodega.nombre}`}));
  return {...app,d:await screen.findByRole('dialog',{name:'Editar bodega'})};
}

describe('Fase 4 · permisos y requisitos vigentes',()=>{
  it.each([6,9,10])('consulta ve el bloqueo de A%i aunque no reciba acciones',async(etapa)=>{
    const api=apiPedido(pedidoDetalle({etapaActual:etapa}));
    servidor.use(http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json(flujoPendiente())),...api.manejadores);
    abrirConSesion({...ana,roles:['consulta']},ruta);
    await screen.findByText('Documentos pendientes de definición');
    expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Nueva observación')).not.toBeInTheDocument();
    expect(api.cuenta('POST','/acciones')).toBe(0);
  });

  it.each([['compras_china','A6 · Embarque'],['logistica','A9 · Bodega'],['admin','A10 · Incidencias']])
  ('bandeja informa pendientes aplicables a %s sin inventar pedidos',async(rol,esperado)=>{
    servidor.use(http.get('*/api/v1/pedidos/bandeja',()=>HttpResponse.json({elementos:[],pagina:0,tamano:100,total:0})),
      http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json(flujoPendiente())));
    abrirConSesion({...ana,roles:[rol]},'/bandeja');
    await screen.findByText('Requisitos documentales pendientes en la definición vigente');
    expect(screen.getByRole('note')).toHaveTextContent(esperado);
    expect(await screen.findByText('No tienes pedidos pendientes.')).toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
  });

  it('403 tras abrir una acción bloquea posteriores envíos y conserva el borrador',async()=>{
    const {api,app}=await abrir(8,[accion('APROBAR',9)]);let envios=0;
    servidor.use(http.post(`${api.base}/acciones`,()=>{envios++;return problema(403,'SIN_PERMISO','detalle interno ficticio');}));
    await app.usuario.click(screen.getByRole('button',{name:'Aprobar'}));const d=await screen.findByRole('dialog');
    await app.usuario.type(within(d).getByLabelText('Observación (opcional)'),'Conservar texto');
    await app.usuario.click(within(d).getByRole('button',{name:'Aprobar'}));
    await within(d).findByText(/Tu cuenta ya no tiene permiso/);
    expect(within(d).getByRole('button',{name:'Aprobar'})).toBeDisabled();
    expect(within(d).getByLabelText('Observación (opcional)')).toHaveValue('Conservar texto');
    fireEvent.submit(within(d).getByRole('button',{name:'Aprobar'}).closest('form')!);
    expect(envios).toBe(1);expect(screen.queryByText('detalle interno ficticio')).not.toBeInTheDocument();
  });
});

describe('Demora · servidor y fecha estricta',()=>{
  it.each(['2026-02-30','2026-10-19T00:00:00Z','19/10/2026',''])('la entrada de fecha %s no sale como fecha válida',async(fecha)=>{
    const {api,app}=await abrir(7,[accion('REPORTAR_DEMORA',null)]);let envios=0;
    servidor.use(http.post(`${api.base}/acciones`,()=>{envios++;return HttpResponse.json({});}));
    await app.usuario.click(screen.getByRole('button',{name:'Reportar demora'}));const d=await screen.findByRole('dialog');
    await app.usuario.type(within(d).getByLabelText('Observación'),'Demora ficticia');
    fireEvent.change(within(d).getByLabelText('Nueva fecha estimada de llegada'),{target:{value:fecha}});
    await app.usuario.click(within(d).getByRole('button',{name:'Reportar demora'}));
    expect(within(d).getByText('Indica la fecha estimada de llegada.')).toBeInTheDocument();expect(envios).toBe(0);
  });

  it('fecha rechazada por el servidor muestra error, conserva campos y no cambia el pedido',async()=>{
    const {api,app}=await abrir(7,[accion('REPORTAR_DEMORA',null)]);let envios=0;
    servidor.use(http.post(`${api.base}/acciones`,()=>{envios++;return problema(400,'VALIDACION','Interno ficticio',[{campo:'nuevaFechaEstimadaLlegada',mensaje:'La estimación debe avanzar.'}]);}));
    await app.usuario.click(screen.getByRole('button',{name:'Reportar demora'}));const d=await screen.findByRole('dialog');
    await app.usuario.type(within(d).getByLabelText('Observación'),'Demora ficticia');
    fireEvent.change(within(d).getByLabelText('Nueva fecha estimada de llegada'),{target:{value:'2026-10-19'}});
    await app.usuario.click(within(d).getByRole('button',{name:'Reportar demora'}));
    await within(d).findByText('La estimación debe avanzar.');
    expect(within(d).getByLabelText('Nueva fecha estimada de llegada')).toHaveValue('2026-10-19');
    expect(within(d).getByLabelText('Observación')).toHaveValue('Demora ficticia');expect(envios).toBe(1);
    expect(screen.queryByText(/Última demora/)).not.toBeInTheDocument();
  });

  it('mientras espera respuesta impide doble envío y cierre, sin anunciar demora confirmada',async()=>{
    const {api,app}=await abrir(7,[accion('REPORTAR_DEMORA',null)]);
    let liberar!:()=>void;const respuesta=new Promise<void>(r=>{liberar=r;});let envios=0;
    servidor.use(http.post(`${api.base}/acciones`,async()=>{envios++;await respuesta;return problema(503,'ERROR_INTERNO','Ficticio');}));
    await app.usuario.click(screen.getByRole('button',{name:'Reportar demora'}));const d=await screen.findByRole('dialog');
    await app.usuario.type(within(d).getByLabelText('Observación'),'Demora ficticia');
    fireEvent.change(within(d).getByLabelText('Nueva fecha estimada de llegada'),{target:{value:'2026-10-19'}});
    await app.usuario.dblClick(within(d).getByRole('button',{name:'Reportar demora'}));
    await waitFor(()=>expect(envios).toBe(1));expect(within(d).getByRole('button',{name:'Cancelar'})).toBeDisabled();
    expect(within(d).getByLabelText('Observación')).toBeDisabled();
    await app.usuario.keyboard('{Escape}');expect(d).toBeInTheDocument();
    expect(screen.queryByText(/Demora registrada/)).not.toBeInTheDocument();
    await act(async()=>liberar());await within(d).findByText('No se pudo ejecutar la acción. Intenta de nuevo.');expect(envios).toBe(1);
  });
});

describe('Bodegas · consulta y mantenimiento sin éxitos ficticios',()=>{
  it('fallo de lectura se recupera con Actualizar y búsqueda respeta nombre/ciudad',async()=>{
    let lecturas=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>++lecturas===1?problema(503,'ERROR_INTERNO','Ficticio'):HttpResponse.json([bodega,{...bodega,id:'otra',nombre:'Otra ficticia',ciudad:'Quito',activa:false}])));
    const app=abrirConSesion(admin,'/bodegas');await screen.findByText('No se pudo cargar el catálogo');
    await app.usuario.click(screen.getByRole('button',{name:'Actualizar'}));await screen.findByRole('cell',{name:'Inactiva'});
    await app.usuario.type(screen.getByLabelText('Buscar por nombre o ciudad'),'  quiTO ');
    expect(screen.getByRole('cell',{name:'Otra ficticia'})).toBeInTheDocument();expect(screen.queryByRole('cell',{name:bodega.nombre})).not.toBeInTheDocument();
    await app.usuario.clear(screen.getByLabelText('Buscar por nombre o ciudad'));await app.usuario.type(screen.getByLabelText('Buscar por nombre o ciudad'),'sin coincidencias');
    expect(screen.getByText('Ninguna bodega coincide con la búsqueda.')).toBeInTheDocument();
  });

  it('catálogo vacío no genera bodegas y valida nombre/ciudad antes de crear',async()=>{
    let envios=0;servidor.use(http.get('*/api/v1/bodegas',()=>HttpResponse.json([])),http.post('*/api/v1/bodegas',()=>{envios++;return HttpResponse.json({});}));
    const app=abrirConSesion(admin,'/bodegas');await screen.findByText('Todavía no hay bodegas registradas.');
    await app.usuario.click(screen.getByRole('button',{name:'Nueva bodega'}));const d=await screen.findByRole('dialog');
    await app.usuario.click(within(d).getByRole('button',{name:'Guardar bodega'}));
    expect(within(d).getByText('Escribe un nombre de 1 a 100 caracteres.')).toBeInTheDocument();
    expect(within(d).getByText('Escribe una ciudad de 1 a 100 caracteres.')).toBeInTheDocument();expect(envios).toBe(0);
    await app.usuario.click(within(d).getByRole('button',{name:'Cancelar'}));expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it.each([409,400,403,503])('error %i al editar no actualiza la fila ni reintenta solo',async(status)=>{
    let envios=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>HttpResponse.json([bodega])),http.patch('*/api/v1/bodegas/:id',()=>{
      envios++;return status===409?problema(409,'DUPLICADO','Interno ficticio')
        :status===400?problema(400,'VALIDACION','Interno ficticio',[{campo:'nombre',mensaje:'Nombre no válido.'}])
        :problema(status,status===403?'SIN_PERMISO':'ERROR_INTERNO','Interno ficticio');
    }));
    const {d,usuario}=await abrirEdicion();
    await usuario.clear(within(d).getByLabelText('Nombre'));await usuario.type(within(d).getByLabelText('Nombre'),'Borrador ficticio');
    await usuario.click(within(d).getByRole('button',{name:'Guardar bodega'}));
    const texto=status===409?'Ese nombre ya está registrado.':status===400?'Nombre no válido.':status===403?/Tu cuenta ya no tiene permiso/:'No se pudo guardar la bodega. Puedes intentarlo de nuevo.';
    await within(d).findByText(texto);expect(envios).toBe(1);expect(within(d).getByLabelText('Nombre')).toHaveValue('Borrador ficticio');
    expect(screen.queryByText('Interno ficticio')).not.toBeInTheDocument();
    if(status===403){expect(within(d).getByRole('button',{name:'Guardar bodega'})).toBeDisabled();fireEvent.submit(within(d).getByRole('button',{name:'Guardar bodega'}).closest('form')!);expect(envios).toBe(1);}
    await usuario.click(within(d).getByRole('button',{name:'Cancelar'}));expect(screen.getByRole('cell',{name:bodega.nombre})).toBeInTheDocument();
  });

  it('desactivar usa versión leída y muestra la respuesta del servidor',async()=>{
    let actual=bodega;let cuerpo:unknown;
    servidor.use(http.get('*/api/v1/bodegas',()=>HttpResponse.json([actual])),http.patch('*/api/v1/bodegas/:id',async({request})=>{
      cuerpo=await request.json();actual={...bodega,activa:false,version:1};return HttpResponse.json(actual);
    }));
    const {d,usuario}=await abrirEdicion();await usuario.click(within(d).getByRole('checkbox',{name:'Disponible para nuevas asignaciones'}));
    await usuario.click(within(d).getByRole('button',{name:'Guardar bodega'}));
    expect(await screen.findByRole('cell',{name:'Inactiva'})).toBeInTheDocument();expect(cuerpo).toEqual({nombre:bodega.nombre,ciudad:bodega.ciudad,activa:false,version:0});
  });

  it('una desactivación concurrente invalida selección y no elige otra automáticamente',async()=>{
    const {api,app}=await abrir(9,[accion('APROBAR',10,{requiereBodega:true})]);let activas=[bodega],envios=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>HttpResponse.json(activas)),http.post(`${api.base}/acciones`,()=>{
      envios++;activas=[];return problema(400,'VALIDACION','Ficticio',[{campo:'bodegaId',mensaje:'La bodega ya no está activa.'}]);
    }));
    await app.usuario.click(screen.getByRole('button',{name:'Aprobar'}));const d=await screen.findByRole('dialog');
    await app.usuario.click(await within(d).findByRole('combobox',{name:'Bodega de destino'}));await app.usuario.click(await screen.findByRole('option',{name:'Destino ficticio · Ciudad ficticia'}));
    await app.usuario.click(within(d).getByRole('button',{name:'Aprobar'}));await within(d).findByText('La bodega ya no está activa.');
    await within(d).findByText('La selección ya no está disponible. Elige otra bodega activa.');expect(within(d).getByRole('button',{name:'Aprobar'})).toBeDisabled();
    fireEvent.submit(within(d).getByRole('button',{name:'Aprobar'}).closest('form')!);expect(envios).toBe(1);
  });
});

describe('Fin automático · datos del servidor',()=>{
  it('una confirmación en fixture no anuncia Fin antes de recibir finalizado del backend',async()=>{
    // Versión explícita de interfaz posterior al bloqueo, sin modificar la definición real PENDIENTE.
    const inicial={...pedidoDetalle({etapaActual:10,acciones:[accion('APROBAR',11)]}),versionFlujo:901};
    const api=apiPedido(inicial);let liberar!:()=>void;const gate=new Promise<void>(r=>{liberar=r;});let envios=0;
    servidor.use(http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json({...definicionFlujoA,version:901})),...api.manejadores,
      http.post(`${api.base}/acciones`,async()=>{envios++;await gate;
        api.estado.detalle={...pedidoDetalle({etapaActual:11,finalizado:true}),versionFlujo:901};
        api.estado.historial=[evento('sistema-ficticio','PEDIDO_FINALIZADO',11,{usuario:null,rolIntervencion:null})];return HttpResponse.json(api.estado.detalle);
      }));
    const app=abrirConSesion(admin,ruta);await app.usuario.click(await screen.findByRole('button',{name:'Aprobar'}));
    const d=await screen.findByRole('dialog',{name:'Confirmar y finalizar'});await app.usuario.dblClick(within(d).getByRole('button',{name:'Confirmar y finalizar'}));
    await waitFor(()=>expect(envios).toBe(1));expect(screen.queryByText('Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.')).not.toBeInTheDocument();
    await act(async()=>liberar());await screen.findAllByText('Finalizado', {exact:true});
    expect(await screen.findByText('Sistema')).toBeInTheDocument();expect(screen.queryByRole('button',{name:/^(Aprobar|Confirmar y finalizar|Reasignar)$/})).not.toBeInTheDocument();
  });

  it('estado de bodega inactiva y llegada se conservan al releer el pedido',async()=>{
    servidor.use(...apiPedido(pedidoDetalle({etapaActual:10,bodega:{...bodega,activa:false},llegadaAduanaEn:'2026-10-16T22:00:00Z'})).manejadores);
    abrirConSesion({...ana,roles:['consulta']},ruta);
    expect(await screen.findByText('Destino ficticio · Ciudad ficticia · inactiva para nuevas asignaciones')).toBeInTheDocument();
    expect(screen.queryByText('Sin llegada confirmada')).not.toBeInTheDocument();expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
  });
});

describe('Historial y bloqueos · ramas de error y datos confirmados',()=>{
  it('bandeja recupera un fallo sin inventar pedidos ni transiciones locales',async()=>{
    let lecturas=0;
    servidor.use(http.get('*/api/v1/pedidos/bandeja',({request})=>{if(new URL(request.url).searchParams.get('tamano')!=='100')return HttpResponse.json(pagina([]));return ++lecturas===1?problema(503,'ERROR_INTERNO','Privado ficticio'):HttpResponse.json(pagina([resumenDe(pedidoDetalle({etapaActual:8}))]));}),
      http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json(definicionFlujoA)));
    const app=abrirConSesion({...ana,roles:['logistica']},'/bandeja');await screen.findByText('No se pudo cargar tu bandeja.');
    expect(screen.queryByRole('heading',{level:2,name:'A8 · Autorización de salida'})).not.toBeInTheDocument();
    await app.usuario.click(screen.getByRole('button',{name:'Reintentar'}));await screen.findByRole('heading',{level:2,name:'A8 · Autorización de salida'});
    expect(lecturas).toBe(2);expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
  });

  it('releer el detalle después de un fallo conserva A10 bloqueada',async()=>{
    const p=pedidoDetalle({etapaActual:10,acciones:[accion('APROBAR',11,{requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]});
    const api=apiPedido(p);let lecturas=0;
    servidor.use(http.get(api.base,()=>++lecturas===1?problema(503,'ERROR_INTERNO','Privado ficticio'):HttpResponse.json(p)),...api.manejadores);
    const app=abrirConSesion(admin,ruta);await screen.findByRole('heading',{name:'No se pudo cargar el pedido'});
    await app.usuario.click(screen.getByRole('button',{name:'Reintentar'}));await screen.findByRole('heading',{level:2,name:'A10 · Incidencias'});
    expect(lecturas).toBe(2);expect(screen.getByRole('button',{name:'Aprobar'})).toBeDisabled();
    expect(screen.getByText('Documentos pendientes de definición')).toBeInTheDocument();
  });

  it('catálogo no permite cerrar ni reenviar mientras guarda; un 400 sin campos no anuncia éxito',async()=>{
    let liberar!:()=>void;const gate=new Promise<void>(r=>{liberar=r;});let envios=0;
    servidor.use(http.get('*/api/v1/bodegas',()=>HttpResponse.json([bodega])),http.patch('*/api/v1/bodegas/:id',async()=>{
      envios++;await gate;return problema(400,'VALIDACION','Privado ficticio');
    }));
    const {d,usuario}=await abrirEdicion();await usuario.dblClick(within(d).getByRole('button',{name:'Guardar bodega'}));
    await waitFor(()=>expect(envios).toBe(1));expect(within(d).getByLabelText('Ciudad')).toBeDisabled();expect(within(d).getByRole('button',{name:'Cancelar'})).toBeDisabled();
    await usuario.keyboard('{Escape}');expect(d).toBeInTheDocument();
    await act(async()=>liberar());await within(d).findByText('Revisa los campos de la bodega.');expect(envios).toBe(1);
    expect(screen.queryByText('Privado ficticio')).not.toBeInTheDocument();await usuario.keyboard('{Escape}');
    await waitFor(()=>expect(screen.queryByRole('dialog')).not.toBeInTheDocument());expect(screen.getByRole('cell',{name:bodega.nombre})).toBeInTheDocument();
  });

  it('un fallo del historial permite releer sin inventar eventos',async()=>{
    const api=apiPedido(pedidoDetalle({etapaActual:10}));let lecturas=0;
    servidor.use(http.get(`${api.base}/historial`,()=>++lecturas===1?problema(503,'ERROR_INTERNO','Privado ficticio'):HttpResponse.json([])),...api.manejadores);
    const app=abrirConSesion({...ana,roles:['consulta']},ruta);const h=await screen.findByRole('region',{name:'Historial'});
    await within(h).findByText('No se pudo cargar el historial.');expect(screen.queryByText('Privado ficticio')).not.toBeInTheDocument();
    await app.usuario.click(within(h).getByRole('button',{name:'Reintentar'}));await within(h).findByText('Sin eventos todavía.');
    expect(lecturas).toBe(2);expect(within(h).queryByText('Pedido finalizado')).not.toBeInTheDocument();
  });

  it('conserva el orden devuelto por API, incluso con UUID y horas que no sirven para reordenarlo',async()=>{
    const hechos=[
      evento('zz-fin','PEDIDO_FINALIZADO',11,{usuario:null,rolIntervencion:null}),
      evento('aa-aprobacion','ETAPA_APROBADA',10),
      evento('zz-asignacion','BODEGA_ASIGNADA',9,{detalle:{bodegaId:bodega.id}}),
      evento('aa-demora','DEMORA_REPORTADA',7,{detalle:{fechaEstimadaAnterior:'2026-10-16',fechaEstimadaSolicitada:'2026-10-17',nuevaFechaEstimadaLlegada:'2026-10-19'}}),
    ];
    servidor.use(...apiPedido(pedidoDetalle({etapaActual:11,finalizado:true,bodega}),{historial:hechos}).manejadores);
    abrirConSesion(admin,ruta);const h=await screen.findByRole('region',{name:'Historial'});await within(h).findByText(/Bodega: Destino ficticio/);
    const filas=within(h).getAllByRole('listitem');expect(filas).toHaveLength(4);
    ['Pedido finalizado','Etapa aprobada','Bodega asignada','Demora reportada'].forEach((texto,i)=>expect(filas[i]).toHaveTextContent(texto));
    expect(filas[0]).toHaveTextContent('Sistema');expect(filas[2]).toHaveTextContent('nombre vigente');
    expect(filas[3]).toHaveTextContent('16/10/2026');expect(filas[3]).toHaveTextContent('17/10/2026');expect(filas[3]).toHaveTextContent('19/10/2026');
    expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
  });

  it('no inventa el nombre de una bodega histórica distinta y nombra los archivos por sus metadatos',async()=>{
    const archivo=adjunto('00000000-0000-4000-8000-000000000099',9,entradaDe(9),'Documento ficticio','Evidencia ficticia.pdf');
    servidor.use(...apiPedido(pedidoDetalle({etapaActual:10,bodega}),{adjuntos:{9:[archivo]},historial:[
      evento('bodega-anterior','BODEGA_ASIGNADA',9,{detalle:{bodegaId:'00000000-0000-4000-8000-000000000098'},adjuntoIds:[archivo.id]}),
    ]}).manejadores);
    abrirConSesion({...ana,roles:['consulta']},ruta);const h=await screen.findByRole('region',{name:'Historial'});
    await within(h).findByText('Evidencia ficticia.pdf');expect(within(h).getByText('Asignación de bodega registrada por el servidor.')).toBeInTheDocument();
    expect(within(h).queryByText(/nombre vigente/)).not.toBeInTheDocument();expect(within(h).queryByRole('link')).not.toBeInTheDocument();
  });

  it('una asignación que dejó de estar vigente no concede intervención aunque conserve el rol',async()=>{
    servidor.use(...apiPedido(pedidoDetalle({etapaActual:8,asignaciones:{8:{persona:ana,vigente:false}}})).manejadores);
    abrirConSesion({...ana,roles:['consulta','logistica']},ruta);
    await screen.findByText('Ya no puede actuar: solo el admin actúa o reasigna');
    expect(screen.queryByRole('button',{name:'Aprobar'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Reasignar'})).not.toBeInTheDocument();
    expect(screen.queryByRole('button',{name:'Agregar observación'})).not.toBeInTheDocument();
  });

  it('si falla la definición se informa la limitación y conserva el bloqueo enviado en las acciones',async()=>{
    const api=apiPedido(pedidoDetalle({etapaActual:10,acciones:[accion('APROBAR',11,{requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]}));
    servidor.use(http.get('*/api/v1/flujos/EC/A',()=>problema(503,'ERROR_INTERNO','Privado ficticio')),...api.manejadores);
    abrirConSesion(admin,ruta);await screen.findByText(/No se pudo cargar la definición del flujo/);
    expect(screen.getByText('Documentos pendientes de definición')).toBeInTheDocument();expect(screen.getByRole('button',{name:'Aprobar'})).toBeDisabled();
    expect(screen.queryByText('Privado ficticio')).not.toBeInTheDocument();
  });

  it.each([6,9,10])('tener archivos en A%i no resuelve documentos PENDIENTES',async(etapa)=>{
    const archivo=adjunto('00000000-0000-4000-8000-000000000099',etapa,entradaDe(etapa),'Documento ficticio','No resuelve pendientes.pdf');
    const api=apiPedido(pedidoDetalle({etapaActual:etapa,acciones:[accion('APROBAR',etapa+1,{requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]}),{adjuntos:{[etapa]:[archivo]}});
    servidor.use(...api.manejadores);const app=abrirConSesion(admin,ruta);await screen.findByTitle('No resuelve pendientes.pdf');
    expect(screen.getByText('Documentos pendientes de definición')).toBeInTheDocument();
    const aprobar=screen.getByRole('button',{name:'Aprobar'});expect(aprobar).toBeDisabled();await app.usuario.click(aprobar);await app.usuario.keyboard('{Enter}');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();expect(api.cuenta('POST','/acciones')).toBe(0);
  });

  it('en fixture aislado A10 presenta solo documentos elegidos, mantiene 422 y no anuncia Fin',async()=>{
    const archivo=adjunto('00000000-0000-4000-8000-000000000099',10,entradaDe(10),'Documento ficticio','Cierre ficticio.pdf');
    const p={...pedidoDetalle({etapaActual:10,acciones:[accion('APROBAR',11,{documentosObligatorios:['Documento ficticio']})]}),versionFlujo:901};
    const api=apiPedido(p,{adjuntos:{10:[archivo]}});let cuerpo:unknown,envios=0;
    servidor.use(http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json({...definicionFlujoA,version:901})),...api.manejadores,
      http.post(`${api.base}/acciones`,async({request})=>{envios++;cuerpo=await request.json();return HttpResponse.json({type:'about:blank',title:'Requisito faltante',status:422,codigo:'REQUISITO_FALTANTE',detail:'Privado ficticio',faltantes:['Documento ficticio']},{status:422,headers:{'Content-Type':'application/problem+json'}});}));
    const app=abrirConSesion(admin,ruta);await app.usuario.click(await screen.findByRole('button',{name:'Aprobar'}));
    const d=await screen.findByRole('dialog',{name:'Confirmar y finalizar'});const confirmar=within(d).getByRole('button',{name:'Confirmar y finalizar'});
    const elegido=within(d).getByRole('checkbox',{name:/Cierre ficticio.pdf/});
    await app.usuario.click(elegido);expect(confirmar).toBeDisabled();expect(within(d).getByText('· falta')).toBeInTheDocument();
    fireEvent.submit(confirmar.closest('form')!);expect(envios).toBe(0);
    await app.usuario.click(elegido);expect(confirmar).toBeEnabled();await app.usuario.click(confirmar);
    await within(d).findByText('Falta: Documento ficticio.');expect(envios).toBe(1);
    expect(cuerpo).toEqual({accion:'APROBAR',etapaEsperada:10,adjuntoIds:[archivo.id]});
    expect(screen.queryByText('Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.')).not.toBeInTheDocument();
    await app.usuario.click(within(d).getByRole('button',{name:'Cancelar'}));
    expect(screen.getByRole('heading',{level:2,name:'A10 · Incidencias'})).toBeInTheDocument();
  });
});

describe('Pendientes activos · historial y recuperación de consultas', () => {
  it.each([
    { anterior: admin.id, nueva: ana.id, esperado: `De ${admin.nombre} a ${ana.nombre}` },
    { anterior: null, nueva: ana.id, esperado: `Asignada a ${ana.nombre}` },
    { anterior: 'persona-desconocida', nueva: 'otra-desconocida', esperado: 'De otra persona a otra persona' },
  ])('reasignación histórica: $esperado', async ({ anterior, nueva, esperado }) => {
    const p = pedidoDetalle({ etapaActual: 10, asignaciones: { 8: { persona: ana, vigente: false } } });
    servidor.use(...apiPedido(p, { historial: [evento('reasignacion-ficticia', 'ETAPA_REASIGNADA', 8, {
      detalle: { asignadoAnterior: anterior, asignadoA: nueva }, rolIntervencion: 'logistica',
    })] }).manejadores);
    abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    const historial = await screen.findByRole('region', { name: 'Historial' });
    await within(historial).findByText(esperado);
    expect(within(historial).getByText(`${admin.nombre} · Logística`)).toBeInTheDocument();
    expect(historial).not.toHaveTextContent('persona-desconocida');
    expect(historial).not.toHaveTextContent('otra-desconocida');
    expect(screen.queryByRole('button', { name: 'Reasignar' })).not.toBeInTheDocument();
  });

  it.each([
    { motivo: 'ERROR', texto: 'Devolución por error', nivel: null, etiqueta: null },
    { motivo: 'PRODUCTO_NUEVO', texto: 'Devolución por producto nuevo', nivel: 'URGENTE', etiqueta: 'Urgente · 4 h' },
  ])('presenta $texto y el fin de urgencia solo en el evento que lo confirma', async ({ motivo, texto, nivel, etiqueta }) => {
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 10 }), { historial: [
      evento('fin-urgencia-ficticio', 'ETAPA_APROBADA', 4, { detalle: { urgenciaTerminada: true, etapaDestino: 5 } }),
      evento('devolucion-ficticia', 'PEDIDO_DEVUELTO', 4, {
        detalle: { motivoDevolucion: motivo, etapaDestino: 2 }, nivelUrgencia: nivel, observacion: 'Motivo ficticio del servidor',
      }),
    ] }).manejadores);
    abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    const historial = await screen.findByRole('region', { name: 'Historial' });
    await within(historial).findByText(texto);
    const filas = within(historial).getAllByRole('listitem');
    expect(filas).toHaveLength(2);
    expect(filas[0]).toHaveTextContent('La urgencia terminó');
    expect(filas[0]).toHaveTextContent('A4 → A5');
    expect(filas[1]).toHaveTextContent(texto);
    expect(filas[1]).toHaveTextContent('A4 → A2');
    expect(filas[1]).toHaveTextContent('Motivo ficticio del servidor');
    expect(filas[1]).not.toHaveTextContent('La urgencia terminó');
    if (etiqueta) expect(within(filas[1]).getByText(etiqueta)).toBeInTheDocument();
    else expect(within(filas[1]).queryByText(/Urgente|Prioritario|Normal/)).not.toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it.each([null, undefined])('admite los metadatos opcionales %s sin inventar actor, archivos o detalles', async (ausente) => {
    servidor.use(...apiPedido(pedidoDetalle({ etapaActual: 10 }), { historial: [
      evento('observacion-compatible', 'OBSERVACION_AGREGADA', 8, {
        usuario: ana, rolIntervencion: ausente, detalle: ausente, adjuntoIds: ausente,
        observacion: 'Texto recibido como dato: <script>ficticio()</script>',
      }),
    ] }).manejadores);
    abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    const historial = await screen.findByRole('region', { name: 'Historial' });
    await within(historial).findByText(ana.nombre);
    expect(within(historial).getByText('Texto recibido como dato: <script>ficticio()</script>')).toBeInTheDocument();
    expect(historial.querySelector('script')).toBeNull();
    expect(within(historial).queryByText('Sistema')).not.toBeInTheDocument();
    expect(within(historial).queryByRole('list', { name: 'Archivos del evento' })).not.toBeInTheDocument();
    expect(within(historial).getAllByRole('time')).toHaveLength(1);
  });

  it.each(['sin metadatos', 'consulta fallida'])('recupera el nombre del archivo tras %s sin perder ni duplicar su evento', async (situacion) => {
    const archivo = adjunto('00000000-0000-4000-8000-000000000097', 8, entradaDe(8), 'Documento ficticio', 'Metadato recuperado.pdf');
    const api = apiPedido(pedidoDetalle({ etapaActual: 10 }), { historial: [
      evento('archivo-historico', 'ARCHIVO_SUBIDO', 8, { adjuntoIds: [archivo.id] }),
    ] });
    let disponible = false;
    let lecturas = 0;
    servidor.use(http.get(`${api.base}/etapas/8/archivos`, () => {
      lecturas++;
      return disponible ? HttpResponse.json([archivo]) : situacion === 'consulta fallida'
        ? problema(503, 'ERROR_INTERNO', 'Detalle técnico ficticio') : HttpResponse.json([]);
    }), ...api.manejadores);
    const app = abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    const historial = await screen.findByRole('region', { name: 'Historial' });
    const archivos = await within(historial).findByRole('list', { name: 'Archivos del evento' });
    await waitFor(() => expect(lecturas).toBe(1));
    expect(within(archivos).getByText('Archivo')).toBeInTheDocument();
    expect(within(historial).queryByText('Detalle técnico ficticio')).not.toBeInTheDocument();
    disponible = true;
    // Simula una actualización externa de consultas activas por la API pública de TanStack;
    // los datos vuelven por HTTP/MSW, no se escriben directamente en la caché.
    await act(async () => { await app.consultas.refetchQueries({ type: 'active' }); });
    await within(historial).findByText('Metadato recuperado.pdf');
    expect(lecturas).toBe(2);
    expect(within(historial).getByRole('list', { name: 'Archivos del evento' })).toBeInTheDocument();
    expect(within(historial).getAllByText(/Archivo subido/)).toHaveLength(1);
    expect(within(historial).queryByText('Archivo', { exact: true })).not.toBeInTheDocument();
    expect(within(historial).queryByRole('link')).not.toBeInTheDocument();
  });

  it('recupera la definición de bandeja conservando pedidos y mostrando los pendientes recibidos', async () => {
    let disponible = false, lecturasFlujo = 0;
    servidor.use(
      http.get('*/api/v1/pedidos/bandeja', () => HttpResponse.json(pagina([resumenDe(pedidoDetalle({ etapaActual: 9 }))]))),
      http.get('*/api/v1/flujos/EC/A', () => {
        lecturasFlujo++;
        return disponible ? HttpResponse.json(flujoPendiente()) : problema(503, 'ERROR_INTERNO', 'Detalle técnico ficticio');
      }),
    );
    const app = abrirConSesion({ ...ana, roles: ['logistica'] }, '/bandeja');
    await screen.findByRole('heading', { level: 2, name: 'A9 · Bodega' });
    await waitFor(() => expect(lecturasFlujo).toBe(1));
    expect(screen.queryByText('Requisitos documentales pendientes en la definición vigente')).not.toBeInTheDocument();
    const numero = within(screen.getByRole('region', {name:'Bandeja por etapa'})).getByRole('link', { name: 'DK-EC-2026-0001' });
    disponible = true;
    await act(async () => { await app.consultas.refetchQueries({ type: 'active' }); });
    await screen.findByText('Requisitos documentales pendientes en la definición vigente');
    expect(screen.getByRole('note')).toHaveTextContent('A9 · Bodega');
    expect(screen.getByRole('note')).toHaveTextContent('A10 · Incidencias');
    expect(within(screen.getByRole('region', {name:'Bandeja por etapa'})).getByRole('link', { name: 'DK-EC-2026-0001' })).toHaveAttribute('href', numero.getAttribute('href'));
    expect(within(screen.getByRole('region', {name:'Bandeja por etapa'})).getAllByRole('link', { name: 'DK-EC-2026-0001' })).toHaveLength(1);
    expect(lecturasFlujo).toBe(2);
    expect(screen.queryByRole('button', { name: 'Aprobar' })).not.toBeInTheDocument();
  });

  it('recuperar el flujo del detalle conserva la etapa consultada y su resumen, sin levantar pendientes', async () => {
    const api = apiPedido(pedidoDetalle({ etapaActual: 10, resumenes: { 6: 'Resumen confirmado del servidor' } }));
    let disponible = false;
    servidor.use(http.get('*/api/v1/flujos/EC/A', () => disponible ? HttpResponse.json(flujoPendiente()) : problema(503, 'ERROR_INTERNO', 'Detalle técnico ficticio')), ...api.manejadores);
    const app = abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    await screen.findByText(/No se pudo cargar la definición del flujo/);
    await app.usuario.click(screen.getByRole('button', { name: /^A6 / }));
    await screen.findByRole('heading', { level: 2, name: 'A6 · Embarque' });
    expect(screen.getByText('Resumen confirmado del servidor')).toBeInTheDocument();
    disponible = true;
    await act(async () => { await app.consultas.refetchQueries({ type: 'active' }); });
    await screen.findByText('Documentos pendientes de definición');
    expect(screen.queryByText(/No se pudo cargar la definición del flujo/)).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { level: 2, name: 'A6 · Embarque' })).toBeInTheDocument();
    expect(screen.getByText('Resumen confirmado del servidor')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^A10 .*, etapa actual$/ })).toHaveAttribute('aria-current', 'step');
    expect(screen.queryByRole('button', { name: 'Aprobar' })).not.toBeInTheDocument();
  });

  it('al releer un cambio de etapa vuelve al estado actual del servidor y mantiene historial coherente', async () => {
    const api = apiPedido(pedidoDetalle({ etapaActual: 8 }));
    servidor.use(http.get('*/api/v1/flujos/EC/A', () => HttpResponse.json(flujoPendiente())), ...api.manejadores);
    const app = abrirConSesion({ ...ana, roles: ['consulta'] }, ruta);
    await screen.findByRole('heading', { level: 2, name: 'A8 · Autorización de salida' });
    await app.usuario.click(screen.getByRole('button', { name: /^A5 / }));
    await screen.findByRole('heading', { level: 2, name: 'A5 · Fabricación' });
    // Fixture de lectura de otro estado; no acredita una transición A9 real ni omite su bloqueo.
    api.estado.detalle = pedidoDetalle({ etapaActual: 10, resumenes: { 10: 'Estado externo ficticio confirmado' } });
    api.estado.historial = [evento('observacion-externa', 'OBSERVACION_AGREGADA', 10, { observacion: 'Actualización externa ficticia' })];
    await act(async () => { await app.consultas.refetchQueries({ type: 'active' }); });
    await screen.findByRole('heading', { level: 2, name: 'A10 · Incidencias' });
    expect(screen.getByText('Estado externo ficticio confirmado')).toBeInTheDocument();
    expect(await screen.findByText('Actualización externa ficticia')).toBeInTheDocument();
    expect(screen.getByText('Documentos pendientes de definición')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /^A10 .*, etapa actual$/ })).toHaveAttribute('aria-current', 'step');
    expect(screen.queryByRole('button', { name: 'Aprobar' })).not.toBeInTheDocument();
  });
});
