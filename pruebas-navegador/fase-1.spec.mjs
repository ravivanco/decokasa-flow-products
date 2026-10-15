import { pathToFileURL } from 'node:url';
import { readFile } from 'node:fs/promises';
const { test, expect } = await import(process.env.PLAYWRIGHT_TEST_MODULE
  ? pathToFileURL(process.env.PLAYWRIGHT_TEST_MODULE).href : 'playwright/test');
const usuarios = JSON.parse(await readFile(new URL('../backend/semilla/usuarios.ejemplo.json', import.meta.url), 'utf8'));
const CLAVE = process.env.B2_CLAVE_FICTICIA || 'Clave-Prueba-1';
const origen = process.env.B2_BASE_URL || 'http://localhost:3000';
const api = '/api/v1';
const titulos = ['Administración', 'Codificación', 'Revisión', 'Compras China', 'Logística', 'Consulta'];

async function entrar(page, usuario) {
  await page.goto('/login');
  await page.getByLabel('Correo, usuario o teléfono').fill(usuario);
  await page.getByLabel('Contraseña', { exact: true }).fill(CLAVE);
  const respuesta = page.waitForResponse(r => r.url().endsWith('/auth/login') && r.request().method() === 'POST');
  await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
  expect((await respuesta).status()).toBe(200);
  await expect(page.getByRole('heading', { level: 1, name: /^Hola,/ })).toBeVisible();
}

test.beforeEach(async ({ context }) => {
  await context.addInitScript(() => {
    window.__violacionesCsp = [];
    document.addEventListener('securitypolicyviolation', e => {
      window.__violacionesCsp.push({ directiva: e.violatedDirective, recurso: e.blockedURI });
    });
  });
});

for (const [indice, usuario] of usuarios.entries()) {
  test(`API real: dashboard de ${usuario.usuario}, cookie, recarga y cierre`, async ({ page, context }) => {
    const errores = [];
    page.on('pageerror', e => errores.push(e.message));
    const documento = await page.goto('/login');
    expect(documento.headers()['content-security-policy']).toContain("script-src 'self'");
    await expect(page.getByLabel('Correo, usuario o teléfono')).toBeVisible();
    await expect(page.getByText(/Demo2026|Acceso rápido|Simulador de Roles|Acceso local/)).toHaveCount(0);
    await entrar(page, usuario.usuario);
    await expect(page.getByRole('heading', { name: `Hola, ${usuario.nombre}`, exact: true })).toBeVisible();
    await expect(page.getByRole('heading', { level: 2, name: titulos[indice], exact: true })).toBeVisible();
    if (indice === 5) {
      await expect(page.getByText('Provisional', { exact: true })).toHaveCount(1);
      await expect(page.getByText(/Alcance provisional, pendiente de confirmar con el cliente/)).toBeVisible();
    }
    const cookie = (await context.cookies()).find(c => c.name === 'decokasa_renovacion');
    expect(cookie).toMatchObject({ httpOnly: true, secure: true, sameSite: 'Strict', path: '/api/v1/auth' });
    expect(await page.evaluate(() => document.cookie)).not.toContain('decokasa_renovacion');
    expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
    const renovar = page.waitForResponse(r => r.url().endsWith('/auth/renovar'));
    await page.reload();
    expect((await renovar).status()).toBe(200);
    await expect(page.getByRole('heading', { name: `Hola, ${usuario.nombre}`, exact: true })).toBeVisible();
    const nueva = (await context.cookies()).find(c => c.name === cookie.name);
    expect(nueva.value).not.toBe(cookie.value);
    if (indice !== 0) {
      await page.goto('/usuarios');
      await expect(page.getByRole('heading', { name: 'Sin permiso', exact: true })).toBeVisible();
      await expect(page.getByRole('link', { name: 'Roles', exact: true })).toHaveCount(0);
    }
    expect(await page.evaluate(() => window.__violacionesCsp)).toEqual([]);
    expect(errores).toEqual([]);
    await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await expect(page.getByLabel('Correo, usuario o teléfono')).toBeVisible();
    expect((await context.cookies()).filter(c => c.name === cookie.name)).toHaveLength(0);
    await page.reload();
    await expect(page.getByLabel('Correo, usuario o teléfono')).toBeVisible();
  });
}

