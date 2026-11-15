// Lector mínimo de hojas de cálculo para ver Excel (.xlsx) y CSV dentro de DECOKASA, sin dependencias externas. Usa lo que
// trae el navegador: un .xlsx es un ZIP con XML, que se abre con DecompressionStream y DOMParser. Solo lee valores
// (no ejecuta fórmulas ni macros) y limita filas y columnas para no bloquear la pantalla con archivos enormes.

export interface Hoja {
  nombre: string;
  filas: string[][];
  /** Había más filas o columnas que las mostradas. */
  recortada: boolean;
}

export const MAX_FILAS = 1000;
export const MAX_COLUMNAS = 60;

/** Entradas de un ZIP leídas desde el directorio central. */
async function leerZip(bytes: ArrayBuffer): Promise<Map<string, Uint8Array>> {
  const vista = new DataView(bytes);
  // Fin del directorio central: firma 0x06054b50, buscada desde el final.
  let fin = -1;
  for (let i = bytes.byteLength - 22; i >= Math.max(0, bytes.byteLength - 65557); i--) {
    if (vista.getUint32(i, true) === 0x06054b50) { fin = i; break; }
  }
  if (fin < 0) throw new Error('El archivo no es un Excel válido.');
  const total = vista.getUint16(fin + 10, true);
  let posicion = vista.getUint32(fin + 16, true);
  const entradas = new Map<string, Uint8Array>();
  const decodificador = new TextDecoder();
  for (let n = 0; n < total; n++) {
    if (vista.getUint32(posicion, true) !== 0x02014b50) throw new Error('El archivo no es un Excel válido.');
    const metodo = vista.getUint16(posicion + 10, true);
    const comprimido = vista.getUint32(posicion + 20, true);
    const largoNombre = vista.getUint16(posicion + 28, true);
    const largoExtra = vista.getUint16(posicion + 30, true);
    const largoComentario = vista.getUint16(posicion + 32, true);
    const local = vista.getUint32(posicion + 42, true);
    const nombre = decodificador.decode(new Uint8Array(bytes, posicion + 46, largoNombre));
    posicion += 46 + largoNombre + largoExtra + largoComentario;
    if (!nombre.endsWith('.xml') && !nombre.endsWith('.rels')) continue;
    const inicio = local + 30 + vista.getUint16(local + 26, true) + vista.getUint16(local + 28, true);
    const datos = new Uint8Array(bytes, inicio, comprimido);
    if (metodo === 0) {
      entradas.set(nombre, datos);
    } else if (metodo === 8) {
      const flujo = new Blob([datos]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
      entradas.set(nombre, new Uint8Array(await new Response(flujo).arrayBuffer()));
    }
  }
  return entradas;
}

function xml(entradas: Map<string, Uint8Array>, nombre: string): Document | null {
  const datos = entradas.get(nombre);
  return datos ? new DOMParser().parseFromString(new TextDecoder().decode(datos), 'application/xml') : null;
}

/** Elementos por nombre local, sin depender del prefijo del espacio de nombres. */
function hijos(nodo: Document | Element, nombre: string): Element[] {
  return Array.from(nodo.getElementsByTagNameNS('*', nombre));
}

/** "AB12" → índice de columna 27. */
function columna(referencia: string): number {
  let n = 0;
  for (const letra of referencia.replace(/[0-9]/g, '')) n = n * 26 + (letra.charCodeAt(0) - 64);
  return n - 1;
}

export async function leerXlsx(bytes: ArrayBuffer): Promise<Hoja[]> {
  const entradas = await leerZip(bytes);
  const compartidos = (() => {
    const documento = xml(entradas, 'xl/sharedStrings.xml');
    return documento ? hijos(documento, 'si').map((si) => hijos(si, 't').map((t) => t.textContent ?? '').join('')) : [];
  })();
  const libro = xml(entradas, 'xl/workbook.xml');
  const relaciones = xml(entradas, 'xl/_rels/workbook.xml.rels');
  if (!libro) throw new Error('El archivo no es un Excel válido.');
  const destinos = new Map(relaciones ? hijos(relaciones, 'Relationship').map((r) => [r.getAttribute('Id'), r.getAttribute('Target')]) : []);
  return hijos(libro, 'sheet').map((hoja, indice) => {
    const relacion = hoja.getAttribute('r:id') ?? hoja.getAttributeNS('http://schemas.openxmlformats.org/officeDocument/2006/relationships', 'id');
    const destino = (relacion && destinos.get(relacion)) || `worksheets/sheet${indice + 1}.xml`;
    const ruta = destino.startsWith('/') ? destino.slice(1) : `xl/${destino.replace(/^\.\//, '')}`;
    const documento = xml(entradas, ruta);
    const filas: string[][] = [];
    let recortada = false;
    if (documento) {
      for (const fila of hijos(documento, 'row')) {
        const numero = Number(fila.getAttribute('r') ?? filas.length + 1) - 1;
        if (numero >= MAX_FILAS) { recortada = true; break; }
        const celdas: string[] = [];
        for (const celda of hijos(fila, 'c')) {
          const indiceColumna = columna(celda.getAttribute('r') ?? 'A1');
          if (indiceColumna >= MAX_COLUMNAS) { recortada = true; continue; }
          const tipo = celda.getAttribute('t');
          const valor = hijos(celda, 'v')[0]?.textContent ?? '';
          celdas[indiceColumna] = tipo === 's' ? (compartidos[Number(valor)] ?? '')
            : tipo === 'inlineStr' ? hijos(celda, 't').map((t) => t.textContent ?? '').join('')
              : tipo === 'b' ? (valor === '1' ? 'VERDADERO' : 'FALSO') : valor;
        }
        filas[numero] = Array.from(celdas, (c) => c ?? '');
      }
    }
    return { nombre: hoja.getAttribute('name') ?? `Hoja ${indice + 1}`, filas: Array.from(filas, (f) => f ?? []), recortada };
  });
}

/** CSV con comillas, separador coma o punto y coma (Excel en español usa ';'). */
export function leerCsv(texto: string): Hoja[] {
  const primera = texto.split(/\r?\n/, 1)[0] ?? '';
  const separador = (primera.match(/;/g)?.length ?? 0) > (primera.match(/,/g)?.length ?? 0) ? ';' : ',';
  const filas: string[][] = [];
  let fila: string[] = [];
  let campo = '';
  let entreComillas = false;
  let recortada = false;
  for (let i = 0; i < texto.length; i++) {
    const c = texto[i];
    if (entreComillas) {
      if (c === '"' && texto[i + 1] === '"') { campo += '"'; i++; }
      else if (c === '"') entreComillas = false;
      else campo += c;
    } else if (c === '"') entreComillas = true;
    else if (c === separador) { fila.push(campo); campo = ''; }
    else if (c === '\n' || c === '\r') {
      if (c === '\r' && texto[i + 1] === '\n') i++;
      fila.push(campo); filas.push(fila.slice(0, MAX_COLUMNAS)); fila = []; campo = '';
      if (filas.length >= MAX_FILAS) { recortada = true; break; }
    } else campo += c;
  }
  if (!recortada && (campo || fila.length)) { fila.push(campo); filas.push(fila.slice(0, MAX_COLUMNAS)); }
  return [{ nombre: 'CSV', filas, recortada }];
}
