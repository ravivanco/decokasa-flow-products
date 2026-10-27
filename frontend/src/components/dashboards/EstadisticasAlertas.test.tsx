import '@/test/radix';
import { fireEvent, screen, waitFor, within } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { admin, ana, problema } from '@/test/apiFicticia';
import { estadisticasVacias } from '@/test/estadisticasFicticias';
import { apiPedido, definicionFlujoA, pagina, PEDIDO_ID, pedidoDetalle } from '@/test/pedidosFicticios';
import { abrirConSesion } from '@/test/renderizarApp';
import { servidor } from '@/test/servidor';
const anio=new Date().getFullYear();
function datos() {
  const d=estadisticasVacias(anio);
  d.tarjetas={creados:2,enCurso:1,finalizados:1,conRetraso:0,medianaResolucionSegundos:120,resolucionObservaciones:1};
  d.porAnio=[{anio:anio-2,pedidos:1},{anio:anio-1,pedidos:0},{anio,pedidos:1}];
  d.porMes=Array.from({length:12},(_,i)=>({mes:i+1,pedidos:i===0?2:null}));
  d.tiempoPorEtapa=[{tipoFlujo:'A',pais:'EC',etapa:2,nombreEtapa:'Codificación',medianaSegundos:120,promedioSegundos:120,terminadas:1,abiertas:1}];
  d.tiempoPorPedido=[
    {pedidoId:PEDIDO_ID,numero:'FICTICIO-CERRADO',tipoFlujo:'A',pais:'EC',creadoEn:'2026-01-01T05:00:00Z',finalizadoEn:'2026-01-01T05:02:00Z',finalizado:true,segundos:120},
    {pedidoId:'00000000-0000-4000-8000-000000000123',numero:'FICTICIO-ABIERTO',tipoFlujo:'B',pais:'EC',creadoEn:'2026-01-01T05:00:00Z',finalizadoEn:null,finalizado:false,segundos:90},
  ];d.tiempoPorPedidoTotal=2;d.estado={enCurso:1,finalizados:1,enCursoConRetraso:0,enCursoConUrgencia:0,enCursoConDemora:0};
  return d;
}
function preparar() {
  const consultas:URLSearchParams[]=[];
  servidor.use(http.get('*/api/v1/flujos/EC/A',()=>HttpResponse.json(definicionFlujoA)),
    http.get('*/api/v1/estadisticas/pedidos',({request})=>{consultas.push(new URL(request.url).searchParams);return HttpResponse.json(datos());}));
  return consultas;
}
describe('Inicio estadístico y alertas vigentes ADR-0029',()=>{
  it('estadísticas principales, duración corta y tablas accesibles para todas las series',async()=>{
    preparar();const app=abrirConSesion(admin,'/inicio');
    await screen.findByText(/Mediana de 1 pedido finalizado/);
    expect(screen.getAllByText('2 min',{exact:true})[0]).toBeVisible();
    expect(screen.getByText(/pocos casos/)).toBeVisible();
    expect(screen.queryByRole('heading',{name:'Mi trabajo'})).not.toBeInTheDocument();
    const graficos=screen.getAllByRole('button',{name:'Ver como tabla'});expect(graficos).toHaveLength(5);
    for(const boton of graficos)await app.usuario.click(boton);
    expect(screen.getAllByRole('table')).toHaveLength(5);
    expect(screen.getByRole('rowheader',{name:'FICTICIO-CERRADO'})).toBeVisible();
    expect(screen.getByRole('rowheader',{name:'FICTICIO-ABIERTO'})).toBeVisible();
    expect(screen.getAllByText('aún no llega').length).toBeGreaterThan(0);
  });
  it('filtra año, país y flujo en el servidor y restablece al país activo',async()=>{
    const consultas=preparar();sessionStorage.setItem(`decokasa.pais.${admin.id}`,'EC');
    const app=abrirConSesion({...admin,paises:['EC','CO']},'/inicio');
    await screen.findByText(/Mediana de 1 pedido finalizado/);
    const filtros=within(screen.getByRole('group',{name:'Filtros del dashboard'}));
    await app.usuario.selectOptions(filtros.getByLabelText('Año'),String(anio-2));
    await app.usuario.selectOptions(filtros.getByLabelText('País'),'todos');
    await app.usuario.selectOptions(filtros.getByLabelText('Flujo'),'B');
    await waitFor(()=>expect(consultas.at(-1)?.get('tipoFlujo')).toBe('B'));
    expect(consultas.at(-1)?.get('pais')).toBeNull();expect(consultas.at(-1)?.get('anio')).toBe(String(anio-2));
    await app.usuario.click(screen.getByRole('button',{name:'Restablecer'}));
    await waitFor(()=>expect(filtros.getByLabelText('Año')).toHaveValue(String(anio)));
    expect(filtros.getByLabelText('País')).toHaveValue('EC');expect(filtros.getByLabelText('Flujo')).toHaveValue('');
  });
  it('con fechas invertidas avisa y no emite una consulta nueva',async()=>{
    const consultas=preparar();abrirConSesion(admin,'/inicio');await screen.findByText(/Mediana de 1 pedido finalizado/);
    fireEvent.change(screen.getByLabelText('Desde'),{target:{value:'2026-02-02'}});
    await waitFor(()=>expect(consultas.at(-1)?.get('desde')).toBe('2026-02-02'));
    const antes=consultas.length;fireEvent.change(screen.getByLabelText('Hasta'),{target:{value:'2026-02-01'}});
    expect(await screen.findByText(/La fecha final/)).toBeVisible();
    await waitFor(()=>expect(consultas).toHaveLength(antes));
  });
  it('vacío y error no presentan una mediana ni cifras inventadas',async()=>{
    preparar();servidor.use(http.get('*/api/v1/estadisticas/pedidos',()=>HttpResponse.json(estadisticasVacias())));
    const app=abrirConSesion(admin,'/inicio');expect(await screen.findByText('Sin finalizados')).toBeVisible();
    expect(screen.getAllByText('No hay pedidos creados en este período.').length).toBeGreaterThan(0);
    servidor.use(http.get('*/api/v1/estadisticas/pedidos',()=>problema(503,'ERROR_INTERNO','Ficticio')));
    await app.consultas.resetQueries({queryKey:['sesion']});
    expect(await screen.findAllByRole('alert')).not.toHaveLength(0);
    expect(screen.queryByText('Sin finalizados')).not.toBeInTheDocument();
  });
  it('filtra abiertos/finalizados sin presentar antigüedad como duración final',async()=>{
    preparar();const app=abrirConSesion(ana,'/inicio');await screen.findByText(/Mediana de 1 pedido finalizado/);
    const region=screen.getByRole('region',{name:'Tiempo de cada pedido'});
    await app.usuario.click(within(region).getByRole('button',{name:'Ver como tabla'}));
    await app.usuario.click(within(region).getByRole('button',{name:'Abiertos'}));
    expect(within(region).getByRole('rowheader',{name:'FICTICIO-ABIERTO'})).toBeVisible();
    expect(within(region).queryByRole('rowheader',{name:'FICTICIO-CERRADO'})).not.toBeInTheDocument();
    expect(within(region).getByText('Abierto (antigüedad)')).toBeVisible();
    await app.usuario.click(within(region).getByRole('button',{name:'Ver como gráfico'}));
    expect(within(region).getByText(/y sigue/)).toBeVisible();
  });
  it('campana y bandeja comparten MIAS y desaparece la alerta cuando el servidor la resuelve',async()=>{
    preparar();let lista=[{pedidoId:PEDIDO_ID,numero:'ALERTA-FICTICIA',nombrePedido:'Ficticio',pais:'EC',tipoFlujo:'A',etapa:2,nombreEtapa:'Codificación',tipo:'ETAPA_VENCIDA',vence:'2026-01-01T00:00:00Z',nuevaFechaLlegada:null,motivo:null,asignadoA:null}];
    let lecturas=0;
    servidor.use(...apiPedido(pedidoDetalle()).manejadores,
      http.get('*/api/v1/pedidos/bandeja',()=>HttpResponse.json(pagina([]))),
      http.get('*/api/v1/alertas',({request})=>{expect(new URL(request.url).searchParams.get('ambito')).toBe('MIAS');lecturas++;return HttpResponse.json(lista);}));
    const app=abrirConSesion(ana,'/bandeja');
    expect(await screen.findByRole('button',{name:'Alertas: 1 activa'})).toBeVisible();
    expect(await within(screen.getByRole('region',{name:/Alertas y retrasos/})).findByText('ALERTA-FICTICIA')).toBeVisible();
    expect(lecturas).toBe(1);lista=[];
    await app.consultas.invalidateQueries({predicate:q=>q.queryKey.includes('alertas')});
    expect(await screen.findByRole('button',{name:'Alertas: 0 activas'})).toBeVisible();
    expect(screen.queryByText('ALERTA-FICTICIA')).not.toBeInTheDocument();
  });
});
