"""Agrupa los fallos JUnit por suite y por primera línea del mensaje, para clasificar sus causas.

Uso, desde documentos: python pruebas/fase-3-evidencias/revalidacion-adr25-20261024/causas.py <subdirectorio-junit>
"""
import collections, glob, os, sys
import xml.etree.ElementTree as ET

aqui = os.path.dirname(os.path.abspath(__file__))
origen = os.path.join(aqui, sys.argv[1] if len(sys.argv) > 1 else 'build/test-results/test')
total = collections.Counter()
causas = collections.defaultdict(collections.Counter)
for ruta in sorted(glob.glob(os.path.join(origen, 'TEST-*.xml'))):
    r = ET.parse(ruta).getroot()
    suite = r.get('name').split('.')[-1]
    for k in ('tests', 'failures', 'errors', 'skipped'):
        total[k] += int(r.get(k))
    for caso in r.iter('testcase'):
        f = caso.find('failure') if caso.find('failure') is not None else caso.find('error')
        if f is None:
            continue
        texto = (f.get('message') or f.text or '').strip().splitlines()
        primera = (texto[0] if texto else f.get('type', ''))[:220]
        causas[suite][primera] += 1
print(dict(total))
for suite, c in sorted(causas.items()):
    print(f'== {suite} ({sum(c.values())})')
    for mensaje, n in c.most_common(4):
        print(f'   {n} x {mensaje}')
