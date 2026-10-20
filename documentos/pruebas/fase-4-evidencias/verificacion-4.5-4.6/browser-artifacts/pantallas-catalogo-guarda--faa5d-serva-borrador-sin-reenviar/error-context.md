# Instructions

- Following Playwright test failed.
- Explain why, be concise, respect Playwright best practices.
- Provide a snippet of code with the fix, if possible.

# Test info

- Name: pantallas.spec.mjs >> catalogo guarda version y conflicto conserva borrador sin reenviar
- Location: pantallas.spec.mjs:82:1

# Error details

```
Error: expect(locator).toBeVisible() failed

Locator: getByRole('cell', { name: 'Cambio ajeno' })
Expected: visible
Error: strict mode violation: getByRole('cell', { name: 'Cambio ajeno' }) resolved to 2 elements:
    1) <td data-slot="table-cell" class="p-2 align-middle [&:has([role=checkbox])]:pr-0 max-w-64 whitespace-normal break-words font-medium">Cambio ajeno</td> aka getByRole('cell', { name: 'Cambio ajeno', exact: true })
    2) <td data-slot="table-cell" class="p-2 align-middle whitespace-nowrap [&:has([role=checkbox])]:pr-0 text-right">…</td> aka getByRole('cell', { name: 'Editar bodega Cambio ajeno' })

Call log:
  - Expect "toBeVisible" with timeout 5000ms
  - waiting for getByRole('cell', { name: 'Cambio ajeno' })

```

# Page snapshot

```yaml
- generic [ref=e2]:
  - generic [ref=e3]:
    - banner [ref=e4]:
      - generic [ref=e5]:
        - generic [ref=e6]: LATINOAMÉRICA
        - generic [ref=e7]: Importanto y Exportando el Mundo
      - generic [ref=e8]:
        - generic [ref=e9]: Admin Prueba
        - button "Cerrar sesión" [ref=e10]
    - generic [ref=e11]:
      - navigation "Menú principal" [ref=e12]:
        - list [ref=e13]:
          - listitem [ref=e14]:
            - link "Inicio" [ref=e15] [cursor=pointer]:
              - /url: /inicio
          - listitem [ref=e19]:
            - link "Mi bandeja" [ref=e20] [cursor=pointer]:
              - /url: /bandeja
          - listitem [ref=e24]:
            - link "Pedidos" [ref=e25] [cursor=pointer]:
              - /url: /pedidos
          - listitem [ref=e30]:
            - link "Bodegas" [ref=e31] [cursor=pointer]:
              - /url: /bodegas
      - main [ref=e35]:
        - generic [ref=e36]:
          - generic [ref=e37]:
            - generic [ref=e38]:
              - heading "Bodegas" [level=1] [ref=e39]
              - paragraph [ref=e40]: Nombre, ciudad y disponibilidad para nuevas asignaciones.
            - button "Nueva bodega" [ref=e41]
          - note [ref=e42]:
            - generic [ref=e46]: Las asignaciones existentes se conservan
            - generic [ref=e47]: Desactivar una bodega impide nuevas asignaciones. Los pedidos que ya la tienen no cambian.
          - generic [ref=e48]:
            - group [ref=e49]:
              - generic [ref=e50]: Buscar por nombre o ciudad
              - searchbox "Buscar por nombre o ciudad" [ref=e51]
            - button "Actualizar" [ref=e52]
          - table [ref=e54]:
            - caption [ref=e55]: 1 bodega · catálogo del servidor
            - rowgroup [ref=e56]:
              - row [ref=e57]:
                - columnheader "Nombre" [ref=e58]
                - columnheader "Ciudad" [ref=e59]
                - columnheader "Estado" [ref=e60]
                - columnheader "Mantenimiento" [ref=e61]
            - rowgroup [ref=e62]:
              - row [ref=e63]:
                - cell "Cambio ajeno" [ref=e64]
                - cell "Ciudad ficticia" [ref=e65]
                - cell "Activa" [ref=e66]
                - cell [ref=e68]:
                  - button "Editar bodega Cambio ajeno" [ref=e69]: Editar
  - region "Notifications alt+T"
```

# Test source