test('API real: dos pestañas renuevan sin reutilizar cookies y el cierre se propaga', async ({ page, context }) => {
  await entrar(page, 'adminprueba');
  const segunda = await context.newPage();
  await segunda.goto('/usuarios');
  await expect(segunda.getByRole('heading', { name: 'Usuarios', exact: true })).toBeVisible();
  const resultados = [];
  for (const p of [page, segunda]) p.on('response', r => {
    if (r.url().endsWith('/auth/renovar')) resultados.push(r.status());
  });
  await Promise.all([page.reload(), segunda.reload()]);
  await expect(page.getByRole('heading', { name: 'Hola, Admin Prueba', exact: true })).toBeVisible();
  await expect(segunda.getByRole('heading', { name: 'Usuarios', exact: true })).toBeVisible();
  expect(resultados).toEqual([200, 200]);
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await expect(segunda.getByLabel('Correo, usuario o teléfono')).toBeVisible();
});

test('API real: dos administradores resuelven un conflicto, recargan y reaplican', async ({ page, browser }) => {
  await entrar(page, 'adminprueba');
  const sesion = await page.evaluate(async () => (await fetch('/api/v1/auth/renovar', { method: 'POST' })).json());
  const sufijo = Date.now().toString();
  const cabeceras = { Authorization: `Bearer ${sesion.tokenAcceso}` };
  const creados = [];
  const crear = async (roles, etiqueta) => {
    const r = await page.request.post(`${api}/usuarios`, { headers: cabeceras, data: {
      nombre: `${etiqueta} B2 ${sufijo}`, correo: `b2${etiqueta}${sufijo}@ejemplo.test`,
      usuario: `b2${etiqueta}${sufijo}`, telefono: `+59398${sufijo.slice(-8)}${creados.length}`,
      contrasenaInicial: CLAVE, roles, paises: ['EC'],
    } });
    expect(r.status()).toBe(201);
    const u = await r.json(); creados.push(u); return u;
  };
  const admin2 = await crear(['admin', 'logistica'], 'admin');
  const objetivo = await crear(['marketing'], 'objetivo');
  const otro = await browser.newContext({ baseURL: origen });
  const p2 = await otro.newPage();
  try {
    await entrar(p2, admin2.usuario);
    await expect(p2.getByRole('heading', { level: 2, name: 'Logística', exact: true })).toBeVisible();
    await Promise.all([page.goto('/usuarios'), p2.goto('/usuarios')]);
    for (const p of [page, p2]) {
      await p.getByRole('button', { name: `Editar a ${objetivo.nombre}`, exact: true }).click();
    }
    const d1 = page.getByRole('dialog'); const d2 = p2.getByRole('dialog');
    await d1.getByLabel('Nombre', { exact: true }).fill(`Remoto B2 ${sufijo}`);
    await d1.getByRole('button', { name: 'Guardar', exact: true }).click();
    await expect(d1).toBeHidden();
    await d2.getByLabel('Nombre', { exact: true }).fill(`Propio B2 ${sufijo}`);
    const conflicto = p2.waitForResponse(r => r.request().method() === 'PATCH' && r.status() === 409);
    await d2.getByRole('button', { name: 'Guardar', exact: true }).click();
    expect((await conflicto).status()).toBe(409);
    await d2.getByRole('button', { name: 'Cargar datos actuales', exact: true }).click();
    await expect(d2.getByLabel('Nombre', { exact: true })).toHaveValue(`Remoto B2 ${sufijo}`);
    // Segundo conflicto, ahora se elige reaplicar y guardar deliberadamente.
    await page.getByRole('button', { name: `Editar a Remoto B2 ${sufijo}`, exact: true }).click();
    await d1.getByLabel('Nombre', { exact: true }).fill(`Remoto dos B2 ${sufijo}`);
    await d1.getByRole('button', { name: 'Guardar', exact: true }).click();
    await expect(d1).toBeHidden();
    await d2.getByLabel('Nombre', { exact: true }).fill(`Propio B2 ${sufijo}`);
    await d2.getByRole('button', { name: 'Guardar', exact: true }).click();
    await d2.getByRole('button', { name: 'Reaplicar mis cambios', exact: true }).click();
    await expect(d2.getByRole('alert')).toContainText('Otra persona también cambió el nombre');
    await expect(d2.getByLabel('Nombre', { exact: true })).toHaveValue(`Propio B2 ${sufijo}`);
    await d2.getByRole('button', { name: 'Guardar', exact: true }).click();
    await expect(d2).toBeHidden();
    expect(await page.evaluate(() => window.__violacionesCsp)).toEqual([]);
  } finally {
    await otro.close();
    // Solo se desactivan las dos cuentas ficticias creadas por esta prueba; no se borran filas ni datos previos.
    for (const u of creados.reverse()) {
      const actual = await page.request.get(`${api}/usuarios/${u.id}`, { headers: cabeceras });
      const datos = await actual.json();
      const r = await page.request.patch(`${api}/usuarios/${u.id}`, { headers: cabeceras,
        data: { version: datos.version, activo: false } });
      expect(r.status()).toBe(200);
    }
  }
});

