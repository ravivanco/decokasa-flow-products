"""Resume los XML JUnit de esta revalidación: casos, fallos, errores y omitidos por suite.

Uso, desde documentos: python pruebas/fase-3-evidencias/revalidacion-adr25-20261024/resumir.py <subdirectorio-junit>
"""
import glob, json, os, sys
import xml.etree.ElementTree as ET

aqui = os.path.dirname(os.path.abspath(__file__))
origen = os.path.join(aqui, sys.argv[1] if len(sys.argv) > 1 else 'build/test-results/test')
filas, total = [], {'casos': 0, 'fallos': 0, 'errores': 0, 'omitidos': 0}
for ruta in sorted(glob.glob(os.path.join(origen, 'TEST-*.xml'))):
    r = ET.parse(ruta).getroot()
    fila = {'suite': r.get('name').split('.')[-1], 'casos': int(r.get('tests')), 'fallos': int(r.get('failures')),
            'errores': int(r.get('errors')), 'omitidos': int(r.get('skipped'))}
    fila['fallidos'] = [c.get('name') for c in r.iter('testcase') if c.find('failure') is not None or c.find('error') is not None]
    filas.append(fila)
    for k in total:
        total[k] += fila[k]
print(json.dumps({'origen': os.path.relpath(origen, aqui), 'total': total, 'suites': filas}, ensure_ascii=False, indent=1))