```ts
  1   | import {test,expect} from 'C:/Users/Richard/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright/test.mjs';
  2   | import {readFileSync} from 'node:fs';
  3   | const f=JSON.parse(readFileSync(new URL('./fixtures-browser.json',import.meta.url)));
  4   | const id=f.detalle.id;
  5   | const accion=(extra={})=>({accion:'APROBAR',etapaDestino:null,requiereObservacion:false,requiereUrgencia:false,requiereBodega:false,requisitosDocumentales:'DEFINIDOS',documentosObligatorios:[],documentosFaltantes:[],puedeCompletarse:true,...extra});
  6   | // Estados explicitos para pantallas; NO un motor ni un recorrido A1-Fin aprobado.
  7   | function pedido(etapa,acciones=[],extra={}) {
  8   |  const d=structuredClone(f.detalle);d.etapaActual=etapa;d.nombreEtapaActual=f.flujo.etapas[etapa-1].nombre;
  9   |  d.finalizado=false;d.venceEn=null;d.bodega=null;d.demoraAduana=null;d.llegadaAduanaEn=null;d.accionesDisponibles=acciones;
  10  |  d.lineaDeTiempo=d.lineaDeTiempo.map(e=>({...e,nombre:f.flujo.etapas[e.numero-1].nombre,rolResponsable:f.flujo.etapas[e.numero-1].rolResponsable,estadoPlazo:f.flujo.etapas[e.numero-1].estadoPlazo,estado:e.numero===etapa?'ACTUAL':e.numero<etapa?'COMPLETADA':'PENDIENTE',resumen:null}));
  11  |  return {...d,...extra};
  12  | }
  13  | async function entorno(page,roles,detalle,handler=async()=>false) {
  14  |  const inesperadas=[],errores=[];const usuario={...f.admin,roles};
  15  |  page.on('pageerror',e=>errores.push(e.message));
  16  |  await page.route('**/*',async route=>{
  17  |   const url=new URL(route.request().url());
  18  |   if(url.origin!=='http://127.0.0.1:43184'){inesperadas.push(url.origin);return route.abort();}
  19  |   if(!url.pathname.startsWith('/api/'))return route.continue();
  20  |   const json=body=>route.fulfill({json:body});
  21  |   if(url.pathname.endsWith('/auth/renovar'))return json({tokenAcceso:'ficticio-f4b',expiraEn:900,usuario});
  22  |   if(url.pathname.endsWith('/auth/yo'))return json({...usuario,permisos:[]});
  23  |   if(await handler(route,url,json))return;
  24  |   if(url.pathname===`/api/v1/pedidos/${id}`)return json(detalle());
  25  |   if(url.pathname.endsWith('/historial'))return json([]);
  26  |   if(/\/etapas\/\d+\/archivos$/.test(url.pathname))return json([]);
  27  |   if(url.pathname==='/api/v1/flujos/EC/A')return json(f.flujo);
  28  |   inesperadas.push(url.pathname);return route.fulfill({status:500,json:{}});
  29  |  });
  30  |  return ()=>{expect(inesperadas).toEqual([]);expect(errores).toEqual([]);};
  31  | }
  32  | for(const [roles,etapas] of [[['compras_china'],[5,6,7]],[['logistica'],[8,9,10]],[['consulta','compras_china','logistica'],[5,6,7,8,9,10]]]) {
  33  |  test(`bandejas por roles ${roles.join('+')} · API simulada`,async({page})=>{
  34  |   const fin=await entorno(page,roles,()=>pedido(1),async(r,u,json)=>{
  35  |    if(u.pathname.endsWith('/pedidos/bandeja')){await json({elementos:etapas.map((e,i)=>({...pedido(e),id:`fixture-${i}`,numero:`FICTICIO-${i}`})),pagina:0,tamano:100,total:etapas.length});return true;}
  36  |   });
  37  |   await page.goto('/bandeja');for(const e of etapas)await expect(page.getByRole('heading',{level:2,name:new RegExp(`^A${e} ·`)})).toBeVisible();
  38  |   await expect(page.getByRole('heading',{level:2})).toHaveCount(etapas.length);fin();
  39  |  });
  40  | }
  41  | for(const etapa of [6,9,10])test(`bloqueo documental real visible A${etapa}`,async({page})=>{
  42  |  let envios=0;const fin=await entorno(page,['admin'],()=>pedido(etapa,[accion({etapaDestino:etapa+1,requisitosDocumentales:'PENDIENTES',puedeCompletarse:false})]),async(r,u)=>{if(u.pathname.endsWith('/acciones')){envios++;await r.fulfill({status:500,json:{}});return true;}});
  43  |  await page.goto(`/pedidos/${id}`);await expect(page.getByText('Documentos pendientes de definición',{exact:true})).toBeVisible();
  44  |  await expect(page.getByRole('button',{name:'Aprobar',exact:true})).toBeDisabled();expect(envios).toBe(0);fin();
  45  | });
  46  | test('demora envia ISO y muestra estimacion confirmada, sin avanzar localmente',async({page})=>{
  47  |  let actual=pedido(7,[accion({accion:'REPORTAR_DEMORA'})]),cuerpos=[];
  48  |  const fin=await entorno(page,['compras_china'],()=>actual,async(r,u,json)=>{
  49  |   if(u.pathname.endsWith('/acciones')){cuerpos.push(r.request().postDataJSON());actual=pedido(7,[accion({accion:'REPORTAR_DEMORA'})],{demoraAduana:{reportadaPor:f.admin,reportadaEn:'2026-10-16T22:00:00Z',motivo:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-19'}});await json(actual);return true;}
  50  |  });
  51  |  await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Reportar demora'}).click();const d=page.getByRole('dialog');
  52  |  await d.getByRole('button',{name:'Reportar demora'}).click();expect(cuerpos).toHaveLength(0);
  53  |  await d.getByLabel('Observación').fill('Demora ficticia');await d.getByLabel('Nueva fecha estimada de llegada').fill('2026-10-17');
  54  |  await d.getByRole('button',{name:'Reportar demora'}).click();await expect(d).toBeHidden();
  55  |  expect(cuerpos).toEqual([{accion:'REPORTAR_DEMORA',etapaEsperada:7,observacion:'Demora ficticia',nuevaFechaEstimadaLlegada:'2026-10-17',adjuntoIds:[]}]);
  56  |  await expect(page.getByText('Última demora · llegada estimada 19/10/2026')).toBeVisible();await expect(page.getByRole('button',{name:/^A7 .*, etapa actual$/})).toBeVisible();fin();
  57  | });
  58  | test('llegada usa la marca y conserva solo la fecha del servidor',async({page})=>{
  59  |  let actual=pedido(7,[accion({etapaDestino:8})]);const fin=await entorno(page,['compras_china'],()=>actual,async(r,u,json)=>{
  60  |   if(u.pathname.endsWith('/acciones')){expect(r.request().postDataJSON()).toEqual({accion:'APROBAR',etapaEsperada:7,adjuntoIds:[]});actual=pedido(8,[],{llegadaAduanaEn:'2026-10-16T22:00:00Z'});await json(actual);return true;}
  61  |  });
  62  |  await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Confirmar llegada'}).click();await page.getByRole('dialog').getByRole('button',{name:'Confirmar llegada'}).click();
  63  |  await expect(page.getByRole('dialog')).toBeHidden();await expect(page.getByRole('button',{name:/^A8 .*, etapa actual$/})).toBeVisible();await expect(page.getByText('Sin llegada confirmada')).toHaveCount(0);fin();
  64  | });
  65  | test('fixture A9 definido selecciona bodega y actualiza detalle tras API',async({page})=>{
  66  |  let actual=pedido(9,[accion({etapaDestino:10,requiereBodega:true})]),cuerpo;
  67  |  const fin=await entorno(page,['logistica'],()=>actual,async(r,u,json)=>{
  68  |   if(u.pathname==='/api/v1/bodegas'){expect(u.searchParams.get('activa')).toBe('true');await json([f.bodega]);return true;}
  69  |   if(u.pathname.endsWith('/acciones')){cuerpo=r.request().postDataJSON();actual=pedido(10,[],{bodega:f.bodega});await json(actual);return true;}
  70  |  });
  71  |  await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Aprobar'}).click();const d=page.getByRole('dialog');await expect(d.getByRole('button',{name:'Aprobar'})).toBeDisabled();
  72  |  await d.getByRole('combobox',{name:'Bodega de destino'}).click();await page.getByRole('option',{name:'Bodega ficticia F4 · Ciudad ficticia'}).click();await d.getByRole('button',{name:'Aprobar'}).click();
  73  |  await expect(d).toBeHidden();expect(cuerpo).toEqual({accion:'APROBAR',etapaEsperada:9,bodegaId:f.bodega.id,adjuntoIds:[]});await expect(page.getByText('Bodega ficticia F4 · Ciudad ficticia')).toBeVisible();fin();
  74  | });
  75  | test('conflicto de etapa no reenvia y relee el estado',async({page})=>{
  76  |  let actual=pedido(8,[accion({etapaDestino:9})]),envios=0;const fin=await entorno(page,['logistica'],()=>actual,async(r,u)=>{
  77  |   if(u.pathname.endsWith('/acciones')){envios++;actual=pedido(9);await r.fulfill({status:409,contentType:'application/problem+json',json:{status:409,title:'Cambio ficticio',codigo:'ETAPA_CAMBIO'}});return true;}
  78  |  });
  79  |  await page.goto(`/pedidos/${id}`);await page.getByRole('button',{name:'Aprobar'}).click();await page.getByRole('dialog').getByRole('button',{name:'Aprobar'}).click();
  80  |  await expect(page.getByText('Otro usuario actualizó este pedido')).toBeVisible();await page.getByRole('button',{name:'Ver estado actual'}).click();await expect(page.getByRole('button',{name:/^A9 .*, etapa actual$/})).toBeVisible();expect(envios).toBe(1);fin();
  81  | });
  82  | test('catalogo guarda version y conflicto conserva borrador sin reenviar',async({page})=>{
  83  |  let actual=f.bodega,envios=0,lecturas=0;const fin=await entorno(page,['consulta','logistica'],()=>pedido(1),async(r,u,json)=>{
  84  |   if(u.pathname==='/api/v1/bodegas'){lecturas++;await json([actual]);return true;}
  85  |   if(u.pathname===`/api/v1/bodegas/${actual.id}`){envios++;expect(r.request().postDataJSON()).toMatchObject({nombre:'Mi borrador',version:0});actual={...f.bodega,nombre:'Cambio ajeno',version:1};await r.fulfill({status:409,contentType:'application/problem+json',json:{status:409,title:'Cambio ficticio',codigo:'VERSION_DESACTUALIZADA'}});return true;}
  86  |  });
  87  |  await page.goto('/bodegas');await page.getByRole('button',{name:'Editar bodega Bodega ficticia F4'}).click();const d=page.getByRole('dialog');await d.getByLabel('Nombre',{exact:true}).fill('Mi borrador');await d.getByRole('button',{name:'Guardar bodega'}).click();
  88  |  await expect(d.getByText(/Otra persona modificó esta bodega/)).toBeVisible();await expect(d.getByLabel('Nombre',{exact:true})).toHaveValue('Mi borrador');await expect(d.getByRole('button',{name:'Guardar bodega'})).toBeDisabled();expect(envios).toBe(1);expect(lecturas).toBeGreaterThan(1);
> 89  |  await d.getByRole('button',{name:'Cerrar y revisar versión actual'}).click();await expect(page.getByRole('cell',{name:'Cambio ajeno'})).toBeVisible();fin();
      |                                                                                                                                          ^ Error: expect(locator).toBeVisible() failed
  90  | });
  91  | test('solo consulta no accede a catalogo ni dispara su API',async({page})=>{
  92  |  let lecturas=0;const fin=await entorno(page,['consulta'],()=>pedido(1),async(r,u,json)=>{if(u.pathname==='/api/v1/bodegas'){lecturas++;await json([]);return true;}});
  93  |  await page.goto('/bodegas');await expect(page.getByRole('heading',{name:'Sin permiso'})).toBeVisible();await expect(page.getByRole('link',{name:'Bodegas'})).toHaveCount(0);expect(lecturas).toBe(0);fin();
  94  | });
  95  | test('Fin confirmado por API muestra Sistema y resumen sin acciones manuales',async({page})=>{
  96  |  const actual=pedido(11,[],{finalizado:true});actual.lineaDeTiempo[10].estado='COMPLETADA';actual.lineaDeTiempo[9].resumen='Resumen ficticio del servidor';
  97  |  const fin=await entorno(page,['admin'],()=>actual,async(r,u,json)=>{if(u.pathname.endsWith('/historial')){await json([{id:'fin-ficticio',pedidoId:id,etapa:11,tipo:'PEDIDO_FINALIZADO',usuario:null,rolIntervencion:null,fecha:'2026-10-16T22:00:00Z',detalle:null,adjuntoIds:[]}]);return true;}});
  98  |  await page.goto(`/pedidos/${id}`);await expect(page.getByText('Sistema',{exact:true})).toBeVisible();await expect(page.getByText('Pedido finalizado por el servidor. Fin es automático y no admite acciones manuales.')).toBeVisible();
  99  |  await expect(page.getByRole('button',{name:/^(Aprobar|Enviar|Reasignar|Confirmar llegada|Subir archivo)$/})).toHaveCount(0);await page.getByRole('button',{name:/^A10 /}).click();await expect(page.getByText('Resumen ficticio del servidor')).toBeVisible();fin();
  100 | });
  101 | 
```