function puerta() {
  let abrir;
  const espera = new Promise(resolve => { abrir = resolve; });
  return { espera, abrir };
}

// Se retiene A primero, se observa B esperando el Web Lock y se libera A antes de retener B.
// Las dos intenciones de login se solapan; ningún paso espera al dueño del bloqueo mientras lo retiene el otro.
async function prepararCambio(page, context, usuarioB = 'marketingprueba') {
  const segunda = await context.newPage();
  await Promise.all([page.goto('/login'), segunda.goto('/login')]);
  for (const [p, usuario] of [[page, 'adminprueba'], [segunda, usuarioB]]) {
    await p.getByLabel('Correo, usuario o teléfono').fill(usuario);
    await p.getByLabel('Contraseña', { exact: true }).fill(CLAVE);
  }
  const salidaA = puerta(); const llegadaA = puerta();
  const salidaB = puerta(); const llegadaB = puerta();
  await page.route('**/auth/login', async route => {
    llegadaA.abrir(); await salidaA.espera; await route.continue();
  });
  await segunda.route('**/auth/login', async route => {
    llegadaB.abrir(); await salidaB.espera; await route.continue();
  });
  try {
    await page.getByRole('button', { name: 'Ingresar', exact: true }).click();
    await llegadaA.espera;
    await segunda.getByRole('button', { name: 'Ingresar', exact: true }).click();
    await expect.poll(() => page.evaluate(async () => {
      const locks = await navigator.locks.query();
      return locks.pending.some(l => l.name === 'decokasa-renovacion');
    })).toBe(true);
    salidaA.abrir();
    await llegadaB.espera;
    await expect(page.getByRole('banner')).toContainText('Admin Prueba');
    return { segunda, liberar: salidaB.abrir };
  } catch (error) { salidaA.abrir(); salidaB.abrir(); throw error; }
}

async function comprobarMarketing(page) {
  await expect(page.getByRole('banner')).toContainText('Marketing Prueba');
  await expect(page.getByRole('alert').filter({ hasText: 'Cambio de sesión' }))
    .toContainText('Ahora estás usando la cuenta de Marketing Prueba');
  await expect(page.getByRole('link', { name: 'Usuarios', exact: true })).toHaveCount(0);
  await expect(page.getByRole('link', { name: 'Roles', exact: true })).toHaveCount(0);
  await expect(page.getByRole('dialog')).toHaveCount(0);
  await expect(page.getByRole('row')).toHaveCount(0);
}

test('API real: login distinto en otra pestaña no debe mezclar identidades', async ({ page, context }, testInfo) => {
  const errores = [];
  page.on('pageerror', error => errores.push(error.message));
  const cambio = await prepararCambio(page, context);
  try {
    await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
    await expect(page.getByRole('row').filter({ hasText: 'admin@ejemplo.test' })).toBeVisible();
    await page.getByRole('button', { name: 'Nuevo usuario', exact: true }).click();
    await page.getByRole('dialog').getByLabel('Nombre', { exact: true }).fill('Formulario anterior B2-D01');
    const renovacion = page.waitForResponse(r => r.url().endsWith('/auth/renovar') && r.status() === 200);
    cambio.liberar();
    await comprobarMarketing(page);
    await expect(cambio.segunda.getByRole('banner')).toContainText('Marketing Prueba');
    const recuperada = await (await renovacion).json();
    const yo = await page.request.get(`${api}/auth/yo`, {
      headers: { Authorization: `Bearer ${recuperada.tokenAcceso}` },
    });
    expect(yo.status()).toBe(200);
    expect((await yo.json()).nombre).toBe('Marketing Prueba');
    const sinPermiso = await page.request.get(`${api}/usuarios`, {
      headers: { Authorization: `Bearer ${recuperada.tokenAcceso}` },
    });
    expect(sinPermiso.status()).toBe(403);
    await expect(page.getByText('Formulario anterior B2-D01')).toHaveCount(0);
    await expect(page.getByRole('heading', { name: 'Sin permiso', exact: true })).toBeVisible();
    expect(await page.evaluate(() => window.__violacionesCsp)).toEqual([]);
    expect(errores).toEqual([]);
    const antes = await page.getByRole('banner').innerText();
    await testInfo.attach('identidad-comunicada', { body: await page.screenshot(), contentType: 'image/png' });
    await page.reload();
    await expect(page.getByRole('banner')).toContainText('Marketing Prueba');
    const despues = await page.getByRole('banner').innerText();
    expect(despues).toBe(antes);
    await testInfo.attach('identidades-sin-tokens', { body: JSON.stringify({ antes, despues,
      identidadApi: (await yo.json()).id, permisoApi: sinPermiso.status(), formularioDescartado: true }),
      contentType: 'application/json' });
    await cambio.segunda.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
    await expect(page.getByLabel('Correo, usuario o teléfono')).toBeVisible();
    expect(await page.evaluate(() => [localStorage.length, sessionStorage.length])).toEqual([0, 0]);
    expect(await page.evaluate(() => document.cookie)).not.toContain('decokasa_renovacion');
  } finally { cambio.liberar(); }
});

test('API real: una lectura tardía de Admin no restaura datos tras cambiar a Marketing', async ({ page, context }, testInfo) => {
  const cambio = await prepararCambio(page, context);
  const respuestaLista = puerta(); const listaObtenida = puerta();
  let estadoServidor; let resultadoEntrega;
  await page.route('**/api/v1/usuarios', async route => {
    const respuesta = await route.fetch(); // Respuesta de PostgreSQL/API real; no se modifica el cuerpo.
    estadoServidor = respuesta.status(); listaObtenida.abrir();
    await respuestaLista.espera;
    try { await route.fulfill({ response: respuesta }); resultadoEntrega = 'entregada'; }
    catch { resultadoEntrega = 'petición cancelada por el cliente'; }
  });
  try {
    await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
    await listaObtenida.espera;
    expect(estadoServidor).toBe(200);
    cambio.liberar();
    await comprobarMarketing(page);
    respuestaLista.abrir();
    await expect.poll(() => resultadoEntrega).toBeTruthy();
    await comprobarMarketing(page);
    await page.getByRole('link', { name: 'Inicio', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Hola, Marketing Prueba', exact: true })).toBeVisible();
    await testInfo.attach('lectura-tardia-sin-datos', { body: JSON.stringify({ estadoServidor, resultadoEntrega,
      identidadFinal: 'Marketing Prueba', filasDeAdmin: 0 }), contentType: 'application/json' });
  } finally { cambio.liberar(); respuestaLista.abrir(); }
});

async function prepararRolPendiente(page, context) {
  const login = page.waitForResponse(r => r.url().endsWith('/auth/login') && r.status() === 200);
  const cambio = await prepararCambio(page, context);
  const sesionAdmin = await (await login).json();
  await page.getByRole('link', { name: 'Roles', exact: true }).click();
  await page.getByRole('button', { name: 'Nuevo rol', exact: true }).click();
  const codigo = `bdos_${Date.now().toString().replace(/\d/g, d => String.fromCharCode(97 + Number(d)))}`;
  await page.getByRole('dialog').getByLabel('Código', { exact: true }).fill(codigo);
  await page.getByRole('dialog').getByLabel('Nombre', { exact: true }).fill('Rol ficticio de respuesta tardía');
  return { ...cambio, sesionAdmin, codigo };
}

test('API real: el cambio de cuenta descarta caché caliente y las consultas usan la identidad nueva', async ({ page, context, request }, testInfo) => {
  const loginFixture = await request.post(`${api}/auth/login`, { data: { identificador: 'adminprueba', contrasena: CLAVE } });
  expect(loginFixture.status()).toBe(200);
  const fixture = await loginFixture.json();
  const headers = { Authorization: `Bearer ${fixture.tokenAcceso}` };
  const creados = [];
  const crear = async roles => {
    const sufijo = `${Date.now()}${creados.length}`;
    const r = await request.post(`${api}/usuarios`, { headers, data: {
      nombre: `Cuenta cache Bdos ${sufijo}`, correo: `cache${sufijo}@ejemplo.test`, usuario: `cache${sufijo}`,
      telefono: `+59397${sufijo.slice(-9)}`, contrasenaInicial: CLAVE, roles, paises: ['EC'],
    } });
    expect(r.status()).toBe(201);
    const u = await r.json(); creados.push(u); return u;
  };
  let cambio;
  const peticiones = [];
  page.on('request', r => {
    if (!/\/api\/v1\/(usuarios|roles)$/.test(r.url()) || r.method() !== 'GET') return;
    const token = r.headers().authorization?.replace(/^Bearer /, '');
    if (token) peticiones.push({ ruta: new URL(r.url()).pathname,
      sujeto: JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString()).sub });
  });
  try {
    const otra = await crear(['admin', 'marketing']);
    cambio = await prepararCambio(page, context, otra.usuario);
    await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Nuevo usuario', exact: true })).toBeVisible();
    await expect.poll(() => peticiones.filter(p => p.sujeto === fixture.usuario.id).length).toBeGreaterThanOrEqual(2);
    await page.getByRole('button', { name: 'Nuevo usuario', exact: true }).click();
    await page.getByRole('dialog').getByLabel('Nombre', { exact: true }).fill('Borrador que debe desaparecer');
    // Dato añadido después de calentar la caché de A: B debe volver a consultar, aunque staleTime siga vigente.
    const nuevo = await crear(['marketing']);
    cambio.liberar();
    await expect(page.getByRole('banner')).toContainText(otra.nombre);
    await expect(page.getByRole('alert').filter({ hasText: 'Cambio de sesión' })).toContainText(otra.nombre);
    await expect(page.getByRole('dialog')).toHaveCount(0);
    await expect(page.getByRole('row').filter({ hasText: nuevo.correo })).toBeVisible();
    expect(peticiones.filter(p => p.sujeto === otra.id).map(p => p.ruta))
      .toEqual(expect.arrayContaining([`${api}/usuarios`, `${api}/roles`]));
    await testInfo.attach('cache-e-identidad-sin-tokens', { body: JSON.stringify({
      cuentaVisible: otra.nombre, sujetoNuevo: otra.id,
      peticionesDeLaCuentaNueva: peticiones.filter(p => p.sujeto === otra.id),
      datoCreadoDespuesDeCalentarCacheVisible: true, formularioAnteriorDescartado: true,
    }), contentType: 'application/json' });
  } finally {
    cambio?.liberar();
    for (const u of creados.reverse()) {
      const r = await request.get(`${api}/usuarios/${u.id}`, { headers });
      expect(r.status()).toBe(200);
      const actual = await r.json();
      const cierre = await request.patch(`${api}/usuarios/${u.id}`, { headers, data: { version: actual.version, activo: false } });
      expect(cierre.status()).toBe(200);
    }
  }
});

async function retenerEscrituraReal(page) {
  const obtenida = puerta(); const liberar = puerta();
  const solicitudes = []; let estadoServidor; let entrega;
  await page.route('**/api/v1/roles', async route => {
    if (route.request().method() !== 'POST') { await route.continue(); return; }
    const token = route.request().headers().authorization?.replace(/^Bearer /, '');
    solicitudes.push(JSON.parse(Buffer.from(token.split('.')[1], 'base64url').toString()).sub);
    const respuesta = await route.fetch();
    estadoServidor = respuesta.status(); obtenida.abrir();
    await liberar.espera;
    try { await route.fulfill({ response: respuesta }); entrega = 'entregada'; }
    catch { entrega = 'petición cancelada por el cliente'; }
  });
  return { obtenida: obtenida.espera, liberar: liberar.abrir, solicitudes,
    estado: () => estadoServidor, entrega: () => entrega };
}

test('API real: una escritura tardía no restaura formulario ni caché de Admin', async ({ page, context }, testInfo) => {
  const cambio = await prepararRolPendiente(page, context);
  const escritura = await retenerEscrituraReal(page);
  try {
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar', exact: true }).click();
    await escritura.obtenida;
    expect(escritura.estado()).toBe(201);
    cambio.liberar();
    await comprobarMarketing(page);
    escritura.liberar();
    await expect.poll(escritura.entrega).toBeTruthy();
    await comprobarMarketing(page);
    await page.getByRole('link', { name: 'Inicio', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Hola, Marketing Prueba', exact: true })).toBeVisible();
    expect(escritura.solicitudes).toHaveLength(1);
    await testInfo.attach('escritura-tardia-sin-tokens', { body: JSON.stringify({ estadoServidor: escritura.estado(),
      resultadoEntrega: escritura.entrega(), envios: escritura.solicitudes.length, codigoFicticioConservado: cambio.codigo,
      formularioDescartado: true }), contentType: 'application/json' });
  } finally { cambio.liberar(); escritura.liberar(); }
});

test('API real parametrizada: una operación antigua con 401 no se reenvía como Marketing', async ({ browser }, testInfo) => {
  test.setTimeout(120_000);
  const context = await browser.newContext({ baseURL: 'http://localhost:3001' });
  const page = await context.newPage();
  let cambio; let escritura;
  try {
    cambio = await prepararRolPendiente(page, context);
    expect(cambio.sesionAdmin.expiraEn).toBe(3);
    // El rechazo es del backend real, incluida su tolerancia temporal. No se inventa un 401.
    await expect.poll(async () => (await page.request.get(`${api}/auth/yo`, {
      headers: { Authorization: `Bearer ${cambio.sesionAdmin.tokenAcceso}` },
    })).status(), { timeout: 90_000, intervals: [1000] }).toBe(401);
    escritura = await retenerEscrituraReal(page);
    await page.getByRole('dialog').getByRole('button', { name: 'Guardar', exact: true }).click();
    await escritura.obtenida;
    expect(escritura.estado()).toBe(401);
    cambio.liberar();
    await comprobarMarketing(page);
    escritura.liberar();
    await expect.poll(escritura.entrega).toBeTruthy();
    await comprobarMarketing(page);
    await page.getByRole('link', { name: 'Inicio', exact: true }).click();
    await expect(page.getByRole('heading', { name: 'Hola, Marketing Prueba', exact: true })).toBeVisible();
    await page.reload();
    await expect(page.getByRole('banner')).toContainText('Marketing Prueba');
    // Un único POST, autorizado con el token de Admin; ninguno se reenvió con la cuenta nueva.
    const sujetoAdmin = JSON.parse(Buffer.from(cambio.sesionAdmin.tokenAcceso.split('.')[1], 'base64url').toString()).sub;
    expect(escritura.solicitudes).toEqual([sujetoAdmin]);
    await testInfo.attach('operacion-anterior-sin-tokens', { body: JSON.stringify({ estadoServidor: escritura.estado(),
      resultadoEntrega: escritura.entrega(), enviosComoAdmin: 1, enviosComoMarketing: 0,
      identidadTrasRecarga: 'Marketing Prueba' }), contentType: 'application/json' });
  } finally { cambio?.liberar(); escritura?.liberar(); await context.close(); }
});

test('API real parametrizada: acceso vence, renueva y el límite absoluto obliga a entrar otra vez', async ({ browser }, testInfo) => {
  test.setTimeout(180_000);
  const contexto = await browser.newContext({ baseURL: 'http://localhost:3001' });
  const page = await contexto.newPage();
  try {
    const respuestaLogin = page.waitForResponse(r => r.url().endsWith('/auth/login'));
    await entrar(page, 'adminprueba');
    const sesion = await (await respuestaLogin).json();
    const cookieInicial = (await contexto.cookies()).find(c => c.name === 'decokasa_renovacion');
    expect(sesion.expiraEn).toBe(3);
    const comienzo = Date.now();
    // Espera por la decisión real del servidor; incluye la tolerancia temporal de su validador JWT.
    await expect.poll(async () => (await page.request.get(`${api}/auth/yo`, {
      headers: { Authorization: `Bearer ${sesion.tokenAcceso}` },
    })).status(), { timeout: 90_000, intervals: [1000] }).toBe(401);
    const segundosHasta401 = (Date.now() - comienzo) / 1000;
    const renovacion = page.waitForResponse(r => r.url().endsWith('/auth/renovar'));
    await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
    expect((await renovacion).status()).toBe(200);
    await expect(page.getByRole('heading', { name: 'Usuarios', exact: true })).toBeVisible();
    const rotada = (await contexto.cookies()).find(c => c.name === cookieInicial.name);
    expect(rotada.value).not.toBe(cookieInicial.value);
    expect(Math.abs(rotada.expires - cookieInicial.expires)).toBeLessThanOrEqual(1);
    await expect.poll(() => Date.now() / 1000, { timeout: 130_000, intervals: [1000] })
      .toBeGreaterThan(cookieInicial.expires + 1);
    const vencida = page.waitForResponse(r => r.url().endsWith('/auth/renovar'));
    await page.reload();
    expect((await vencida).status()).toBe(401);
    await expect(page.getByLabel('Correo, usuario o teléfono')).toBeVisible();
    await testInfo.attach('tiempos-sin-tokens', { body: JSON.stringify({ accesoConfigurado: 3,
      renovacionConfigurada: 120, segundosHasta401, expiracionCookieConservada: true }), contentType: 'application/json' });
  } finally { await contexto.close(); }
});
